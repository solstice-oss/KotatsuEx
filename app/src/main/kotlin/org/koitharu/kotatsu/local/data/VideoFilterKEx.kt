package org.koitharu.kotatsu.local.data

import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import java.io.File

private val VIDEO_EXTENSIONS = setOf(
	"mp4", "mkv", "webm", "avi", "mov", "flv", "3gp", "m4v", "ts",
)

fun isSupportedVideo(string: String): Boolean {
	val ext = string.substringAfterLast('.', "").lowercase()
	return ext in VIDEO_EXTENSIONS
}

fun hasVideoExtension(string: String): Boolean {
	val ext = string.substringAfterLast('.', "").lowercase()
	return ext in VIDEO_EXTENSIONS
}

val File.isVideoFile: Boolean
	get() = isFile && hasVideoExtension(name)

val Manga.isVideo: Boolean
	get() = hasVideoExtension(url.substringBefore('#')) ||
		chapters?.any { hasVideoExtension(it.url.substringBefore('#')) } == true

val MangaChapter.isVideo: Boolean
	get() = hasVideoExtension(url.substringBefore('#'))
