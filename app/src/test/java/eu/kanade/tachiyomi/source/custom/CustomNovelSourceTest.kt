package eu.kanade.tachiyomi.source.custom

import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.UpdateStrategy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CustomNovelSourceTest {

    @Test
    fun `rebase url replaces original host with custom host`() {
        assertEquals(
            "https://custom.example/chapter-1",
            rebaseCustomSourceUrl("https://old.example/chapter-1", "https://custom.example", "https://old.example"),
        )
        assertEquals(
            "https://custom.example/chapter-1",
            rebaseCustomSourceUrl("/chapter-1", "https://custom.example", "https://old.example"),
        )
    }

    @Test
    fun `rebasing manga chapter page and list uses custom base`() {
        val manga = SManga.create().also {
            it.title = "Novel"
            it.url = "https://old.example/novel"
            it.thumbnail_url = "https://old.example/cover.jpg"
            it.status = SManga.UNKNOWN
            it.update_strategy = UpdateStrategy.ALWAYS_UPDATE
            it.initialized = true
        }
        val chapter = SChapter.create().also {
            it.name = "Chapter 1"
            it.url = "https://old.example/chapter-1"
            it.date_upload = 0L
            it.chapter_number = 1f
            it.scanlator = null
            it.locked = false
        }
        val page = Page(0, "https://old.example/chapter-1")
        val mangasPage = MangasPage(listOf(manga), hasNextPage = true)

        assertEquals("https://custom.example/novel", rebaseCustomSourceManga(manga, "https://custom.example", "https://old.example").url)
        assertEquals("https://custom.example/cover.jpg", rebaseCustomSourceManga(manga, "https://custom.example", "https://old.example").thumbnail_url)
        assertEquals("https://custom.example/chapter-1", rebaseCustomSourceChapter(chapter, "https://custom.example", "https://old.example").url)
        assertEquals("https://custom.example/chapter-1", rebaseCustomSourcePage(page, "https://custom.example", "https://old.example").url)
        assertEquals(
            "https://custom.example/novel",
            rebaseCustomSourceMangasPage(mangasPage, "https://custom.example", "https://old.example").mangas.first().url,
        )
        assertTrue(rebaseCustomSourcePage(page, "https://custom.example", "https://old.example").url.startsWith("https://custom.example"))
    }

    @Test
    fun `toBaseSourceUrl restores delegated host from custom url`() {
        assertEquals(
            "https://old.example/series/test",
            mapCustomUrlToSourceUrl("https://custom.example/series/test", "https://custom.example", "https://old.example"),
        )
        assertEquals(
            "https://old.example/series/test",
            mapCustomUrlToSourceUrl("https://old.example/series/test", "https://custom.example", "https://old.example"),
        )
    }
}