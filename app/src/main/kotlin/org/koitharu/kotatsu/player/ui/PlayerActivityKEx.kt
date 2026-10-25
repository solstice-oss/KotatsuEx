package org.koitharu.kotatsu.player.ui

import android.app.PictureInPictureParams
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.View
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
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

	private val noisyReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context?, intent: Intent?) {
			if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
				viewModel.pause()
			}
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityPlayerKexBinding.inflate(layoutInflater))
		hideSystemUI()

		// Keep screen on during playback
		window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

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

		// Sync screen brightness gesture with Window
		lifecycleScope.launch {
			viewModel.currentBrightnessPercent.collectLatest { percent ->
				val lp = window.attributes
				lp.screenBrightness = percent.coerceIn(0.01f, 1.0f)
				window.attributes = lp
			}
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

	override fun onStart() {
		super.onStart()
		val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
		ContextCompat.registerReceiver(
			this,
			noisyReceiver,
			filter,
			ContextCompat.RECEIVER_NOT_EXPORTED,
		)
	}

	override fun onStop() {
		super.onStop()
		try {
			unregisterReceiver(noisyReceiver)
		} catch (e: Exception) {
			// Already unregistered
		}
	}

	override fun onUserLeaveHint() {
		super.onUserLeaveHint()
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
			packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE) &&
			viewModel.isPlaying.value
		) {
			val params = PictureInPictureParams.Builder()
				.setAspectRatio(Rational(16, 9))
				.build()
			enterPictureInPictureMode(params)
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
