package org.koitharu.kotatsu.aniyomi.model

data class AniyomiExtensionInfoKEx(
	val name: String,
	val pkgName: String,
	val versionName: String,
	val versionCode: Long,
	val lang: String,
	val isNsfw: Boolean = false,
	val hasReadme: Boolean = false,
	val hasChangelog: Boolean = false,
	val isTorrent: Boolean = false,
	val signatures: List<String> = emptyList(),
)
