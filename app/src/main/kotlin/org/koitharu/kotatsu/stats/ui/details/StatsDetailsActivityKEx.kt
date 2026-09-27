package org.koitharu.kotatsu.stats.ui.details

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePaddingRelative
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.BaseActivity
import org.koitharu.kotatsu.core.util.ext.end
import org.koitharu.kotatsu.core.util.ext.start
import org.koitharu.kotatsu.databinding.ActivityStatsDetailsKexBinding
import org.koitharu.kotatsu.settings.compose.DropSauceTheme

@AndroidEntryPoint
class StatsDetailsActivityKEx : BaseActivity<ActivityStatsDetailsKexBinding>() {

	private val viewModel: StatsDetailsViewModelKEx by viewModels()

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
		setContentView(ActivityStatsDetailsKexBinding.inflate(layoutInflater))
		setDisplayHomeAsUp(isEnabled = true, showUpAsClose = false)
		setTitle(R.string.statistics_details)

		viewBinding.composeView.setViewCompositionStrategy(
			ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed,
		)
		viewBinding.composeView.setContent {
			DropSauceTheme {
				val density = LocalDensity.current
				StatsDetailsScreenKEx(
					viewModel = viewModel,
					bottomInset = with(density) { bottomInset.intValue.toDp() },
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
