package org.koitharu.kotatsu.aniyomi.model

import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import org.koitharu.kotatsu.core.model.ContentWarning
import org.koitharu.kotatsu.extensions.runtime.getExternalExtensionLanguageAutonym
import org.koitharu.kotatsu.parsers.model.MangaSource

data class AniyomiAnimeSourceKEx(
	val catalogueSource: AnimeCatalogueSource,
	val pkgName: String,
	val contentWarning: ContentWarning = ContentWarning.SAFE,
	val hasLanguageSuffix: Boolean = false,
) : MangaSource {

	override val name: String
		get() = "ANIYOMI_${catalogueSource.id}"

	val displayName: String
		get() = catalogueSource.name

	val languageDisplayName: String
		get() = getExternalExtensionLanguageAutonym(language)

	val language: String
		get() = catalogueSource.lang

	val sourceId: Long
		get() = catalogueSource.id

	val supportsLatest: Boolean
		get() = catalogueSource.supportsLatest

	val isAnime: Boolean
		get() = true

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is MangaSource) return false
		val raw = other.name.removePrefix("ANIYOMI_").substringBefore(':')
		return raw.toLongOrNull() == sourceId
	}

	override fun hashCode(): Int {
		return sourceId.hashCode()
	}

	override fun toString(): String {
		return "AniyomiAnimeSourceKEx(id=${catalogueSource.id}, name=${catalogueSource.name}, lang=$language)"
	}
}
