package tachiyomi.domain.manga.interactor

import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.repository.MangaRepository

class NetworkToLocalManga(
    private val mangaRepository: MangaRepository,
) {

    suspend operator fun invoke(manga: Manga): Manga {
        return invoke(listOf(manga)).single()
    }

    suspend operator fun invoke(manga: List<Manga>): List<Manga> {
        return mangaRepository.insertNetworkManga(manga.map { it.canonicalizeUrl() })
    }

    /**
     * Drop trailing slashes so the same entry isn't stored twice when one source/add
     * path emits "/abc/" and another emits "/abc". Single choke point for all add
     * paths (browse, search, deeplink, migration, mass import). Fragments are left
     * intact: some plugins encode identity in the fragment (hashbang/SPA routes).
     */
    private fun Manga.canonicalizeUrl(): Manga {
        val trimmed = url.trimEnd('/')
        return if (trimmed.isNotEmpty() && trimmed != url) copy(url = trimmed) else this
    }
}
