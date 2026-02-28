package eu.kanade.tachiyomi.data.backup.restore

import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

@Execution(ExecutionMode.CONCURRENT)
class LNReaderBackupImporterTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Test
    fun `LNChapter bookmark as integer 0 should deserialize correctly`() {
        val jsonStr = """{"id":1,"novelId":1,"path":"/ch1","name":"Chapter 1","bookmark":0,"unread":1}"""
        val chapter = json.decodeFromString<LNReaderBackupImporter.LNChapter>(jsonStr)
        chapter.bookmark shouldBe 0
        chapter.unread shouldBe 1
    }

    @Test
    fun `LNChapter bookmark as integer 1 should deserialize correctly`() {
        val jsonStr = """{"id":2,"novelId":1,"path":"/ch2","name":"Chapter 2","bookmark":1,"unread":0}"""
        val chapter = json.decodeFromString<LNReaderBackupImporter.LNChapter>(jsonStr)
        chapter.bookmark shouldBe 1
        chapter.unread shouldBe 0
    }

    @Test
    fun `LNChapter boolean conversion - bookmark 1 means bookmarked`() {
        val chapter = LNReaderBackupImporter.LNChapter(bookmark = 1, unread = 0)
        val isBookmarked = chapter.bookmark != 0
        val isRead = chapter.unread == 0
        isBookmarked shouldBe true
        isRead shouldBe true
    }

    @Test
    fun `LNChapter boolean conversion - bookmark 0 means not bookmarked`() {
        val chapter = LNReaderBackupImporter.LNChapter(bookmark = 0, unread = 1)
        val isBookmarked = chapter.bookmark != 0
        val isRead = chapter.unread == 0
        isBookmarked shouldBe false
        isRead shouldBe false
    }

    @Test
    fun `LNChapter defaults should be sensible`() {
        val chapter = LNReaderBackupImporter.LNChapter()
        chapter.bookmark shouldBe 0
        chapter.unread shouldBe 1
    }

    @Test
    fun `LNNovel should deserialize with chapters containing int booleans`() {
        val jsonStr = """
        {
            "id": 1,
            "path": "/novel/test",
            "pluginId": "test-plugin",
            "name": "Test Novel",
            "inLibrary": 1,
            "chapters": [
                {
                    "id": 1,
                    "novelId": 1,
                    "path": "/ch1",
                    "name": "Chapter 1",
                    "bookmark": 0,
                    "unread": 1
                },
                {
                    "id": 2,
                    "novelId": 1,
                    "path": "/ch2",
                    "name": "Chapter 2",
                    "bookmark": 1,
                    "unread": 0,
                    "chapterNumber": 2.0
                }
            ]
        }
        """.trimIndent()

        val novel = json.decodeFromString<LNReaderBackupImporter.LNNovel>(jsonStr)
        novel.name shouldBe "Test Novel"
        novel.chapters.size shouldBe 2
        novel.chapters[0].bookmark shouldBe 0
        novel.chapters[0].unread shouldBe 1
        novel.chapters[1].bookmark shouldBe 1
        novel.chapters[1].unread shouldBe 0
        novel.chapters[1].chapterNumber shouldBe 2.0f
    }

    @Test
    fun `LNReader device-local cover path starting with Novels should be recognized`() {
        val cover = "/Novels/novelfire/818/cover.png"
        val result = if (cover.startsWith("/Novels/") || cover.startsWith("/storage/")) null else cover
        result shouldBe null
    }

    @Test
    fun `LNReader device-local cover path starting with storage should be recognized`() {
        val cover = "/storage/emulated/0/Android/data/com.rajarsheechatterjee.LNReader/files/Novels/local/922/bookcover-generated.jpg"
        val result = if (cover.startsWith("/Novels/") || cover.startsWith("/storage/")) null else cover
        result shouldBe null
    }

    @Test
    fun `LNReader HTTP cover URL should be preserved`() {
        val cover = "https://example.com/covers/novel-cover.jpg"
        val result = if (cover.startsWith("/Novels/") || cover.startsWith("/storage/")) null else cover
        result shouldBe "https://example.com/covers/novel-cover.jpg"
    }

    @Test
    fun `LNReader null cover should stay null`() {
        val cover: String? = null
        val result = cover?.let {
            if (it.startsWith("/Novels/") || it.startsWith("/storage/")) null else it
        }
        result shouldBe null
    }

    @Test
    fun `LNReader empty cover should be preserved as-is`() {
        val cover = ""
        val result = if (cover.startsWith("/Novels/") || cover.startsWith("/storage/")) null else cover
        result shouldBe ""
    }

    @Test
    fun `LNNovel cover field deserializes correctly`() {
        val jsonStr = """
        {
            "id": 1,
            "path": "/novel/test",
            "pluginId": "novelfire",
            "name": "Test",
            "cover": "/Novels/novelfire/818/cover.png",
            "inLibrary": 1,
            "chapters": []
        }
        """.trimIndent()

        val novel = json.decodeFromString<LNReaderBackupImporter.LNNovel>(jsonStr)
        novel.cover shouldBe "/Novels/novelfire/818/cover.png"
    }
}
