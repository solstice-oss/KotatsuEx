package org.koitharu.kotatsu.settings.sources.catalog

private const val ANIME_EXTENSION_PACKAGE_PREFIX = "eu.kanade.tachiyomi.animeextension"

val ExternalExtensionRepoEntry.isAnimeExtension: Boolean
	get() = packageName.startsWith(ANIME_EXTENSION_PACKAGE_PREFIX)

fun List<ExternalExtensionRepoEntry>.extensionStoreKindKEx(): ExtensionStoreKind? {
	if (isEmpty()) return null
	val animeCount = count { it.isAnimeExtension }
	val novelCount = count { it.isNovelExtension }
	return when {
		animeCount == size -> ExtensionStoreKind.ANIME
		novelCount == size -> ExtensionStoreKind.NOVEL
		animeCount == 0 && novelCount == 0 -> ExtensionStoreKind.MANGA
		else -> ExtensionStoreKind.MIXED
	}
}
