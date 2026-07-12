package eu.kanade.tachiyomi.ui.reader.quote

import android.content.Context
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.util.storage.DiskUtil
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import logcat.LogPriority
import logcat.logcat
import tachiyomi.domain.storage.service.StorageManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.IOException

/**
 * Manager for handling quote storage and retrieval.
 *
 * Quotes are stored in a portable, human-readable layout that mirrors downloads:
 *   {quotes}/{source name}/{novel title}.json
 * so they survive re-imports and can be moved between installs.
 */
class QuoteManager(private val context: Context) {

    private val jsonFormat = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val storageManager: StorageManager by lazy {
        Injekt.get<StorageManager>()
    }

    private val quotesDir: UniFile? by lazy {
        storageManager.getQuotesDirectory()
    }

    private fun getSourceDirName(sourceName: String): String = DiskUtil.buildValidFilename(sourceName)

    private fun getNovelFileName(novelTitle: String): String = "${DiskUtil.buildValidFilename(novelTitle)}.json"

    private fun findSourceDir(sourceName: String): UniFile? {
        return quotesDir?.findFile(getSourceDirName(sourceName))
    }

    private fun getOrCreateSourceDir(sourceName: String): UniFile? {
        val name = getSourceDirName(sourceName)
        val dir = quotesDir ?: return null
        return dir.findFile(name)?.takeIf { it.isDirectory } ?: dir.createDirectory(name)
    }

    private fun getQuotesFile(sourceName: String, novelTitle: String): UniFile? {
        return findSourceDir(sourceName)?.findFile(getNovelFileName(novelTitle))
    }

    /**
     * Save quotes for a novel
     */
    fun saveQuotes(sourceName: String, novelTitle: String, quotes: List<Quote>) {
        try {
            val fileName = getNovelFileName(novelTitle)

            // Delete existing file first to avoid duplicate files
            val existingFile = getQuotesFile(sourceName, novelTitle)
            if (existingFile?.exists() == true) {
                existingFile.delete()
            }

            if (quotes.isEmpty()) return

            val json = jsonFormat.encodeToString(NovelQuotes(quotes))
            val file = getOrCreateSourceDir(sourceName)?.createFile(fileName) ?: return
            file.openOutputStream().use { outputStream ->
                outputStream.write(json.toByteArray())
            }
            logcat(LogPriority.DEBUG) { "Quotes saved for $sourceName/$novelTitle: ${quotes.size} quotes" }
        } catch (e: IOException) {
            logcat(LogPriority.ERROR) { "Failed to save quotes for $sourceName/$novelTitle: ${e.message}" }
        } catch (e: SerializationException) {
            logcat(LogPriority.ERROR) { "Failed to serialize quotes for $sourceName/$novelTitle: ${e.message}" }
        }
    }

    /**
     * Load quotes for a novel
     */
    fun loadQuotes(sourceName: String, novelTitle: String): List<Quote> {
        return try {
            val file = getQuotesFile(sourceName, novelTitle)
            if (file == null || !file.exists()) {
                emptyList()
            } else {
                val json = file.openInputStream().use { inputStream ->
                    String(inputStream.readBytes())
                }
                jsonFormat.decodeFromString<NovelQuotes>(json).quotes
            }
        } catch (e: IOException) {
            logcat(LogPriority.ERROR) { "Failed to load quotes for $sourceName/$novelTitle: ${e.message}" }
            emptyList()
        } catch (e: SerializationException) {
            logcat(LogPriority.ERROR) { "Failed to deserialize quotes for $sourceName/$novelTitle: ${e.message}" }
            emptyList()
        }
    }

    /**
     * Add a new quote for a novel
     */
    fun addQuote(sourceName: String, novelTitle: String, quote: Quote) {
        val existingQuotes = loadQuotes(sourceName, novelTitle).toMutableList()
        existingQuotes.add(quote)
        saveQuotes(sourceName, novelTitle, existingQuotes)
    }

    /**
     * Remove a quote by ID
     */
    fun removeQuote(sourceName: String, novelTitle: String, quoteId: String) {
        val existingQuotes = loadQuotes(sourceName, novelTitle).toMutableList()
        existingQuotes.removeAll { it.id == quoteId }
        saveQuotes(sourceName, novelTitle, existingQuotes)
    }

    /**
     * Update an existing quote
     */
    fun updateQuote(sourceName: String, novelTitle: String, updatedQuote: Quote) {
        val existingQuotes = loadQuotes(sourceName, novelTitle).toMutableList()
        val index = existingQuotes.indexOfFirst { it.id == updatedQuote.id }
        if (index >= 0) {
            existingQuotes[index] = updatedQuote
            saveQuotes(sourceName, novelTitle, existingQuotes)
        }
    }

    /**
     * Get all quotes for a novel, preserving the stored order
     */
    fun getQuotes(sourceName: String, novelTitle: String): List<Quote> {
        return loadQuotes(sourceName, novelTitle)
    }

    /**
     * Clear all quotes for a novel
     */
    fun clearQuotes(sourceName: String, novelTitle: String) {
        val file = getQuotesFile(sourceName, novelTitle)
        if (file?.exists() == true) {
            file.delete()
        }
    }

    /**
     * Get quote count for a novel
     */
    fun getQuoteCount(sourceName: String, novelTitle: String): Int {
        return loadQuotes(sourceName, novelTitle).size
    }

    /**
     * Reorder quotes for a novel
     */
    fun reorderQuotes(sourceName: String, novelTitle: String, quotes: List<Quote>) {
        saveQuotes(sourceName, novelTitle, quotes)
        logcat(LogPriority.DEBUG) { "Quotes reordered for $sourceName/$novelTitle: ${quotes.size} quotes" }
    }
}

/**
 * Get QuoteManager instance
 */
val Context.quoteManager: QuoteManager
    get() = QuoteManager(this)
