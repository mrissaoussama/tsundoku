package tachiyomi.data.history

import kotlinx.coroutines.flow.Flow
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.history.model.History
import tachiyomi.domain.history.model.HistoryUpdate
import tachiyomi.domain.history.model.HistoryWithRelations
import tachiyomi.domain.history.repository.HistoryRepository
import tachiyomi.domain.manga.model.MangaCover
import java.util.Date

class HistoryRepositoryImpl(
    private val handler: DatabaseHandler,
) : HistoryRepository {

    /**
     * Maps cache table columns (last_read as Long) to HistoryWithRelations.
     * The cache table stores last_read as INTEGER NOT NULL (Long),
     * while the mapper expects Date?.
     */
    private fun mapCacheHistoryWithRelations(
        id: Long,
        mangaId: Long,
        chapterId: Long,
        title: String,
        thumbnailUrl: String?,
        source: Long,
        favorite: Boolean,
        coverLastModified: Long,
        chapterNumber: Double,
        readAt: Long,
        readDuration: Long,
    ): HistoryWithRelations = HistoryWithRelations(
        id = id,
        chapterId = chapterId,
        mangaId = mangaId,
        title = title,
        chapterNumber = chapterNumber,
        readAt = if (readAt > 0) Date(readAt) else null,
        readDuration = readDuration,
        coverData = MangaCover(
            mangaId = mangaId,
            sourceId = source,
            isMangaFavorite = favorite,
            url = thumbnailUrl,
            lastModified = coverLastModified,
        ),
    )

    override fun getHistory(query: String): Flow<List<HistoryWithRelations>> {
        return handler.subscribeToList {
            history_cacheQueries.getHistoryWithSearch(query, ::mapCacheHistoryWithRelations)
        }
    }

    override suspend fun getLastHistory(): HistoryWithRelations? {
        return handler.awaitOneOrNull {
            history_cacheQueries.getLatestHistoryCache(::mapCacheHistoryWithRelations)
        }
    }

    override suspend fun getTotalReadDuration(): Long {
        return handler.awaitOne { historyQueries.getReadDuration() }
    }

    override suspend fun getHistoryByMangaId(mangaId: Long): List<History> {
        return handler.awaitList { historyQueries.getHistoryByMangaId(mangaId, HistoryMapper::mapHistory) }
    }

    override suspend fun resetHistory(historyId: Long) {
        try {
            handler.await {
                historyQueries.resetHistoryById(historyId)
            }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, throwable = e)
        }
    }

    override suspend fun resetHistoryByMangaId(mangaId: Long) {
        try {
            handler.await {
                historyQueries.resetHistoryByMangaId(mangaId)
            }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, throwable = e)
        }
    }

    override suspend fun deleteAllHistory(): Boolean {
        return try {
            handler.await {
                historyQueries.removeAllHistory()
            }
            true
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, throwable = e)
            false
        }
    }

    override suspend fun upsertHistory(historyUpdate: HistoryUpdate) {
        try {
            handler.await {
                historyQueries.upsert(
                    historyUpdate.chapterId,
                    historyUpdate.readAt,
                    historyUpdate.sessionReadDuration,
                )
            }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, throwable = e)
        }
    }

    override suspend fun refreshHistoryCache() {
        logcat(LogPriority.INFO) { "HistoryRepositoryImpl.refreshHistoryCache: Rebuilding history cache (limited to 1000)" }
        try {
            handler.await(inTransaction = true) {
                history_cacheQueries.clearAll()
                history_cacheQueries.rebuildHistoryCacheLimited()
            }
            logcat(LogPriority.INFO) { "HistoryRepositoryImpl.refreshHistoryCache: Cache rebuilt" }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Failed to rebuild history cache" }
        }
    }
}
