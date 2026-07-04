package app.chimahon.shared

import com.googlecode.d2j.dex.Dex2jar
import com.googlecode.d2j.reader.MultiDexFileReader
import com.googlecode.dex2jar.tools.BaksmaliBaseDexExceptionHandler
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceFactory
import eu.kanade.tachiyomi.source.SourceRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.dongliu.apk.parser.ApkFile
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.commons.SimpleRemapper
import tachiyomi.core.platform.storage.PlatformStorageDirectories
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.Enumeration
import java.util.Properties
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.xml.parsers.DocumentBuilderFactory

internal actual class ChimahonPlatformApkExtensionManager actual constructor(
    storageDirectories: PlatformStorageDirectories,
    private val sourceRegistry: SourceRegistry,
) {
    private val extensionDirectory = Path.of(storageDirectories.filesDir.toString(), APK_EXTENSION_DIRECTORY)
    private val loadedPackages = linkedMapOf<String, LoadedPackage>()
    private val lifecycleLock = Any()
    private var lastErrors = emptyList<String>()
    private var initialized = false

    actual suspend fun reload(): List<ChimahonInstalledExtensionEntry> = withContext(Dispatchers.IO) {
        synchronized(lifecycleLock) {
            reloadLocked(reportTotalFailure = true)
        }
    }

    private fun reloadLocked(reportTotalFailure: Boolean): List<ChimahonInstalledExtensionEntry> {
        if (initialized) return installedEntries()
        Files.createDirectories(extensionDirectory)
        recoverInterruptedUpdates()
        cleanupTemporaryFiles()
        val apkPaths = Files.list(extensionDirectory).use { paths ->
            paths
                .filter { it.fileName.toString().endsWith(APK_SUFFIX, ignoreCase = true) }
                .sorted()
                .toList()
        }
        val failures = mutableListOf<String>()
        apkPaths.forEach { apkPath ->
            runCatching {
                loadPersistedPackage(apkPath)
            }.onSuccess {
                Files.deleteIfExists(errorPath(apkPath))
            }.onFailure { error ->
                val message = "${apkPath.fileName}: ${error.userMessage()}"
                failures += message
                writeErrorReport(apkPath, message)
            }
        }
        initialized = true
        lastErrors = failures
        if (reportTotalFailure && apkPaths.isNotEmpty() && loadedPackages.isEmpty() && failures.isNotEmpty()) {
            error(
                buildString {
                    append("No installed APK extensions could be loaded. ")
                    append(failures.take(MAX_REPORTED_ERRORS).joinToString(" | "))
                    if (failures.size > MAX_REPORTED_ERRORS) {
                        append(" | ${failures.size - MAX_REPORTED_ERRORS} more failure(s)")
                    }
                }
            )
        }
        return installedEntries()
    }

    actual suspend fun install(
        extension: ChimahonRepoExtensionEntry,
        apkBytes: ByteArray,
    ): ChimahonInstalledExtensionEntry = withContext(Dispatchers.IO) {
        synchronized(lifecycleLock) {
            installLocked(extension, apkBytes)
        }
    }

    private fun installLocked(
        extension: ChimahonRepoExtensionEntry,
        apkBytes: ByteArray,
    ): ChimahonInstalledExtensionEntry {
        requireValidPackageId(extension.id)
        require(apkBytes.isNotEmpty()) { "Downloaded APK is empty." }
        Files.createDirectories(extensionDirectory)
        if (!initialized) {
            reloadLocked(reportTotalFailure = false)
        }
        val fileStem = extension.id.toSafeFileName()
        val temporaryApk = Files.createTempFile(extensionDirectory, "$fileStem-", ".download")
        val temporaryJar = Files.createTempFile(extensionDirectory, "$fileStem-", ".jar")
        val temporaryMetadata = Files.createTempFile(extensionDirectory, "$fileStem-", ".properties")
        val finalApk = extensionDirectory.resolve("$fileStem$APK_SUFFIX")
        val finalJar = extensionDirectory.resolve("$fileStem$JAR_SUFFIX")
        val finalMetadata = extensionDirectory.resolve("$fileStem$METADATA_SUFFIX")
        val backupApk = finalApk.resolveSibling("${finalApk.fileName}$BACKUP_SUFFIX")
        val backupJar = finalJar.resolveSibling("${finalJar.fileName}$BACKUP_SUFFIX")
        val backupMetadata = finalMetadata.resolveSibling("${finalMetadata.fileName}$BACKUP_SUFFIX")
        val oldLoaded = loadedPackages[extension.id]
        try {
            Files.write(temporaryApk, apkBytes)
            val manifest = parseManifest(temporaryApk)
            require(manifest.packageName == extension.id) {
                "Downloaded package ${manifest.packageName} does not match ${extension.id}."
            }
            require(manifest.isTachiyomiExtension) {
                "${extension.name} is not a Tachiyomi/Mihon extension package."
            }
            validateLibraryVersion(manifest)
            require(manifest.signerFingerprints.isNotEmpty()) {
                "${extension.name} is not signed and cannot be installed safely."
            }
            validateUpdate(finalApk, finalMetadata, manifest)

            convertApk(temporaryApk, temporaryJar)
            val candidate = loadPackage(
                packageId = extension.id,
                name = extension.name,
                version = manifest.versionName,
                manifest = manifest,
                jarPath = temporaryJar,
            )
            try {
                validateCandidate(candidate, extension.id)
            } finally {
                candidate.classLoader.close()
            }

            val metadata = StoredPackageMetadata(
                packageId = extension.id,
                name = extension.name,
                version = manifest.versionName,
                versionCode = manifest.versionCode,
                apkSha256 = apkBytes.sha256(),
                signerFingerprints = manifest.signerFingerprints,
                compatibilityVersion = DESKTOP_COMPATIBILITY_VERSION,
                repoBaseUrl = extension.repoBaseUrl,
                artifactUrl = extension.artifactUrl,
            )
            writeMetadata(temporaryMetadata, metadata)

            removeBackup(backupJar)
            removeBackup(backupMetadata)
            removeBackup(backupApk)
            deactivatePackage(extension.id)
            try {
                backup(finalApk, backupApk)
                backup(finalJar, backupJar)
                backup(finalMetadata, backupMetadata)
                moveReplacing(temporaryApk, finalApk)
                moveReplacing(temporaryJar, finalJar)
                moveReplacing(temporaryMetadata, finalMetadata)
                val installed = activatePackage(
                    packageId = extension.id,
                    name = extension.name,
                    version = manifest.versionName,
                    manifest = manifest,
                    jarPath = finalJar,
                ).entry
                removeBackup(backupJar)
                removeBackup(backupMetadata)
                removeBackup(backupApk)
                Files.deleteIfExists(errorPath(finalApk))
                lastErrors = emptyList()
                initialized = true
                return installed
            } catch (commitError: Throwable) {
                deactivatePackage(extension.id)
                restoreBackup(finalApk, backupApk)
                restoreBackup(finalJar, backupJar)
                restoreBackup(finalMetadata, backupMetadata)
                if (oldLoaded != null && Files.exists(finalApk) && Files.exists(finalJar)) {
                    runCatching {
                        val restoredManifest = parseManifest(finalApk)
                        activatePackage(
                            packageId = oldLoaded.entry.id,
                            name = oldLoaded.entry.name,
                            version = oldLoaded.entry.version,
                            manifest = restoredManifest,
                            jarPath = finalJar,
                        )
                    }.onFailure(commitError::addSuppressed)
                }
                throw commitError
            }
        } catch (error: Throwable) {
            val message = "Could not install ${extension.name}: ${error.userMessage()}"
            lastErrors = listOf(message)
            throw IllegalStateException(message, error)
        } finally {
            Files.deleteIfExists(temporaryApk)
            Files.deleteIfExists(temporaryJar)
            Files.deleteIfExists(temporaryMetadata)
            Files.deleteIfExists(temporaryJar.resolveSibling("${temporaryJar.fileName}.errors.txt"))
        }
    }

    actual suspend fun uninstall(packageId: String): Boolean = withContext(Dispatchers.IO) {
        synchronized(lifecycleLock) {
            requireValidPackageId(packageId)
            Files.createDirectories(extensionDirectory)
            val fileStem = packageId.toSafeFileName()
            val apkPath = extensionDirectory.resolve("$fileStem$APK_SUFFIX")
            val jarPath = extensionDirectory.resolve("$fileStem$JAR_SUFFIX")
            val metadataPath = extensionDirectory.resolve("$fileStem$METADATA_SUFFIX")
            val existed = loadedPackages.containsKey(packageId) ||
                Files.exists(apkPath) ||
                Files.exists(jarPath) ||
                Files.exists(metadataPath)
            deactivatePackage(packageId)
            listOf(
                apkPath,
                jarPath,
                metadataPath,
                errorPath(apkPath),
                apkPath.resolveSibling("${apkPath.fileName}$BACKUP_SUFFIX"),
                jarPath.resolveSibling("${jarPath.fileName}$BACKUP_SUFFIX"),
                metadataPath.resolveSibling("${metadataPath.fileName}$BACKUP_SUFFIX"),
            ).forEach(Files::deleteIfExists)
            lastErrors = lastErrors.filterNot { it.contains(packageId) || it.contains(fileStem) }
            existed
        }
    }

    actual fun status(): ChimahonApkExtensionManagerStatus = synchronized(lifecycleLock) {
        ChimahonApkExtensionManagerStatus(
            isSupported = true,
            installedExtensionCount = loadedPackages.size,
            registeredSourceCount = loadedPackages.values.sumOf { it.catalogueSourceCount },
            errors = lastErrors,
        )
    }

    actual fun close() {
        synchronized(lifecycleLock) {
            loadedPackages.keys.toList().forEach(::deactivatePackage)
            initialized = false
        }
    }

    private fun loadPersistedPackage(apkPath: Path): LoadedPackage {
        val fileStem = apkPath.fileName.toString().removeSuffix(APK_SUFFIX)
        val jarPath = apkPath.resolveSibling("$fileStem$JAR_SUFFIX")
        val metadataPath = apkPath.resolveSibling("$fileStem$METADATA_SUFFIX")
        val manifest = parseManifest(apkPath)
        require(manifest.isTachiyomiExtension) {
            "${manifest.packageName} is not a Tachiyomi/Mihon extension package."
        }
        validateLibraryVersion(manifest)
        require(manifest.signerFingerprints.isNotEmpty()) {
            "${manifest.displayName} is not signed and cannot be loaded safely."
        }
        val apkHash = Files.readAllBytes(apkPath).sha256()
        val stored = readMetadata(metadataPath)
            ?.takeIf { it.packageId == manifest.packageName }
        val displayName = stored
            ?.name
            ?.takeIf(String::isNotBlank)
            ?: manifest.displayName
        val shouldConvert = Files.notExists(jarPath) ||
            stored?.apkSha256 != apkHash ||
            stored.compatibilityVersion != DESKTOP_COMPATIBILITY_VERSION
        if (shouldConvert) {
            val temporaryJar = Files.createTempFile(extensionDirectory, "$fileStem-", ".jar")
            try {
                convertApk(apkPath, temporaryJar)
                val candidate = loadPackage(
                    packageId = manifest.packageName,
                    name = displayName,
                    version = manifest.versionName,
                    manifest = manifest,
                    jarPath = temporaryJar,
                )
                try {
                    validateCandidate(candidate, manifest.packageName)
                } finally {
                    candidate.classLoader.close()
                }
                moveReplacing(temporaryJar, jarPath)
            } finally {
                Files.deleteIfExists(temporaryJar)
                Files.deleteIfExists(temporaryJar.resolveSibling("${temporaryJar.fileName}.errors.txt"))
            }
        }
        writeMetadataAtomically(
            metadataPath,
            StoredPackageMetadata(
                packageId = manifest.packageName,
                name = displayName,
                version = manifest.versionName,
                versionCode = manifest.versionCode,
                apkSha256 = apkHash,
                signerFingerprints = manifest.signerFingerprints,
                compatibilityVersion = DESKTOP_COMPATIBILITY_VERSION,
                repoBaseUrl = stored?.repoBaseUrl.orEmpty(),
                artifactUrl = stored?.artifactUrl.orEmpty(),
            ),
        )
        return activatePackage(
            packageId = manifest.packageName,
            name = displayName,
            version = manifest.versionName,
            manifest = manifest,
            jarPath = jarPath,
        )
    }

    private fun activatePackage(
        packageId: String,
        name: String,
        version: String,
        manifest: ApkManifest,
        jarPath: Path,
    ): LoadedPackage {
        val candidate = loadPackage(
            packageId = packageId,
            name = name,
            version = version,
            manifest = manifest,
            jarPath = jarPath,
        )
        try {
            validateCandidate(candidate, packageId)
        } catch (error: Throwable) {
            candidate.classLoader.close()
            throw error
        }

        deactivatePackage(packageId)
        try {
            candidate.sources.forEach(sourceRegistry::register)
            loadedPackages[packageId] = candidate
            return candidate
        } catch (error: Throwable) {
            candidate.sources.forEach { source ->
                if (sourceRegistry.get(source.id) === source) {
                    sourceRegistry.unregister(source.id)
                }
            }
            candidate.classLoader.close()
            throw error
        }
    }

    private fun loadPackage(
        packageId: String,
        name: String,
        version: String,
        manifest: ApkManifest,
        jarPath: Path,
    ): LoadedPackage {
        val classLoader = SystemFirstUrlClassLoader(arrayOf(jarPath.toUri().toURL()))
        try {
            val sources = manifest.sourceClasses.flatMap { className ->
                val instance = try {
                    Class.forName(className, true, classLoader)
                        .getDeclaredConstructor()
                        .newInstance()
                } catch (error: Throwable) {
                    throw IllegalStateException(
                        "Could not initialize source class $className: ${error.compatibilityMessage()}",
                        error,
                    )
                }
                when (instance) {
                    is Source -> listOf(instance)
                    is SourceFactory -> instance.createSources()
                    else -> error("$className is not a Source or SourceFactory.")
                }
            }
            return LoadedPackage(
                entry = ChimahonInstalledExtensionEntry(
                    id = packageId,
                    name = name,
                    version = version,
                    sourceCount = sources.count { it is CatalogueSource },
                    packageType = ChimahonExtensionPackageType.AndroidApk,
                ),
                sources = sources,
                classLoader = classLoader,
            )
        } catch (error: Throwable) {
            classLoader.close()
            if (error is IllegalStateException) throw error
            throw IllegalStateException(error.compatibilityMessage(), error)
        }
    }

    private fun validateCandidate(candidate: LoadedPackage, packageId: String) {
        require(candidate.catalogueSourceCount > 0) {
            "The extension did not provide any catalogue sources."
        }
        val duplicateIds = candidate.sources
            .groupBy(Source::id)
            .filterValues { it.size > 1 }
            .keys
        require(duplicateIds.isEmpty()) {
            "The extension declares duplicate source id(s): ${duplicateIds.sorted().joinToString()}."
        }
        val ownedSources = loadedPackages[packageId]
            ?.sources
            .orEmpty()
            .associateBy(Source::id)
        val collision = candidate.sources.firstOrNull { source ->
            val registered = sourceRegistry.get(source.id)
            registered != null && registered !== ownedSources[source.id]
        }
        require(collision == null) {
            "Source id ${collision?.id} is already registered by another extension."
        }
    }

    private fun deactivatePackage(packageId: String) {
        loadedPackages.remove(packageId)?.let { loaded ->
            loaded.sources.forEach { source ->
                if (sourceRegistry.get(source.id) === source) {
                    sourceRegistry.unregister(source.id)
                }
            }
            loaded.classLoader.close()
        }
    }

    private fun installedEntries(): List<ChimahonInstalledExtensionEntry> {
        return loadedPackages.values
            .map(LoadedPackage::entry)
            .sortedBy { it.name.lowercase() }
    }

    private fun parseManifest(apkPath: Path): ApkManifest {
        val parsed = ApkFile(apkPath.toFile()).use { apkFile ->
            val signerFingerprints = buildList {
                runCatching { apkFile.certificateMetaList }
                    .getOrDefault(emptyList())
                    .mapTo(this) { certificate -> certificate.data.sha256() }
                runCatching { apkFile.apkV2Singers }
                    .getOrDefault(emptyList())
                    .flatMap { signer -> signer.certificateMetas }
                    .mapTo(this) { certificate -> certificate.data.sha256() }
            }.distinct().sorted()
            ParsedApk(
                manifestXml = apkFile.manifestXml,
                packageName = apkFile.apkMeta.packageName,
                versionName = apkFile.apkMeta.versionName,
                versionCode = apkFile.apkMeta.versionCode,
                label = apkFile.apkMeta.label,
                signerFingerprints = signerFingerprints,
            )
        }
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
            runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
            runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
            isXIncludeAware = false
            isExpandEntityReferences = false
        }
        val document = factory.newDocumentBuilder()
            .parse(ByteArrayInputStream(parsed.manifestXml.toByteArray()))
        val manifest = document.documentElement
        val packageName = parsed.packageName
            ?.takeIf(String::isNotBlank)
            ?: manifest.getAttribute("package")
        require(packageName.isNotBlank()) { "Extension manifest has no package name." }
        requireValidPackageId(packageName)
        val versionName = parsed.versionName
            ?.takeIf(String::isNotBlank)
            ?: manifest.getAttributeNS(ANDROID_NAMESPACE, "versionName").ifBlank { "unknown" }
        val versionCode = parsed.versionCode
            ?: manifest.getAttributeNS(ANDROID_NAMESPACE, "versionCode").toLongOrNull()
            ?: 0L
        val application = document.getElementsByTagName("application").item(0)
            ?: error("Extension manifest has no application element.")
        val label = parsed.label
            ?.takeIf(String::isNotBlank)
            ?: application.attributes
            ?.getNamedItemNS(ANDROID_NAMESPACE, "label")
            ?.nodeValue
            ?.takeUnless { it.startsWith("@") }
            ?: packageName.substringAfterLast(".")
        val metadata = buildMap {
            val elements = document.getElementsByTagName("meta-data")
            for (index in 0 until elements.length) {
                val attributes = elements.item(index).attributes ?: continue
                val key = attributes.getNamedItemNS(ANDROID_NAMESPACE, "name")?.nodeValue ?: continue
                val value = attributes.getNamedItemNS(ANDROID_NAMESPACE, "value")?.nodeValue ?: continue
                put(key, value)
            }
        }
        val sourceClasses = metadata[METADATA_SOURCE_CLASS]
            ?.split(";")
            ?.map(String::trim)
            ?.filter(String::isNotBlank)
            ?.map { className ->
                if (className.startsWith(".")) packageName + className else className
            }
            .orEmpty()
        require(sourceClasses.isNotEmpty()) {
            "Extension manifest does not declare $METADATA_SOURCE_CLASS."
        }
        val isExtension = buildList {
            val features = document.getElementsByTagName("uses-feature")
            for (index in 0 until features.length) {
                features.item(index).attributes
                    ?.getNamedItemNS(ANDROID_NAMESPACE, "name")
                    ?.nodeValue
                    ?.let(::add)
            }
        }.contains(EXTENSION_FEATURE)

        return ApkManifest(
            packageName = packageName,
            versionName = versionName,
            versionCode = versionCode,
            displayName = label
                .substringAfter("Tachiyomi: ")
                .substringAfter("Mihon: ")
                .ifBlank { packageName.substringAfterLast(".") },
            sourceClasses = sourceClasses,
            isTachiyomiExtension = isExtension,
            signerFingerprints = parsed.signerFingerprints,
        )
    }

    private fun validateLibraryVersion(manifest: ApkManifest) {
        val libraryVersion = manifest.versionName.substringBeforeLast(".").toDoubleOrNull()
        require(
            libraryVersion != null &&
                libraryVersion >= MIN_EXTENSION_LIBRARY_VERSION &&
                libraryVersion <= MAX_EXTENSION_LIBRARY_VERSION,
        ) {
            "${manifest.displayName} uses extension library ${libraryVersion ?: manifest.versionName}; " +
                "desktop supports $MIN_EXTENSION_LIBRARY_VERSION to $MAX_EXTENSION_LIBRARY_VERSION."
        }
    }

    private fun validateUpdate(
        currentApk: Path,
        currentMetadataPath: Path,
        candidate: ApkManifest,
    ) {
        if (Files.notExists(currentApk)) return
        val currentManifest = runCatching { parseManifest(currentApk) }.getOrNull()
        val currentMetadata = readMetadata(currentMetadataPath)
        val currentVersionCode = currentManifest?.versionCode ?: currentMetadata?.versionCode ?: 0L
        require(candidate.versionCode >= currentVersionCode) {
            "Downgrading ${candidate.packageName} from version code $currentVersionCode " +
                "to ${candidate.versionCode} is not allowed."
        }
        val currentSigners = currentManifest
            ?.signerFingerprints
            ?.takeIf { it.isNotEmpty() }
            ?: currentMetadata?.signerFingerprints.orEmpty()
        require(currentSigners.isEmpty() || candidate.signerFingerprints.containsAll(currentSigners)) {
            "The update signature does not match the installed ${candidate.packageName} package."
        }
    }

    private fun readMetadata(path: Path): StoredPackageMetadata? {
        if (Files.notExists(path)) return null
        return runCatching {
            val properties = Properties()
            Files.newInputStream(path).use(properties::load)
            StoredPackageMetadata(
                packageId = properties.getProperty(METADATA_PACKAGE_ID).orEmpty(),
                name = properties.getProperty(METADATA_NAME).orEmpty(),
                version = properties.getProperty(METADATA_VERSION).orEmpty(),
                versionCode = properties.getProperty(METADATA_VERSION_CODE)?.toLongOrNull() ?: 0L,
                apkSha256 = properties.getProperty(METADATA_APK_SHA256).orEmpty(),
                signerFingerprints = properties.getProperty(METADATA_SIGNERS)
                    .orEmpty()
                    .split(",")
                    .filter(String::isNotBlank),
                compatibilityVersion = properties.getProperty(METADATA_COMPATIBILITY_VERSION)
                    ?.toIntOrNull()
                    ?: 0,
                repoBaseUrl = properties.getProperty(METADATA_REPO_BASE_URL).orEmpty(),
                artifactUrl = properties.getProperty(METADATA_ARTIFACT_URL).orEmpty(),
            )
        }.getOrNull()
    }

    private fun writeMetadata(path: Path, metadata: StoredPackageMetadata) {
        val properties = Properties().apply {
            setProperty(METADATA_PACKAGE_ID, metadata.packageId)
            setProperty(METADATA_NAME, metadata.name)
            setProperty(METADATA_VERSION, metadata.version)
            setProperty(METADATA_VERSION_CODE, metadata.versionCode.toString())
            setProperty(METADATA_APK_SHA256, metadata.apkSha256)
            setProperty(METADATA_SIGNERS, metadata.signerFingerprints.joinToString(","))
            setProperty(METADATA_COMPATIBILITY_VERSION, metadata.compatibilityVersion.toString())
            setProperty(METADATA_REPO_BASE_URL, metadata.repoBaseUrl)
            setProperty(METADATA_ARTIFACT_URL, metadata.artifactUrl)
        }
        Files.newOutputStream(path).use { output ->
            properties.store(output, "Chimahon APK extension metadata")
        }
    }

    private fun writeMetadataAtomically(path: Path, metadata: StoredPackageMetadata) {
        val temporary = Files.createTempFile(extensionDirectory, "${path.fileName}-", ".tmp")
        try {
            writeMetadata(temporary, metadata)
            moveReplacing(temporary, path)
        } finally {
            Files.deleteIfExists(temporary)
        }
    }

    private fun cleanupTemporaryFiles() {
        Files.list(extensionDirectory).use { paths ->
            paths
                .filter { path ->
                    val name = path.fileName.toString()
                    name.endsWith(".download") ||
                        name.endsWith(".tmp") ||
                        (
                            (name.endsWith(JAR_SUFFIX) || name.endsWith(METADATA_SUFFIX)) &&
                                Files.notExists(
                                    path.resolveSibling(
                                        "${name.substringBeforeLast(".")}$APK_SUFFIX",
                                    ),
                                )
                            )
                }
                .forEach(Files::deleteIfExists)
        }
    }

    private fun recoverInterruptedUpdates() {
        val apkBackups = Files.list(extensionDirectory).use { paths ->
            paths
                .filter { it.fileName.toString().endsWith("$APK_SUFFIX$BACKUP_SUFFIX") }
                .toList()
        }
        apkBackups.forEach { backupApk ->
            val finalApk = backupApk.resolveSibling(
                backupApk.fileName.toString().removeSuffix(BACKUP_SUFFIX),
            )
            val fileStem = finalApk.fileName.toString().removeSuffix(APK_SUFFIX)
            val finalJar = finalApk.resolveSibling("$fileStem$JAR_SUFFIX")
            val finalMetadata = finalApk.resolveSibling("$fileStem$METADATA_SUFFIX")
            restoreBackup(finalJar, finalJar.resolveSibling("${finalJar.fileName}$BACKUP_SUFFIX"))
            restoreBackup(
                finalMetadata,
                finalMetadata.resolveSibling("${finalMetadata.fileName}$BACKUP_SUFFIX"),
            )
            restoreBackup(finalApk, backupApk)
        }
    }

    private fun writeErrorReport(apkPath: Path, message: String) {
        runCatching {
            Files.writeString(errorPath(apkPath), message)
        }
    }

    private fun errorPath(apkPath: Path): Path {
        return apkPath.resolveSibling("${apkPath.fileName}$ERROR_SUFFIX")
    }

    private fun backup(source: Path, target: Path) {
        if (Files.exists(source)) {
            moveReplacing(source, target)
        }
    }

    private fun restoreBackup(target: Path, backup: Path) {
        if (Files.exists(backup)) {
            Files.deleteIfExists(target)
            moveReplacing(backup, target)
        } else {
            Files.deleteIfExists(target)
        }
    }

    private fun removeBackup(path: Path) {
        runCatching { Files.deleteIfExists(path) }
    }

    private fun moveReplacing(source: Path, target: Path) {
        try {
            Files.move(
                source,
                target,
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun convertApk(
        apkPath: Path,
        jarPath: Path,
    ) {
        val exceptionHandler = BaksmaliBaseDexExceptionHandler()
        val reader = MultiDexFileReader.open(Files.readAllBytes(apkPath))
        Dex2jar.from(reader)
            .withExceptionHandler(exceptionHandler)
            .reUseReg(false)
            .topoLogicalSort()
            .skipDebug(true)
            .optimizeSynchronized(false)
            .printIR(false)
            .noCode(false)
            .skipExceptions(false)
            .dontSanitizeNames(true)
            .to(jarPath)
        if (exceptionHandler.hasException()) {
            exceptionHandler.dump(
                jarPath.resolveSibling("${jarPath.fileName}.errors.txt"),
                emptyArray(),
            )
        }
        adaptJarForDesktop(apkPath, jarPath)
    }

    private fun adaptJarForDesktop(
        apkPath: Path,
        jarPath: Path,
    ) {
        val output = ByteArrayOutputStream()
        val writtenEntries = linkedSetOf<String>()
        ZipOutputStream(output).use { zipOutput ->
            Files.newInputStream(jarPath).use { input ->
                ZipInputStream(input).use { zipInput ->
                    var entry = zipInput.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory && !entry.name.startsWith("META-INF/")) {
                            val bytes = zipInput.readBytes()
                            writeZipEntry(
                                output = zipOutput,
                                name = entry.name,
                                bytes = if (entry.name.endsWith(".class")) remapClass(bytes) else bytes,
                                writtenEntries = writtenEntries,
                            )
                        }
                        entry = zipInput.nextEntry
                    }
                }
            }
            Files.newInputStream(apkPath).use { input ->
                ZipInputStream(input).use { zipInput ->
                    var entry = zipInput.nextEntry
                    while (entry != null) {
                        if (
                            !entry.isDirectory &&
                            (entry.name.startsWith("assets/") || entry.name.startsWith("res/raw/"))
                        ) {
                            writeZipEntry(
                                output = zipOutput,
                                name = entry.name,
                                bytes = zipInput.readBytes(),
                                writtenEntries = writtenEntries,
                            )
                        }
                        entry = zipInput.nextEntry
                    }
                }
            }
        }
        Files.write(jarPath, output.toByteArray())
    }

    private fun remapClass(bytes: ByteArray): ByteArray {
        val reader = ClassReader(bytes)
        val writer = ClassWriter(reader, 0)
        reader.accept(
            ClassRemapper(writer, SimpleRemapper(DESKTOP_CLASS_REMAP)),
            0,
        )
        return writer.toByteArray()
    }

    private fun writeZipEntry(
        output: ZipOutputStream,
        name: String,
        bytes: ByteArray,
        writtenEntries: MutableSet<String>,
    ) {
        if (!writtenEntries.add(name)) return
        output.putNextEntry(ZipEntry(name))
        output.write(bytes)
        output.closeEntry()
    }

    private data class ParsedApk(
        val manifestXml: String,
        val packageName: String?,
        val versionName: String?,
        val versionCode: Long?,
        val label: String?,
        val signerFingerprints: List<String>,
    )

    private data class ApkManifest(
        val packageName: String,
        val versionName: String,
        val versionCode: Long,
        val displayName: String,
        val sourceClasses: List<String>,
        val isTachiyomiExtension: Boolean,
        val signerFingerprints: List<String>,
    )

    private data class StoredPackageMetadata(
        val packageId: String,
        val name: String,
        val version: String,
        val versionCode: Long,
        val apkSha256: String,
        val signerFingerprints: List<String>,
        val compatibilityVersion: Int,
        val repoBaseUrl: String,
        val artifactUrl: String,
    )

    private data class LoadedPackage(
        val entry: ChimahonInstalledExtensionEntry,
        val sources: List<Source>,
        val classLoader: SystemFirstUrlClassLoader,
    ) {
        val catalogueSourceCount = sources.count { it is CatalogueSource }
    }

    private class SystemFirstUrlClassLoader(
        urls: Array<URL>,
    ) : URLClassLoader(urls, null) {
        private val systemClassLoader = getSystemClassLoader()

        override fun loadClass(
            name: String,
            resolve: Boolean,
        ): Class<*> {
            var loaded = findLoadedClass(name)
            if (loaded == null) {
                loaded = runCatching { systemClassLoader.loadClass(name) }.getOrNull()
            }
            if (loaded == null) {
                loaded = runCatching { findClass(name) }.getOrElse {
                    super.loadClass(name, resolve)
                }
            }
            if (resolve) resolveClass(loaded)
            return loaded
        }

        override fun getResource(name: String): URL? {
            return systemClassLoader.getResource(name)
                ?: findResource(name)
                ?: super.getResource(name)
        }

        override fun getResources(name: String): Enumeration<URL> {
            val resources = buildList {
                listOf(
                    systemClassLoader.getResources(name),
                    findResources(name),
                    parent?.getResources(name),
                ).forEach { enumeration ->
                    while (enumeration?.hasMoreElements() == true) {
                        add(enumeration.nextElement())
                    }
                }
            }
            return object : Enumeration<URL> {
                private val iterator = resources.iterator()

                override fun hasMoreElements(): Boolean = iterator.hasNext()

                override fun nextElement(): URL = iterator.next()
            }
        }
    }

    private companion object {
        const val APK_EXTENSION_DIRECTORY = "apk-extensions"
        const val APK_SUFFIX = ".apk"
        const val JAR_SUFFIX = ".jar"
        const val METADATA_SUFFIX = ".properties"
        const val BACKUP_SUFFIX = ".bak"
        const val ERROR_SUFFIX = ".error.txt"
        const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
        const val EXTENSION_FEATURE = "tachiyomi.extension"
        const val METADATA_SOURCE_CLASS = "tachiyomi.extension.class"
        const val METADATA_PACKAGE_ID = "packageId"
        const val METADATA_NAME = "name"
        const val METADATA_VERSION = "version"
        const val METADATA_VERSION_CODE = "versionCode"
        const val METADATA_APK_SHA256 = "apkSha256"
        const val METADATA_SIGNERS = "signers"
        const val METADATA_COMPATIBILITY_VERSION = "compatibilityVersion"
        const val METADATA_REPO_BASE_URL = "repoBaseUrl"
        const val METADATA_ARTIFACT_URL = "artifactUrl"
        const val DESKTOP_COMPATIBILITY_VERSION = 2
        const val MIN_EXTENSION_LIBRARY_VERSION = 1.4
        const val MAX_EXTENSION_LIBRARY_VERSION = 1.5
        const val MAX_REPORTED_ERRORS = 3

        val DESKTOP_CLASS_REMAP = mapOf(
            "eu/kanade/tachiyomi/network/NetworkHelper" to
                "eu/kanade/tachiyomi/source/online/SourceNetworkContext",
            "android/net/Uri" to
                "eu/kanade/tachiyomi/source/model/PlatformUri",
        )
    }
}

private fun String.toSafeFileName(): String {
    return map { character ->
        if (character.isLetterOrDigit() || character == '.' || character == '-' || character == '_') {
            character
        } else {
            '_'
        }
    }.joinToString("").ifBlank { "extension" }
}

private fun requireValidPackageId(packageId: String) {
    require(packageId.matches(PACKAGE_ID_PATTERN)) {
        "Extension package id must contain only letters, numbers, dots, dashes, or underscores."
    }
}

private fun ByteArray.sha256(): String {
    return MessageDigest.getInstance("SHA-256")
        .digest(this)
        .joinToString("") { byte -> "%02x".format(byte) }
}

private fun Throwable.compatibilityMessage(): String {
    val causes = generateSequence(this) { error ->
        error.cause?.takeUnless { it === error }
    }.toList()
    val missingClass = causes.firstNotNullOfOrNull { error ->
        when (error) {
            is ClassNotFoundException,
            is NoClassDefFoundError,
            -> error.message
                ?.substringBefore(" (")
                ?.replace('/', '.')
                ?.takeIf(String::isNotBlank)
            else -> null
        }
    }
    if (missingClass != null) {
        return "Desktop compatibility is missing class $missingClass required by this extension."
    }
    val root = causes.lastOrNull() ?: this
    return root.message
        ?.takeIf(String::isNotBlank)
        ?: root::class.simpleName
        ?: "Unknown extension loading error"
}

private fun Throwable.userMessage(): String {
    return message
        ?.takeIf(String::isNotBlank)
        ?: rootCause().message
        ?.takeIf(String::isNotBlank)
        ?: this::class.simpleName
        ?: "Unknown extension error"
}

private fun Throwable.rootCause(): Throwable {
    var result = this
    while (result.cause != null && result.cause !== result) {
        result = result.cause!!
    }
    return result
}

private val PACKAGE_ID_PATTERN = Regex("[A-Za-z0-9._-]+")
