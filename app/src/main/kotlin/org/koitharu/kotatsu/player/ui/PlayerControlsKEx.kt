package org.koitharu.kotatsu.player.ui

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.media3.common.util.UnstableApi
import eu.kanade.tachiyomi.animesource.model.Video
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.player.utils.PlayerAudioTrackKEx
import org.koitharu.kotatsu.player.utils.PlayerSubtitleTrackKEx

@OptIn(ExperimentalMaterial3Api::class, UnstableApi::class)
@Composable
fun PlayerControlsKEx(
    isVisible: Boolean,
    title: String,
    subtitle: String,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    bufferedPosition: Long,
    videos: List<Video>,
    selectedVideo: Video?,
    audioTracks: List<PlayerAudioTrackKEx>,
    selectedAudioTrack: PlayerAudioTrackKEx?,
    subtitleTracks: List<PlayerSubtitleTrackKEx>,
    selectedSubtitleTrack: PlayerSubtitleTrackKEx?,
    speed: Float,
    resizeMode: Int,
    autoPlayNext: Boolean,
    subtitleFontSize: Float,
    subtitleTextColor: Int,
    subtitleBgColor: Int,
    subtitleBold: Boolean,
    subtitleOffsetMs: Long,
    chapters: List<MangaChapter>,
    currentChapter: MangaChapter?,
    hasPreviousChapter: Boolean,
    hasNextChapter: Boolean,
    topInset: Dp = 0.dp,
    bottomInset: Dp = 0.dp,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onRewind10: () -> Unit,
    onForward10: () -> Unit,
    onPreviousChapter: () -> Unit,
    onNextChapter: () -> Unit,
    onSelectVideo: (Video) -> Unit,
    onSelectAudioTrack: (PlayerAudioTrackKEx) -> Unit,
    onSelectSubtitleTrack: (PlayerSubtitleTrackKEx) -> Unit,
    onDisableSubtitles: () -> Unit,
    onImportSubtitleFile: () -> Unit,
    onSelectSpeed: (Float) -> Unit,
    onSelectResizeMode: (Int) -> Unit,
    onAutoPlayNextChange: (Boolean) -> Unit,
    onSubtitleFontSizeChange: (Float) -> Unit,
    onSubtitleTextColorChange: (Int) -> Unit,
    onSubtitleBgColorChange: (Int) -> Unit,
    onSubtitleBoldChange: (Boolean) -> Unit,
    onSubtitleOffsetChange: (Long) -> Unit,
    onSelectChapter: (MangaChapter) -> Unit,
    onEpisodesClick: () -> Unit,
    onEpisodesDismissed: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
	var showQualitySheet by remember { mutableStateOf(false) }
	var showAudioTracksSheet by remember { mutableStateOf(false) }
	var showSubtitleTracksSheet by remember { mutableStateOf(false) }
	var showSubtitleSettingsSheet by remember { mutableStateOf(false) }
	var showSpeedSheet by remember { mutableStateOf(false) }
	var showAspectSheet by remember { mutableStateOf(false) }
	var showEpisodesSheet by remember { mutableStateOf(false) }

	AnimatedVisibility(
		visible = isVisible,
		enter = fadeIn(),
		exit = fadeOut(),
		modifier = modifier,
	) {
		val effectiveTopInset = if (topInset > 0.dp) topInset else 28.dp
		val effectiveBottomInset = if (bottomInset > 0.dp) bottomInset else 20.dp

		Box(
			modifier = Modifier
				.fillMaxSize()
				.background(Color.Black.copy(alpha = 0.45f)),
		) {
			// Top Bar
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.align(Alignment.TopCenter)
					.padding(start = 16.dp, end = 16.dp, top = effectiveTopInset + 12.dp, bottom = 12.dp),
				verticalAlignment = Alignment.CenterVertically,
			) {
				IconButton(onClick = onBack) {
					Icon(
						imageVector = Icons.AutoMirrored.Filled.ArrowBack,
						contentDescription = "Back",
						tint = Color.White,
					)
				}
				Spacer(Modifier.width(8.dp))
				Column(modifier = Modifier.weight(1f)) {
					Text(
						text = title,
						style = MaterialTheme.typography.titleMedium,
						color = Color.White,
						fontWeight = FontWeight.Bold,
					)
					if (subtitle.isNotBlank()) {
						Text(
							text = subtitle,
							style = MaterialTheme.typography.bodySmall,
							color = Color.White.copy(alpha = 0.8f),
						)
					}
				}

				if (chapters.isNotEmpty()) {
					IconButton(
						onClick = {
							onEpisodesClick()
							showEpisodesSheet = true
						},
					) {
						Icon(
							imageVector = Icons.AutoMirrored.Filled.List,
							contentDescription = "Episodes",
							tint = Color.White,
						)
					}
				}

				if (videos.isNotEmpty()) {
					IconButton(onClick = { showQualitySheet = true }) {
						Icon(
							imageVector = Icons.Default.Settings,
							contentDescription = "Quality / Server",
							tint = Color.White,
						)
					}
				}

				IconButton(onClick = { showAudioTracksSheet = true }) {
					Icon(
						imageVector = Icons.Default.Audiotrack,
						contentDescription = "Audio Tracks",
						tint = Color.White,
					)
				}

				IconButton(onClick = { showSubtitleTracksSheet = true }) {
					Icon(
						imageVector = Icons.Default.Subtitles,
						contentDescription = "Subtitles",
						tint = if (selectedSubtitleTrack != null) MaterialTheme.colorScheme.primary else Color.White,
					)
				}

				IconButton(onClick = { showSpeedSheet = true }) {
					Icon(
						imageVector = Icons.Default.Speed,
						contentDescription = "Playback Speed",
						tint = Color.White,
					)
				}

				IconButton(onClick = { showAspectSheet = true }) {
					Icon(
						imageVector = Icons.Default.AspectRatio,
						contentDescription = "Aspect Ratio",
						tint = Color.White,
					)
				}
			}

			// Center Controls
			Row(
				modifier = Modifier.align(Alignment.Center),
				horizontalArrangement = Arrangement.spacedBy(16.dp),
				verticalAlignment = Alignment.CenterVertically,
			) {
				IconButton(
					onClick = onPreviousChapter,
					enabled = hasPreviousChapter,
					modifier = Modifier.size(48.dp),
				) {
					Icon(
						imageVector = Icons.Default.SkipPrevious,
						contentDescription = "Previous Episode",
						tint = if (hasPreviousChapter) Color.White else Color.White.copy(alpha = 0.38f),
						modifier = Modifier.size(32.dp),
					)
				}

				IconButton(
					onClick = onRewind10,
					modifier = Modifier.size(48.dp),
				) {
					Icon(
						imageVector = Icons.Default.FastRewind,
						contentDescription = "-10s",
						tint = Color.White,
						modifier = Modifier.size(32.dp),
					)
				}

				FilledIconButton(
					onClick = onTogglePlayPause,
					shape = RoundedCornerShape(24.dp),
					colors = IconButtonDefaults.filledIconButtonColors(
						containerColor = MaterialTheme.colorScheme.primary,
						contentColor = MaterialTheme.colorScheme.onPrimary,
					),
					modifier = Modifier.size(68.dp),
				) {
					Icon(
						imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
						contentDescription = if (isPlaying) "Pause" else "Play",
						modifier = Modifier.size(36.dp),
					)
				}

				IconButton(
					onClick = onForward10,
					modifier = Modifier.size(48.dp),
				) {
					Icon(
						imageVector = Icons.Default.FastForward,
						contentDescription = "+10s",
						tint = Color.White,
						modifier = Modifier.size(32.dp),
					)
				}

				IconButton(
					onClick = onNextChapter,
					enabled = hasNextChapter,
					modifier = Modifier.size(48.dp),
				) {
					Icon(
						imageVector = Icons.Default.SkipNext,
						contentDescription = "Next Episode",
						tint = if (hasNextChapter) Color.White else Color.White.copy(alpha = 0.38f),
						modifier = Modifier.size(32.dp),
					)
				}
			}

			// Bottom Bar
			Column(
				modifier = Modifier
					.fillMaxWidth()
					.align(Alignment.BottomCenter)
					.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = effectiveBottomInset + 12.dp),
			) {
				Row(
					modifier = Modifier.fillMaxWidth(),
					horizontalArrangement = Arrangement.SpaceBetween,
					verticalAlignment = Alignment.CenterVertically,
				) {
					Text(
						text = formatTime(currentPosition),
						style = MaterialTheme.typography.labelMedium,
						color = Color.White,
					)
					Text(
						text = formatTime(duration),
						style = MaterialTheme.typography.labelMedium,
						color = Color.White,
					)
				}
				Spacer(Modifier.height(4.dp))
				val progress = if (duration > 0) currentPosition.toFloat() / duration else 0f
				Slider(
					value = progress.coerceIn(0f, 1f),
					onValueChange = { onSeekTo((it * duration).toLong()) },
					colors = SliderDefaults.colors(
						thumbColor = MaterialTheme.colorScheme.primary,
						activeTrackColor = MaterialTheme.colorScheme.primary,
						inactiveTrackColor = Color.White.copy(alpha = 0.3f),
					),
					modifier = Modifier.fillMaxWidth(),
				)
			}
		}

		// Quality Bottom Sheet
		if (showQualitySheet) {
			QualitySheetKEx(
				videos = videos,
				selectedVideo = selectedVideo,
				onSelectVideo = onSelectVideo,
				onDismiss = { showQualitySheet = false },
			)
		}

		// Audio Tracks Bottom Sheet
		if (showAudioTracksSheet) {
			AudioTracksSheetKEx(
				audioTracks = audioTracks,
				onSelectAudioTrack = onSelectAudioTrack,
				onDismiss = { showAudioTracksSheet = false },
			)
		}

		// Subtitle Tracks Bottom Sheet
		if (showSubtitleTracksSheet) {
			SubtitleTracksSheetKEx(
				subtitleTracks = subtitleTracks,
				onSelectSubtitleTrack = onSelectSubtitleTrack,
				onDisableSubtitles = onDisableSubtitles,
				onImportSubtitleFile = onImportSubtitleFile,
				onOpenSubtitleSettings = {
					showSubtitleTracksSheet = false
					showSubtitleSettingsSheet = true
				},
				onDismiss = { showSubtitleTracksSheet = false },
			)
		}

		// Subtitle Settings Bottom Sheet
		if (showSubtitleSettingsSheet) {
			SubtitleSettingsSheetKEx(
				fontSizeSp = subtitleFontSize,
				textColor = subtitleTextColor,
				bgColor = subtitleBgColor,
				isBold = subtitleBold,
				subtitleOffsetMs = subtitleOffsetMs,
				onFontSizeChange = onSubtitleFontSizeChange,
				onTextColorChange = onSubtitleTextColorChange,
				onBgColorChange = onSubtitleBgColorChange,
				onBoldChange = onSubtitleBoldChange,
				onOffsetChange = onSubtitleOffsetChange,
				onDismiss = { showSubtitleSettingsSheet = false },
			)
		}

		// Playback Speed Bottom Sheet
		if (showSpeedSheet) {
			PlaybackSpeedSheetKEx(
				currentSpeed = speed,
				onSelectSpeed = onSelectSpeed,
				onDismiss = { showSpeedSheet = false },
			)
		}

		// Aspect Ratio / Fit Bottom Sheet
		if (showAspectSheet) {
			VideoAspectSheetKEx(
				currentResizeMode = resizeMode,
				onSelectResizeMode = onSelectResizeMode,
				onDismiss = { showAspectSheet = false },
			)
		}

		// Episodes Bottom Sheet
		if (showEpisodesSheet) {
			EpisodesSheetKEx(
				chapters = chapters,
				currentChapter = currentChapter,
				onSelectChapter = onSelectChapter,
				onDismiss = {
					showEpisodesSheet = false
					onEpisodesDismissed()
				},
			)
		}
	}
}

private fun formatTime(millis: Long): String {
	if (millis <= 0) return "00:00"
	val totalSeconds = millis / 1000
	val seconds = totalSeconds % 60
	val minutes = (totalSeconds / 60) % 60
	val hours = totalSeconds / 3600
	return if (hours > 0) {
		String.format(java.util.Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
	} else {
		String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
	}
}
