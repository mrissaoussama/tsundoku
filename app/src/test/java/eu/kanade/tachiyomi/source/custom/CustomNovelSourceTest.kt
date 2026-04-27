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

    @Test
    fun `rebase absolute url with sourceBase provided should convert host`() {
        // rebaseCustomSourceUrl is designed to convert URLs from sourceBase to customBase
        val absoluteUrl = "https://allnovelfull.com/library-of-heavens-path.html"
        val customBase = "https://custom.example"
        val sourceBase = "https://allnovelfull.com"
        
        // When sourceBase is provided and matches, it should convert
        assertEquals(
            "https://custom.example/library-of-heavens-path.html",
            rebaseCustomSourceUrl(absoluteUrl, customBase, sourceBase),
            "URL starting with sourceBase should be converted to custom base"
        )
    }

    @Test
    fun `rebase absolute url without sourceBase should return unchanged`() {
        // If sourceBase is NOT provided, absolute URLs should be returned as-is
        val absoluteUrl = "https://allnovelfull.com/library-of-heavens-path.html"
        val customBase = "https://custom.example"
        
        assertEquals(
            absoluteUrl,
            rebaseCustomSourceUrl(absoluteUrl, customBase, null),
            "Absolute URLs should be returned unchanged when no sourceBase provided"
        )
    }

    @Test
    fun `rebase with sourceBase prefix should convert host`() {
        val sourceUrl = "https://allnovelfull.com/library-of-heavens-path.html"
        val customBase = "https://custom.example"
        val sourceBase = "https://allnovelfull.com"
        
        // If it matches sourceBase, convert to custom base
        val result = rebaseCustomSourceUrl(sourceUrl, customBase, sourceBase)
        assertEquals("https://custom.example/library-of-heavens-path.html", result)
    }

    @Test
    fun `mapCustomUrlToSourceUrl should handle absolute urls correctly`() {
        val customUrl = "https://custom.example/library-of-heavens-path.html"
        val customBase = "https://custom.example"
        val sourceBase = "https://allnovelfull.com"
        
        val result = mapCustomUrlToSourceUrl(customUrl, customBase, sourceBase)
        assertEquals("https://allnovelfull.com/library-of-heavens-path.html", result)
    }

    @Test
    fun `round trip conversion custom to source and back`() {
        val customUrl = "https://custom.example/library-of-heavens-path.html"
        val customBase = "https://custom.example"
        val sourceBase = "https://allnovelfull.com"
        
        // Custom URL → Source URL
        val toSource = mapCustomUrlToSourceUrl(customUrl, customBase, sourceBase)
        assertEquals("https://allnovelfull.com/library-of-heavens-path.html", toSource)
        
        // Source URL → Custom URL
        val backToCustom = rebaseCustomSourceUrl(toSource, customBase, sourceBase)
        assertEquals(customUrl, backToCustom)
    }

    @Test
    fun `test allnovelfull scenario from runtime`() {
        // This mimics the actual scenario from the app
        val sourceBaseUrl = "https://allnovelfull.com"
        val customBaseUrl = "https://custom.example"
        
        // Scenario 1: Source returns absolute URL
        val sourceReturnedUrl = "https://allnovelfull.com/library-of-heavens-path.html"
        
        // When we rebase it to custom for display/storage
        val rebasedForCustom = rebaseCustomSourceUrl(sourceReturnedUrl, customBaseUrl, sourceBaseUrl)
        assertEquals(
            "https://custom.example/library-of-heavens-path.html",
            rebasedForCustom,
            "Source absolute URL should be converted to custom host"
        )
        
        // Scenario 2: We need to convert custom URL back to source for HTTP request
        val customStoredUrl = "https://custom.example/library-of-heavens-path.html"
        val convertedToSource = mapCustomUrlToSourceUrl(customStoredUrl, customBaseUrl, sourceBaseUrl)
        assertEquals(
            "https://allnovelfull.com/library-of-heavens-path.html",
            convertedToSource,
            "Custom URL should be converted back to source host"
        )
        
        // Scenario 3: The interceptor should then rewrite source host back to custom for the actual request
        val sourceUrl = convertedToSource
        val finalInterceptedUrl = if (sourceUrl != null && sourceUrl.startsWith(sourceBaseUrl)) {
            customBaseUrl + sourceUrl.removePrefix(sourceBaseUrl)
        } else {
            sourceUrl
        }
        assertEquals(
            "https://custom.example/library-of-heavens-path.html",
            finalInterceptedUrl,
            "Interceptor should rewrite source URLs to custom base"
        )
    }

    @Test
    fun `interceptor behavior when HttpSource baseUrl is preserved`() {
        // The interceptor is set up to convert sourceBase -> customBase
        val sourceBaseUrl = "https://allnovelfull.com"
        val customBaseUrl = "https://custom.example"
        
        // If HttpSource keeps its original baseUrl, requests will be built as:
        val httpSourceBuiltUrl = "https://allnovelfull.com/library-of-heavens-path.html"
        
        // Interceptor logic:
        val interceptorResult = if (httpSourceBuiltUrl.startsWith(sourceBaseUrl)) {
            customBaseUrl + httpSourceBuiltUrl.removePrefix(sourceBaseUrl)
        } else {
            httpSourceBuiltUrl
        }
        
        assertEquals(
            "https://custom.example/library-of-heavens-path.html",
            interceptorResult,
            "Interceptor correctly rewrites original sourceBase URL to custom"
        )
    }

    @Test
    fun `problem HttpSource baseUrl changed to custom breaks interceptor`() {
        // THE BUG: When we set HttpSource.baseUrl = customBaseUrl
        val sourceBaseUrl = "https://allnovelfull.com"
        val customBaseUrl = "https://custom.example"
        val modifiedHttpSourceBaseUrl = customBaseUrl // <- This is what patchHttpSourceForCustomBaseUrl does!
        
        // HttpSource will now build requests as:
        val httpSourceBuiltUrl = modifiedHttpSourceBaseUrl + "/library-of-heavens-path.html"
        
        // Interceptor tries to match and convert:
        val interceptorResult = if (httpSourceBuiltUrl.startsWith(sourceBaseUrl)) {
            customBaseUrl + httpSourceBuiltUrl.removePrefix(sourceBaseUrl)
        } else {
            httpSourceBuiltUrl // <- Returns unchanged!
        }
        
        assertEquals(
            "https://custom.example/library-of-heavens-path.html",
            interceptorResult,
            "This should work, but only by luck since URL is already custom"
        )
        
        // But the REAL problem: what if the URL is a relative path?
        val relativeUrl = "/library-of-heavens-path.html"
        val httpSourceBuiltFromRelative = modifiedHttpSourceBaseUrl + relativeUrl
        
        // Interceptor sees this and since it doesn't start with sourceBase, leaves it alone
        val interceptorResultFromRelative = if (httpSourceBuiltFromRelative.startsWith(sourceBaseUrl)) {
            customBaseUrl + httpSourceBuiltFromRelative.removePrefix(sourceBaseUrl)
        } else {
            httpSourceBuiltFromRelative
        }
        
        assertEquals(
            "https://custom.example/library-of-heavens-path.html",
            interceptorResultFromRelative,
            "This also works, but again only by accident"
        )
    }
}