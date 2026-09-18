package org.koitharu.kotatsu.massmigrate

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.core.view.WindowInsetsCompat
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.exceptions.resolve.SnackbarErrorObserver
import org.koitharu.kotatsu.core.model.parcelable.ParcelableManga
import org.koitharu.kotatsu.core.nav.router
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.ui.BaseActivity
import org.koitharu.kotatsu.core.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.core.ui.widgets.TipView
import org.koitharu.kotatsu.core.util.ext.consumeAllSystemBarsInsets
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.observeEvent
import org.koitharu.kotatsu.core.util.ext.systemBarsInsets
import org.koitharu.kotatsu.databinding.ActivitySearchBinding
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
import javax.inject.Inject

const val KEY_ORIGINAL_ID = "original_id"

@AndroidEntryPoint
class AlternativesSearchActivityKEx :
	BaseActivity<ActivitySearchBinding>(),
	MangaListListener {

	@Inject
	lateinit var settings: AppSettings

	private val viewModel by viewModels<SearchViewModel>()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivitySearchBinding.inflate(layoutInflater))
		title = viewModel.query

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
		supportActionBar?.setSubtitle(R.string.search_results)

		viewModel.list.observe(this, adapter)
		viewModel.onError.observeEvent(this, SnackbarErrorObserver(viewBinding.recyclerView, null))
	}

	override fun onApplyWindowInsets(v: View, insets: WindowInsetsCompat): WindowInsetsCompat {
		val barsInsets = insets.systemBarsInsets
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
