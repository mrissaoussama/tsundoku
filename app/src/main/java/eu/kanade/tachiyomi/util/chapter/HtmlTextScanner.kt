package eu.kanade.tachiyomi.util.chapter

/**
 * Single-pass walk over chapter HTML or plain text that avoids building a DOM.
 * Comments, scripts, styles and the head are skipped, and entities are decoded.
 */
object HtmlTextScanner {

    private val SKIPPED_ELEMENTS = setOf("script", "style", "title", "head")
    private val INLINE_ELEMENTS = setOf(
        "a", "b", "i", "u", "em", "strong", "span", "small", "sup", "sub", "font", "mark", "s", "ruby", "rt", "rp",
    )
    private val NAMED_ENTITIES = mapOf(
        "amp" to '&',
        "lt" to '<',
        "gt" to '>',
        "quot" to '"',
        "apos" to '\'',
        "nbsp" to ' ',
        "ensp" to ' ',
        "emsp" to ' ',
        "thinsp" to ' ',
        "hellip" to '…',
        "mdash" to '—',
        "ndash" to '–',
        "lsquo" to '‘',
        "rsquo" to '’',
        "ldquo" to '“',
        "rdquo" to '”',
    )
    private const val MAX_ENTITY_LENGTH = 10

    /** Calls [onChar] for each text character and [onBreak] where a block-level tag or comment splits the text. */
    inline fun scan(content: String, onChar: (Char) -> Unit, onBreak: () -> Unit) {
        val length = content.length
        var i = 0
        while (i < length) {
            val char = content[i]
            if (char == '<' && i + 1 < length && isTagStart(content[i + 1])) {
                if (isBlockBoundary(content, i)) onBreak()
                i = indexAfterTag(content, i)
            } else if (char == '&') {
                val entity = decodeEntityAt(content, i)
                if (entity >= 0) {
                    onChar(entity.toInt().toChar())
                    i = (entity shr 16).toInt()
                } else {
                    onChar(char)
                    i++
                }
            } else {
                onChar(char)
                i++
            }
        }
    }

    @PublishedApi
    internal fun isTagStart(char: Char) = char.isLetter() || char == '/' || char == '!' || char == '?'

    @PublishedApi
    internal fun isBlockBoundary(content: String, start: Int): Boolean {
        if (content.startsWith("<!--", start)) return true
        return tagName(content, start) !in INLINE_ELEMENTS
    }

    /** Index just past the tag at [start], including the body of a skipped element. [content].length if unterminated. */
    @PublishedApi
    internal fun indexAfterTag(content: String, start: Int): Int {
        val length = content.length
        if (content.startsWith("<!--", start)) {
            val end = content.indexOf("-->", start + 4)
            return if (end < 0) length else end + 3
        }
        val end = content.indexOf('>', start)
        if (end < 0) return length
        val closing = content[start + 1] == '/'
        val name = tagName(content, start)
        if (closing || name !in SKIPPED_ELEMENTS || content[end - 1] == '/') return end + 1
        val close = content.indexOf("</$name", end + 1, ignoreCase = true)
        if (close < 0) return length
        val closeEnd = content.indexOf('>', close)
        return if (closeEnd < 0) length else closeEnd + 1
    }

    /** Packs the decoded character (low 16 bits) and the index after the entity, or -1 if there is no entity at [start]. */
    @PublishedApi
    internal fun decodeEntityAt(content: String, start: Int): Long {
        var semi = -1
        val limit = minOf(content.length - 1, start + MAX_ENTITY_LENGTH)
        for (j in start + 2..limit) {
            if (content[j] == ';') {
                semi = j
                break
            }
        }
        if (semi < 0) return -1
        val decoded = decodeEntity(content.substring(start + 1, semi)) ?: return -1
        return ((semi + 1).toLong() shl 16) or decoded.code.toLong()
    }

    private fun tagName(content: String, start: Int): String {
        var nameStart = start + 1
        if (content[nameStart] == '/') nameStart++
        var nameEnd = nameStart
        while (nameEnd < content.length && content[nameEnd].isLetterOrDigit()) nameEnd++
        return content.substring(nameStart, nameEnd).lowercase()
    }

    private fun decodeEntity(name: String): Char? {
        if (name.startsWith("#")) {
            val code = if (name.length > 1 && (name[1] == 'x' || name[1] == 'X')) {
                name.substring(2).toIntOrNull(16)
            } else {
                name.substring(1).toIntOrNull()
            }
            return code?.takeIf { it in 1..0xFFFF }?.toChar()
        }
        return NAMED_ENTITIES[name]
    }
}
