package org.koitharu.kotatsu.player.ui

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreenKEx(
	viewModel: PlayerViewModelKEx,
	topInset: Dp = 0.dp,
	bottomInset: Dp = 0.dp,
	onBack: () -> Unit,
) {
	val isPlaying by viewModel.isPlaying.collectAsState()
	val currentPosition by viewModel.currentPosition.collectAsState()
	val duration by viewModel.duration.collectAsState()
	val bufferedPosition by viewModel.bufferedPosition.collectAsState()
	val videos by viewModel.videos.collectAsState()
	val selectedVideo by viewModel.selectedVideo.collectAsState()
	val speed by viewModel.speed.collectAsState()
	val manga by viewModel.manga.collectAsState()
	val currentChapter by viewModel.currentChapter.collectAsState()
	val chapters by viewModel.chapters.collectAsState()

	var controlsVisible by remember { mutableStateOf(true) }

	LaunchedEffect(controlsVisible, isPlaying) {
		if (controlsVisible && isPlaying) {
			delay(4000)
			controlsVisible = false
		}
	}

	val player = remember { viewModel.getOrCreatePlayer() }

	DisposableEffect(Unit) {
		onDispose {
			// Player cleared in ViewModel
		}
	}

	Box(
		modifier = Modifier
			.fillMaxSize()
			.background(Color.Black),
	) {
		AndroidView(
			factory = { ctx ->
				PlayerView(ctx).apply {
					this.player = player
					useController = false
				}
			},
			modifier = Modifier.fillMaxSize(),
		)

		PlayerControlsKEx(
			isVisible = controlsVisible,
			title = manga?.title.orEmpty(),
			subtitle = currentChapter?.title ?: currentChapter?.numberString()?.let { "Episode $it" }.orEmpty(),
			isPlaying = isPlaying,
			currentPosition = currentPosition,
			duration = duration,
			bufferedPosition = bufferedPosition,
			videos = videos,
			selectedVideo = selectedVideo,
			speed = speed,
			chapters = chapters,
			currentChapter = currentChapter,
			topInset = topInset,
			bottomInset = bottomInset,
			onTogglePlayPause = { viewModel.togglePlayPause() },
			onSeekTo = { viewModel.seekTo(it) },
			onRewind10 = { viewModel.rewind10() },
			onForward10 = { viewModel.forward10() },
			onSelectVideo = { viewModel.playVideo(it) },
			onSelectSpeed = { viewModel.setSpeed(it) },
			onSelectChapter = { viewModel.playChapter(it) },
			onBack = onBack,
			onBackgroundClick = { controlsVisible = !controlsVisible },
		)
	}
}
