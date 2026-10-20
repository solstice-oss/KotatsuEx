package org.koitharu.kotatsu.core.db

import org.koitharu.kotatsu.core.db.entity.MangaEntity
import org.koitharu.kotatsu.core.model.isAnimeSource
import org.koitharu.kotatsu.parsers.model.Manga

object AnimeEntityKEx {
	const val SERIES_TYPE_MANGA = 0
	const val SERIES_TYPE_NOVEL = 1
	const val SERIES_TYPE_ANIME = 2

	fun seriesTypeOf(manga: Manga): Int = when {
		manga.source.isAnimeSource -> SERIES_TYPE_ANIME
		else -> SERIES_TYPE_MANGA
	}

	fun isAnimeSeries(mangaEntity: MangaEntity): Boolean =
		mangaEntity.seriesType == SERIES_TYPE_ANIME || mangaEntity.source.startsWith("ANIYOMI_")
}
