package org.koitharu.kotatsu.player.ui

import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.model.parcelable.ParcelableManga
import org.koitharu.kotatsu.core.nav.AppRouter
import org.koitharu.kotatsu.settings.compose.DropSauceTheme

@AndroidEntryPoint
class PlayerActivityKEx : AppCompatActivity() {

	private val viewModel: PlayerViewModelKEx by viewModels()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		setContentView(R.layout.activity_player_kex)
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

		findViewById<ComposeView>(R.id.compose_view_kex).setContent {
			DropSauceTheme {
				PlayerScreenKEx(
					viewModel = viewModel,
					onBack = { finish() },
				)
			}
		}
	}

	private fun hideSystemUI() {
		WindowCompat.setDecorFitsSystemWindows(window, false)
		WindowInsetsControllerCompat(window, window.decorView).apply {
			hide(WindowInsetsCompat.Type.systemBars())
			systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
		}
	}
}
