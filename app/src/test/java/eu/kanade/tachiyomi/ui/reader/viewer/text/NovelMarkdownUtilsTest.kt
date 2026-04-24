package eu.kanade.tachiyomi.ui.reader.viewer.text

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NovelMarkdownUtilsTest {

    @Test
    fun `isMarkdownUrl detects md and markdown extensions`() {
        assertTrue(NovelMarkdownUtils.isMarkdownUrl("localnovel/chapter.md"))
        assertTrue(NovelMarkdownUtils.isMarkdownUrl("localnovel/chapter.markdown"))
        assertTrue(NovelMarkdownUtils.isMarkdownUrl("localnovel/chapter.md#section-1"))
    }

    @Test
    fun `toHtml converts headings hr and inline emphasis`() {
        val markdown = """
            # Chapter 1

            Intro *italic* and **bold**

            ---

            Next paragraph.
        """.trimIndent()

        val html = NovelMarkdownUtils.toHtml(markdown)

        assertTrue(html.contains("<h1>Chapter 1</h1>"))
        assertTrue(html.contains("<em>italic</em>"))
        assertTrue(html.contains("<strong>bold</strong>"))
        assertTrue(html.contains("<hr />"))
    }

    @Test
    fun `toHtml extracts frontmatter title`() {
        val markdown = """
            ---
            title: Frontmatter Title
            translator: Someone
            ---

            Body line.
        """.trimIndent()

        val html = NovelMarkdownUtils.toHtml(markdown)

        assertTrue(html.startsWith("<h1>Frontmatter Title</h1>"))
        assertTrue(html.contains("<p>Body line.</p>"))
    }

    @Test
    fun `toHtml escapes unsafe html in plain text`() {
        val markdown = "Use <script>alert(1)</script> and <tag>."

        val html = NovelMarkdownUtils.toHtml(markdown)

        assertEquals("<p>Use &lt;script&gt;alert(1)&lt;/script&gt; and &lt;tag&gt;.</p>", html)

    @Test
    fun `normalizeContentForHtml keeps txt content as escaped plain text`() {
        val content = "Line 1\n\n<punch>\n  indented line"

        val html = NovelViewerTextUtils.normalizeContentForHtml(content, "chapter.txt")

        assertTrue(html.contains("data-tsundoku-plain-text=\"1\""))
        assertTrue(html.contains("white-space: pre-wrap"))
        assertTrue(html.contains("&lt;punch&gt;"))
        assertTrue(html.contains("indented line"))
    }

    @Test
    fun `normalizeContentForHtml treats angle bracket words as plain text when not html`() {
        val content = "<punch> is not an html tag"

        val html = NovelViewerTextUtils.normalizeContentForHtml(content, "chapter")

        assertTrue(html.contains("data-tsundoku-plain-text=\"1\""))
        assertTrue(html.contains("&lt;punch&gt; is not an html tag"))
    }
}
