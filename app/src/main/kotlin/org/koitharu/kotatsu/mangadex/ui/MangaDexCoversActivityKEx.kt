package org.koitharu.kotatsu.mangadex.ui

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePaddingRelative
import coil3.ImageLoader
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.nav.router
import org.koitharu.kotatsu.core.ui.BaseActivity
import org.koitharu.kotatsu.core.util.ext.end
import org.koitharu.kotatsu.core.util.ext.start
import org.koitharu.kotatsu.databinding.ActivityMangadexCoversKexBinding
import org.koitharu.kotatsu.settings.compose.DropSauceTheme
import javax.inject.Inject

@AndroidEntryPoint
class MangaDexCoversActivityKEx : BaseActivity<ActivityMangadexCoversKexBinding>() {

	@Inject lateinit var coil: ImageLoader

	private val viewModel: MangaDexCoversViewModelKEx by viewModels()

	private val bottomInset = mutableIntStateOf(0)

	override fun setTitle(title: CharSequence?) {
		super.setTitle(title)
		viewBinding.collapsingToolbarLayout.title = title
	}

	override fun setTitle(titleId: Int) {
		setTitle(getText(titleId))
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityMangadexCoversKexBinding.inflate(layoutInflater))
		setDisplayHomeAsUp(isEnabled = true, showUpAsClose = false)
		setTitle(R.string.mangadex_covers)

		viewBinding.composeView.setViewCompositionStrategy(
			ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed,
		)
		viewBinding.composeView.setContent {
			DropSauceTheme {
				val density = LocalDensity.current
				MangaDexCoversScreenKEx(
					viewModel = viewModel,
					imageLoader = coil,
					bottomInset = with(density) { bottomInset.intValue.toDp() },
					onCoverClick = { cover ->
						viewModel.manga?.let { manga ->
							router.openImage(
								url = cover.url,
								source = manga.source,
								manga = manga,
							)
						}
					},
				)
			}
		}
	}

	override fun onApplyWindowInsets(v: View, insets: WindowInsetsCompat): WindowInsetsCompat {
		val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
		viewBinding.appbar.updatePaddingRelative(
			start = bars.start(v),
			top = bars.top,
			end = bars.end(v),
		)
		bottomInset.intValue = bars.bottom
		return insets
	}
}
