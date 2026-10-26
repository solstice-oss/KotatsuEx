package org.koitharu.kotatsu.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.settings.compose.BaseComposeSettingsFragment
import org.koitharu.kotatsu.settings.compose.DropSauceTheme
import org.koitharu.kotatsu.settings.compose.ListSettingsItem
import org.koitharu.kotatsu.settings.compose.SettingsGroup
import org.koitharu.kotatsu.settings.compose.SettingsScaffold
import org.koitharu.kotatsu.settings.compose.SliderSettingsItem
import org.koitharu.kotatsu.settings.compose.SwitchSettingsItem
import org.koitharu.kotatsu.settings.compose.rememberBooleanPref
import org.koitharu.kotatsu.settings.compose.rememberIntPref
import org.koitharu.kotatsu.settings.compose.rememberStringPref

@AndroidEntryPoint
class PlayerSettingsFragmentKEx : BaseComposeSettingsFragment(R.string.player_settings) {

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?,
	): View = ComposeView(requireContext()).apply {
		setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
		setContent {
			DropSauceTheme {
				PlayerSettingsScreen()
			}
		}
	}
}

@OptIn(UnstableApi::class)
@Composable
private fun PlayerSettingsScreen() {
	val ctx = LocalContext.current

	// Options for Playback Speed
	val speedEntries = listOf("0.25x", "0.5x", "0.75x", "1.0x", "1.25x", "1.5x", "1.75x", "2.0x", "2.5x", "3.0x")
	val speedValues = listOf("0.25", "0.5", "0.75", "1.0", "1.25", "1.5", "1.75", "2.0", "2.5", "3.0")

	// Options for Aspect Ratio / Resize Mode
	val aspectEntries = remember { ctx.resources.getStringArray(R.array.aspect_ratio_entries).toList() }
	val aspectValues = listOf(
		AspectRatioFrameLayout.RESIZE_MODE_FIT.toString(),
		AspectRatioFrameLayout.RESIZE_MODE_ZOOM.toString(),
		AspectRatioFrameLayout.RESIZE_MODE_FILL.toString(),
		AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH.toString(),
		AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT.toString(),
	)

	// Options for Double Tap Seek Duration
	val seekEntries = remember { ctx.resources.getStringArray(R.array.double_tap_seek_entries).toList() }
	val seekValues = listOf("5", "10", "15", "20", "30")

	// Options for Preferred Subtitle / Audio Language
	val langEntries = remember { ctx.resources.getStringArray(R.array.preferred_lang_entries).toList() }
	val langValues = listOf("auto", "ja", "en", "es", "fr", "de")

	// Preferences state
	var speed by rememberStringPref("pref_player_speed_str", "1.0")
	var resizeMode by rememberStringPref(
		"pref_player_resize_mode_str",
		AspectRatioFrameLayout.RESIZE_MODE_FIT.toString(),
	)
	var autoPlayNext by rememberBooleanPref("pref_player_auto_play_next", true)
	var preservePosition by rememberBooleanPref("pref_player_preserve_position", true)
	var doubleTapSeek by rememberStringPref("pref_player_double_tap_seek", "10")
	var gesturesEnabled by rememberBooleanPref("pref_player_gestures_enabled", true)
	var rememberVolBright by rememberBooleanPref("pref_remember_brightness_volume", false)

	// Subtitle preferences
	var preferredSubLang by rememberStringPref("pref_player_preferred_sub_lang", "auto")
	var preferredAudioLang by rememberStringPref("pref_player_preferred_audio_lang", "auto")
	var subtitleFontSize by rememberIntPref("pref_subtitle_font_size_int", 18)
	var subtitleBold by rememberBooleanPref("pref_subtitle_bold", false)

	SettingsScaffold {
		item {
			SettingsGroup(title = stringResource(R.string.playback_automation)) {
				item { pos ->
					ListSettingsItem(
						title = stringResource(R.string.pref_default_playback_speed),
						entries = speedEntries,
						entryValues = speedValues,
						selectedValue = speed,
						onValueChange = { speed = it },
						shape = pos.shape,
						icon = R.drawable.ic_play,
					)
				}
				item { pos ->
					SwitchSettingsItem(
						title = stringResource(R.string.pref_auto_play_next_episode),
						subtitle = stringResource(R.string.pref_auto_play_next_episode_summary),
						checked = autoPlayNext,
						onCheckedChange = { autoPlayNext = it },
						shape = pos.shape,
						icon = R.drawable.ic_skip_next,
					)
				}
				item { pos ->
					SwitchSettingsItem(
						title = stringResource(R.string.pref_remember_playback_position),
						subtitle = stringResource(R.string.pref_remember_playback_position_summary),
						checked = preservePosition,
						onCheckedChange = { preservePosition = it },
						shape = pos.shape,
						icon = R.drawable.ic_bookmark_checked,
					)
				}
				item { pos ->
					SwitchSettingsItem(
						title = stringResource(R.string.pref_remember_brightness_volume),
						subtitle = stringResource(R.string.pref_remember_brightness_volume_summary),
						checked = rememberVolBright,
						onCheckedChange = { rememberVolBright = it },
						shape = pos.shape,
						icon = R.drawable.ic_eye_tracking,
					)
				}
			}
		}

		item { Spacer(Modifier.height(16.dp)) }

		item {
			SettingsGroup(title = stringResource(R.string.video_controls)) {
				item { pos ->
					ListSettingsItem(
						title = stringResource(R.string.pref_video_aspect_ratio),
						entries = aspectEntries,
						entryValues = aspectValues,
						selectedValue = resizeMode,
						onValueChange = { resizeMode = it },
						shape = pos.shape,
						icon = R.drawable.ic_crop,
					)
				}
				item { pos ->
					ListSettingsItem(
						title = stringResource(R.string.pref_double_tap_seek_duration),
						entries = seekEntries,
						entryValues = seekValues,
						selectedValue = doubleTapSeek,
						onValueChange = { doubleTapSeek = it },
						shape = pos.shape,
						icon = R.drawable.ic_interaction_large,
					)
				}
				item { pos ->
					SwitchSettingsItem(
						title = stringResource(R.string.pref_enable_swipe_gestures),
						subtitle = stringResource(R.string.pref_enable_swipe_gestures_summary),
						checked = gesturesEnabled,
						onCheckedChange = { gesturesEnabled = it },
						shape = pos.shape,
						icon = R.drawable.ic_gesture_vertical,
					)
				}
			}
		}

		item { Spacer(Modifier.height(16.dp)) }

		item {
			SettingsGroup(title = stringResource(R.string.audio_subtitles)) {
				item { pos ->
					ListSettingsItem(
						title = stringResource(R.string.pref_preferred_audio_language),
						entries = langEntries,
						entryValues = langValues,
						selectedValue = preferredAudioLang,
						onValueChange = { preferredAudioLang = it },
						shape = pos.shape,
						icon = R.drawable.ic_language,
					)
				}
				item { pos ->
					ListSettingsItem(
						title = stringResource(R.string.pref_preferred_subtitle_language),
						entries = langEntries,
						entryValues = langValues,
						selectedValue = preferredSubLang,
						onValueChange = { preferredSubLang = it },
						shape = pos.shape,
						icon = R.drawable.ic_language_filter,
					)
				}
				item { pos ->
					SliderSettingsItem(
						title = stringResource(R.string.pref_subtitle_font_size),
						value = subtitleFontSize,
						valueFrom = 12,
						valueTo = 36,
						stepSize = 1,
						unitSuffix = " " + stringResource(R.string.unit_sp),
						onValueChange = { subtitleFontSize = it },
						shape = pos.shape,
						icon = R.drawable.ic_feed_normal,
					)
				}
				item { pos ->
					SwitchSettingsItem(
						title = stringResource(R.string.pref_bold_subtitles),
						subtitle = stringResource(R.string.pref_bold_subtitles_summary),
						checked = subtitleBold,
						onCheckedChange = { subtitleBold = it },
						shape = pos.shape,
						icon = R.drawable.ic_feed_checked,
					)
				}
			}
		}
	}
}
