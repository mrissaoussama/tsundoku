package tachiyomi.domain.manga.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject

/**
 * Per-field user overrides for a manga's source-fetched metadata. Stored inside the manga's
 * [Manga.memo] JSON under the [MEMO_KEY] key. A null field means "not overridden": the source
 * value flows through on refresh. Title and thumbnail are intentionally excluded: title keeps its
 * own update behaviour (and alternative titles), and the cover has its own custom-cover mechanism.
 */
@Serializable
data class CustomMangaInfo(
    val author: String? = null,
    val artist: String? = null,
    val description: String? = null,
    val genre: List<String>? = null,
    val status: Long? = null,
) {
    fun isEmpty(): Boolean =
        author == null && artist == null && description == null && genre == null && status == null

    companion object {
        const val MEMO_KEY = "customInfo"

        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
        }

        /** Decode the override stored in [memo], or null when none is set. */
        fun from(memo: JsonObject): CustomMangaInfo? {
            val element = memo[MEMO_KEY] ?: return null
            return try {
                json.decodeFromJsonElement(serializer(), element).takeUnless { it.isEmpty() }
            } catch (_: Exception) {
                null
            }
        }

        /**
         * Return a copy of [memo] with this override merged in under [MEMO_KEY], preserving every
         * other key. An empty/all-null override removes the key entirely.
         */
        fun CustomMangaInfo?.writeInto(memo: JsonObject): JsonObject = buildJsonObject {
            memo.forEach { (key, value) -> if (key != MEMO_KEY) put(key, value) }
            if (this@writeInto != null && !this@writeInto.isEmpty()) {
                put(MEMO_KEY, json.encodeToJsonElement(serializer(), this@writeInto))
            }
        }
    }
}
