package eu.kanade.tachiyomi.source.online

import tachiyomi.core.extensions.ScriptHttpRequest

fun resolveScriptSourceUrl(baseUrl: String, url: String): String? {
    val candidate = url.trim()
    if (candidate.isBlank()) return null
    return candidate.resolveAgainstBaseUrl(baseUrl)
}

internal fun ScriptHttpRequest.resolveAgainst(baseUrl: String): ScriptHttpRequest {
    val resolvedUrl = resolveScriptSourceUrl(baseUrl, url) ?: url.trim()
    return if (resolvedUrl == url) this else copy(url = resolvedUrl)
}

private fun String.resolveAgainstBaseUrl(baseUrl: String): String {
    val candidate = this
    if (candidate.hasUrlScheme()) return candidate

    val root = baseUrl.trimEnd('/')
    if (candidate.startsWith("//")) {
        val scheme = root.substringBefore("://", missingDelimiterValue = "https")
        return "$scheme:$candidate"
    }
    if (candidate.startsWith("/")) {
        return "${root.origin()}$candidate"
    }
    if (candidate.startsWith("?") || candidate.startsWith("#")) {
        return "$root$candidate"
    }
    return "$root/${candidate.trimStart('/')}"
}

private fun String.origin(): String {
    val schemeSplit = indexOf("://")
    if (schemeSplit == -1) return trimEnd('/')

    val hostStart = schemeSplit + 3
    val hostEnd = indexOf('/', startIndex = hostStart).takeIf { it >= 0 } ?: length
    return take(hostEnd)
}

private fun String.hasUrlScheme(): Boolean {
    val colon = indexOf(':')
    if (colon <= 0) return false
    val scheme = take(colon)
    return scheme.first().isLetter() &&
        scheme.all { it.isLetterOrDigit() || it == '+' || it == '-' || it == '.' }
}
