package tachiyomi.source.local

import mihon.core.archive.EpubReader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EpubChapterListBuilderTest {

    @Test
    fun `buildEpubChaptersFromToc preserves fragment-based chapters`() {
        val tocChapters = listOf(
            EpubReader.EpubChapter(
                title = "Capitolo 1",
                href = "chapter.xhtml#one",
                order = 0,
            ),
            EpubReader.EpubChapter(
                title = "Capitolo 1",
                href = "chapter.xhtml#one",
                order = 1,
            ),
            EpubReader.EpubChapter(
                title = "Capitolo 2",
                href = "chapter.xhtml#two",
                order = 2,
            ),
        )

        val chapters = buildEpubChaptersFromToc(
            mangaUrl = "local-novels/book",
            chapterFileName = "volume.epub",
            chapterFileNameWithoutExtension = "volume",
            chapterLastModified = 123456789L,
            tocChapters = tocChapters,
            hasMultipleEpubFiles = false,
        )

        assertEquals(2, chapters.size)
        assertEquals("local-novels/book/volume.epub#chapter.xhtml#one", chapters[0].url)
        assertEquals("local-novels/book/volume.epub#chapter.xhtml#two", chapters[1].url)
        assertEquals("Capitolo 1", chapters[0].name)
        assertEquals("Capitolo 2", chapters[1].name)
        assertEquals(1f, chapters[0].chapter_number)
        assertEquals(2f, chapters[1].chapter_number)
        assertEquals(123456789L, chapters[0].date_upload)
    }
}
