package org.koitharu.kotatsu.player.ui

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.ExperimentalMaterial3Api
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
import eu.kanade.tachiyomi.animesource.model.Video
import org.koitharu.kotatsu.parsers.model.MangaChapter

@OptIn(ExperimentalMaterial3Api::class)
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
	hasPreviousChapter: Boolean,
	hasNextChapter: Boolean,
	topInset: Dp = 0.dp,
	bottomInset: Dp = 0.dp,
	onTogglePlayPause: () -> Unit,
	onSeekTo: (Long) -> Unit,
	onPreviousChapter: () -> Unit,
	onNextChapter: () -> Unit,
	onSelectVideo: (Video) -> Unit,
	onSelectSpeed: (Float) -> Unit,
	onSelectChapter: (MangaChapter) -> Unit,
	onEpisodesClick: () -> Unit,
	onEpisodesDismissed: () -> Unit,
	onBack: () -> Unit,
	modifier: Modifier = Modifier,
) {
	var showQualitySheet by remember { mutableStateOf(false) }
	var showSpeedSheet by remember { mutableStateOf(false) }
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
					.padding(start = 16.dp, end = 16.dp, top = effectiveTopInset + 16.dp, bottom = 16.dp),
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
					onClick = onPreviousChapter,
					enabled = hasPreviousChapter,
					modifier = Modifier.size(56.dp),
				) {
					Icon(
						imageVector = Icons.Default.SkipPrevious,
						contentDescription = "Previous Episode",
						tint = if (hasPreviousChapter) Color.White else Color.White.copy(alpha = 0.38f),
						modifier = Modifier.size(36.dp),
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
					onClick = onNextChapter,
					enabled = hasNextChapter,
					modifier = Modifier.size(56.dp),
				) {
					Icon(
						imageVector = Icons.Default.SkipNext,
						contentDescription = "Next Episode",
						tint = if (hasNextChapter) Color.White else Color.White.copy(alpha = 0.38f),
						modifier = Modifier.size(36.dp),
					)
				}
			}

			// Bottom Bar
			Column(
				modifier = Modifier
					.fillMaxWidth()
					.align(Alignment.BottomCenter)
					.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = effectiveBottomInset + 16.dp),
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

		// Episodes Bottom Sheet
		if (showEpisodesSheet) {
			ModalBottomSheet(
				onDismissRequest = {
					showEpisodesSheet = false
					onEpisodesDismissed()
				},
				containerColor = MaterialTheme.colorScheme.surfaceContainer,
				contentColor = MaterialTheme.colorScheme.onSurface,
			) {
				Column(modifier = Modifier.padding(16.dp)) {
					Text(
						text = "Episodes",
						style = MaterialTheme.typography.titleMedium,
						fontWeight = FontWeight.Bold,
						color = MaterialTheme.colorScheme.onSurface,
						modifier = Modifier.padding(bottom = 12.dp),
					)
					LazyColumn {
						items(chapters) { chapter ->
							val isSelected = chapter.id == currentChapter?.id
							Surface(
								onClick = {
									onSelectChapter(chapter)
									showEpisodesSheet = false
									onEpisodesDismissed()
								},
								shape = RoundedCornerShape(12.dp),
								color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
								contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
								modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
							) {
								Row(
									modifier = Modifier
										.fillMaxWidth()
										.padding(16.dp),
									verticalAlignment = Alignment.CenterVertically,
								) {
									Text(
										text = chapter.title.orEmpty().ifBlank {
											chapter.numberString()?.let { "Episode $it" } ?: "Chapter ${chapter.number}"
										},
										style = MaterialTheme.typography.bodyMedium,
										fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
										color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
										modifier = Modifier.weight(1f),
									)
								}
							}
						}
					}
				}
			}
		}

		// Quality Bottom Sheet
		if (showQualitySheet) {
			ModalBottomSheet(
				onDismissRequest = { showQualitySheet = false },
				containerColor = MaterialTheme.colorScheme.surfaceContainer,
				contentColor = MaterialTheme.colorScheme.onSurface,
			) {
				Column(modifier = Modifier.padding(16.dp)) {
					Text(
						text = "Video Quality / Server",
						style = MaterialTheme.typography.titleMedium,
						fontWeight = FontWeight.Bold,
						color = MaterialTheme.colorScheme.onSurface,
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
								contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
								modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
							) {
								Text(
									text = video.videoTitle.ifBlank { video.quality.ifBlank { "Stream" } },
									style = MaterialTheme.typography.bodyMedium,
									color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
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
				containerColor = MaterialTheme.colorScheme.surfaceContainer,
				contentColor = MaterialTheme.colorScheme.onSurface,
			) {
				Column(modifier = Modifier.padding(16.dp)) {
					Text(
						text = "Playback Speed",
						style = MaterialTheme.typography.titleMedium,
						fontWeight = FontWeight.Bold,
						color = MaterialTheme.colorScheme.onSurface,
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
							contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
							modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
						) {
							Text(
								text = "${s}x",
								style = MaterialTheme.typography.bodyMedium,
								color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
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
		String.format(java.util.Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
	} else {
		String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
	}
}
