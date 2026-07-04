package tachiyomi.core.platform.settings

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

class FilePlatformSettingsStore(
    private val settingsFile: Path,
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
) : PlatformSettingsStore {

    private val mutex = Mutex()
    private var loaded = false
    private val values = mutableMapOf<String, String>()

    override suspend fun readString(key: String): String? {
        return mutex.withLock {
            ensureLoaded()
            values[key]
        }
    }

    override suspend fun writeString(key: String, value: String) {
        require(key.isNotBlank()) { "Settings key must not be blank." }
        mutex.withLock {
            ensureLoaded()
            values[key] = value
            persist()
        }
    }

    override suspend fun remove(key: String) {
        mutex.withLock {
            ensureLoaded()
            if (values.remove(key) != null) {
                persist()
            }
        }
    }

    override suspend fun clear() {
        mutex.withLock {
            ensureLoaded()
            values.clear()
            persist()
        }
    }

    override suspend fun snapshot(): Map<String, String> {
        return mutex.withLock {
            ensureLoaded()
            values.toMap()
        }
    }

    private fun ensureLoaded() {
        if (loaded) return
        values.clear()
        if (fileSystem.exists(settingsFile)) {
            fileSystem.read(settingsFile) {
                readUtf8()
            }
                .lines()
                .mapNotNull(::decodeLine)
                .forEach { (key, value) -> values[key] = value }
        }
        loaded = true
    }

    private fun persist() {
        settingsFile.parent?.let(fileSystem::createDirectories)
        val encoded = values
            .entries
            .sortedBy { entry -> entry.key }
            .joinToString(separator = "\n", postfix = if (values.isEmpty()) "" else "\n") { (key, value) ->
                "${encodeToken(key)}\t${encodeToken(value)}"
            }
        val temporaryFile = settingsFile.temporaryFile()
        fileSystem.write(temporaryFile) {
            writeUtf8(encoded)
        }
        fileSystem.atomicMove(temporaryFile, settingsFile)
    }

    private fun Path.temporaryFile(): Path {
        return parent?.let { directory -> directory / "$name.tmp" } ?: "$name.tmp".toPath()
    }
}

private fun decodeLine(line: String): Pair<String, String>? {
    if (line.isBlank()) return null
    val separator = line.indexOf('\t')
    if (separator <= 0) return null
    return decodeToken(line.substring(0, separator)) to decodeToken(line.substring(separator + 1))
}

private fun encodeToken(value: String): String {
    return buildString(value.length) {
        value.forEach { char ->
            when (char) {
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(char)
            }
        }
    }
}

private fun decodeToken(value: String): String {
    return buildString(value.length) {
        var escaped = false
        value.forEach { char ->
            if (escaped) {
                when (char) {
                    '\\' -> append('\\')
                    'n' -> append('\n')
                    'r' -> append('\r')
                    't' -> append('\t')
                    else -> {
                        append('\\')
                        append(char)
                    }
                }
                escaped = false
            } else if (char == '\\') {
                escaped = true
            } else {
                append(char)
            }
        }
        if (escaped) {
            append('\\')
        }
    }
}
