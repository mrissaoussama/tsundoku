package eu.kanade.tachiyomi.ui.reader.viewer.text

object NovelMarkdownUtils {
    private val headingRegex = Regex("^(#{1,6})\\s+(.*)$")
    private val horizontalRuleRegex = Regex("^(?:-{3,}|\\*{3,})\\s*$")
    private val frontmatterTitleRegex = Regex("^title\\s*:\\s*(.+)$", RegexOption.IGNORE_CASE)

    fun isMarkdownUrl(url: String): Boolean {
        if (url.isBlank()) return false
        val normalized = url.substringBefore('#').lowercase()
        return normalized.endsWith(".md") || normalized.endsWith(".markdown")
    }

    fun toHtml(markdown: String): String {
        val normalized = markdown.replace("\r\n", "\n").replace("\r", "\n")
        val (frontmatterTitle, body) = extractFrontmatter(normalized)

        val out = StringBuilder()
        if (!frontmatterTitle.isNullOrBlank()) {
            out.append("<h1>")
                .append(escapeHtml(frontmatterTitle))
                .append("</h1>\n")
        }

        val paragraphBuffer = mutableListOf<String>()
        fun flushParagraph() {
            if (paragraphBuffer.isEmpty()) return
            val text = paragraphBuffer.joinToString(" ") { it.trim() }.trim()
            if (text.isNotEmpty()) {
                out.append("<p>")
                    .append(applyInlineMarkdown(text))
                    .append("</p>\n")
            }
            paragraphBuffer.clear()
        }

        body.lines().forEach { rawLine ->
            val trimmed = rawLine.trim()
            when {
                trimmed.isEmpty() -> flushParagraph()
                horizontalRuleRegex.matches(trimmed) -> {
                    flushParagraph()
                    out.append("<hr />\n")
                }
                else -> {
                    val heading = headingRegex.matchEntire(trimmed)
                    if (heading != null) {
                        flushParagraph()
                        val level = heading.groupValues[1].length.coerceIn(1, 6)
                        val headingText = applyInlineMarkdown(heading.groupValues[2].trim())
                        out.append("<h$level>")
                            .append(headingText)
                            .append("</h$level>\n")
                    } else {
                        paragraphBuffer += rawLine
                    }
                }
            }
        }
        flushParagraph()

        return out.toString().trim()
    }

    private fun extractFrontmatter(markdown: String): Pair<String?, String> {
        val lines = markdown.lines()
        if (lines.isEmpty() || lines.first().trim() != "---") {
            return null to markdown
        }

        var closingIndex = -1
        for (i in 1 until lines.size) {
            if (lines[i].trim() == "---") {
                closingIndex = i
                break
            }
        }
        if (closingIndex == -1) {
            return null to markdown
        }

        val title = lines
            .subList(1, closingIndex)
            .firstNotNullOfOrNull { line ->
                frontmatterTitleRegex.matchEntire(line.trim())
                    ?.groupValues
                    ?.getOrNull(1)
                    ?.trim()
                    ?.trim('"', '\'')
            }

        val body = lines.drop(closingIndex + 1).joinToString("\n")
        return title to body
    }

    private fun applyInlineMarkdown(text: String): String {
        val escaped = escapeHtml(text)
        return escaped
            .replace(Regex("\\*\\*(.+?)\\*\\*"), "<strong>$1</strong>")
            .replace(Regex("\\*(.+?)\\*"), "<em>$1</em>")
    }

    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }
}
