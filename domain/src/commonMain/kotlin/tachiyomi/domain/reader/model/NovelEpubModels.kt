package tachiyomi.domain.reader.model

import kotlinx.serialization.Serializable

data class NovelEpubCreator(
    val name: String? = null,
    val role: String? = null,
    val fileAs: String? = null,
)

data class NovelEpubMetadata(
    val title: String? = null,
    val identifier: String? = null,
    val language: String? = null,
    val creator: NovelEpubCreator? = null,
    val contributor: NovelEpubCreator? = null,
    val publisher: String? = null,
    val date: String? = null,
    val description: String? = null,
    val rights: String? = null,
    val subject: String? = null,
    val coverage: String? = null,
    val format: String? = null,
    val relation: String? = null,
    val source: String? = null,
    val type: String? = null,
    val coverId: String? = null,
)

enum class NovelEpubMediaType(val value: String) {
    GIF("image/gif"),
    JPEG("image/jpeg"),
    PNG("image/png"),
    SVG("image/svg+xml"),
    XHTML("application/xhtml+xml"),
    OPF2("application/x-dtbncx+xml"),
    JAVASCRIPT("application/javascript"),
    OPENTYPE("application/font-sfnt"),
    WOFF("application/font-woff"),
    WOFF2("font/woff2"),
    MEDIA_OVERLAYS("application/smil+xml"),
    PLS("application/pls+xml"),
    MP3("audio/mpeg"),
    MP4("audio/mp4"),
    CSS("text/css"),
    UNKNOWN("application/octet-stream"),
    ;

    companion object {
        fun fromString(value: String?): NovelEpubMediaType {
            if (value == null) return UNKNOWN
            return entries.find { it.value.equals(value, ignoreCase = true) } ?: UNKNOWN
        }
    }
}

data class NovelEpubManifestItem(
    val id: String,
    val href: String,
    val mediaType: NovelEpubMediaType = NovelEpubMediaType.UNKNOWN,
    val properties: String? = null,
)

data class NovelEpubManifest(
    val id: String? = null,
    val items: Map<String, NovelEpubManifestItem> = emptyMap(),
)

@Serializable
enum class NovelEpubSpineItemType {
    TEXT,
    IMAGE_ONLY,
}

@Serializable
data class NovelEpubSpineItem(
    val idref: String,
    val id: String? = null,
    val linear: Boolean = true,
    val type: NovelEpubSpineItemType = NovelEpubSpineItemType.TEXT,
    val imageUrl: String? = null,
)

@Serializable
enum class NovelEpubPageProgressionDirection {
    LTR,
    RTL,
    DEFAULT,
    ;

    companion object {
        fun fromString(value: String?): NovelEpubPageProgressionDirection {
            return when (value?.lowercase()) {
                "ltr" -> LTR
                "rtl" -> RTL
                else -> DEFAULT
            }
        }
    }
}

@Serializable
data class NovelEpubSpine(
    val id: String? = null,
    val toc: String? = null,
    val pageProgressionDirection: NovelEpubPageProgressionDirection = NovelEpubPageProgressionDirection.DEFAULT,
    val items: List<NovelEpubSpineItem> = emptyList(),
)

data class NovelEpubTocEntry(
    val id: String,
    val label: String,
    val href: String? = null,
    val children: List<NovelEpubTocEntry> = emptyList(),
)
