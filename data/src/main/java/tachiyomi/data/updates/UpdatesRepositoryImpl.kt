package tachiyomi.data.updates

import kotlinx.coroutines.flow.Flow
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.manga.model.MangaCover
import tachiyomi.domain.updates.model.UpdatesWithRelations
import tachiyomi.domain.updates.repository.UpdatesRepository

class UpdatesRepositoryImpl(
    private val databaseHandler: DatabaseHandler,
) : UpdatesRepository {

    override suspend fun awaitWithRead(
        read: Boolean,
        after: Long,
        limit: Long,
    ): List<UpdatesWithRelations> {
        return databaseHandler.awaitList {
            updates_cacheQueries.getUpdatesByReadStatus(
                read = read,
                after = after,
                limit = limit,
                mapper = ::mapUpdatesWithRelations,
            )
        }
    }

    override fun subscribeAll(after: Long, limit: Long): Flow<List<UpdatesWithRelations>> {
        return databaseHandler.subscribeToList {
            updates_cacheQueries.getRecentUpdates(after, limit, ::mapUpdatesWithRelations)
        }
    }

    override fun subscribeWithRead(
        read: Boolean,
        after: Long,
        limit: Long,
    ): Flow<List<UpdatesWithRelations>> {
        return databaseHandler.subscribeToList {
            updates_cacheQueries.getUpdatesByReadStatus(
                read = read,
                after = after,
                limit = limit,
                mapper = ::mapUpdatesWithRelations,
            )
        }
    }

    override suspend fun clearAllUpdates() {
        databaseHandler.await { updates_cacheQueries.clearAll() }
    }

    override suspend fun clearUpdatesOlderThan(timestamp: Long) {
        databaseHandler.await { updates_cacheQueries.clearOlderThan(timestamp) }
    }

    override suspend fun clearUpdatesKeepLatest(keep: Long) {
    }

    private fun mapUpdatesWithRelations(
        mangaId: Long,
        mangaTitle: String,
        chapterId: Long,
        chapterName: String,
        scanlator: String?,
        chapterUrl: String,
        read: Boolean,
        bookmark: Boolean,
        lastPageRead: Long,
        sourceId: Long,
        favorite: Boolean,
        thumbnailUrl: String?,
        coverLastModified: Long,
        dateUpload: Long,
        dateFetch: Long,
    ): UpdatesWithRelations = UpdatesWithRelations(
        mangaId = mangaId,
        mangaTitle = mangaTitle,
        chapterId = chapterId,
        chapterName = chapterName,
        scanlator = scanlator,
        chapterUrl = chapterUrl,
        read = read,
        bookmark = bookmark,
        lastPageRead = lastPageRead,
        sourceId = sourceId,
        dateFetch = dateFetch,
        coverData = MangaCover(
            mangaId = mangaId,
            sourceId = sourceId,
            isMangaFavorite = favorite,
            url = thumbnailUrl,
            lastModified = coverLastModified,
        ),
    )

    override suspend fun refreshUpdatesCache() {
        logcat(LogPriority.INFO) { "UpdatesRepositoryImpl.refreshUpdatesCache: Rebuilding updates cache (limited to 1000)" }
        try {
            databaseHandler.await(inTransaction = true) {
                updates_cacheQueries.clearAll()
                updates_cacheQueries.rebuildUpdatesCacheLimited()
            }
            logcat(LogPriority.INFO) { "UpdatesRepositoryImpl.refreshUpdatesCache: Cache rebuilt" }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Failed to rebuild updates cache" }
        }
    }

    override suspend fun checkUpdatesCacheIntegrity(): Pair<Long, Long> {
        val count = databaseHandler.awaitOne { updates_cacheQueries.countAll() }
        return Pair(count, count)
    }
}
