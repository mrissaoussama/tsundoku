package tachiyomi.domain.manga.interactor

import tachiyomi.domain.manga.model.CustomMangaInfo
import tachiyomi.domain.manga.model.CustomMangaInfo.Companion.writeInto
import tachiyomi.domain.manga.model.MangaUpdate
import tachiyomi.domain.manga.repository.MangaRepository

class SetCustomMangaInfo(
    private val mangaRepository: MangaRepository,
) {

    /**
     * Read the current override for [mangaId], apply [transform], and persist the result back into
     * the manga's memo. Other memo keys are preserved; an all-null result clears the override.
     */
    suspend fun await(mangaId: Long, transform: (CustomMangaInfo) -> CustomMangaInfo): Boolean {
        val memo = mangaRepository.getMemo(mangaId)
        val current = CustomMangaInfo.from(memo) ?: CustomMangaInfo()
        val updated = transform(current).takeUnless { it.isEmpty() }
        val newMemo = updated.writeInto(memo)
        return mangaRepository.update(MangaUpdate(id = mangaId, memo = newMemo))
    }

    /** Remove all custom overrides for [mangaId]. */
    suspend fun clear(mangaId: Long): Boolean {
        val memo = mangaRepository.getMemo(mangaId)
        if (CustomMangaInfo.from(memo) == null) return false
        val newMemo = (null as CustomMangaInfo?).writeInto(memo)
        return mangaRepository.update(MangaUpdate(id = mangaId, memo = newMemo))
    }
}
