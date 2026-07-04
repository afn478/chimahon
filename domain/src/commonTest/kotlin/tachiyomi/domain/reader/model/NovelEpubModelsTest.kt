package tachiyomi.domain.reader.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NovelEpubModelsTest {
    @Test
    fun mediaTypeParsingMatchesKnownEpubValuesIgnoringCase() {
        assertEquals(NovelEpubMediaType.XHTML, NovelEpubMediaType.fromString("APPLICATION/XHTML+XML"))
        assertEquals(NovelEpubMediaType.CSS, NovelEpubMediaType.fromString("text/css"))
        assertEquals(NovelEpubMediaType.PNG, NovelEpubMediaType.fromString("image/png"))
        assertEquals(NovelEpubMediaType.UNKNOWN, NovelEpubMediaType.fromString(null))
        assertEquals(NovelEpubMediaType.UNKNOWN, NovelEpubMediaType.fromString("application/unknown"))
    }

    @Test
    fun pageProgressionDirectionParsingUsesDefaultForUnknownValues() {
        assertEquals(NovelEpubPageProgressionDirection.LTR, NovelEpubPageProgressionDirection.fromString("ltr"))
        assertEquals(NovelEpubPageProgressionDirection.RTL, NovelEpubPageProgressionDirection.fromString("RTL"))
        assertEquals(NovelEpubPageProgressionDirection.DEFAULT, NovelEpubPageProgressionDirection.fromString(null))
        assertEquals(NovelEpubPageProgressionDirection.DEFAULT, NovelEpubPageProgressionDirection.fromString("vertical"))
    }

    @Test
    fun manifestItemDefaultsToUnknownMediaType() {
        val item = NovelEpubManifestItem(
            id = "chapter-1",
            href = "chapter.xhtml",
        )

        assertEquals("chapter-1", item.id)
        assertEquals("chapter.xhtml", item.href)
        assertEquals(NovelEpubMediaType.UNKNOWN, item.mediaType)
        assertNull(item.properties)
    }

    @Test
    fun spineDefaultsToDefaultDirectionAndTextItems() {
        val spineItem = NovelEpubSpineItem(idref = "chapter-1")
        val spine = NovelEpubSpine(items = listOf(spineItem))

        assertEquals("chapter-1", spineItem.idref)
        assertEquals(true, spineItem.linear)
        assertEquals(NovelEpubSpineItemType.TEXT, spineItem.type)
        assertNull(spineItem.imageUrl)
        assertEquals(NovelEpubPageProgressionDirection.DEFAULT, spine.pageProgressionDirection)
        assertEquals(listOf(spineItem), spine.items)
    }

    @Test
    fun metadataAndTocModelsPreserveNestedValues() {
        val creator = NovelEpubCreator(
            name = "Writer",
            role = "aut",
            fileAs = "Writer, The",
        )
        val metadata = NovelEpubMetadata(
            title = "Novel title",
            language = "en",
            creator = creator,
            coverId = "cover",
        )
        val toc = NovelEpubTocEntry(
            id = "root",
            label = "Root",
            children = listOf(
                NovelEpubTocEntry(
                    id = "chapter-1",
                    label = "Chapter 1",
                    href = "chapter.xhtml",
                ),
            ),
        )

        assertEquals("Writer", metadata.creator?.name)
        assertEquals("cover", metadata.coverId)
        assertEquals("Chapter 1", toc.children.single().label)
        assertEquals("chapter.xhtml", toc.children.single().href)
    }
}
