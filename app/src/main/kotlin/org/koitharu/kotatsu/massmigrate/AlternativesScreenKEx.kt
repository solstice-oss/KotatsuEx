package org.koitharu.kotatsu.massmigrate

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.MenuProvider
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import coil3.ImageLoader
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.alternatives.domain.AlternativesUseCase
import org.koitharu.kotatsu.alternatives.ui.MangaAlternativeModel
import org.koitharu.kotatsu.alternatives.ui.alternativeAD
import org.koitharu.kotatsu.core.exceptions.resolve.SnackbarErrorObserver
import org.koitharu.kotatsu.core.model.chaptersCount
import org.koitharu.kotatsu.core.model.getTitle
import org.koitharu.kotatsu.core.model.parcelable.ParcelableManga
import org.koitharu.kotatsu.core.nav.AppRouter
import org.koitharu.kotatsu.core.nav.router
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.core.prefs.ListMode
import org.koitharu.kotatsu.core.ui.BaseActivity
import org.koitharu.kotatsu.core.ui.BaseListAdapter
import org.koitharu.kotatsu.core.ui.BaseViewModel
import org.koitharu.kotatsu.core.ui.dialog.buildAlertDialog
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.util.ext.MutableEventFlow
import org.koitharu.kotatsu.core.util.ext.append
import org.koitharu.kotatsu.core.util.ext.call
import org.koitharu.kotatsu.core.util.ext.consumeAllSystemBarsInsets
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.observeEvent
import org.koitharu.kotatsu.core.util.ext.require
import org.koitharu.kotatsu.core.util.ext.roundTopCorners
import org.koitharu.kotatsu.core.util.ext.systemBarsInsets
import org.koitharu.kotatsu.databinding.ActivityAlternativesBinding
import org.koitharu.kotatsu.explore.data.MangaSourcesRepository
import org.koitharu.kotatsu.list.domain.MangaListMapper
import org.koitharu.kotatsu.list.ui.adapter.ListItemType
import org.koitharu.kotatsu.list.ui.adapter.ListStateHolderListener
import org.koitharu.kotatsu.list.ui.adapter.TypedListSpacingDecoration
import org.koitharu.kotatsu.list.ui.adapter.buttonFooterAD
import org.koitharu.kotatsu.list.ui.adapter.emptyStateListAD
import org.koitharu.kotatsu.list.ui.adapter.loadingFooterAD
import org.koitharu.kotatsu.list.ui.adapter.loadingStateAD
import org.koitharu.kotatsu.list.ui.model.EmptyState
import org.koitharu.kotatsu.list.ui.model.ListModel
import org.koitharu.kotatsu.list.ui.model.LoadingFooter
import org.koitharu.kotatsu.list.ui.model.LoadingState
import org.koitharu.kotatsu.list.ui.model.MangaGridModel
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.parsers.util.suspendlazy.getOrDefault
import org.koitharu.kotatsu.parsers.util.suspendlazy.suspendLazy
import org.koitharu.kotatsu.settings.compose.DropSauceTheme
import javax.inject.Inject

@AndroidEntryPoint
class AlternativesActivityKEx : BaseActivity<ActivityAlternativesBinding>(),
	ListStateHolderListener,
	OnListItemClickListener<MangaAlternativeModel> {

	@Inject
	lateinit var coil: ImageLoader

	private val viewModel by viewModels<AlternativesViewModelKEx>()

	override fun setTitle(title: CharSequence?) {
		super.setTitle(title)
		viewBinding.collapsingToolbarLayout.title = title
	}

	override fun setTitle(titleId: Int) {
		setTitle(getText(titleId))
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityAlternativesBinding.inflate(layoutInflater))
		setDisplayHomeAsUp(isEnabled = true, showUpAsClose = false)
		val listAdapter = BaseListAdapter<ListModel>()
			.addDelegate(ListItemType.MANGA_LIST_DETAILED, alternativeAD(coil, this, this))
			.addDelegate(ListItemType.STATE_EMPTY, emptyStateListAD(null))
			.addDelegate(ListItemType.FOOTER_LOADING, loadingFooterAD())
			.addDelegate(ListItemType.STATE_LOADING, loadingStateAD())
			.addDelegate(ListItemType.FOOTER_BUTTON, buttonFooterAD(this))
		with(viewBinding.recyclerView) {
			roundTopCorners(
				radius = resources.getDimension(R.dimen.tile_corner),
				extraInset = resources.getDimensionPixelOffset(R.dimen.list_spacing_normal),
			)
			setHasFixedSize(true)
			addItemDecoration(TypedListSpacingDecoration(context, addHorizontalPadding = false))
			adapter = listAdapter
		}

		title = viewModel.query ?: viewModel.manga.title

		viewBinding.composeViewFilter.setViewCompositionStrategy(
			ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed,
		)
		viewBinding.composeViewFilter.setContent {
			DropSauceTheme {
				val installedSources by viewModel.installedSources.collectAsState()
				val selectedKeys by viewModel.selectedSourceKeys.collectAsState()
				var showDialog by remember { mutableStateOf(false) }

				Row(
					modifier = Modifier
						.fillMaxWidth()
						.padding(horizontal = 16.dp, vertical = 6.dp),
				) {
					FilterChip(
						selected = selectedKeys.isNotEmpty(),
						onClick = { showDialog = true },
						label = {
							Text(
								text = if (selectedKeys.isEmpty()) {
									stringResource(R.string.source)
								} else {
									"${stringResource(R.string.source)} (${selectedKeys.size})"
								},
							)
						},
						leadingIcon = {
							Icon(
								painter = painterResource(R.drawable.ic_manga_source),
								contentDescription = null,
								modifier = Modifier.size(18.dp),
							)
						},
					)
				}

				if (showDialog) {
					SourcesFilterDialogKEx(
						installedSources = installedSources,
						initialSelectedKeys = selectedKeys,
						onDismiss = { showDialog = false },
						onApply = { keys ->
							viewModel.setSourceFilter(keys)
						},
					)
				}
			}
		}

		addMenuProvider(object : MenuProvider {
			override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
				menu.add(Menu.NONE, 1001, Menu.NONE, R.string.search).apply {
					setIcon(R.drawable.ic_search)
					setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
				}
			}

			override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
				if (menuItem.itemId == 1001) {
					showEditQueryDialog()
					return true
				}
				return false
			}
		})

		viewModel.onError.observeEvent(this, SnackbarErrorObserver(viewBinding.recyclerView, null))
		viewModel.list.observe(this, listAdapter)
		viewModel.onMigrated.observeEvent(this) {
			Toast.makeText(this, R.string.migration_completed, Toast.LENGTH_SHORT).show()
			router.openDetails(it)
			finishAfterTransition()
		}
	}

	private fun showEditQueryDialog() {
		val context = this
		val editText = EditText(context).apply {
			setText(viewModel.query ?: viewModel.manga.title)
			setSelection(text.length)
		}
		buildAlertDialog(context) {
			setTitle(R.string.search)
			setView(editText)
			setNegativeButton(android.R.string.cancel, null)
			setPositiveButton(android.R.string.ok) { _, _ ->
				val newQuery = editText.text.toString()
				if (newQuery.isNotBlank() && newQuery != (viewModel.query ?: viewModel.manga.title)) {
					val intent = Intent(context, AlternativesActivityKEx::class.java)
						.putExtra(AppRouter.KEY_MANGA, ParcelableManga(viewModel.manga))
						.putExtra(AppRouter.KEY_QUERY, newQuery)
					startActivity(intent)
					finish()
				}
			}
		}.show()
	}

	override fun onApplyWindowInsets(
		v: View,
		insets: WindowInsetsCompat
	): WindowInsetsCompat {
		val barsInsets = insets.systemBarsInsets
		viewBinding.appbar.updatePadding(
			left = barsInsets.left,
			right = barsInsets.right,
			top = barsInsets.top,
		)
		viewBinding.recyclerView.updatePadding(
			left = barsInsets.left,
			right = barsInsets.right,
			bottom = barsInsets.bottom,
		)
		return insets.consumeAllSystemBarsInsets()
	}

	override fun onItemClick(item: MangaAlternativeModel, view: View) {
		when (view.id) {
			R.id.chip_source -> router.openSearch(item.manga.source, viewModel.manga.title)
			R.id.button_migrate -> confirmMigration(item.manga)
			else -> router.openDetails(item.manga)
		}
	}

	override fun onRetryClick(error: Throwable) = viewModel.retry()

	override fun onEmptyActionClick() = Unit

	override fun onFooterButtonClick() = Unit

	private fun confirmMigration(target: Manga) {
		buildAlertDialog(this, isCentered = true) {
			setIcon(R.drawable.ic_swap)
			setTitle(R.string.manga_migration)
			setMessage(
				getString(
					R.string.migrate_confirmation,
					viewModel.manga.title,
					viewModel.manga.source.getTitle(context),
					target.title,
					target.source.getTitle(context),
				),
			)
			setNegativeButton(android.R.string.cancel, null)
			setPositiveButton(R.string.migrate) { _, _ ->
				viewModel.migrate(target)
			}
		}.show()
	}
}

@HiltViewModel
class AlternativesViewModelKEx @Inject constructor(
	savedStateHandle: SavedStateHandle,
	private val mangaRepositoryFactory: MangaRepository.Factory,
	private val alternativesUseCase: AlternativesUseCase,
	private val massMigrateUseCase: MassMigrateUseCaseKEx,
	private val mangaListMapper: MangaListMapper,
	private val sourcesRepository: MangaSourcesRepository,
) : BaseViewModel() {

	val manga = savedStateHandle.require<ParcelableManga>(AppRouter.KEY_MANGA).manga
	val query = savedStateHandle.get<String>(AppRouter.KEY_QUERY)

	val installedSources: StateFlow<List<MangaSource>> = sourcesRepository.observeEnabledSources()
		.map { list -> list.map { it.mangaSource } }
		.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, emptyList())

	val selectedSourceKeys = MutableStateFlow<Set<String>>(emptySet())

	private val results = MutableStateFlow<List<MangaAlternativeModel>>(emptyList())

	private var migrationJob: Job? = null
	private var searchJob: Job? = null

	private val mangaDetails = suspendLazy {
		mangaRepositoryFactory.create(manga.source).getDetails(manga)
	}

	val onMigrated = MutableEventFlow<Manga>()

	val list: StateFlow<List<ListModel>> = combine(
		results,
		isLoading,
	) { list, loading ->
		when {
			list.isEmpty() -> listOf(
				when {
					loading -> LoadingState
					else -> EmptyState(
						icon = R.drawable.ic_empty_common,
						textPrimary = R.string.nothing_found,
						textSecondary = R.string.text_search_holder_secondary,
						actionStringRes = 0,
					)
				},
			)

			loading -> list + LoadingFooter()
			else -> list
		}
	}.stateIn(viewModelScope + Dispatchers.Default, SharingStarted.Eagerly, listOf(LoadingState))

	init {
		doSearch()
	}

	fun setSourceFilter(keys: Set<String>) {
		if (selectedSourceKeys.value != keys) {
			selectedSourceKeys.value = keys
			retry()
		}
	}

	fun retry() {
		searchJob?.cancel()
		results.value = emptyList()
		doSearch()
	}

	fun migrate(target: Manga) {
		if (migrationJob?.isActive == true) {
			return
		}
		migrationJob = launchLoadingJob(Dispatchers.Default) {
			massMigrateUseCase(manga, target)
			onMigrated.call(target)
		}
	}

	private fun doSearch() {
		val prevJob = searchJob
		searchJob = launchLoadingJob(Dispatchers.Default) {
			prevJob?.cancelAndJoin()
			val ref = mangaDetails.getOrDefault(manga)
			val refCount = ref.chaptersCount()
			val keys = selectedSourceKeys.value
			val targetSources = if (keys.isNotEmpty()) {
				sourcesRepository.getEnabledSources().filter { it.name in keys }
			} else {
				null
			}
			alternativesUseCase.invoke(ref, query, targetSources)
				.collect {
					val model = MangaAlternativeModel(
						mangaModel = mangaListMapper.toListModel(it, ListMode.GRID) as MangaGridModel,
						referenceChapters = refCount,
					)
					results.append(model)
				}
		}
	}
}
