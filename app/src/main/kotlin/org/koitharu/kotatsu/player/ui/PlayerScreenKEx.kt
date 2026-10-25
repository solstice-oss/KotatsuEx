package org.koitharu.kotatsu.player.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import org.koitharu.kotatsu.player.utils.SubtitleUtilsKEx

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
	val errorMessage by viewModel.errorMessage.collectAsState()
	val videos by viewModel.videos.collectAsState()
	val selectedVideo by viewModel.selectedVideo.collectAsState()
	val audioTracks by viewModel.audioTracks.collectAsState()
	val selectedAudioTrack by viewModel.selectedAudioTrack.collectAsState()
	val subtitleTracks by viewModel.subtitleTracks.collectAsState()
	val selectedSubtitleTrack by viewModel.selectedSubtitleTrack.collectAsState()
	val speed by viewModel.speed.collectAsState()
	val resizeMode by viewModel.resizeMode.collectAsState()
	val autoPlayNext by viewModel.autoPlayNext.collectAsState()
	val subtitleFontSize by viewModel.subtitleFontSize.collectAsState()
	val subtitleTextColor by viewModel.subtitleTextColor.collectAsState()
	val subtitleBgColor by viewModel.subtitleBgColor.collectAsState()
	val subtitleBold by viewModel.subtitleBold.collectAsState()
	val subtitleOffsetMs by viewModel.subtitleOffsetMs.collectAsState()
	val currentVolumePercent by viewModel.currentVolumePercent.collectAsState()
	val currentBrightnessPercent by viewModel.currentBrightnessPercent.collectAsState()
	val manga by viewModel.manga.collectAsState()
	val currentChapter by viewModel.currentChapter.collectAsState()
	val chapters by viewModel.chapters.collectAsState()

	var controlsVisible by remember { mutableStateOf(true) }
	var wasPlayingBeforeEpisodesSheet by remember { mutableStateOf(false) }

	val subtitlePickerLauncher = rememberLauncherForActivityResult(
		contract = ActivityResultContracts.OpenDocument(),
	) { uri: Uri? ->
		if (uri != null) {
			viewModel.importSubtitleUri(uri)
		}
	}

	LaunchedEffect(controlsVisible, isPlaying) {
		if (controlsVisible && isPlaying) {
			delay(4500)
			controlsVisible = false
		}
	}

	val player = remember { viewModel.getOrCreatePlayer() }

	DisposableEffect(Unit) {
		onDispose {
			// Cleaned up in ViewModel
		}
	}

	Box(
		modifier = Modifier
			.fillMaxSize()
			.background(Color.Black),
	) {
		// Player Video View
		AndroidView(
			factory = { ctx ->
				PlayerView(ctx).apply {
					this.player = player
					this.useController = false
					this.resizeMode = resizeMode
					SubtitleUtilsKEx.applySubtitleStyle(
						this,
						subtitleFontSize,
						subtitleTextColor,
						subtitleBgColor,
						subtitleBold,
					)
				}
			},
			update = { view ->
				view.resizeMode = resizeMode
				SubtitleUtilsKEx.applySubtitleStyle(
					view,
					subtitleFontSize,
					subtitleTextColor,
					subtitleBgColor,
					subtitleBold,
				)
			},
			modifier = Modifier.fillMaxSize(),
		)

		// Full-screen Gesture Handler
		GestureHandlerKEx(
			onTap = { controlsVisible = !controlsVisible },
			onDoubleTapLeft = { viewModel.rewind10() },
			onDoubleTapRight = { viewModel.forward10() },
			onVolumeChangeDelta = { delta ->
				val newVolume = (currentVolumePercent + delta).coerceIn(0f, 1f)
				viewModel.setVolumePercent(newVolume)
			},
			onBrightnessChangeDelta = { delta ->
				val newBrightness = (currentBrightnessPercent + delta).coerceIn(0f, 1f)
				viewModel.setBrightnessPercent(newBrightness)
			},
			onScrubSeekDelta = { deltaMs ->
				viewModel.scrubSeek(deltaMs)
			},
			currentVolumePercent = currentVolumePercent,
			currentBrightnessPercent = currentBrightnessPercent,
			modifier = Modifier.fillMaxSize(),
		)

		// Error Banner
		AnimatedVisibility(
			visible = errorMessage != null,
			enter = fadeIn(),
			exit = fadeOut(),
			modifier = Modifier
				.align(Alignment.TopCenter)
				.padding(top = topInset + 64.dp, start = 24.dp, end = 24.dp),
		) {
			Surface(
				shape = RoundedCornerShape(12.dp),
				color = MaterialTheme.colorScheme.errorContainer,
				contentColor = MaterialTheme.colorScheme.onErrorContainer,
			) {
				Row(
					modifier = Modifier
						.fillMaxWidth()
						.padding(16.dp),
					verticalAlignment = Alignment.CenterVertically,
				) {
					Icon(
						imageVector = Icons.Default.ErrorOutline,
						contentDescription = "Error",
						tint = MaterialTheme.colorScheme.error,
					)
					Spacer(Modifier.width(12.dp))
					Text(
						text = errorMessage ?: "Playback error",
						style = MaterialTheme.typography.bodyMedium,
						fontWeight = FontWeight.Bold,
					)
				}
			}
		}

		// Overlay Controls
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
			audioTracks = audioTracks,
			selectedAudioTrack = selectedAudioTrack,
			subtitleTracks = subtitleTracks,
			selectedSubtitleTrack = selectedSubtitleTrack,
			speed = speed,
			resizeMode = resizeMode,
			autoPlayNext = autoPlayNext,
			subtitleFontSize = subtitleFontSize,
			subtitleTextColor = subtitleTextColor,
			subtitleBgColor = subtitleBgColor,
			subtitleBold = subtitleBold,
			subtitleOffsetMs = subtitleOffsetMs,
			chapters = chapters,
			currentChapter = currentChapter,
			hasPreviousChapter = viewModel.hasPreviousChapter(),
			hasNextChapter = viewModel.hasNextChapter(),
			topInset = topInset,
			bottomInset = bottomInset,
			onTogglePlayPause = { viewModel.togglePlayPause() },
			onSeekTo = { viewModel.seekTo(it) },
			onRewind10 = { viewModel.rewind10() },
			onForward10 = { viewModel.forward10() },
			onPreviousChapter = { viewModel.playPreviousChapter() },
			onNextChapter = { viewModel.playNextChapter() },
			onSelectVideo = { viewModel.playVideo(it) },
			onSelectAudioTrack = { viewModel.selectAudioTrack(it) },
			onSelectSubtitleTrack = { viewModel.selectSubtitleTrack(it) },
			onDisableSubtitles = { viewModel.disableSubtitles() },
			onImportSubtitleFile = {
				subtitlePickerLauncher.launch(arrayOf("text/*", "application/*", "*/*"))
			},
			onSelectSpeed = { viewModel.setSpeed(it) },
			onSelectResizeMode = { viewModel.setResizeMode(it) },
			onAutoPlayNextChange = { viewModel.setAutoPlayNext(it) },
			onSubtitleFontSizeChange = { viewModel.setSubtitleFontSize(it) },
			onSubtitleTextColorChange = { viewModel.setSubtitleTextColor(it) },
			onSubtitleBgColorChange = { viewModel.setSubtitleBgColor(it) },
			onSubtitleBoldChange = { viewModel.setSubtitleBold(it) },
			onSubtitleOffsetChange = { viewModel.setSubtitleOffsetMs(it) },
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
