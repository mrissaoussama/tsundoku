package tachiyomi.domain.manga.interactor

import tachiyomi.domain.manga.model.CustomMangaInfo
import tachiyomi.domain.manga.repository.MangaRepository

class GetCustomMangaInfo(
    private val mangaRepository: MangaRepository,
) {

    suspend fun await(mangaId: Long): CustomMangaInfo? {
        return CustomMangaInfo.from(mangaRepository.getMemo(mangaId))
    }
}
