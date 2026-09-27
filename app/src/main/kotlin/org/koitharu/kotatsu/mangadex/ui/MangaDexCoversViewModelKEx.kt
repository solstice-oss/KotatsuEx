package org.koitharu.kotatsu.mangadex.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koitharu.kotatsu.core.model.parcelable.ParcelableManga
import org.koitharu.kotatsu.core.nav.AppRouter
import org.koitharu.kotatsu.mangadex.data.MangaDexCoversRepositoryKEx
import org.koitharu.kotatsu.parsers.model.Manga
import javax.inject.Inject

@HiltViewModel
class MangaDexCoversViewModelKEx @Inject constructor(
	savedStateHandle: SavedStateHandle,
	private val repository: MangaDexCoversRepositoryKEx,
) : ViewModel() {

	val manga: Manga? = savedStateHandle.get<ParcelableManga>(AppRouter.KEY_MANGA)?.manga

	private val _uiState = MutableStateFlow<MangaDexCoversUiStateKEx>(MangaDexCoversUiStateKEx.Loading)
	val uiState: StateFlow<MangaDexCoversUiStateKEx> = _uiState.asStateFlow()

	init {
		loadCovers()
	}

	fun retry() {
		loadCovers()
	}

	private fun loadCovers() {
		val title = manga?.title.orEmpty()
		val url = manga?.publicUrl?.ifBlank { null } ?: manga?.url

		if (url.isNullOrBlank()) {
			_uiState.value = MangaDexCoversUiStateKEx.Error("Invalid Manga URL", title)
			return
		}

		viewModelScope.launch {
			_uiState.value = MangaDexCoversUiStateKEx.Loading
			repository.fetchCovers(url)
				.onSuccess { covers ->
					_uiState.value = if (covers.isEmpty()) {
						MangaDexCoversUiStateKEx.Empty(title)
					} else {
						MangaDexCoversUiStateKEx.Success(covers, title)
					}
				}
				.onFailure { error ->
					_uiState.value = MangaDexCoversUiStateKEx.Error(error.message, title)
				}
		}
	}
}
