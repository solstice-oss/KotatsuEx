package org.koitharu.kotatsu.player.ui

import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.core.model.parcelable.ParcelableManga
import org.koitharu.kotatsu.core.nav.AppRouter
import org.koitharu.kotatsu.core.ui.BaseActivity
import org.koitharu.kotatsu.databinding.ActivityPlayerKexBinding
import org.koitharu.kotatsu.settings.compose.DropSauceTheme

@AndroidEntryPoint
class PlayerActivityKEx : BaseActivity<ActivityPlayerKexBinding>() {

	private val viewModel: PlayerViewModelKEx by viewModels()

	private val topInset = mutableIntStateOf(0)
	private val bottomInset = mutableIntStateOf(0)

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityPlayerKexBinding.inflate(layoutInflater))
		hideSystemUI()

		val parcelableManga: ParcelableManga? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			intent.getParcelableExtra(AppRouter.KEY_MANGA, ParcelableManga::class.java)
		} else {
			@Suppress("DEPRECATION")
			intent.getParcelableExtra(AppRouter.KEY_MANGA)
		}

		val chapterId = intent.getLongExtra(AppRouter.KEY_ID, -1L).takeIf { it != -1L }

		if (parcelableManga != null) {
			viewModel.initPlayer(parcelableManga.manga, chapterId)
		}

		viewBinding.composeViewKex.setViewCompositionStrategy(
			ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed,
		)
		viewBinding.composeViewKex.setContent {
			DropSauceTheme {
				val density = LocalDensity.current
				PlayerScreenKEx(
					viewModel = viewModel,
					topInset = with(density) { topInset.intValue.toDp() },
					bottomInset = with(density) { bottomInset.intValue.toDp() },
					onBack = { finish() },
				)
			}
		}
	}

	override fun onApplyWindowInsets(v: View, insets: WindowInsetsCompat): WindowInsetsCompat {
		val bars = insets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.systemBars())
		topInset.intValue = bars.top
		bottomInset.intValue = bars.bottom
		return insets
	}

	private fun hideSystemUI() {
		WindowCompat.setDecorFitsSystemWindows(window, false)
		WindowInsetsControllerCompat(window, window.decorView).apply {
			hide(WindowInsetsCompat.Type.systemBars())
			systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
		}
	}
}
