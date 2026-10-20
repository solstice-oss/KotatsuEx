package org.koitharu.kotatsu.aniyomi

import android.content.Context
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Headers
import okhttp3.Response
import org.koitharu.kotatsu.core.cache.MemoryContentCache
import org.koitharu.kotatsu.core.exceptions.UnsupportedSourceException
import org.koitharu.kotatsu.core.model.MissingMangaSource
import org.koitharu.kotatsu.core.parser.MangaRepository
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
class LazyAniyomiAnimeRepositoryKEx(
	override val source: MissingMangaSource,
	private val extensionManager: AniyomiExtensionManagerKEx,
	private val cache: MemoryContentCache,
	private val context: Context,
) : MangaRepository {

	@Volatile
	private var delegate: AniyomiAnimeRepositoryKEx? = null
	private val resolveMutex = Mutex()

	override val sortOrders: Set<SortOrder>
		get() = delegate?.sortOrders ?: EnumSet.allOf(SortOrder::class.java)

	override var defaultSortOrder: SortOrder
		get() = delegate?.defaultSortOrder ?: SortOrder.POPULARITY
		set(value) {
			delegate?.defaultSortOrder = value
		}

	override val filterCapabilities: MangaListFilterCapabilities
		get() = delegate?.filterCapabilities ?: MangaListFilterCapabilities()

	override suspend fun getList(offset: Int, order: SortOrder?, filter: MangaListFilter?): List<Manga> =
		resolve().getList(offset, order, filter)

	override suspend fun getDetails(manga: Manga): Manga = resolve(manga).getDetails(manga)

	override suspend fun getPages(manga: Manga, chapter: MangaChapter): List<MangaPage> = resolve().getPages(manga, chapter)

	override suspend fun getPageUrl(page: MangaPage): String = resolve().getPageUrl(page)

	override suspend fun getChapterUrl(manga: Manga, chapter: MangaChapter): String? =
		resolve().getChapterUrl(manga, chapter)

	override suspend fun getImageRequestHeaders(imageUrl: String, page: MangaPage): Headers? =
		resolve().getImageRequestHeaders(imageUrl, page)

	override suspend fun getImageStream(pageUrl: String, page: MangaPage): Response? =
		resolve().getImageStream(pageUrl, page)

	override suspend fun getCoverStream(url: String): Response? = resolve().getCoverStream(url)

	override suspend fun getFilterOptions(): MangaListFilterOptions = resolve().getFilterOptions()

	override suspend fun getRelated(seed: Manga): List<Manga> = resolve().getRelated(seed)

	override suspend fun find(manga: Manga): Manga? = resolve(manga).find(manga)

	private suspend fun resolve(manga: Manga? = null): AniyomiAnimeRepositoryKEx {
		delegate?.let { return it }
		return resolveMutex.withLock {
			delegate?.let { return@withLock it }
			extensionManager.ensureReady()
			val animeSource = extensionManager.getAniyomiAnimeSourceByName(source.name)
				?: throw UnsupportedSourceException(
					"Install the matching Aniyomi extension to view this source",
					manga,
				)
			val real = AniyomiAnimeRepositoryKEx(animeSource, cache, context)
			delegate = real
			real
		}
	}
}
