package org.koitharu.kotatsu.mangadex.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import coil3.ImageLoader
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.core.nav.router
import org.koitharu.kotatsu.settings.compose.DropSauceTheme
import javax.inject.Inject

@AndroidEntryPoint
class MangaDexCoversActivityKEx : AppCompatActivity() {

	@Inject lateinit var coil: ImageLoader

	private val viewModel: MangaDexCoversViewModelKEx by viewModels()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		WindowCompat.setDecorFitsSystemWindows(window, false)

		setContent {
			DropSauceTheme {
				MangaDexCoversScreenKEx(
					viewModel = viewModel,
					imageLoader = coil,
					onBackClick = { finish() },
					onCoverClick = { cover ->
						val manga = viewModel.manga
						if (manga != null) {
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
}
