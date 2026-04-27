package tachiyomi.source.local

import eu.kanade.tachiyomi.source.model.SChapter
import mihon.core.archive.EpubReader

internal fun buildEpubChaptersFromToc(
    mangaUrl: String,
    chapterFileName: String?,
    chapterFileNameWithoutExtension: String?,
    chapterLastModified: Long,
    tocChapters: List<EpubReader.EpubChapter>,
    hasMultipleEpubFiles: Boolean,
): List<SChapter> {
    if (tocChapters.isEmpty()) return emptyList()

    val emittedUrls = linkedSetOf<String>()
    var chapterNumber = 0

    return tocChapters.mapNotNull { tocEntry ->
        val chapterHref = tocEntry.href.trim()
        if (chapterHref.isBlank() || !emittedUrls.add(chapterHref)) {
            return@mapNotNull null
        }

        chapterNumber += 1
        val resolvedTitle = tocEntry.title.trim().ifBlank { "Chapter $chapterNumber" }
        val chapterDisplayName = if (hasMultipleEpubFiles) {
            "${chapterFileNameWithoutExtension.orEmpty()} - $resolvedTitle"
        } else {
            resolvedTitle
        }

        SChapter.create().apply {
            url = "$mangaUrl/${chapterFileName.orEmpty()}#$chapterHref"
            name = chapterDisplayName
            date_upload = chapterLastModified
            chapter_number = chapterNumber.toFloat()
        }
    }
}
