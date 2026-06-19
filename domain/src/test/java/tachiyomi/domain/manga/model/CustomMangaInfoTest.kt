package tachiyomi.domain.manga.model

import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import tachiyomi.domain.manga.model.CustomMangaInfo.Companion.writeInto

@Execution(ExecutionMode.CONCURRENT)
class CustomMangaInfoTest {

    private fun emptyMemo() = JsonObject(emptyMap())

    @Test
    fun `from returns null when no override present`() {
        CustomMangaInfo.from(emptyMemo()) shouldBe null
    }

    @Test
    fun `round-trips through memo`() {
        val info = CustomMangaInfo(
            author = "Author",
            artist = "Artist",
            description = "Desc",
            genre = listOf("a", "b"),
            status = 2L,
        )
        val memo = info.writeInto(emptyMemo())
        CustomMangaInfo.from(memo) shouldBe info
    }

    @Test
    fun `writeInto preserves unrelated memo keys`() {
        val base = buildJsonObject { put("pluginData", "keep-me") }
        val memo = CustomMangaInfo(author = "A").writeInto(base)
        (memo["pluginData"] as? JsonPrimitive)?.content shouldBe "keep-me"
        CustomMangaInfo.from(memo)?.author shouldBe "A"
    }

    @Test
    fun `all-null override removes the key`() {
        val withOverride = CustomMangaInfo(author = "A").writeInto(buildJsonObject { put("x", "y") })
        withOverride.containsKey(CustomMangaInfo.MEMO_KEY) shouldBe true

        val cleared = (null as CustomMangaInfo?).writeInto(withOverride)
        cleared.containsKey(CustomMangaInfo.MEMO_KEY) shouldBe false
        // sibling key survives the clear
        (cleared["x"] as? JsonPrimitive)?.content shouldBe "y"
        CustomMangaInfo.from(cleared) shouldBe null
    }

    @Test
    fun `empty override is treated as no override`() {
        val memo = CustomMangaInfo().writeInto(emptyMemo())
        memo.containsKey(CustomMangaInfo.MEMO_KEY) shouldBe false
        CustomMangaInfo.from(memo) shouldBe null
    }

    @Test
    fun `partial override leaves other fields null so source flows through`() {
        val memo = CustomMangaInfo(status = 1L).writeInto(emptyMemo())
        val info = CustomMangaInfo.from(memo)!!
        info.status shouldBe 1L
        info.author shouldBe null
        info.genre shouldBe null
    }

    @Test
    fun `tolerates malformed customInfo`() {
        val memo = buildJsonObject { put(CustomMangaInfo.MEMO_KEY, JsonPrimitive("not-an-object")) }
        CustomMangaInfo.from(memo) shouldBe null
    }

    @Test
    fun `ignores unknown fields in stored json`() {
        val raw = """{"${CustomMangaInfo.MEMO_KEY}":{"author":"A","unknown":"z"}}"""
        val memo = Json.parseToJsonElement(raw) as JsonObject
        CustomMangaInfo.from(memo)?.author shouldBe "A"
    }
}
