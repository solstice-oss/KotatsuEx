package org.koitharu.kotatsu.massmigrate

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koitharu.kotatsu.core.db.MangaDatabase
import org.koitharu.kotatsu.core.db.entity.toMangaList
import org.koitharu.kotatsu.core.model.chaptersCount
import org.koitharu.kotatsu.core.model.MangaSource as createMangaSource
import org.koitharu.kotatsu.core.model.getStoredTitleOrNull
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.explore.data.MangaSourcesRepository
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import org.koitharu.kotatsu.search.domain.SearchKind
import org.koitharu.kotatsu.search.domain.SearchV2Helper
import javax.inject.Inject

@HiltViewModel
class MassMigrateViewModelKEx @Inject constructor(
	private val database: MangaDatabase,
	private val sourcesRepository: MangaSourcesRepository,
	private val searchHelperFactory: SearchV2Helper.Factory,
	private val mangaRepositoryFactory: MangaRepository.Factory,
	private val massMigrateUseCase: MassMigrateUseCaseKEx,
) : BaseViewModel() {

	private val _state = MutableStateFlow(MassMigrateState())
	val state: StateFlow<MassMigrateState> = _state.asStateFlow()

	init {
		loadSources()
	}

	private fun loadSources() {
		viewModelScope.launch {
			_state.update { it.copy(isLoading = true) }
			database.getMangaDao().observeLibrarySourceUsage().collect { usages ->
				val sources = usages.map { 
					val sourceObj = createMangaSource(it.source)
					val displayTitle = sourceObj.getStoredTitleOrNull() 
						?: it.sourceTitle?.takeIf { t -> t.isNotBlank() && t.toLongOrNull() == null } 
						?: it.source
					LibrarySourceOptionKEx(
						key = it.source, 
						title = displayTitle, 
						mangaCount = it.mangaCount
					) 
				}
				_state.update { it.copy(isLoading = false, sources = sources) }
			}
		}
	}

	fun onSourceSelected(sourceKey: String) {
		viewModelScope.launch {
			_state.update { it.copy(isLoading = true, selectedSourceKey = sourceKey) }
			val manga = withContext(Dispatchers.IO) {
				database.getMangaDao().findLibraryMangaBySources(listOf(sourceKey)).toMangaList()
			}
			_state.update { 
				it.copy(
					isLoading = false, 
					mangaList = manga, 
					selectedMangaIds = manga.map { m -> m.id }.toSet(),
					step = MassMigrateStep.SELECT_MANGA
				) 
			}
		}
	}

	fun toggleMangaSelection(mangaId: Long) {
		_state.update { current ->
			current.copy(
				selectedMangaIds = current.selectedMangaIds.toMutableSet().apply {
					if (!add(mangaId)) remove(mangaId)
				}
			)
		}
	}

	fun onMangaSelectionConfirmed() {
		viewModelScope.launch {
			_state.update { it.copy(isLoading = true) }
			val enabledSources = sourcesRepository.getEnabledSources().toList()
			_state.update { 
				it.copy(
					isLoading = false, 
					availableTargetSources = enabledSources, 
					selectedTargetSourceKeys = emptySet(),
					step = MassMigrateStep.SELECT_TARGETS
				) 
			}
		}
	}

	fun toggleTargetSource(source: MangaSource) {
		_state.update { current ->
			current.copy(
				selectedTargetSourceKeys = current.selectedTargetSourceKeys.toMutableSet().apply {
					if (!add(source.name)) remove(source.name)
				}
			)
		}
	}

	fun prepareComparison() {
		val mangas = state.value.mangaList.filter { it.id in state.value.selectedMangaIds }
		val targetSources = state.value.availableTargetSources.filter { it.name in state.value.selectedTargetSourceKeys }
		
		if (mangas.isEmpty() || targetSources.isEmpty()) return

		viewModelScope.launch {
			_state.update { it.copy(isLoading = true, step = MassMigrateStep.COMPARISON) }
			
			val items = withContext(Dispatchers.IO) {
				mangas.map { manga ->
					val best = findBestAlternative(manga, targetSources)
					MigrationPairKEx(original = manga, alternative = best)
				}
			}

			_state.update { it.copy(isLoading = false, comparisonItems = items) }
		}
	}

	fun removeItemFromComparison(originalId: Long) {
		_state.update { current ->
			current.copy(comparisonItems = current.comparisonItems.filter { it.original.id != originalId })
		}
	}

	fun updateAlternative(originalId: Long, newAlternative: Manga) {
		_state.update { current ->
			current.copy(
				comparisonItems = current.comparisonItems.map { item ->
					if (item.original.id == originalId) item.copy(alternative = newAlternative) else item
				}
			)
		}
	}

	fun executeFinalMigration() {
		val items = state.value.comparisonItems.filter { it.alternative != null }
		if (items.isEmpty()) return

		viewModelScope.launch {
			_state.update { 
				it.copy(
					step = MassMigrateStep.MIGRATING, 
					totalToMigrate = items.size, 
					migratedCount = 0,
					successCount = 0,
					failedCount = 0,
					failedMangas = emptyList()
				) 
			}
			
			var successes = 0
			var failures = 0
			val failuresList = mutableListOf<Manga>()

			for (item in items) {
				val alt = item.alternative
				val success = if (alt != null) {
					runCatchingCancellable {
						massMigrateUseCase(item.original, alt)
						true
					}.getOrDefault(false)
				} else {
					false
				}

				if (success) {
					successes++
				} else {
					failures++
					failuresList.add(item.original)
				}
				_state.update { 
					it.copy(
						migratedCount = it.migratedCount + 1,
						successCount = successes,
						failedCount = failures,
						failedMangas = failuresList.toList()
					) 
				}
			}
			_state.update { it.copy(step = MassMigrateStep.COMPLETED) }
		}
	}

	private suspend fun findBestAlternative(manga: Manga, targets: List<MangaSource>): Manga? {
		return withContext(Dispatchers.IO) {
			val candidates = mutableListOf<Manga>()
			for (source in targets) {
				val helper = searchHelperFactory.create(source)
				val result = runCatchingCancellable {
					helper(manga.title, SearchKind.TITLE)?.manga
				}.getOrNull()
				result?.forEach { candidates.add(it) }
			}
			
			candidates.filter { it.id != manga.id }
				.map { m ->
					runCatchingCancellable {
						mangaRepositoryFactory.create(m.source).getDetails(m)
					}.getOrDefault(m)
				}
				.maxByOrNull { it.chaptersCount() }
		}
	}

	fun navigateBack(): Boolean {
		val currentStep = state.value.step
		return if (currentStep == MassMigrateStep.SELECT_SOURCE) {
			false
		} else {
			_state.update { 
				it.copy(step = when(currentStep) {
					MassMigrateStep.SELECT_MANGA -> MassMigrateStep.SELECT_SOURCE
					MassMigrateStep.SELECT_TARGETS -> MassMigrateStep.SELECT_MANGA
					MassMigrateStep.COMPARISON -> MassMigrateStep.SELECT_TARGETS
					MassMigrateStep.MIGRATING -> MassMigrateStep.COMPARISON
					MassMigrateStep.COMPLETED -> MassMigrateStep.SELECT_SOURCE
					else -> MassMigrateStep.SELECT_SOURCE
				}) 
			}
			true
		}
	}
}

data class MassMigrateState(
	val step: MassMigrateStep = MassMigrateStep.SELECT_SOURCE,
	val isLoading: Boolean = false,
	val sources: List<LibrarySourceOptionKEx> = emptyList(),
	val selectedSourceKey: String? = null,
	val mangaList: List<Manga> = emptyList(),
	val selectedMangaIds: Set<Long> = emptySet(),
	val availableTargetSources: List<MangaSource> = emptyList(),
	val selectedTargetSourceKeys: Set<String> = emptySet(),
	val comparisonItems: List<MigrationPairKEx> = emptyList(),
	val totalToMigrate: Int = 0,
	val migratedCount: Int = 0,
	val successCount: Int = 0,
	val failedCount: Int = 0,
	val failedMangas: List<Manga> = emptyList()
)

data class MigrationPairKEx(
	val original: Manga,
	val alternative: Manga?
)

enum class MassMigrateStep {
	SELECT_SOURCE,
	SELECT_MANGA,
	SELECT_TARGETS,
	COMPARISON,
	MIGRATING,
	COMPLETED
}

data class LibrarySourceOptionKEx(
	val key: String,
	val title: String,
	val mangaCount: Int
)
