package tachiyomi.core.platform.storage

import okio.Path

interface PlatformStorageDirectories {
    val cacheDir: Path
    val filesDir: Path
    val temporaryDir: Path

    fun defaultDownloadsDir(appName: String): Path

    fun fileUri(path: Path): String
}

fun Path.resolveDirectoryName(name: String): Path {
    return this / sanitizePathSegment(name)
}

internal fun sanitizePathSegment(name: String): String {
    val sanitized = name
        .trim()
        .map { char ->
            when (char) {
                '/', '\\', ':', '*', '?', '"', '<', '>', '|' -> '_'
                else -> char
            }
        }
        .joinToString("")
        .trim('.')

    return sanitized.ifBlank { "app" }
}

internal fun encodedFileUri(path: Path): String {
    val normalizedPath = path.toString().replace('\\', '/')
    val encodedPath = normalizedPath.encodeFileUriPath()
    return when {
        encodedPath.startsWith("//") -> "file:$encodedPath"
        encodedPath.startsWith("/") -> "file://$encodedPath"
        else -> "file:///$encodedPath"
    }
}

private fun String.encodeFileUriPath(): String {
    return encodeToByteArray().joinToString(separator = "") { byte ->
        val value = byte.toInt() and 0xFF
        val character = value.toChar()
        if (character.isFileUriPathCharacter()) {
            character.toString()
        } else {
            "%${value.toString(16).uppercase().padStart(2, '0')}"
        }
    }
}

private fun Char.isFileUriPathCharacter(): Boolean {
    return this in 'A'..'Z' ||
        this in 'a'..'z' ||
        this in '0'..'9' ||
        this == '/' ||
        this == '-' ||
        this == '.' ||
        this == '_' ||
        this == '~' ||
        this == ':'
}
