package org.koitharu.kotatsu.massmigrate

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
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
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.exceptions.resolve.SnackbarErrorObserver
import org.koitharu.kotatsu.core.model.parcelable.ParcelableManga
import org.koitharu.kotatsu.core.nav.AppRouter
import org.koitharu.kotatsu.core.nav.router
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.ui.BaseActivity
import org.koitharu.kotatsu.core.ui.dialog.buildAlertDialog
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.ui.widgets.TipView
import org.koitharu.kotatsu.core.util.ext.consumeAllSystemBarsInsets
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.observeEvent
import org.koitharu.kotatsu.core.util.ext.systemBarsInsets
import org.koitharu.kotatsu.databinding.ActivityAlternativesBinding
import org.koitharu.kotatsu.list.domain.ListFilterOption
import org.koitharu.kotatsu.list.ui.MangaSelectionDecoration
import org.koitharu.kotatsu.list.ui.adapter.MangaListListener
import org.koitharu.kotatsu.list.ui.adapter.TypedListSpacingDecoration
import org.koitharu.kotatsu.list.ui.model.ListHeader
import org.koitharu.kotatsu.list.ui.model.MangaListModel
import org.koitharu.kotatsu.list.ui.size.DynamicItemSizeResolver
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaTag
import org.koitharu.kotatsu.search.ui.multi.SearchViewModel
import org.koitharu.kotatsu.search.ui.multi.SearchResultsListModel
import org.koitharu.kotatsu.search.ui.multi.adapter.SearchAdapter
import org.koitharu.kotatsu.settings.compose.DropSauceTheme
import javax.inject.Inject

const val KEY_ORIGINAL_ID = "original_id"

@AndroidEntryPoint
class AlternativesSearchActivityKEx :
	BaseActivity<ActivityAlternativesBinding>(),
	MangaListListener {

	@Inject
	lateinit var settings: AppSettings

	private val viewModel by viewModels<SearchViewModel>()

	private val subSearchLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
		if (result.resultCode == RESULT_OK) {
			setResult(RESULT_OK, result.data)
			finish()
		}
	}

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
		setTitle(viewModel.query)

		val itemClickListener = OnListItemClickListener<SearchResultsListModel> { item, _ ->
			val selectedManga = item.list?.firstOrNull()?.toMangaWithOverride() ?: return@OnListItemClickListener
			val originalId = intent.getLongExtra(KEY_ORIGINAL_ID, 0L)
			setResult(RESULT_OK, Intent().putExtra(KEY_ORIGINAL_ID, originalId).putExtra("selected_manga", ParcelableManga(selectedManga)))
			finish()
		}

		val sizeResolver = DynamicItemSizeResolver(resources, this, settings, adjustWidth = true)
		val selectionDecoration = MangaSelectionDecoration(this)
		val adapter = SearchAdapter(
			listener = this,
			itemClickListener = itemClickListener,
			sizeResolver = sizeResolver,
			selectionDecoration = selectionDecoration,
		)
		viewBinding.recyclerView.adapter = adapter
		viewBinding.recyclerView.setHasFixedSize(true)
		viewBinding.recyclerView.addItemDecoration(TypedListSpacingDecoration(this, true))

		setDisplayHomeAsUp(isEnabled = true, showUpAsClose = false)

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

		viewModel.list.observe(this, adapter)
		viewModel.onError.observeEvent(this, SnackbarErrorObserver(viewBinding.recyclerView, null))
	}

	private fun showEditQueryDialog() {
		val context = this
		val editText = EditText(context).apply {
			setText(viewModel.query)
			setSelection(text.length)
		}
		buildAlertDialog(context) {
			setTitle(R.string.search)
			setView(editText)
			setNegativeButton(android.R.string.cancel, null)
			setPositiveButton(android.R.string.ok) { _, _ ->
				val newQuery = editText.text.toString()
				if (newQuery.isNotBlank() && newQuery != viewModel.query) {
					val intent = Intent(context, AlternativesSearchActivityKEx::class.java)
						.putExtra(AppRouter.KEY_QUERY, newQuery)
						.putExtra(KEY_ORIGINAL_ID, intent.getLongExtra(KEY_ORIGINAL_ID, 0L))
					subSearchLauncher.launch(intent)
				}
			}
		}.show()
	}

	override fun onApplyWindowInsets(v: View, insets: WindowInsetsCompat): WindowInsetsCompat {
		val barsInsets = insets.systemBarsInsets
		viewBinding.appbar.updatePadding(
			top = barsInsets.top,
			left = barsInsets.left,
			right = barsInsets.right,
		)
		viewBinding.recyclerView.setPadding(
			left = barsInsets.left,
			top = 0,
			right = barsInsets.right,
			bottom = barsInsets.bottom,
		)
		return insets.consumeAllSystemBarsInsets()
	}

	override fun onItemClick(item: MangaListModel, view: View) {
		val originalId = intent.getLongExtra(KEY_ORIGINAL_ID, 0L)
		val selectedManga = item.toMangaWithOverride()
		setResult(RESULT_OK, Intent().putExtra(KEY_ORIGINAL_ID, originalId).putExtra("selected_manga", ParcelableManga(selectedManga)))
		finish()
	}

	override fun onItemLongClick(item: MangaListModel, view: View): Boolean {
		router.openDetails(item.toMangaWithOverride())
		return true
	}

	override fun onItemContextClick(item: MangaListModel, view: View): Boolean = false
	override fun onReadClick(manga: Manga, view: View) { router.openDetails(manga) }
	override fun onTagClick(manga: Manga, tag: MangaTag, view: View) {}
	override fun onRetryClick(error: Throwable) { viewModel.retry() }
	override fun onFilterOptionClick(option: ListFilterOption) = Unit
	override fun onFilterClick(view: View?) = Unit
	override fun onEmptyActionClick() = Unit
	override fun onListHeaderClick(item: ListHeader, view: View) = Unit
	override fun onFooterButtonClick() = Unit
	override fun onPrimaryButtonClick(tipView: TipView) = Unit
	override fun onSecondaryButtonClick(tipView: TipView) = Unit
}
