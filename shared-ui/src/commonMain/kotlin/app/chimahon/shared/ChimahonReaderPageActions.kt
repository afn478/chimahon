package app.chimahon.shared

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import kotlin.random.Random

data class ChimahonReaderPageActionFile(
    val filename: String,
    val path: String,
)

sealed interface ChimahonReaderPageActionResult {
    val isSuccess: Boolean
    val message: String
    val file: ChimahonReaderPageActionFile?

    data class Success(
        override val message: String,
        override val file: ChimahonReaderPageActionFile? = null,
    ) : ChimahonReaderPageActionResult {
        override val isSuccess: Boolean = true
    }

    data class Unavailable(
        override val message: String,
        override val file: ChimahonReaderPageActionFile? = null,
    ) : ChimahonReaderPageActionResult {
        override val isSuccess: Boolean = false
    }

    data class Failure(
        override val message: String,
        val cause: Throwable? = null,
        override val file: ChimahonReaderPageActionFile? = null,
    ) : ChimahonReaderPageActionResult {
        override val isSuccess: Boolean = false
    }
}

object ChimahonReaderPageActions {
    fun pageDisplayNumber(page: ChimahonReaderPage): Int {
        return page.index + 1
    }

    fun pageUrl(page: ChimahonReaderPage): String? {
        return page.imageUrl?.trim()?.takeIf(String::isNotBlank)
            ?: page.url.trim().takeIf(String::isNotBlank)
    }

    fun pageText(
        request: ChimahonReaderRequest,
        page: ChimahonReaderPage,
    ): String {
        return buildString {
            append(request.mangaTitle.ifBlank { "Untitled manga" })
            append('\n')
            append(request.chapterName.ifBlank { "Chapter" })
            append('\n')
            append("Page ").append(pageDisplayNumber(page))
            pageUrl(page)?.let { url ->
                append('\n')
                append(url)
            }
        }
    }

    fun generatedPageFilename(
        request: ChimahonReaderRequest,
        page: ChimahonReaderPage,
        bytes: ByteArray,
        fileExtension: String? = null,
    ): String {
        val extension = imageExtension(bytes, fileExtension)
        val suffix = " - ${pageDisplayNumber(page)}.$extension"
        val base = safeFileName(
            input = "${request.mangaTitle} - ${request.chapterName}",
            maxBytes = (MAX_FILE_NAME_BYTES - suffix.byteSize()).coerceAtLeast(1),
        )
        return "$base$suffix"
    }

    fun copyPageUrlToClipboard(page: ChimahonReaderPage): ChimahonReaderPageActionResult {
        val url = pageUrl(page)
            ?: return ChimahonReaderPageActionResult.Unavailable("Page URL is unavailable.")
        return copyText(
            text = url,
            successMessage = "Page URL copied.",
            unavailableMessage = "Copying text is unavailable on this platform.",
        )
    }

    fun copyPageTextToClipboard(
        request: ChimahonReaderRequest,
        page: ChimahonReaderPage,
    ): ChimahonReaderPageActionResult {
        return copyText(
            text = pageText(request, page),
            successMessage = "Page text copied.",
            unavailableMessage = "Copying text is unavailable on this platform.",
        )
    }

    fun sharePageText(
        request: ChimahonReaderRequest,
        page: ChimahonReaderPage,
        title: String? = request.mangaTitle,
    ): ChimahonReaderPageActionResult {
        val text = pageText(request, page)
        val shared = runCatching {
            ChimahonPlatformIntegration.shareText(text, title)
        }.getOrDefault(false)

        return if (shared) {
            ChimahonReaderPageActionResult.Success("Page text shared.")
        } else {
            ChimahonReaderPageActionResult.Unavailable(
                "Sharing text is unavailable on this platform.",
            )
        }
    }

    suspend fun savePageBytes(
        request: ChimahonReaderRequest,
        page: ChimahonReaderPage,
        bytes: ByteArray,
        fileExtension: String? = null,
    ): ChimahonReaderPageActionResult {
        val file = writePageBytes(
            request = request,
            page = page,
            bytes = bytes,
            fileExtension = fileExtension,
            location = ReaderPageActionFileLocation.Downloads,
        )
        return when (file) {
            is WritePageFileResult.Success -> {
                ChimahonReaderPageActionResult.Success(
                    message = "Page image saved.",
                    file = file.file,
                )
            }
            is WritePageFileResult.Failure -> {
                ChimahonReaderPageActionResult.Failure(
                    message = file.message,
                    cause = file.cause,
                )
            }
        }
    }

    suspend fun sharePageBytes(
        request: ChimahonReaderRequest,
        page: ChimahonReaderPage,
        bytes: ByteArray,
        fileExtension: String? = null,
        title: String? = request.mangaTitle,
    ): ChimahonReaderPageActionResult {
        val file = writePageBytes(
            request = request,
            page = page,
            bytes = bytes,
            fileExtension = fileExtension,
            location = ReaderPageActionFileLocation.Cache,
        )
        if (file is WritePageFileResult.Failure) {
            return ChimahonReaderPageActionResult.Failure(
                message = file.message,
                cause = file.cause,
            )
        }

        val actionFile = (file as WritePageFileResult.Success).file
        val shared = runCatching {
            ChimahonPlatformIntegration.shareFile(actionFile.path, title)
        }.getOrDefault(false)

        return if (shared) {
            ChimahonReaderPageActionResult.Success(
                message = "Page image shared.",
                file = actionFile,
            )
        } else {
            ChimahonReaderPageActionResult.Unavailable(
                message = "Page image was prepared, but file sharing is unavailable on this platform.",
                file = actionFile,
            )
        }
    }

    private fun copyText(
        text: String,
        successMessage: String,
        unavailableMessage: String,
    ): ChimahonReaderPageActionResult {
        if (text.isBlank()) {
            return ChimahonReaderPageActionResult.Unavailable("There is no page text to copy.")
        }

        val copied = runCatching {
            ChimahonPlatformIntegration.copyText(text)
        }.getOrDefault(false)

        return if (copied) {
            ChimahonReaderPageActionResult.Success(successMessage)
        } else {
            ChimahonReaderPageActionResult.Unavailable(unavailableMessage)
        }
    }
}

private enum class ReaderPageActionFileLocation {
    Cache,
    Downloads,
}

private sealed interface WritePageFileResult {
    data class Success(val file: ChimahonReaderPageActionFile) : WritePageFileResult

    data class Failure(
        val message: String,
        val cause: Throwable? = null,
    ) : WritePageFileResult
}

private suspend fun writePageBytes(
    request: ChimahonReaderRequest,
    page: ChimahonReaderPage,
    bytes: ByteArray,
    fileExtension: String?,
    location: ReaderPageActionFileLocation,
    fileSystem: FileSystem = FileSystem.SYSTEM,
): WritePageFileResult {
    if (bytes.isEmpty()) {
        return WritePageFileResult.Failure(
            message = "Page image is empty.",
            cause = IllegalArgumentException("Page image is empty."),
        )
    }

    val filename = ChimahonReaderPageActions.generatedPageFilename(
        request = request,
        page = page,
        bytes = bytes,
        fileExtension = fileExtension,
    )
    val directory = readerPageActionDirectory(location)
    val path = directory / filename

    return try {
        writeReaderPageBytesAtomically(path, bytes, fileSystem)
        WritePageFileResult.Success(
            ChimahonReaderPageActionFile(
                filename = filename,
                path = path.toString(),
            ),
        )
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (failure: Throwable) {
        WritePageFileResult.Failure(
            message = failure.message ?: "Page image could not be written.",
            cause = failure,
        )
    }
}

internal suspend fun writeReaderPageBytesAtomically(
    path: Path,
    bytes: ByteArray,
    fileSystem: FileSystem = FileSystem.SYSTEM,
) {
    val parent = checkNotNull(path.parent) { "Reader page image path has no parent." }
    val temporaryPath = uniqueReaderPageTemporarySibling(path, fileSystem)

    try {
        currentCoroutineContext().ensureActive()
        fileSystem.createDirectories(parent)
        fileSystem.write(temporaryPath) {
            write(bytes)
        }
        currentCoroutineContext().ensureActive()
        fileSystem.atomicMove(temporaryPath, path)
    } catch (cancellation: CancellationException) {
        deleteReaderPageTemporaryFile(temporaryPath, fileSystem)
        throw cancellation
    } catch (failure: Throwable) {
        deleteReaderPageTemporaryFile(temporaryPath, fileSystem)
        throw failure
    }
}

private fun uniqueReaderPageTemporarySibling(
    target: Path,
    fileSystem: FileSystem,
): Path {
    val parent = checkNotNull(target.parent) { "Reader page image path has no parent." }
    repeat(READER_PAGE_TEMPORARY_PATH_ATTEMPTS) {
        val candidate = parent / "${target.name}.tmp-${Random.nextInt(Int.MAX_VALUE)}"
        if (!fileSystem.exists(candidate)) return candidate
    }
    error("Unable to allocate a temporary reader page image path beside $target.")
}

private fun deleteReaderPageTemporaryFile(
    path: Path,
    fileSystem: FileSystem,
) {
    if (fileSystem.exists(path)) {
        runCatching { fileSystem.delete(path) }
    }
}

private fun readerPageActionDirectory(location: ReaderPageActionFileLocation): Path {
    val paths = ChimahonPlatformIntegration.storagePaths()
    return when (location) {
        ReaderPageActionFileLocation.Cache -> paths.cacheDir.toPath() / READER_PAGE_SHARE_DIRECTORY
        ReaderPageActionFileLocation.Downloads -> paths.downloadsDir.toPath() / READER_PAGE_SAVE_DIRECTORY
    }
}

private fun imageExtension(
    bytes: ByteArray,
    requestedExtension: String?,
): String {
    val requested = requestedExtension
        ?.trim()
        ?.trimStart('.')
        ?.lowercase()
        ?.let { if (it == "jpeg") "jpg" else it }
        ?.takeIf { it in SUPPORTED_IMAGE_EXTENSIONS }
    return requested ?: sniffImageExtension(bytes)
}

private fun sniffImageExtension(bytes: ByteArray): String {
    return when {
        bytes.startsWith(0x89, 0x50, 0x4e, 0x47) -> "png"
        bytes.startsWith(0xff, 0xd8, 0xff) -> "jpg"
        bytes.startsWithAscii("GIF87a") || bytes.startsWithAscii("GIF89a") -> "gif"
        bytes.size >= 12 &&
            bytes.sliceEquals(0, "RIFF") &&
            bytes.sliceEquals(8, "WEBP") -> "webp"
        bytes.startsWith(0x42, 0x4d) -> "bmp"
        bytes.size >= 12 &&
            bytes.sliceEquals(4, "ftyp") &&
            (bytes.sliceEquals(8, "avif") || bytes.sliceEquals(8, "avis")) -> "avif"
        else -> DEFAULT_IMAGE_EXTENSION
    }
}

private fun safeFileName(
    input: String,
    maxBytes: Int = MAX_FILE_NAME_BYTES,
): String {
    val trimmed = input.trim('.', ' ')
    if (trimmed.isEmpty()) return INVALID_FILE_NAME
    val sanitized = buildString(trimmed.length) {
        trimmed.forEach { character ->
            append(
                when {
                    character.code in 0x00..0x1f -> '_'
                    character == '\u007f' -> '_'
                    character in INVALID_FILE_NAME_CHARACTERS -> '_'
                    else -> character
                },
            )
        }
    }
    if (sanitized.byteSize() <= maxBytes) return sanitized

    var byteCount = 0
    return buildString {
        sanitized.forEach { character ->
            val bytes = character.toString().byteSize()
            if (byteCount + bytes > maxBytes) return@buildString
            append(character)
            byteCount += bytes
        }
    }.ifBlank { INVALID_FILE_NAME }
}

private fun String.byteSize(): Int {
    return encodeToByteArray().size
}

private fun ByteArray.startsWith(vararg prefix: Int): Boolean {
    if (size < prefix.size) return false
    return prefix.indices.all { index -> this[index].toInt() and 0xff == prefix[index] }
}

private fun ByteArray.startsWithAscii(prefix: String): Boolean {
    return sliceEquals(0, prefix)
}

private fun ByteArray.sliceEquals(offset: Int, value: String): Boolean {
    val encoded = value.encodeToByteArray()
    if (size < offset + encoded.size) return false
    return encoded.indices.all { index -> this[offset + index] == encoded[index] }
}

private const val READER_PAGE_SAVE_DIRECTORY = "reader-pages"
private const val READER_PAGE_SHARE_DIRECTORY = "shared-reader-pages"
private const val READER_PAGE_TEMPORARY_PATH_ATTEMPTS = 16
private const val MAX_FILE_NAME_BYTES = 240
private const val DEFAULT_IMAGE_EXTENSION = "jpg"
private const val INVALID_FILE_NAME = "(invalid)"
private val INVALID_FILE_NAME_CHARACTERS = setOf('"', '*', '/', ':', '<', '>', '?', '\\', '|')
private val SUPPORTED_IMAGE_EXTENSIONS = setOf("avif", "bmp", "gif", "jpg", "png", "webp")
