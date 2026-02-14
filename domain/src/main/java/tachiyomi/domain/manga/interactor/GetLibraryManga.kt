package tachiyomi.domain.manga.interactor

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.library.model.LibraryMangaForUpdate
import tachiyomi.domain.manga.repository.MangaRepository

/**
 * Interactor for getting library manga.
 *
 * This class uses a manual StateFlow that is ONLY refreshed when explicitly requested,
 * preventing the query from running hundreds of times during normal app usage.
 */
class GetLibraryManga(
    private val mangaRepository: MangaRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    // Cached library data - ONLY updated via refresh()
    private val _libraryState = MutableStateFlow<List<LibraryManga>>(emptyList())
    private val _isLoading = MutableStateFlow(true)
    private var isInitialized = false
    private var lastRefreshTime = 0L

    // Minimum time between refreshes
    private val minRefreshIntervalMs = 2000L

    private var _version = 0

    init {
        scope.launch {
            val (favoriteCount, cacheCount) = mangaRepository.checkLibraryCacheIntegrity()
            when {
                cacheCount == 0L && favoriteCount > 0L -> {
                    logcat(LogPriority.INFO) { "GetLibraryManga: Cache is empty ($favoriteCount favorites), performing full rebuild" }
                    refreshInternal(force = true)
                }
                cacheCount != favoriteCount -> {
                    logcat(LogPriority.INFO) { "GetLibraryManga: Cache mismatch (favorites=$favoriteCount, cached=$cacheCount), incremental refresh" }
                    mangaRepository.refreshLibraryCacheIncremental()
                    refreshInternal(force = false)
                }
                else -> {
                    logcat(LogPriority.INFO) { "GetLibraryManga: Cache is valid ($cacheCount items), loading from cache" }
                    refreshInternal(force = false)
                }
            }
        }
    }

    /**
     * Get the current cached library synchronously (may be empty if not yet loaded).
     */
    suspend fun await(): List<LibraryManga> {
        val caller = Thread.currentThread().stackTrace.getOrNull(3)?.let { "${it.className}.${it.methodName}" } ?: "unknown"
        logcat(LogPriority.DEBUG) { "GetLibraryManga.await() called by: $caller" }
        // If not initialized, wait for initial load
        if (!isInitialized) {
            logcat(LogPriority.INFO) { "GetLibraryManga.await() triggering initial load (called by $caller)" }
            refreshInternal(force = true)
        }
        return _libraryState.value
    }

    /**
     * Check if library is currently loading
     */
    fun isLoading(): Flow<Boolean> = _isLoading.asStateFlow()

    /**
     * Force a refresh of the library cache.
     */
    fun refresh() {
        scope.launch {
            refreshInternal(force = false)
        }
    }

    /**
     * Force the library StateFlow to re-emit its current value.
     * This triggers downstream recomputation (e.g., download badges) without a full DB refresh.
     */
    fun notifyChanged() {
        _version++
        _libraryState.value = _libraryState.value.toList()
    }

    /**
     * Force a refresh and bypass the minimum refresh interval.
     * Use when user explicitly requests a reload (e.g., after backup restore).
     * Returns the updated library list.
     *
     * This is a suspend function that waits for the refresh to complete.
     */
    suspend fun refreshForced(): List<LibraryManga> {
        refreshInternal(force = true)
        return _libraryState.value
    }

    /**
     * Await refresh - waits for the current refresh to complete.
     * Use this when you need to ensure the library is up-to-date before proceeding.
     */
    suspend fun awaitRefresh() {
        refreshInternal(force = false)
    }

    /**
     * Apply category updates to the in-memory library list without a full DB refresh.
     * This keeps UI responsive for small, targeted changes.
     */
    suspend fun applyCategoryUpdates(
        mangaIds: List<Long>,
        addCategories: List<Long>,
        removeCategories: List<Long>,
    ) {
        if (mangaIds.isEmpty()) return
        mutex.withLock {
            val idSet = mangaIds.toSet()
            _libraryState.value = _libraryState.value.map { item ->
                if (item.id !in idSet) return@map item

                val updated = item.categories.toMutableSet()
                addCategories.forEach { updated.add(it) }
                removeCategories.forEach { updated.remove(it) }
                item.copy(categories = updated.toList())
            }
        }
    }

    /**
     * Apply chapter count/read updates to the in-memory library list without a full DB refresh.
     * Call this after chapters are read, downloaded, or deleted to keep badges accurate.
     */
    suspend fun applyChapterUpdates(mangaId: Long, totalChapters: Long? = null, readCount: Long? = null, bookmarkCount: Long? = null, lastRead: Long? = null) {
        mutex.withLock {
            _libraryState.value = _libraryState.value.map { item ->
                if (item.id != mangaId) return@map item
                item.copy(
                    totalChapters = totalChapters ?: item.totalChapters,
                    readCount = readCount ?: item.readCount,
                    bookmarkCount = bookmarkCount ?: item.bookmarkCount,
                    lastRead = lastRead ?: item.lastRead,
                )
            }
        }
    }

    /**
     * Apply batch chapter count updates for multiple manga at once.
     * More efficient than calling applyChapterUpdates individually.
     */
    suspend fun applyBatchChapterUpdates(updates: Map<Long, LibraryManga.() -> LibraryManga>) {
        if (updates.isEmpty()) return
        mutex.withLock {
            _libraryState.value = _libraryState.value.map { item ->
                val updater = updates[item.id]
                if (updater != null) updater(item) else item
            }
        }
    }

    /**
     * Apply manga detail updates (title, cover URL, status, etc.) to the in-memory list.
     * This avoids a full DB refresh when a single manga's metadata changes.
     */
    suspend fun applyMangaDetailUpdate(mangaId: Long, updater: (tachiyomi.domain.manga.model.Manga) -> tachiyomi.domain.manga.model.Manga) {
        mutex.withLock {
            _libraryState.value = _libraryState.value.map { item ->
                if (item.id != mangaId) return@map item
                item.copy(manga = updater(item.manga))
            }
        }
    }

    private suspend fun refreshInternal(force: Boolean) {
        mutex.withLock {
            val now = System.currentTimeMillis()
            if (!force && (now - lastRefreshTime) < minRefreshIntervalMs) {
                logcat(LogPriority.DEBUG) { "GetLibraryManga: Skipping refresh (too soon, ${now - lastRefreshTime}ms since last)" }
                return
            }

            _isLoading.value = true
            val caller = Thread.currentThread().stackTrace.getOrNull(3)?.let { "${it.className}.${it.methodName}" } ?: "unknown"
            logcat(LogPriority.INFO) { "GetLibraryManga: Refreshing library cache (force=$force, caller=$caller)" }

            if (force) {
                logcat(LogPriority.INFO) { "GetLibraryManga: Rebuilding library_cache table (forced)" }
                mangaRepository.refreshLibraryCache()
            } else {
                mangaRepository.invalidateLibraryCache()
            }
            val startTime = System.currentTimeMillis()

            try {
                val library = mangaRepository.getLibraryManga()
                _libraryState.value = library
                isInitialized = true
                lastRefreshTime = System.currentTimeMillis()

                val duration = System.currentTimeMillis() - startTime
                logcat(LogPriority.INFO) { "GetLibraryManga: Refresh complete in ${duration}ms, ${library.size} items" }
            } catch (e: Exception) {
                logcat(LogPriority.ERROR, e) { "GetLibraryManga: Failed to refresh library" }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Get a lightweight list of library manga for update filtering.
     * This query is faster as it skips heavy fields like description, genre, etc.
     */
    suspend fun awaitForUpdate(): List<LibraryMangaForUpdate> {
        return mangaRepository.getLibraryMangaForUpdate()
    }

    /**
     * Get only genres for tag counting - much faster than await().
     * This avoids the expensive libraryView JOIN and only fetches _id + genre from mangas table.
     */
    suspend fun awaitGenresOnly(): List<Pair<Long, List<String>?>> {
        return mangaRepository.getFavoriteGenres()
    }

    /**
     * Get genres with source ID for tag counting filtered by content type.
     * This avoids the expensive libraryView JOIN and only fetches _id + source + genre from mangas table.
     */
    suspend fun awaitGenresWithSource(): List<Triple<Long, Long, List<String>?>> {
        return mangaRepository.getFavoriteGenresWithSource()
    }

    /**
     * Get just the distinct source IDs from favorites - ultra-lightweight for extension listing.
     * This avoids the expensive libraryView JOIN and only fetches source IDs.
     */
    suspend fun awaitSourceIds(): List<Long> {
        return mangaRepository.getFavoriteSourceIds()
    }

    /**
     * Subscribe to library changes. Returns a StateFlow that is ONLY updated when refresh() is called.
     * This prevents SQLDelight from re-running the expensive query on every table change.
     */
    fun subscribe(): Flow<List<LibraryManga>> {
        return _libraryState.asStateFlow()
    }
}
