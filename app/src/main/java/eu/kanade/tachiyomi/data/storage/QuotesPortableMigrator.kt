package eu.kanade.tachiyomi.data.storage

import android.app.Application
import eu.kanade.tachiyomi.ui.reader.quote.NovelQuotes
import eu.kanade.tachiyomi.ui.reader.quote.QuoteManager
import kotlinx.serialization.json.Json
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.manga.repository.MangaRepository
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.storage.service.StorageManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Moves quotes from the legacy id-keyed layout (quotes/novel_{id}.json) to the portable
 * source/title layout used by [QuoteManager]. Invoked manually from Advanced settings;
 * idempotent (only touches files still in the legacy layout).
 */
object QuotesPortableMigrator {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun run(): Int {
        val context = Injekt.get<Application>()
        val storageManager = Injekt.get<StorageManager>()
        val mangaRepository = Injekt.get<MangaRepository>()
        val sourceManager = Injekt.get<SourceManager>()

        val quotesDir = storageManager.getQuotesDirectory() ?: return 0
        val legacyFiles = quotesDir.listFiles()
            ?.filter { f -> f.name?.let { it.startsWith("novel_") && it.endsWith(".json") } == true }
            ?: return 0

        val quoteManager = QuoteManager(context)
        var migrated = 0
        for (file in legacyFiles) {
            val name = file.name ?: continue
            val novelId = name.removePrefix("novel_").removeSuffix(".json").toLongOrNull() ?: continue
            try {
                val content = file.openInputStream().use { String(it.readBytes()) }
                val quotes = json.decodeFromString<NovelQuotes>(content).quotes
                if (quotes.isEmpty()) {
                    file.delete()
                    continue
                }
                val manga = mangaRepository.getMangaById(novelId)
                val sourceName = sourceManager.getOrStub(manga.source).toString()
                quoteManager.saveQuotes(sourceName, manga.title, quotes)
                // saveQuotes swallows IO errors; only drop the legacy file once the
                // new one is confirmed written, so a failed write can't lose quotes.
                if (quoteManager.getQuotes(sourceName, manga.title).size == quotes.size) {
                    file.delete()
                    migrated++
                }
            } catch (e: Exception) {
                logcat(LogPriority.WARN, e) { "Failed to migrate quotes file $name" }
            }
        }
        return migrated
    }
}
