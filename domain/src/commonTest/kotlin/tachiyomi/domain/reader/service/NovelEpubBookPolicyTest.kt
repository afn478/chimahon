package tachiyomi.domain.reader.service

import tachiyomi.domain.reader.model.NovelEpubManifest
import tachiyomi.domain.reader.model.NovelEpubManifestItem
import tachiyomi.domain.reader.model.NovelEpubMetadata
import tachiyomi.domain.reader.model.NovelEpubSpine
import tachiyomi.domain.reader.model.NovelEpubSpineItem
import tachiyomi.domain.reader.model.NovelEpubSpineItemType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NovelEpubBookPolicyTest {
    @Test
    fun coverHrefUsesCoverIdAndContentDirectory() {
        val result = NovelEpubBookPolicy.coverHref(
            metadata = NovelEpubMetadata(coverId = "cover-image"),
            manifest = manifest(
                "cover-image" to "images/cover.jpg",
            ),
            contentDirectory = "OEBPS/",
        )

        assertEquals("OEBPS/images/cover.jpg", result)
    }

    @Test
    fun coverHrefReturnsNullWhenCoverIdOrManifestItemIsMissing() {
        val manifest = manifest("chapter-1" to "chapter.xhtml")

        assertNull(
            NovelEpubBookPolicy.coverHref(
                metadata = NovelEpubMetadata(coverId = null),
                manifest = manifest,
                contentDirectory = "",
            ),
        )
        assertNull(
            NovelEpubBookPolicy.coverHref(
                metadata = NovelEpubMetadata(coverId = "missing"),
                manifest = manifest,
                contentDirectory = "",
            ),
        )
    }

    @Test
    fun linearSpineItemsFiltersNonLinearEntries() {
        val linear = spineItem(idref = "chapter-1")
        val nonLinear = spineItem(idref = "nav", linear = false)

        val result = NovelEpubBookPolicy.linearSpineItems(
            NovelEpubSpine(items = listOf(nonLinear, linear)),
        )

        assertEquals(listOf(linear), result)
    }

    @Test
    fun chapterHrefUsesLinearSpineIndexAndContentDirectory() {
        val result = NovelEpubBookPolicy.chapterHref(
            index = 1,
            spine = NovelEpubSpine(
                items = listOf(
                    spineItem(idref = "nav", linear = false),
                    spineItem(idref = "chapter-1"),
                    spineItem(idref = "chapter-2"),
                ),
            ),
            manifest = manifest(
                "chapter-1" to "chapter-1.xhtml",
                "chapter-2" to "chapter-2.xhtml",
            ),
            contentDirectory = "item/xhtml/",
        )

        assertEquals("item/xhtml/chapter-2.xhtml", result)
    }

    @Test
    fun chapterHrefReturnsNullForMissingIndexOrManifestItem() {
        val spine = NovelEpubSpine(items = listOf(spineItem(idref = "missing")))

        assertNull(
            NovelEpubBookPolicy.chapterHref(
                index = 1,
                spine = spine,
                manifest = manifest(),
                contentDirectory = "",
            ),
        )
        assertNull(
            NovelEpubBookPolicy.chapterHref(
                index = 0,
                spine = spine,
                manifest = manifest(),
                contentDirectory = "",
            ),
        )
    }

    @Test
    fun imageUrlReturnsCachedUrlOnlyForImageOnlySpineItem() {
        val spine = NovelEpubSpine(
            items = listOf(
                spineItem(
                    idref = "image-page",
                    type = NovelEpubSpineItemType.IMAGE_ONLY,
                    imageUrl = "file:///book/page.jpg",
                ),
                spineItem(
                    idref = "chapter-1",
                    type = NovelEpubSpineItemType.TEXT,
                    imageUrl = "file:///book/chapter.jpg",
                ),
            ),
        )

        assertEquals("file:///book/page.jpg", NovelEpubBookPolicy.imageUrl(index = 0, spine = spine))
        assertNull(NovelEpubBookPolicy.imageUrl(index = 1, spine = spine))
        assertNull(NovelEpubBookPolicy.imageUrl(index = 2, spine = spine))
    }

    @Test
    fun isImageOnlySpineHrefMatchesNativeImageSpineFiles() {
        assertEquals(true, NovelEpubBookPolicy.isImageOnlySpineHref("images/page.JPG"))
        assertEquals(true, NovelEpubBookPolicy.isImageOnlySpineHref("images/page.jpeg?cache=1#fragment"))
        assertEquals(true, NovelEpubBookPolicy.isImageOnlySpineHref("images/page.png"))
        assertEquals(true, NovelEpubBookPolicy.isImageOnlySpineHref("images/page.webp"))
        assertEquals(false, NovelEpubBookPolicy.isImageOnlySpineHref("images/page.gif"))
        assertEquals(false, NovelEpubBookPolicy.isImageOnlySpineHref("text/chapter.xhtml"))
        assertEquals(false, NovelEpubBookPolicy.isImageOnlySpineHref(null))
    }

    private fun manifest(vararg items: Pair<String, String>): NovelEpubManifest {
        return NovelEpubManifest(
            items = items.associate { (id, href) ->
                id to NovelEpubManifestItem(
                    id = id,
                    href = href,
                )
            },
        )
    }

    private fun spineItem(
        idref: String,
        linear: Boolean = true,
        type: NovelEpubSpineItemType = NovelEpubSpineItemType.TEXT,
        imageUrl: String? = null,
    ): NovelEpubSpineItem {
        return NovelEpubSpineItem(
            idref = idref,
            linear = linear,
            type = type,
            imageUrl = imageUrl,
        )
    }
}
