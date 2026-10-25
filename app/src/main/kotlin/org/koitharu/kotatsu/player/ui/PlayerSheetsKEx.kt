@file:OptIn(
	ExperimentalMaterial3Api::class,
	UnstableApi::class,
)

package org.koitharu.kotatsu.player.ui

import kotlin.OptIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import eu.kanade.tachiyomi.animesource.model.Video
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.player.utils.PlayerAudioTrackKEx
import org.koitharu.kotatsu.player.utils.PlayerSubtitleTrackKEx

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QualitySheetKEx(
	videos: List<Video>,
	selectedVideo: Video?,
	onSelectVideo: (Video) -> Unit,
	onDismiss: () -> Unit,
) {
	ModalBottomSheet(
		onDismissRequest = onDismiss,
		containerColor = MaterialTheme.colorScheme.surfaceContainer,
		contentColor = MaterialTheme.colorScheme.onSurface,
	) {
		Column(modifier = Modifier.padding(20.dp)) {
			Text(
				text = "Video Quality / Server",
				style = MaterialTheme.typography.titleLarge,
				fontWeight = FontWeight.Bold,
				modifier = Modifier.padding(bottom = 16.dp),
			)
			LazyColumn {
				items(videos) { video ->
					val isSelected = video == selectedVideo
					Surface(
						onClick = {
							onSelectVideo(video)
							onDismiss()
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
								text = video.videoTitle.ifBlank { "Stream" },
								style = MaterialTheme.typography.bodyLarge,
								fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
								modifier = Modifier.weight(1f),
							)
							if (isSelected) {
								Icon(
									imageVector = Icons.Default.Check,
									contentDescription = "Selected",
									tint = MaterialTheme.colorScheme.primary,
								)
							}
						}
					}
				}
			}
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioTracksSheetKEx(
    audioTracks: List<PlayerAudioTrackKEx>,
    onSelectAudioTrack: (PlayerAudioTrackKEx) -> Unit,
    onDismiss: () -> Unit,
) {
	ModalBottomSheet(
		onDismissRequest = onDismiss,
		containerColor = MaterialTheme.colorScheme.surfaceContainer,
		contentColor = MaterialTheme.colorScheme.onSurface,
	) {
		Column(modifier = Modifier.padding(20.dp)) {
			Text(
				text = "Audio Tracks",
				style = MaterialTheme.typography.titleLarge,
				fontWeight = FontWeight.Bold,
				modifier = Modifier.padding(bottom = 16.dp),
			)
			if (audioTracks.isEmpty()) {
				Text(
					text = "No additional audio tracks found",
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
					modifier = Modifier.padding(vertical = 16.dp),
				)
			} else {
				LazyColumn {
					items(audioTracks) { track ->
						Surface(
							onClick = {
								onSelectAudioTrack(track)
								onDismiss()
							},
							shape = RoundedCornerShape(12.dp),
							color = if (track.isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
							modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
						) {
							Row(
								modifier = Modifier
									.fillMaxWidth()
									.padding(16.dp),
								verticalAlignment = Alignment.CenterVertically,
							) {
								RadioButton(
									selected = track.isSelected,
									onClick = null,
								)
								Spacer(Modifier.width(12.dp))
								Text(
									text = track.label,
									style = MaterialTheme.typography.bodyLarge,
									fontWeight = if (track.isSelected) FontWeight.Bold else FontWeight.Normal,
								)
							}
						}
					}
				}
			}
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubtitleTracksSheetKEx(
    subtitleTracks: List<PlayerSubtitleTrackKEx>,
    onSelectSubtitleTrack: (PlayerSubtitleTrackKEx) -> Unit,
    onDisableSubtitles: () -> Unit,
    onImportSubtitleFile: () -> Unit,
    onOpenSubtitleSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
	ModalBottomSheet(
		onDismissRequest = onDismiss,
		containerColor = MaterialTheme.colorScheme.surfaceContainer,
		contentColor = MaterialTheme.colorScheme.onSurface,
	) {
		Column(modifier = Modifier.padding(20.dp)) {
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.SpaceBetween,
				verticalAlignment = Alignment.CenterVertically,
			) {
				Text(
					text = "Subtitles",
					style = MaterialTheme.typography.titleLarge,
					fontWeight = FontWeight.Bold,
				)
				IconButton(onClick = {
					onOpenSubtitleSettings()
					onDismiss()
				}) {
					Icon(
						imageVector = Icons.Default.Subtitles,
						contentDescription = "Subtitle Settings",
					)
				}
			}

			Spacer(Modifier.height(12.dp))

			OutlinedButton(
				onClick = {
					onImportSubtitleFile()
					onDismiss()
				},
				modifier = Modifier.fillMaxWidth(),
			) {
				Icon(imageVector = Icons.Default.Add, contentDescription = null)
				Spacer(Modifier.width(8.dp))
				Text("Import External Subtitle (.srt, .vtt, .ass)")
			}

			Spacer(Modifier.height(16.dp))

			val isAnySelected = subtitleTracks.any { it.isSelected }

			// Option to disable subtitles
			Surface(
				onClick = {
					onDisableSubtitles()
					onDismiss()
				},
				shape = RoundedCornerShape(12.dp),
				color = if (!isAnySelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
				modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
			) {
				Row(
					modifier = Modifier.padding(16.dp),
					verticalAlignment = Alignment.CenterVertically,
				) {
					RadioButton(selected = !isAnySelected, onClick = null)
					Spacer(Modifier.width(12.dp))
					Text("Off", style = MaterialTheme.typography.bodyLarge)
				}
			}

			LazyColumn {
				items(subtitleTracks) { track ->
					Surface(
						onClick = {
							onSelectSubtitleTrack(track)
							onDismiss()
						},
						shape = RoundedCornerShape(12.dp),
						color = if (track.isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
						modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
					) {
						Row(
							modifier = Modifier.padding(16.dp),
							verticalAlignment = Alignment.CenterVertically,
						) {
							RadioButton(selected = track.isSelected, onClick = null)
							Spacer(Modifier.width(12.dp))
							Text(
								text = track.label + if (track.isExternal) " (External)" else "",
								style = MaterialTheme.typography.bodyLarge,
								fontWeight = if (track.isSelected) FontWeight.Bold else FontWeight.Normal,
							)
						}
					}
				}
			}
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubtitleSettingsSheetKEx(
	fontSizeSp: Float,
	textColor: Int,
	bgColor: Int,
	isBold: Boolean,
	subtitleOffsetMs: Long,
	onFontSizeChange: (Float) -> Unit,
	onTextColorChange: (Int) -> Unit,
	onBgColorChange: (Int) -> Unit,
	onBoldChange: (Boolean) -> Unit,
	onOffsetChange: (Long) -> Unit,
	onDismiss: () -> Unit,
) {
	ModalBottomSheet(
		onDismissRequest = onDismiss,
		containerColor = MaterialTheme.colorScheme.surfaceContainer,
		contentColor = MaterialTheme.colorScheme.onSurface,
	) {
		Column(modifier = Modifier.padding(20.dp)) {
			Text(
				text = "Subtitle Appearance & Timing",
				style = MaterialTheme.typography.titleLarge,
				fontWeight = FontWeight.Bold,
				modifier = Modifier.padding(bottom = 16.dp),
			)

			Text("Font Size (${fontSizeSp.toInt()} sp)", style = MaterialTheme.typography.bodyMedium)
			Slider(
				value = fontSizeSp,
				onValueChange = onFontSizeChange,
				valueRange = 12f..36f,
				modifier = Modifier.fillMaxWidth(),
			)

			Spacer(Modifier.height(12.dp))

			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.SpaceBetween,
				verticalAlignment = Alignment.CenterVertically,
			) {
				Text("Bold Text", style = MaterialTheme.typography.bodyLarge)
				Switch(checked = isBold, onCheckedChange = onBoldChange)
			}

			Spacer(Modifier.height(12.dp))

			Text("Text Color", style = MaterialTheme.typography.bodyMedium)
			Row(
				horizontalArrangement = Arrangement.spacedBy(12.dp),
				modifier = Modifier.padding(vertical = 8.dp),
			) {
				listOf(
					Color.White to "White",
					Color.Yellow to "Yellow",
					Color.Cyan to "Cyan",
					Color.Green to "Green",
				).forEach { (color, label) ->
					Surface(
						onClick = { onTextColorChange(android.graphics.Color.argb(255, (color.red * 255).toInt(), (color.green * 255).toInt(), (color.blue * 255).toInt())) },
						shape = RoundedCornerShape(8.dp),
						color = color,
						modifier = Modifier.height(36.dp).weight(1f),
					) {}
				}
			}

			Spacer(Modifier.height(12.dp))

			Text("Subtitle Sync Offset (${subtitleOffsetMs}ms)", style = MaterialTheme.typography.bodyMedium)
			Row(
				horizontalArrangement = Arrangement.spacedBy(12.dp),
				modifier = Modifier.padding(vertical = 8.dp),
			) {
				Button(onClick = { onOffsetChange(subtitleOffsetMs - 500) }) {
					Text("-0.5s")
				}
				Button(onClick = { onOffsetChange(0) }) {
					Text("Reset")
				}
				Button(onClick = { onOffsetChange(subtitleOffsetMs + 500) }) {
					Text("+0.5s")
				}
			}
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodesSheetKEx(
	chapters: List<MangaChapter>,
	currentChapter: MangaChapter?,
	onSelectChapter: (MangaChapter) -> Unit,
	onDismiss: () -> Unit,
) {
	ModalBottomSheet(
		onDismissRequest = onDismiss,
		containerColor = MaterialTheme.colorScheme.surfaceContainer,
		contentColor = MaterialTheme.colorScheme.onSurface,
	) {
		Column(modifier = Modifier.padding(20.dp)) {
			Text(
				text = "Episodes",
				style = MaterialTheme.typography.titleLarge,
				fontWeight = FontWeight.Bold,
				modifier = Modifier.padding(bottom = 12.dp),
			)
			LazyColumn {
				items(chapters) { chapter ->
					val isSelected = chapter.id == currentChapter?.id
					Surface(
						onClick = {
							onSelectChapter(chapter)
							onDismiss()
						},
						shape = RoundedCornerShape(12.dp),
						color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
						contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
						modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
					) {
						Row(
							modifier = Modifier.padding(16.dp),
							verticalAlignment = Alignment.CenterVertically,
						) {
							Text(
								text = chapter.title.orEmpty().ifBlank {
									chapter.numberString()?.let { "Episode $it" } ?: "Episode ${chapter.number}"
								},
								style = MaterialTheme.typography.bodyLarge,
								fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
								modifier = Modifier.weight(1f),
							)
						}
					}
				}
			}
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackSpeedSheetKEx(
	currentSpeed: Float,
	onSelectSpeed: (Float) -> Unit,
	onDismiss: () -> Unit,
) {
	ModalBottomSheet(
		onDismissRequest = onDismiss,
		containerColor = MaterialTheme.colorScheme.surfaceContainer,
		contentColor = MaterialTheme.colorScheme.onSurface,
	) {
		Column(modifier = Modifier.padding(20.dp)) {
			Text(
				text = "Playback Speed",
				style = MaterialTheme.typography.titleLarge,
				fontWeight = FontWeight.Bold,
				modifier = Modifier.padding(bottom = 12.dp),
			)
			listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f, 3.0f).forEach { speed ->
				val isSelected = currentSpeed == speed
				Surface(
					onClick = {
						onSelectSpeed(speed)
						onDismiss()
					},
					shape = RoundedCornerShape(12.dp),
					color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
					modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
				) {
					Row(
						modifier = Modifier.padding(16.dp),
						verticalAlignment = Alignment.CenterVertically,
					) {
						Text(
							text = "${speed}x",
							style = MaterialTheme.typography.bodyLarge,
							fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
						)
					}
				}
			}
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class, UnstableApi::class)
@Composable
fun VideoAspectSheetKEx(
	currentResizeMode: Int,
	onSelectResizeMode: (Int) -> Unit,
	onDismiss: () -> Unit,
) {
	val aspectModes = listOf(
		AspectRatioFrameLayout.RESIZE_MODE_FIT to "Fit to Screen",
		AspectRatioFrameLayout.RESIZE_MODE_ZOOM to "Crop / Zoom",
		AspectRatioFrameLayout.RESIZE_MODE_FILL to "Stretch / Fill",
		AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH to "Fit Width",
		AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT to "Fit Height",
	)

	ModalBottomSheet(
		onDismissRequest = onDismiss,
		containerColor = MaterialTheme.colorScheme.surfaceContainer,
		contentColor = MaterialTheme.colorScheme.onSurface,
	) {
		Column(modifier = Modifier.padding(20.dp)) {
			Text(
				text = "Video Fit / Aspect Ratio",
				style = MaterialTheme.typography.titleLarge,
				fontWeight = FontWeight.Bold,
				modifier = Modifier.padding(bottom = 12.dp),
			)
			aspectModes.forEach { (mode, label) ->
				val isSelected = currentResizeMode == mode
				Surface(
					onClick = {
						onSelectResizeMode(mode)
						onDismiss()
					},
					shape = RoundedCornerShape(12.dp),
					color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
					modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
				) {
					Row(
						modifier = Modifier.padding(16.dp),
						verticalAlignment = Alignment.CenterVertically,
					) {
						Icon(
							imageVector = Icons.Default.AspectRatio,
							contentDescription = null,
							tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
						)
						Spacer(Modifier.width(12.dp))
						Text(
							text = label,
							style = MaterialTheme.typography.bodyLarge,
							fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
						)
					}
				}
			}
		}
	}
}
