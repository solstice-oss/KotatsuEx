package org.koitharu.kotatsu.player.utils

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.util.TypedValue
import androidx.annotation.OptIn
import androidx.documentfile.provider.DocumentFile
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView

@OptIn(UnstableApi::class)
object SubtitleUtilsKEx {

	fun createSubtitleConfigFromUri(context: Context, uri: Uri): Pair<MediaItem.SubtitleConfiguration, String> {
		val doc = DocumentFile.fromSingleUri(context, uri)
		val filename = doc?.name ?: "external_subtitle.srt"

		val mimeType = when {
			filename.endsWith(".srt", ignoreCase = true) -> MimeTypes.APPLICATION_SUBRIP
			filename.endsWith(".vtt", ignoreCase = true) -> MimeTypes.TEXT_VTT
			filename.endsWith(".ass", ignoreCase = true) || filename.endsWith(".ssa", ignoreCase = true) -> MimeTypes.TEXT_SSA
			filename.endsWith(".ttml", ignoreCase = true) || filename.endsWith(".xml", ignoreCase = true) -> MimeTypes.APPLICATION_TTML
			else -> MimeTypes.APPLICATION_SUBRIP
		}

		val label = filename.substringBeforeLast('.')

		val config = MediaItem.SubtitleConfiguration.Builder(uri)
			.setMimeType(mimeType)
			.setLanguage("en")
			.setLabel(label)
			.setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
			.build()

		return Pair(config, label)
	}

	fun applySubtitleStyle(
		playerView: PlayerView,
		fontSizeSp: Float = 18f,
		textColor: Int = Color.WHITE,
		bgColor: Int = Color.argb(160, 0, 0, 0),
		isBold: Boolean = false,
	) {
		val subtitleView = playerView.subtitleView ?: return

		val typeface = if (isBold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT

		val captionStyle = CaptionStyleCompat(
			textColor,
			bgColor,
			Color.TRANSPARENT,
			CaptionStyleCompat.EDGE_TYPE_OUTLINE,
			Color.BLACK,
			typeface,
		)

		subtitleView.setStyle(captionStyle)
		subtitleView.setFixedTextSize(TypedValue.COMPLEX_UNIT_SP, fontSizeSp)
	}
}
