package eu.kanade.domain.manga.model

/**
 * Word statistics for the downloaded chapters of a novel.
 *
 * @property totalWords words across every counted chapter.
 * @property countedChapters chapters whose content could be read and counted.
 * @property totalChapters every chapter of the entry, downloaded or not.
 * @property unreadableChapters chapters that were available but whose content could not be read.
 */
data class WordDensity(
    val totalWords: Long,
    val countedChapters: Int,
    val totalChapters: Int,
    val unreadableChapters: Int = 0,
) {
    /** Chapters left out because they are not downloaded. */
    val notDownloadedChapters: Int
        get() = (totalChapters - countedChapters - unreadableChapters).coerceAtLeast(0)

    /** Average words per counted chapter, the novel's word density. */
    val averageWords: Long
        get() = if (countedChapters == 0) 0 else totalWords / countedChapters

    val tier: Int
        get() = densityTier(averageWords)

    companion object {
        /** Each tier spans this many average words per chapter. */
        const val TIER_STEP_WORDS = 500L

        private val SKIPPED_ELEMENTS = setOf("script", "style", "title", "head")
        private val INLINE_ELEMENTS = setOf(
            "a", "b", "i", "u", "em", "strong", "span", "small", "sup", "sub", "font", "mark", "s", "ruby", "rt", "rp",
        )
        private val SPACE_ENTITIES = setOf("nbsp", "#160", "#xa0", "ensp", "emsp", "thinsp", "#8203", "#x200b")

        /**
         * Maps an average chapter length onto a tier: 1-500 words is 1, 501-1000 is 2,
         * 1001-1500 is 3, and so on without an upper bound.
         */
        fun densityTier(averageWords: Long): Int {
            if (averageWords <= 0) return 1
            return ((averageWords - 1) / TIER_STEP_WORDS + 1).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        }

        /** Lowest average word count that lands in [tier]. */
        fun tierMinWords(tier: Int): Long = if (tier <= 1) 1 else (tier - 1) * TIER_STEP_WORDS + 1

        /** Highest average word count that lands in [tier]. */
        fun tierMaxWords(tier: Int): Long = tier.coerceAtLeast(1) * TIER_STEP_WORDS

        /**
         * Counts whitespace-separated words in chapter HTML or plain text in a single pass.
         * Markup, comments, scripts and styles are skipped, block-level tags and `&nbsp;`
         * style entities separate words, and other entities count as a word character.
         */
        fun countWords(content: String): Int {
            val length = content.length
            var words = 0
            var inWord = false
            var i = 0
            while (i < length) {
                val char = content[i]
                when {
                    char == '<' && i + 1 < length && isTagStart(content[i + 1]) -> {
                        if (content.startsWith("<!--", i)) {
                            val end = content.indexOf("-->", i + 4)
                            i = if (end < 0) length else end + 3
                            inWord = false
                            continue
                        }
                        val end = content.indexOf('>', i)
                        if (end < 0) break
                        var nameStart = i + 1
                        val closing = content[nameStart] == '/'
                        if (closing) nameStart++
                        var nameEnd = nameStart
                        while (nameEnd < end && content[nameEnd].isLetterOrDigit()) nameEnd++
                        val name = content.substring(nameStart, nameEnd).lowercase()
                        i = end + 1
                        if (!closing && name in SKIPPED_ELEMENTS && content[end - 1] != '/') {
                            val close = content.indexOf("</$name", i, ignoreCase = true)
                            i = if (close < 0) {
                                length
                            } else {
                                content.indexOf('>', close).let { if (it < 0) length else it + 1 }
                            }
                        }
                        if (name !in INLINE_ELEMENTS) inWord = false
                    }
                    char == '&' -> {
                        val semi = content.indexOf(';', i + 1)
                        val entity = if (semi in (i + 2)..(i + 9)) content.substring(i + 1, semi).lowercase() else null
                        if (entity != null && entity in SPACE_ENTITIES) {
                            inWord = false
                            i = semi + 1
                        } else {
                            if (!inWord) words++
                            inWord = true
                            i = if (entity != null) semi + 1 else i + 1
                        }
                    }
                    // isSpaceChar also covers no-break spaces
                    char.isWhitespace() || Character.isSpaceChar(char) -> {
                        inWord = false
                        i++
                    }
                    else -> {
                        if (!inWord) words++
                        inWord = true
                        i++
                    }
                }
            }
            return words
        }

        private fun isTagStart(char: Char) = char.isLetter() || char == '/' || char == '!' || char == '?'
    }
}
