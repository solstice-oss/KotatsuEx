package org.koitharu.kotatsu.mangadex.ui

import androidx.compose.runtime.Immutable
import org.koitharu.kotatsu.mangadex.data.MangaDexCoverKEx

@Immutable
sealed interface MangaDexCoversUiStateKEx {
	data object Loading : MangaDexCoversUiStateKEx
	data class Success(val covers: List<MangaDexCoverKEx>, val title: String) : MangaDexCoversUiStateKEx
	data class Empty(val title: String) : MangaDexCoversUiStateKEx
	data class Error(val message: String? = null, val title: String) : MangaDexCoversUiStateKEx
}
