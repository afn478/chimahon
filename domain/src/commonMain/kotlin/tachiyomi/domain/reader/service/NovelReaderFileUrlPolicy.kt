package tachiyomi.domain.reader.service

object NovelReaderFileUrlPolicy {
    const val FILE_URL_PREFIX = "file://"

    fun fileUrlForAbsolutePath(absolutePath: String): String {
        val normalizedPath = normalizePathSeparators(absolutePath)
        val urlPath = when {
            hasWindowsDrivePrefix(normalizedPath) -> "/$normalizedPath"
            normalizedPath.startsWith("//") -> normalizedPath.removePrefix("//")
            else -> normalizedPath
        }

        return "$FILE_URL_PREFIX$urlPath"
    }

    fun localPathForFileUrlOrPath(pathOrUrl: String): String {
        val strippedValue = stripQueryAndFragment(pathOrUrl)
        val withoutScheme = strippedValue.removePrefix(FILE_URL_PREFIX)
        val localPath = if (
            strippedValue.startsWith(FILE_URL_PREFIX) &&
            withoutScheme.isNotEmpty() &&
            !withoutScheme.startsWith("/") &&
            !hasWindowsDrivePrefix(withoutScheme)
        ) {
            "//$withoutScheme"
        } else {
            withoutScheme
        }
        val normalizedPath = percentDecode(
            normalizePathSeparators(
                localPath,
            ),
        )

        return dropLeadingSlashBeforeWindowsDrive(normalizedPath)
    }

    fun hrefPathForComparison(href: String): String {
        return percentDecode(
            normalizePathSeparators(
                stripQueryAndFragment(href),
            ),
        )
    }

    fun fragmentForUrl(url: String?): String? {
        return url
            ?.substringAfter("#", missingDelimiterValue = "")
            ?.takeIf { it.isNotEmpty() }
    }

    fun stripQueryAndFragment(value: String): String {
        return value
            .substringBefore("#")
            .substringBefore("?")
    }

    fun normalizePathSeparators(path: String): String {
        return path.replace("\\", "/")
    }

    fun percentDecode(value: String): String {
        if ('%' !in value) return value

        val builder = StringBuilder()
        val bytes = mutableListOf<Byte>()

        fun flushBytes() {
            if (bytes.isEmpty()) return
            builder.append(ByteArray(bytes.size) { bytes[it] }.decodeToString())
            bytes.clear()
        }

        var index = 0
        while (index < value.length) {
            val char = value[index]
            val firstHex = value.getOrNull(index + 1)?.hexValue()
            val secondHex = value.getOrNull(index + 2)?.hexValue()

            if (char == '%' && firstHex != null && secondHex != null) {
                bytes += ((firstHex shl 4) + secondHex).toByte()
                index += 3
            } else {
                flushBytes()
                builder.append(char)
                index += 1
            }
        }

        flushBytes()
        return builder.toString()
    }

    private fun dropLeadingSlashBeforeWindowsDrive(path: String): String {
        return if (path.length >= 3 && path.first() == '/' && hasWindowsDrivePrefix(path.drop(1))) {
            path.drop(1)
        } else {
            path
        }
    }

    private fun hasWindowsDrivePrefix(path: String): Boolean {
        return path.length >= 2 && path[0].isLetter() && path[1] == ':'
    }

    private fun Char.hexValue(): Int? {
        return when (this) {
            in '0'..'9' -> this - '0'
            in 'a'..'f' -> this - 'a' + 10
            in 'A'..'F' -> this - 'A' + 10
            else -> null
        }
    }
}
