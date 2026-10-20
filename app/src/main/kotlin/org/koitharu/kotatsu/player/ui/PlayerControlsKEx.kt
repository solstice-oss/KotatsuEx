package org.koitharu.kotatsu.player.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.tachiyomi.animesource.model.Video
import org.koitharu.kotatsu.parsers.model.MangaChapter

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
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
	speed: Float,
	chapters: List<MangaChapter>,
	currentChapter: MangaChapter?,
	onTogglePlayPause: () -> Unit,
	onSeekTo: (Long) -> Unit,
	onRewind10: () -> Unit,
	onForward10: () -> Unit,
	onSelectVideo: (Video) -> Unit,
	onSelectSpeed: (Float) -> Unit,
	onSelectChapter: (MangaChapter) -> Unit,
	onBack: () -> Unit,
	onBackgroundClick: () -> Unit,
	modifier: Modifier = Modifier,
) {
	var showQualitySheet by remember { mutableStateOf(false) }
	var showSpeedSheet by remember { mutableStateOf(false) }
	var showChapterSheet by remember { mutableStateOf(false) }

	AnimatedVisibility(
		visible = isVisible,
		enter = fadeIn(),
		exit = fadeOut(),
		modifier = modifier,
	) {
		Box(
			modifier = Modifier
				.fillMaxSize()
				.background(Color.Black.copy(alpha = 0.45f))
				.clickable(
					interactionSource = remember { MutableInteractionSource() },
					indication = null,
					onClick = onBackgroundClick,
				),
		) {
			// Top Bar
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.align(Alignment.TopCenter)
					.padding(horizontal = 16.dp, vertical = 24.dp),
				verticalAlignment = Alignment.CenterVertically,
			) {
				IconButton(onClick = onBack) {
					Icon(
						imageVector = Icons.Default.ArrowBack,
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
				if (videos.isNotEmpty()) {
					IconButton(onClick = { showQualitySheet = true }) {
						Icon(
							imageVector = Icons.Default.Settings,
							contentDescription = "Quality",
							tint = Color.White,
						)
					}
				}
				IconButton(onClick = { showSpeedSheet = true }) {
					Icon(
						imageVector = Icons.Default.Speed,
						contentDescription = "Speed",
						tint = Color.White,
					)
				}
			}

			// Center Controls
			Row(
				modifier = Modifier.align(Alignment.Center),
				horizontalArrangement = Arrangement.spacedBy(24.dp),
				verticalAlignment = Alignment.CenterVertically,
			) {
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
			}

			// Bottom Bar
			Column(
				modifier = Modifier
					.fillMaxWidth()
					.align(Alignment.BottomCenter)
					.padding(horizontal = 24.dp, vertical = 24.dp),
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
			ModalBottomSheet(
				onDismissRequest = { showQualitySheet = false },
			) {
				Column(modifier = Modifier.padding(16.dp)) {
					Text(
						text = "Video Quality / Server",
						style = MaterialTheme.typography.titleMedium,
						fontWeight = FontWeight.Bold,
						modifier = Modifier.padding(bottom = 12.dp),
					)
					LazyColumn {
						items(videos) { video ->
							val isSelected = video == selectedVideo
							Surface(
								onClick = {
									onSelectVideo(video)
									showQualitySheet = false
								},
								shape = RoundedCornerShape(12.dp),
								color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
								modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
							) {
								Text(
									text = video.videoTitle.ifBlank { video.quality.ifBlank { "Stream" } },
									style = MaterialTheme.typography.bodyMedium,
									modifier = Modifier.padding(16.dp),
								)
							}
						}
					}
				}
			}
		}

		// Speed Bottom Sheet
		if (showSpeedSheet) {
			ModalBottomSheet(
				onDismissRequest = { showSpeedSheet = false },
			) {
				Column(modifier = Modifier.padding(16.dp)) {
					Text(
						text = "Playback Speed",
						style = MaterialTheme.typography.titleMedium,
						fontWeight = FontWeight.Bold,
						modifier = Modifier.padding(bottom = 12.dp),
					)
					listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { s ->
						val isSelected = speed == s
						Surface(
							onClick = {
								onSelectSpeed(s)
								showSpeedSheet = false
							},
							shape = RoundedCornerShape(12.dp),
							color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
							modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
						) {
							Text(
								text = "${s}x",
								style = MaterialTheme.typography.bodyMedium,
								modifier = Modifier.padding(16.dp),
							)
						}
					}
				}
			}
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
		String.format("%d:%02d:%02d", hours, minutes, seconds)
	} else {
		String.format("%02d:%02d", minutes, seconds)
	}
}
