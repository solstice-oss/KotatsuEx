package org.koitharu.kotatsu.local.data

import java.io.File

private val VIDEO_EXTENSIONS = setOf("mp4", "mkv", "avi", "webm", "mov", "flv", "ts", "3gp", "m4v", "wmv")

fun isVideoExtension(ext: String?): Boolean {
	return ext?.lowercase() in VIDEO_EXTENSIONS
}

fun hasVideoExtension(string: String): Boolean {
	val ext = string.substringAfterLast('.', "")
	return isVideoExtension(ext)
}

val File.isVideoFile: Boolean
	get() = isFile && isVideoExtension(extension)
