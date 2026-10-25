package org.koitharu.kotatsu.player.utils

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer

data class PlayerAudioTrackKEx(
	val id: String,
	val language: String,
	val label: String,
	val isSelected: Boolean,
	val trackGroupIndex: Int,
	val trackIndexInGroup: Int,
)

data class PlayerSubtitleTrackKEx(
	val id: String,
	val language: String,
	val label: String,
	val isSelected: Boolean,
	val isExternal: Boolean = false,
	val uri: Uri? = null,
	val mimeType: String? = null,
	val trackGroupIndex: Int? = null,
	val trackIndexInGroup: Int? = null,
)

@OptIn(UnstableApi::class)
object TrackUtilsKEx {

	fun getAudioTracks(player: ExoPlayer): List<PlayerAudioTrackKEx> {
		val audioTracks = mutableListOf<PlayerAudioTrackKEx>()
		val currentTracks = player.currentTracks

		var groupIndex = 0
		for (trackGroup in currentTracks.groups) {
			if (trackGroup.type == C.TRACK_TYPE_AUDIO) {
				val mediaTrackGroup = trackGroup.mediaTrackGroup
				for (i in 0 until mediaTrackGroup.length) {
					val format = mediaTrackGroup.getFormat(i)
					val lang = format.language ?: "und"
					val label = format.label.takeUnless { it.isNullBlank() }
						?: if (lang != "und") lang.uppercase() else "Audio Track ${audioTracks.size + 1}"
					val isSelected = trackGroup.isTrackSelected(i)

					audioTracks.add(
						PlayerAudioTrackKEx(
							id = "${trackGroup.mediaTrackGroup.id}_$i",
							language = lang,
							label = label,
							isSelected = isSelected,
							trackGroupIndex = groupIndex,
							trackIndexInGroup = i,
						),
					)
				}
			}
			groupIndex++
		}
		return audioTracks
	}

	fun selectAudioTrack(player: ExoPlayer, track: PlayerAudioTrackKEx) {
		val currentTracks = player.currentTracks
		var groupIndex = 0
		for (trackGroup in currentTracks.groups) {
			if (trackGroup.type == C.TRACK_TYPE_AUDIO && groupIndex == track.trackGroupIndex) {
				val parameters = player.trackSelectionParameters
					.buildUpon()
					.setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
					.setOverrideForType(
						TrackSelectionOverride(trackGroup.mediaTrackGroup, track.trackIndexInGroup),
					)
					.build()
				player.trackSelectionParameters = parameters
				return
			}
			groupIndex++
		}
	}

	fun getSubtitleTracks(
		player: ExoPlayer,
		externalTracks: List<PlayerSubtitleTrackKEx>,
	): List<PlayerSubtitleTrackKEx> {
		val result = mutableListOf<PlayerSubtitleTrackKEx>()

		// External subtitles
		result.addAll(externalTracks)

		// Internal embedded subtitles in current media stream
		val currentTracks = player.currentTracks
		var groupIndex = 0
		for (trackGroup in currentTracks.groups) {
			if (trackGroup.type == C.TRACK_TYPE_TEXT) {
				val mediaTrackGroup = trackGroup.mediaTrackGroup
				for (i in 0 until mediaTrackGroup.length) {
					val format = mediaTrackGroup.getFormat(i)
					val lang = format.language ?: "und"
					val label = format.label.takeUnless { it.isNullBlank() }
						?: if (lang != "und") lang.uppercase() else "Subtitle ${result.size + 1}"
					val isSelected = trackGroup.isTrackSelected(i)

					result.add(
						PlayerSubtitleTrackKEx(
							id = "internal_${trackGroup.mediaTrackGroup.id}_$i",
							language = lang,
							label = label,
							isSelected = isSelected,
							isExternal = false,
							trackGroupIndex = groupIndex,
							trackIndexInGroup = i,
						),
					)
				}
			}
			groupIndex++
		}
		return result
	}

	fun selectSubtitleTrack(player: ExoPlayer, track: PlayerSubtitleTrackKEx) {
		if (track.isExternal) {
			// External subtitle selection is handled via media item subtitle configurations or tracks
			val currentTracks = player.currentTracks
			for (trackGroup in currentTracks.groups) {
				if (trackGroup.type == C.TRACK_TYPE_TEXT) {
					val mediaTrackGroup = trackGroup.mediaTrackGroup
					for (i in 0 until mediaTrackGroup.length) {
						val format = mediaTrackGroup.getFormat(i)
						if (format.id == track.id || format.label == track.label) {
							val parameters = player.trackSelectionParameters
								.buildUpon()
								.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
								.setOverrideForType(TrackSelectionOverride(mediaTrackGroup, i))
								.build()
							player.trackSelectionParameters = parameters
							return
						}
					}
				}
			}
		} else if (track.trackGroupIndex != null && track.trackIndexInGroup != null) {
			val currentTracks = player.currentTracks
			var groupIndex = 0
			for (trackGroup in currentTracks.groups) {
				if (trackGroup.type == C.TRACK_TYPE_TEXT && groupIndex == track.trackGroupIndex) {
					val parameters = player.trackSelectionParameters
						.buildUpon()
						.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
						.setOverrideForType(
							TrackSelectionOverride(trackGroup.mediaTrackGroup, track.trackIndexInGroup),
						)
						.build()
					player.trackSelectionParameters = parameters
					return
				}
				groupIndex++
			}
		}
	}

	fun disableSubtitles(player: ExoPlayer) {
		val parameters = player.trackSelectionParameters
			.buildUpon()
			.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
			.build()
		player.trackSelectionParameters = parameters
	}

	private fun String?.isNullBlank(): Boolean = this.isNullOrEmpty() || this.isBlank()
}
