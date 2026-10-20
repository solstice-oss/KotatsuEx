@file:Suppress("DEPRECATION")

package org.koitharu.kotatsu.aniyomi

import android.content.Context
import android.util.Log
import eu.kanade.tachiyomi.animesource.model.AnimesPage
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koitharu.kotatsu.aniyomi.model.AniyomiAnimeSourceKEx
import org.koitharu.kotatsu.aniyomi.model.toManga
import org.koitharu.kotatsu.aniyomi.model.toMangaChapter
import org.koitharu.kotatsu.aniyomi.model.toMangaPage
import org.koitharu.kotatsu.aniyomi.model.toSAnime
import org.koitharu.kotatsu.aniyomi.model.toSEpisode
import org.koitharu.kotatsu.core.cache.MemoryContentCache
import org.koitharu.kotatsu.core.parser.CachingMangaRepository
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.prefs.SourceSettings
import org.koitharu.kotatsu.parsers.InternalParsersApi
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.model.MangaListFilter
import org.koitharu.kotatsu.parsers.model.MangaListFilterCapabilities
import org.koitharu.kotatsu.parsers.model.MangaListFilterOptions
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.parsers.model.SortOrder
import java.util.EnumSet

@OptIn(InternalParsersApi::class)
class AniyomiAnimeRepositoryKEx(
	override val source: AniyomiAnimeSourceKEx,
	cache: MemoryContentCache,
	context: Context,
) : CachingMangaRepository(cache) {

	private val sourceSettings = SourceSettings(context, source)
	private val appSettings = AppSettings(context)
	private val sourceMetadata = AniyomiSourceMetadataStoreKEx(context)

	val aniyomiSource = source.catalogueSource

	private class PaginationState {
		var currentPage = 1
		var lastOffset = -1
		@Volatile var hasMorePages = true
	}

	private val paginationStates = java.util.concurrent.ConcurrentHashMap<String, PaginationState>()

	private fun paginationKey(order: SortOrder?, filter: MangaListFilter?): String =
		"$order|${filter?.query}|${filter?.tags}|${filter?.tagsExclude}"

	override val sortOrders: Set<SortOrder> = buildSet {
		add(SortOrder.POPULARITY)
		if (source.supportsLatest) add(SortOrder.UPDATED)
		add(SortOrder.RELEVANCE)
	}.let { EnumSet.copyOf(it) }

	override var defaultSortOrder: SortOrder
		get() = (sourceSettings.defaultSortOrder ?: appSettings.defaultBrowseSortOrder)
			.takeIf { (it == SortOrder.POPULARITY || it == SortOrder.UPDATED) && it in sortOrders }
			?: SortOrder.POPULARITY
		set(value) {
			sourceSettings.defaultSortOrder = value
		}

	override val filterCapabilities: MangaListFilterCapabilities
		get() = MangaListFilterCapabilities(
			isSearchSupported = true,
			isMultipleTagsSupported = true,
			isSearchWithFiltersSupported = true,
		)

	override suspend fun getFilterOptions(): MangaListFilterOptions = withContext(Dispatchers.IO) {
		val animeFilters = runCatching { aniyomiSource.getFilterList() }.getOrNull()
		val availableTags = buildSet {
			animeFilters?.forEach { filter ->
				if (filter is eu.kanade.tachiyomi.animesource.model.AnimeFilter.Select<*>) {
					filter.values.forEach { valName ->
						val str = valName.toString()
						if (str.isNotBlank()) {
							add(org.koitharu.kotatsu.parsers.model.MangaTag(key = str.lowercase(), title = str, source = source))
						}
					}
				}
			}
		}
		MangaListFilterOptions(
			availableTags = availableTags,
		)
	}

	override suspend fun getList(offset: Int, order: SortOrder?, filter: MangaListFilter?): List<Manga> = withContext(Dispatchers.IO) {
		val state = paginationStates.getOrPut(paginationKey(order, filter)) { PaginationState() }
		val page = synchronized(state) {
			if (offset == 0) {
				state.currentPage = 1
				state.hasMorePages = true
				state.lastOffset = 0
			} else if (offset > state.lastOffset && state.hasMorePages) {
				state.currentPage += 1
				state.lastOffset = offset
			}
			state.currentPage
		}

		if (!state.hasMorePages && offset > 0) return@withContext emptyList()

		val pageResult: AnimesPage = when {
			!filter?.query.isNullOrBlank() || filter?.tags?.isNotEmpty() == true -> {
				val animeFilters = aniyomiSource.getFilterList()
				if (filter != null) {
					AniyomiFilterMapperKEx.decode(animeFilters, filter)
				}
				aniyomiSource.getSearchAnime(page, filter?.query.orEmpty(), animeFilters)
			}
			order == SortOrder.UPDATED && source.supportsLatest -> {
				aniyomiSource.getLatestUpdates(page)
			}
			else -> {
				aniyomiSource.getPopularAnime(page)
			}
		}

		state.hasMorePages = pageResult.hasNextPage
		sourceMetadata.saveMemos(source.sourceId, pageResult.animes)

		pageResult.animes.map { sAnime ->
			sAnime.toManga(source)
		}
	}

	override suspend fun getDetailsImpl(manga: Manga): Manga = withContext(Dispatchers.IO) {
		val sAnime = manga.toSAnime()
		sourceMetadata.restore(source.sourceId, manga.url, sAnime)

		val details = if (sAnime.initialized) {
			sAnime
		} else {
			val fetched = aniyomiSource.getAnimeDetails(sAnime)
			sourceMetadata.save(source.sourceId, manga.url, fetched)
			fetched
		}

		val episodes: List<SEpisode> = aniyomiSource.getEpisodeList(details)
		val chapterMemos = mutableMapOf<String, kotlinx.serialization.json.JsonObject>()
		val mappedChapters = episodes.mapIndexed { index, ep ->
			if (!ep.memo.isEmpty()) {
				chapterMemos[ep.url] = ep.memo
			}
			ep.toMangaChapter(source, overrideNumber = (episodes.size - index).toFloat())
		}
		if (chapterMemos.isNotEmpty()) {
			sourceMetadata.saveEpisodeMemos(source.sourceId, manga.url, chapterMemos)
		}

		details.toManga(source = source, chapters = mappedChapters, publicUrl = manga.publicUrl)
	}

	override suspend fun getPagesImpl(manga: Manga, chapter: MangaChapter): List<MangaPage> = withContext(Dispatchers.IO) {
		val httpSource = aniyomiSource as? AnimeHttpSource ?: return@withContext emptyList()
		val sEpisode = chapter.toSEpisode()
		sourceMetadata.restoreEpisodeMemo(source.sourceId, chapter.source.name, chapter.url)?.let {
			sEpisode.memo = it
		}

		val videos: List<Video> = try {
			httpSource.getVideoList(sEpisode)
		} catch (e: Exception) {
			Log.e("AniyomiRepository", "Failed to fetch video list for ${chapter.url}", e)
			emptyList()
		}

		videos.map { video ->
			video.toMangaPage(source, chapter.url)
		}
	}

	override suspend fun getPageUrl(page: MangaPage): String = page.url

	override suspend fun getRelatedMangaImpl(seed: Manga): List<Manga> = emptyList()

	suspend fun getVideoList(chapter: MangaChapter): List<Video> = withContext(Dispatchers.IO) {
		val httpSource = aniyomiSource as? AnimeHttpSource ?: return@withContext emptyList()
		val sEpisode = chapter.toSEpisode()
		sourceMetadata.restoreEpisodeMemo(source.sourceId, chapter.source.name, chapter.url)?.let {
			sEpisode.memo = it
		}
		try {
			httpSource.getVideoList(sEpisode)
		} catch (e: Exception) {
			Log.e("AniyomiRepository", "Failed to fetch video list for ${chapter.url}", e)
			emptyList()
		}
	}
}
