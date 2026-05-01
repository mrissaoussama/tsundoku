package eu.kanade.tachiyomi.ui.library

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.manga.model.Manga

class LibraryItemTest {

    @Test
    fun `field prefixes are case-insensitive`() {
        val item = createLibraryItem(
            title = "The Hero Returns",
            genres = listOf("Fantasy", "Adventure"),
        )

        assertTrue(item.matches("TITLE:^the\\s+hero.*$", useRegex = true))
        assertTrue(item.matches("GENRE:FANTASY"))
    }

    @Test
    fun `search by url is respected when enabled`() {
        val item = createLibraryItem(
            title = "A Different Title",
            url = "/heroes/episode-1",
        )

        assertFalse(item.matches("heroes"))
        assertTrue(item.matches("heroes", searchByUrl = true))
    }

    @Test
    fun `regex matching is applied to field queries`() {
        val item = createLibraryItem(
            title = "The Final Empire",
            author = "Brandon Sanderson",
        )

        assertTrue(item.matches("author:^brandon\\s+sanderson$", useRegex = true))
        assertFalse(item.matches("author:^brandon\\s+stormlight$", useRegex = true))
    }

    private fun createLibraryItem(
        title: String,
        url: String = "/series",
        author: String? = null,
        genres: List<String>? = null,
    ): LibraryItem {
        val manga = Manga.create().copy(
            id = 1L,
            source = 1L,
            title = title,
            url = url,
            author = author,
            genre = genres,
            initialized = true,
        )
        return LibraryItem(
            libraryManga = LibraryManga(
                manga = manga,
                categories = emptyList(),
                totalChapters = 1,
                readCount = 0,
                bookmarkCount = 0,
                latestUpload = 0,
                chapterFetchedAt = 0,
                lastRead = 0,
            ),
        )
    }
}
