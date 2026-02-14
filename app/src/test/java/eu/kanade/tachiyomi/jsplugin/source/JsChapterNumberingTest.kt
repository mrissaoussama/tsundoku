package eu.kanade.tachiyomi.jsplugin.source

import eu.kanade.tachiyomi.source.model.SChapter
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

/**
 * Tests for JS chapter numbering logic extracted from JsSource.getChapterList().
 *
 * The algorithm assigns sequential numbers matching position:
 *   index 0 → chapter_number 1.0
 *   index 1 → chapter_number 2.0
 *   ...
 *
 * This ensures the oldest chapter (first in the list) gets the lowest number,
 * matching the natural reading order.
 */
@Execution(ExecutionMode.CONCURRENT)
class JsChapterNumberingTest {

    private fun assignChapterNumbers(chapters: List<SChapter>): List<SChapter> {
        val total = chapters.size
        chapters.forEachIndexed { index, chapter ->
            if (chapter.chapter_number < 0 || total > 1) {
                chapter.chapter_number = (index + 1).toFloat()
            }
        }
        return chapters
    }

    private fun createChapter(name: String, number: Float = -1f): SChapter {
        return SChapter.create().apply {
            this.name = name
            this.url = "/chapter/$name"
            this.chapter_number = number
        }
    }

    @Test
    fun `first chapter gets number 1`() {
        val chapters = listOf(
            createChapter("Chapter 1"),
            createChapter("Chapter 2"),
            createChapter("Chapter 3"),
        )
        assignChapterNumbers(chapters)
        chapters[0].chapter_number shouldBe 1f
    }

    @Test
    fun `last chapter gets number equal to total`() {
        val chapters = listOf(
            createChapter("Chapter 1"),
            createChapter("Chapter 2"),
            createChapter("Chapter 3"),
        )
        assignChapterNumbers(chapters)
        chapters[2].chapter_number shouldBe 3f
    }

    @Test
    fun `sequential numbering for typical novel with many chapters`() {
        val chapters = (1..100).map { createChapter("Chapter $it") }
        assignChapterNumbers(chapters)

        chapters.forEachIndexed { index, chapter ->
            chapter.chapter_number shouldBe (index + 1).toFloat()
        }
    }

    @Test
    fun `single chapter gets number 1 when default number is negative`() {
        val chapters = listOf(createChapter("Only Chapter"))
        assignChapterNumbers(chapters)
        chapters[0].chapter_number shouldBe 1f
    }

    @Test
    fun `single chapter preserves plugin-provided positive number`() {
        val chapters = listOf(createChapter("Prologue", number = 0f))
        assignChapterNumbers(chapters)
        chapters[0].chapter_number shouldBe 0f
    }

    @Test
    fun `overrides plugin-provided numbers when multiple chapters exist`() {
        val chapters = listOf(
            createChapter("Chapter 1", number = 100f),
            createChapter("Chapter 2", number = 200f),
        )
        assignChapterNumbers(chapters)
        chapters[0].chapter_number shouldBe 1f
        chapters[1].chapter_number shouldBe 2f
    }

    @Test
    fun `chapter numbers reflect oldest-first source order`() {
        val chapters = listOf(
            createChapter("Chapter 1 - First Published"),
            createChapter("Chapter 2 - Second Published"),
            createChapter("Chapter 3 - Third Published"),
            createChapter("Chapter 4 - Most Recent"),
        )
        assignChapterNumbers(chapters)
        chapters[0].chapter_number shouldBe 1f
        chapters[3].chapter_number shouldBe 4f
    }

    @Test
    fun `large chapter list numbering is correct at boundaries`() {
        val count = 4851
        val chapters = (1..count).map { createChapter("Chapter $it") }
        assignChapterNumbers(chapters)

        chapters[0].chapter_number shouldBe 1f
        chapters[count - 1].chapter_number shouldBe count.toFloat()
        chapters[2425].chapter_number shouldBe 2426f
    }
}
