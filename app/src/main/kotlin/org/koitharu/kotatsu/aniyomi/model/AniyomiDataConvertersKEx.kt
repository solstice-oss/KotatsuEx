@file:Suppress("DEPRECATION")

package org.koitharu.kotatsu.aniyomi.model

import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.animesource.model.SEpisode
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import org.koitharu.kotatsu.parsers.model.ContentRating
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.model.MangaPage
import org.koitharu.kotatsu.parsers.model.MangaState
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.parsers.model.RATING_UNKNOWN

fun SAnime.toManga(
	source: AniyomiAnimeSourceKEx,
	chapters: List<MangaChapter> = emptyList(),
	publicUrl: String = "",
): Manga {
	val httpSource = source.catalogueSource as? AnimeHttpSource
	val baseUrl = httpSource?.baseUrl ?: ""

	val safeUrl = try { url } catch (_: UninitializedPropertyAccessException) { "" }
	val safeThumbnail = try { thumbnail_url } catch (_: UninitializedPropertyAccessException) { null }
	val safeTitle = try { title } catch (_: UninitializedPropertyAccessException) { "Unknown" }
	val safeGenres: List<String>? = getGenres()?.takeIf { it.isNotEmpty() }
	val safeAuthor = try { author } catch (_: UninitializedPropertyAccessException) { null }
	val safeArtist = try { artist } catch (_: UninitializedPropertyAccessException) { null }
	val safeDescription = try { description } catch (_: UninitializedPropertyAccessException) { null }
	val safeStatus = try { status } catch (_: UninitializedPropertyAccessException) { SAnime.UNKNOWN }

	val resolvedCover = resolveUrl(baseUrl, safeThumbnail)
	val resolvedUrl = if (publicUrl.isNotBlank()) {
		publicUrl
	} else {
		httpSource?.getAnimeUrl(this).orEmpty().ifBlank { resolveUrl(baseUrl, safeUrl) ?: safeUrl }
	}

	val adultGenres = setOf("adult", "hentai", "18+", "nsfw", "mature", "ecchi")
	val isContentNsfw = safeGenres?.any { it.trim().lowercase() in adultGenres } == true

	return Manga(
		id = aniyomiAnimeId(source.name, safeUrl),
		title = safeTitle,
		altTitles = emptySet(),
		url = safeUrl,
		publicUrl = resolvedUrl,
		rating = RATING_UNKNOWN,
		contentRating = if (isContentNsfw) ContentRating.ADULT else null,
		coverUrl = resolvedCover,
		largeCoverUrl = resolvedCover,
		tags = safeGenres.orEmpty().mapTo(LinkedHashSet()) { genre ->
			MangaTag(key = genre.lowercase(), title = genre, source = source)
		},
		state = when (safeStatus) {
			SAnime.ONGOING -> MangaState.ONGOING
			SAnime.COMPLETED, SAnime.PUBLISHING_FINISHED -> MangaState.FINISHED
			SAnime.CANCELLED -> MangaState.ABANDONED
			SAnime.ON_HIATUS -> MangaState.PAUSED
			SAnime.LICENSED -> MangaState.RESTRICTED
			else -> null
		},
		authors = buildSet {
			safeAuthor?.takeIf { it.isNotBlank() }?.let(::add)
			safeArtist?.takeIf { it.isNotBlank() && it != safeAuthor }?.let(::add)
		},
		description = safeDescription,
		chapters = chapters,
		source = source,
	)
}

fun Manga.toSAnime(): SAnime {
	return SAnime.create().apply {
		this.url = this@toSAnime.url
		this.title = this@toSAnime.title
		this.author = this@toSAnime.authors.firstOrNull()
		this.artist = this@toSAnime.authors.drop(1).firstOrNull()
		this.description = this@toSAnime.description
		this.genre = this@toSAnime.tags.joinToString(", ") { it.title }
		this.status = when (this@toSAnime.state) {
			MangaState.ONGOING -> SAnime.ONGOING
			MangaState.FINISHED -> SAnime.COMPLETED
			MangaState.ABANDONED -> SAnime.CANCELLED
			MangaState.PAUSED -> SAnime.ON_HIATUS
			MangaState.RESTRICTED -> SAnime.LICENSED
			else -> SAnime.UNKNOWN
		}
		this.thumbnail_url = this@toSAnime.coverUrl
		this.initialized = this@toSAnime.description != null || this@toSAnime.chapters != null
	}
}

fun SEpisode.toMangaChapter(source: AniyomiAnimeSourceKEx, overrideNumber: Float? = null): MangaChapter {
	val finalNumber = overrideNumber ?: (if (episode_number >= 0f) episode_number else 0f)
	val normalizedScanlator = scanlator?.trim()?.takeIf { it.isNotEmpty() }
	return MangaChapter(
		id = aniyomiEpisodeId(source.name, url),
		title = name.takeIf { it.isNotBlank() },
		number = finalNumber,
		volume = 0,
		url = url,
		scanlator = normalizedScanlator,
		uploadDate = date_upload,
		branch = normalizedScanlator,
		source = source,
	)
}

fun MangaChapter.toSEpisode(): SEpisode = SEpisode.create().apply {
	url = this@toSEpisode.url
	name = this@toSEpisode.title ?: "Episode ${this@toSEpisode.number}"
	episode_number = this@toSEpisode.number
	date_upload = this@toSEpisode.uploadDate
	scanlator = this@toSEpisode.scanlator ?: this@toSEpisode.branch
}

fun Video.toMangaPage(source: AniyomiAnimeSourceKEx, episodeUrl: String): MangaPage = MangaPage(
	id = stableId(source.name, "video", "$episodeUrl|$videoUrl"),
	url = videoUrl,
	preview = null,
	source = source,
)

private fun stableId(sourceName: String, type: String, value: String): Long {
	return "$sourceName|$type|$value".hashCode().toLong() and Long.MAX_VALUE
}

fun aniyomiAnimeId(sourceName: String, url: String): Long = stableId(sourceName, "anime", url)

fun aniyomiEpisodeId(sourceName: String, url: String): Long = stableId(sourceName, "episode", url)

private fun resolveUrl(baseUrl: String?, value: String?): String? {
	if (value.isNullOrBlank()) return null
	if (value.startsWith("http://") || value.startsWith("https://")) return value
	if (value.startsWith("//")) return "https:$value"
	return baseUrl?.trimEnd('/')?.plus("/")?.plus(value.trimStart('/')) ?: value
}
