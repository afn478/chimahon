package tachiyomi.domain.reader.service

import tachiyomi.domain.reader.model.NovelEpubManifest
import tachiyomi.domain.reader.model.NovelEpubMetadata
import tachiyomi.domain.reader.model.NovelEpubSpine
import tachiyomi.domain.reader.model.NovelEpubSpineItem
import tachiyomi.domain.reader.model.NovelEpubSpineItemType

object NovelEpubBookPolicy {
    private val imageOnlySpineExtensions = setOf("jpg", "jpeg", "png", "webp")

    fun coverHref(
        metadata: NovelEpubMetadata,
        manifest: NovelEpubManifest,
        contentDirectory: String,
    ): String? {
        val coverId = metadata.coverId ?: return null
        val manifestItem = manifest.items[coverId] ?: return null
        return contentHref(
            contentDirectory = contentDirectory,
            href = manifestItem.href,
        )
    }

    fun linearSpineItems(spine: NovelEpubSpine): List<NovelEpubSpineItem> {
        return spine.items.filter { it.linear }
    }

    fun chapterHref(
        index: Int,
        spine: NovelEpubSpine,
        manifest: NovelEpubManifest,
        contentDirectory: String,
    ): String? {
        val spineItem = linearSpineItems(spine).getOrNull(index) ?: return null
        val manifestItem = manifest.items[spineItem.idref] ?: return null
        return contentHref(
            contentDirectory = contentDirectory,
            href = manifestItem.href,
        )
    }

    fun imageUrl(
        index: Int,
        spine: NovelEpubSpine,
    ): String? {
        return linearSpineItems(spine)
            .getOrNull(index)
            ?.takeIf { it.type == NovelEpubSpineItemType.IMAGE_ONLY }
            ?.imageUrl
    }

    fun isImageOnlySpineHref(href: String?): Boolean {
        if (href == null) return false

        val extension = href
            .substringBefore("#")
            .substringBefore("?")
            .substringAfterLast("/", missingDelimiterValue = "")
            .substringAfterLast(".", missingDelimiterValue = "")

        return extension.lowercase() in imageOnlySpineExtensions
    }

    private fun contentHref(
        contentDirectory: String,
        href: String,
    ): String {
        return if (contentDirectory.isNotEmpty()) {
            "$contentDirectory$href"
        } else {
            href
        }
    }
}
