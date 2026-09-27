package org.koitharu.kotatsu.mangadex.data

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.koitharu.kotatsu.core.model.MissingMangaSource
import org.koitharu.kotatsu.core.model.unwrap
import org.koitharu.kotatsu.mihon.model.MihonMangaSource
import org.koitharu.kotatsu.parsers.model.MangaSource

object MangaDexUrlUtilKEx {

	private val TITLE_UUID_REGEX = Regex("""[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}""")

	fun isMangaDexSource(source: MangaSource): Boolean {
		val unwrap = source.unwrap()
		if (unwrap is MihonMangaSource) {
			val pkg = unwrap.pkgName.lowercase()
			val name = unwrap.catalogueSource.name
			return pkg.endsWith(".mangadex") || pkg == "eu.kanade.tachiyomi.extension.all.mangadex" || name.equals("MangaDex", ignoreCase = true) || unwrap.sourceId == 2499283573021220255L
		}
		if (unwrap is MissingMangaSource) {
			val name = unwrap.name.lowercase()
			return name == "mangadex" || name.contains("mangadex")
		}
		val name = source.name.lowercase()
		return name == "mangadex" || name.contains("mangadex")
	}

	fun extractTitleId(rawUrl: String?): String? {
		if (rawUrl.isNullOrBlank()) return null
		val httpUrl = rawUrl.toHttpUrlOrNull() ?: return null
		if (!httpUrl.host.lowercase().contains("mangadex")) return null

		val pathSegments = httpUrl.pathSegments
		val titleIndex = pathSegments.indexOf("title")
		if (titleIndex != -1 && titleIndex + 1 < pathSegments.size) {
			val candidate = pathSegments[titleIndex + 1]
			if (TITLE_UUID_REGEX.matches(candidate)) {
				return candidate
			}
		}

		for (segment in pathSegments) {
			if (TITLE_UUID_REGEX.matches(segment)) {
				return segment
			}
		}

		return null
	}

	fun buildArtTabUrl(rawUrl: String?): String? {
		if (rawUrl.isNullOrBlank()) return null
		val httpUrl = rawUrl.toHttpUrlOrNull() ?: return null
		if (!httpUrl.host.lowercase().contains("mangadex")) return null
		if (extractTitleId(rawUrl) == null) return null

		return httpUrl.newBuilder()
			.setQueryParameter("tab", "art")
			.build()
			.toString()
	}
}
