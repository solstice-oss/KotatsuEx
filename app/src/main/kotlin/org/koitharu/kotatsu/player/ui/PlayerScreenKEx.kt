package org.koitharu.kotatsu.player.ui

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
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
	var showRewindIndicator by remember { mutableStateOf(false) }
	var showForwardIndicator by remember { mutableStateOf(false) }
	var wasPlayingBeforeEpisodesSheet by remember { mutableStateOf(false) }

	LaunchedEffect(controlsVisible, isPlaying) {
		if (controlsVisible && isPlaying) {
			delay(4000)
			controlsVisible = false
		}
	}

	LaunchedEffect(showRewindIndicator) {
		if (showRewindIndicator) {
			delay(600)
			showRewindIndicator = false
		}
	}

	LaunchedEffect(showForwardIndicator) {
		if (showForwardIndicator) {
			delay(600)
			showForwardIndicator = false
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

		// Full-screen gesture detector for tap (show/hide controls) and double-tap (-10s / +10s seek)
		Box(
			modifier = Modifier
				.fillMaxSize()
				.pointerInput(Unit) {
					detectTapGestures(
						onTap = {
							controlsVisible = !controlsVisible
						},
						onDoubleTap = { offset ->
							if (offset.x < size.width / 2f) {
								showRewindIndicator = true
								viewModel.rewind10()
							} else {
								showForwardIndicator = true
								viewModel.forward10()
							}
						},
					)
				},
		)

		// Double-tap visual indicators
		AnimatedVisibility(
			visible = showRewindIndicator,
			enter = fadeIn(),
			exit = fadeOut(),
			modifier = Modifier
				.align(Alignment.CenterStart)
				.padding(start = 48.dp),
		) {
			Surface(
				shape = CircleShape,
				color = Color.Black.copy(alpha = 0.65f),
			) {
				Column(
					modifier = Modifier.padding(20.dp),
					horizontalAlignment = Alignment.CenterHorizontally,
				) {
					Icon(
						imageVector = Icons.Default.FastRewind,
						contentDescription = "-10s",
						tint = Color.White,
						modifier = Modifier.size(32.dp),
					)
					Spacer(Modifier.height(4.dp))
					Text(
						text = "-10s",
						style = MaterialTheme.typography.labelMedium,
						fontWeight = FontWeight.Bold,
						color = Color.White,
					)
				}
			}
		}

		AnimatedVisibility(
			visible = showForwardIndicator,
			enter = fadeIn(),
			exit = fadeOut(),
			modifier = Modifier
				.align(Alignment.CenterEnd)
				.padding(end = 48.dp),
		) {
			Surface(
				shape = CircleShape,
				color = Color.Black.copy(alpha = 0.65f),
			) {
				Column(
					modifier = Modifier.padding(20.dp),
					horizontalAlignment = Alignment.CenterHorizontally,
				) {
					Icon(
						imageVector = Icons.Default.FastForward,
						contentDescription = "+10s",
						tint = Color.White,
						modifier = Modifier.size(32.dp),
					)
					Spacer(Modifier.height(4.dp))
					Text(
						text = "+10s",
						style = MaterialTheme.typography.labelMedium,
						fontWeight = FontWeight.Bold,
						color = Color.White,
					)
				}
			}
		}

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
			hasPreviousChapter = viewModel.hasPreviousChapter(),
			hasNextChapter = viewModel.hasNextChapter(),
			topInset = topInset,
			bottomInset = bottomInset,
			onTogglePlayPause = { viewModel.togglePlayPause() },
			onSeekTo = { viewModel.seekTo(it) },
			onPreviousChapter = { viewModel.playPreviousChapter() },
			onNextChapter = { viewModel.playNextChapter() },
			onSelectVideo = { viewModel.playVideo(it) },
			onSelectSpeed = { viewModel.setSpeed(it) },
			onSelectChapter = { viewModel.playChapter(it) },
			onEpisodesClick = {
				wasPlayingBeforeEpisodesSheet = isPlaying
				if (isPlaying) {
					viewModel.pause()
				}
			},
			onEpisodesDismissed = {
				if (wasPlayingBeforeEpisodesSheet) {
					viewModel.play()
				}
			},
			onBack = onBack,
		)
	}
}
