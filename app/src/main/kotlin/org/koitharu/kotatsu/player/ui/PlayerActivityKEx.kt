package org.koitharu.kotatsu.player.ui

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
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

		hideSystemUI()

		setContentView(R.layout.activity_player_kex)

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

	@Suppress("DEPRECATION")
	private fun hideSystemUI() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			window.insetsController?.let {
				it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
				it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
			}
		} else {
			window.decorView.systemUiVisibility = (
				View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
					or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
					or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
					or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
					or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
					or View.SYSTEM_UI_FLAG_FULLSCREEN
				)
		}
	}
}
