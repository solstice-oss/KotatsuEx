package org.koitharu.kotatsu.player.settings

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(UnstableApi::class)
@Singleton
class PlayerPreferencesKEx @Inject constructor(
	@ApplicationContext private val context: Context,
) {
	private val prefs: SharedPreferences =
		context.getSharedPreferences("player_preferences_kex", Context.MODE_PRIVATE)

	private val _speed = MutableStateFlow(getSpeed())
	val speed: StateFlow<Float> = _speed.asStateFlow()

	private val _resizeMode = MutableStateFlow(getResizeMode())
	val resizeMode: StateFlow<Int> = _resizeMode.asStateFlow()

	private val _autoPlayNext = MutableStateFlow(getAutoPlayNext())
	val autoPlayNext: StateFlow<Boolean> = _autoPlayNext.asStateFlow()

	private val _subtitleFontSize = MutableStateFlow(getSubtitleFontSize())
	val subtitleFontSize: StateFlow<Float> = _subtitleFontSize.asStateFlow()

	private val _subtitleTextColor = MutableStateFlow(getSubtitleTextColor())
	val subtitleTextColor: StateFlow<Int> = _subtitleTextColor.asStateFlow()

	private val _subtitleBgColor = MutableStateFlow(getSubtitleBgColor())
	val subtitleBgColor: StateFlow<Int> = _subtitleBgColor.asStateFlow()

	private val _subtitleBold = MutableStateFlow(getSubtitleBold())
	val subtitleBold: StateFlow<Boolean> = _subtitleBold.asStateFlow()

	fun getSpeed(): Float = prefs.getFloat(KEY_SPEED, 1.0f)
	fun setSpeed(value: Float) {
		prefs.edit().putFloat(KEY_SPEED, value).apply()
		_speed.value = value
	}

	fun getResizeMode(): Int = prefs.getInt(KEY_RESIZE_MODE, AspectRatioFrameLayout.RESIZE_MODE_FIT)
	fun setResizeMode(value: Int) {
		prefs.edit().putInt(KEY_RESIZE_MODE, value).apply()
		_resizeMode.value = value
	}

	fun getAutoPlayNext(): Boolean = prefs.getBoolean(KEY_AUTO_PLAY_NEXT, true)
	fun setAutoPlayNext(value: Boolean) {
		prefs.edit().putBoolean(KEY_AUTO_PLAY_NEXT, value).apply()
		_autoPlayNext.value = value
	}

	fun getSubtitleFontSize(): Float = prefs.getFloat(KEY_SUBTITLE_FONT_SIZE, 18.0f)
	fun setSubtitleFontSize(value: Float) {
		prefs.edit().putFloat(KEY_SUBTITLE_FONT_SIZE, value).apply()
		_subtitleFontSize.value = value
	}

	fun getSubtitleTextColor(): Int = prefs.getInt(KEY_SUBTITLE_TEXT_COLOR, Color.WHITE)
	fun setSubtitleTextColor(value: Int) {
		prefs.edit().putInt(KEY_SUBTITLE_TEXT_COLOR, value).apply()
		_subtitleTextColor.value = value
	}

	fun getSubtitleBgColor(): Int = prefs.getInt(KEY_SUBTITLE_BG_COLOR, Color.argb(160, 0, 0, 0))
	fun setSubtitleBgColor(value: Int) {
		prefs.edit().putInt(KEY_SUBTITLE_BG_COLOR, value).apply()
		_subtitleBgColor.value = value
	}

	fun getSubtitleBold(): Boolean = prefs.getBoolean(KEY_SUBTITLE_BOLD, false)
	fun setSubtitleBold(value: Boolean) {
		prefs.edit().putBoolean(KEY_SUBTITLE_BOLD, value).apply()
		_subtitleBold.value = value
	}

	fun getRememberBrightnessVolume(): Boolean = prefs.getBoolean(KEY_REMEMBER_BRIGHTNESS_VOLUME, false)
	fun setRememberBrightnessVolume(value: Boolean) {
		prefs.edit().putBoolean(KEY_REMEMBER_BRIGHTNESS_VOLUME, value).apply()
	}

	fun getSavedBrightness(): Float = prefs.getFloat(KEY_SAVED_BRIGHTNESS, -1.0f)
	fun setSavedBrightness(value: Float) {
		prefs.edit().putFloat(KEY_SAVED_BRIGHTNESS, value).apply()
	}

	fun getSavedVolume(): Int = prefs.getInt(KEY_SAVED_VOLUME, -1)
	fun setSavedVolume(value: Int) {
		prefs.edit().putInt(KEY_SAVED_VOLUME, value).apply()
	}

	companion object {
		private const val KEY_SPEED = "pref_player_speed"
		private const val KEY_RESIZE_MODE = "pref_player_resize_mode"
		private const val KEY_AUTO_PLAY_NEXT = "pref_player_auto_play_next"
		private const val KEY_SUBTITLE_FONT_SIZE = "pref_subtitle_font_size"
		private const val KEY_SUBTITLE_TEXT_COLOR = "pref_subtitle_text_color"
		private const val KEY_SUBTITLE_BG_COLOR = "pref_subtitle_bg_color"
		private const val KEY_SUBTITLE_BOLD = "pref_subtitle_bold"
		private const val KEY_REMEMBER_BRIGHTNESS_VOLUME = "pref_remember_brightness_volume"
		private const val KEY_SAVED_BRIGHTNESS = "pref_saved_brightness"
		private const val KEY_SAVED_VOLUME = "pref_saved_volume"
	}
}
