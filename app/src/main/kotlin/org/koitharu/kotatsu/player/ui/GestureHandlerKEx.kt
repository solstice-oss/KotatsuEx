package org.koitharu.kotatsu.player.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.abs

@Composable
fun GestureHandlerKEx(
	onTap: () -> Unit,
	onDoubleTapLeft: () -> Unit,
	onDoubleTapRight: () -> Unit,
	onVolumeChangeDelta: (Float) -> Unit,
	onBrightnessChangeDelta: (Float) -> Unit,
	onScrubSeekDelta: (Long) -> Unit,
	currentVolumePercent: Float,
	currentBrightnessPercent: Float,
	modifier: Modifier = Modifier,
) {
	var showRewindIndicator by remember { mutableStateOf(false) }
	var showForwardIndicator by remember { mutableStateOf(false) }

	var isDraggingVertical by remember { mutableStateOf(false) }
	var isDraggingLeft by remember { mutableStateOf(false) }

	var showVolumeOverlay by remember { mutableStateOf(false) }
	var showBrightnessOverlay by remember { mutableStateOf(false) }

	LaunchedEffect(showRewindIndicator) {
		if (showRewindIndicator) {
			delay(700)
			showRewindIndicator = false
		}
	}

	LaunchedEffect(showForwardIndicator) {
		if (showForwardIndicator) {
			delay(700)
			showForwardIndicator = false
		}
	}

	LaunchedEffect(showVolumeOverlay) {
		if (showVolumeOverlay) {
			delay(1200)
			showVolumeOverlay = false
		}
	}

	LaunchedEffect(showBrightnessOverlay) {
		if (showBrightnessOverlay) {
			delay(1200)
			showBrightnessOverlay = false
		}
	}

	Box(
		modifier = modifier
			.fillMaxSize()
			.pointerInput(Unit) {
				detectTapGestures(
					onTap = { onTap() },
					onDoubleTap = { offset ->
						if (offset.x < size.width * 0.4f) {
							showRewindIndicator = true
							onDoubleTapLeft()
						} else if (offset.x > size.width * 0.6f) {
							showForwardIndicator = true
							onDoubleTapRight()
						} else {
							onTap()
						}
					},
				)
			}
			.pointerInput(Unit) {
				var totalDragX = 0f
				var totalDragY = 0f

				detectDragGestures(
					onDragStart = { offset ->
						totalDragX = 0f
						totalDragY = 0f
						isDraggingLeft = offset.x < size.width * 0.5f
					},
					onDragEnd = {
						isDraggingVertical = false
					},
					onDragCancel = {
						isDraggingVertical = false
					},
					onDrag = { change, dragAmount ->
						change.consume()
						totalDragX += dragAmount.x
						totalDragY += dragAmount.y

						if (abs(totalDragY) > abs(totalDragX) && abs(totalDragY) > 10f) {
							isDraggingVertical = true
							val delta = -dragAmount.y / size.height
							if (isDraggingLeft) {
								showBrightnessOverlay = true
								onBrightnessChangeDelta(delta)
							} else {
								showVolumeOverlay = true
								onVolumeChangeDelta(delta)
							}
						} else if (!isDraggingVertical && abs(totalDragX) > 20f) {
							val scrubMs = (dragAmount.x * 200).toLong()
							onScrubSeekDelta(scrubMs)
						}
					},
				)
			},
	) {
		// Left double tap indicator (-10s)
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
				color = Color.Black.copy(alpha = 0.7f),
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

		// Right double tap indicator (+10s)
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
				color = Color.Black.copy(alpha = 0.7f),
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

		// Brightness Overlay Card
		AnimatedVisibility(
			visible = showBrightnessOverlay,
			enter = fadeIn(),
			exit = fadeOut(),
			modifier = Modifier
				.align(Alignment.CenterStart)
				.padding(start = 32.dp),
		) {
			Surface(
				shape = RoundedCornerShape(16.dp),
				color = Color.Black.copy(alpha = 0.8f),
			) {
				Row(
					modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
					verticalAlignment = Alignment.CenterVertically,
				) {
					Icon(
						imageVector = Icons.Default.Brightness6,
						contentDescription = "Brightness",
						tint = Color.White,
						modifier = Modifier.size(24.dp),
					)
					Spacer(Modifier.width(12.dp))
					Column {
						Text(
							text = "${(currentBrightnessPercent * 100).toInt()}%",
							style = MaterialTheme.typography.labelSmall,
							color = Color.White,
							fontWeight = FontWeight.Bold,
						)
						Spacer(Modifier.height(4.dp))
						LinearProgressIndicator(
							progress = { currentBrightnessPercent.coerceIn(0f, 1f) },
							modifier = Modifier.width(80.dp),
							color = MaterialTheme.colorScheme.primary,
							trackColor = Color.White.copy(alpha = 0.3f),
						)
					}
				}
			}
		}

		// Volume Overlay Card
		AnimatedVisibility(
			visible = showVolumeOverlay,
			enter = fadeIn(),
			exit = fadeOut(),
			modifier = Modifier
				.align(Alignment.CenterEnd)
				.padding(end = 32.dp),
		) {
			Surface(
				shape = RoundedCornerShape(16.dp),
				color = Color.Black.copy(alpha = 0.8f),
			) {
				Row(
					modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
					verticalAlignment = Alignment.CenterVertically,
				) {
					Icon(
						imageVector = Icons.AutoMirrored.Filled.VolumeUp,
						contentDescription = "Volume",
						tint = Color.White,
						modifier = Modifier.size(24.dp),
					)
					Spacer(Modifier.width(12.dp))
					Column {
						Text(
							text = "${(currentVolumePercent * 100).toInt()}%",
							style = MaterialTheme.typography.labelSmall,
							color = Color.White,
							fontWeight = FontWeight.Bold,
						)
						Spacer(Modifier.height(4.dp))
						LinearProgressIndicator(
							progress = { currentVolumePercent.coerceIn(0f, 1f) },
							modifier = Modifier.width(80.dp),
							color = MaterialTheme.colorScheme.primary,
							trackColor = Color.White.copy(alpha = 0.3f),
						)
					}
				}
			}
		}
	}
}
