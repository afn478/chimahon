package app.chimahon.shared

import eu.kanade.tachiyomi.source.model.SManga

internal fun SManga.safeSourceUrl(): String {
    return runCatching { url }
        .getOrNull()
        .orEmpty()
}
