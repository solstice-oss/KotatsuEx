package org.koitharu.kotatsu.mangadex.data

import androidx.compose.runtime.Immutable

@Immutable
data class MangaDexCoverKEx(
	val id: String,
	val url: String,
	val volume: String? = null,
	val description: String? = null,
	val locale: String? = null,
)
