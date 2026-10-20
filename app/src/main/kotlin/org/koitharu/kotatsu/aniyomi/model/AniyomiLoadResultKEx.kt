package org.koitharu.kotatsu.aniyomi.model

import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.AnimeSource
import org.koitharu.kotatsu.core.model.ContentWarning

sealed interface AniyomiLoadResultKEx {

	data class Success(
		val info: AniyomiExtensionInfoKEx,
		val pkgName: String,
		val sources: List<AnimeSource>,
		val catalogueSources: List<AnimeCatalogueSource>,
		val contentWarning: ContentWarning,
	) : AniyomiLoadResultKEx

	data class Error(
		val pkgName: String,
		val message: String,
		val exception: Throwable? = null,
	) : AniyomiLoadResultKEx

	data class Untrusted(
		val pkgName: String,
		val signatures: List<String>,
	) : AniyomiLoadResultKEx
}
