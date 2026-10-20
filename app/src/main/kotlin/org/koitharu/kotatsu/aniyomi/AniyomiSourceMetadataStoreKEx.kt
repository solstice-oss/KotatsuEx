package org.koitharu.kotatsu.aniyomi

import android.content.Context
import androidx.core.content.edit
import eu.kanade.tachiyomi.animesource.model.AnimeUpdateStrategy
import eu.kanade.tachiyomi.animesource.model.SAnime
import eu.kanade.tachiyomi.util.lang.Hash
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

internal class AniyomiSourceMetadataStoreKEx(context: Context) {

	private val preferences = context.getSharedPreferences(STORAGE_NAME, Context.MODE_PRIVATE)
	private val json = Json

	fun restore(sourceId: Long, animeUrl: String, anime: SAnime) {
		val prefix = keyPrefix(sourceId, animeUrl)
		readMemo(prefix + KEY_MEMO)?.let { anime.memo = it }
		preferences.getString(prefix + KEY_UPDATE_STRATEGY, null)
			?.let { runCatching { AnimeUpdateStrategy.valueOf(it) }.getOrNull() }
			?.let { anime.update_strategy = it }
		if (preferences.contains(prefix + KEY_INITIALIZED)) {
			anime.initialized = preferences.getBoolean(prefix + KEY_INITIALIZED, anime.initialized)
		}
	}

	fun saveMemos(sourceId: Long, animes: List<SAnime>) {
		val changed = animes.mapNotNull { anime ->
			if (anime.memo.isEmpty()) return@mapNotNull null
			val key = keyPrefix(sourceId, anime.url) + KEY_MEMO
			val value = anime.memo.toString()
			if (preferences.getString(key, null) == value) null else key to value
		}
		if (changed.isEmpty()) return
		preferences.edit {
			for ((key, value) in changed) putString(key, value)
		}
	}

	fun save(sourceId: Long, animeUrl: String, anime: SAnime) {
		val prefix = keyPrefix(sourceId, animeUrl)
		val memo = anime.memo.toString()
		val strategy = anime.update_strategy.name
		if (preferences.getString(prefix + KEY_MEMO, null) == memo &&
			preferences.getString(prefix + KEY_UPDATE_STRATEGY, null) == strategy &&
			preferences.contains(prefix + KEY_INITIALIZED) &&
			preferences.getBoolean(prefix + KEY_INITIALIZED, false) == anime.initialized
		) {
			return
		}
		preferences.edit {
			putString(prefix + KEY_MEMO, memo)
			putString(prefix + KEY_UPDATE_STRATEGY, strategy)
			putBoolean(prefix + KEY_INITIALIZED, anime.initialized)
		}
	}

	fun restoreEpisodeMemo(sourceId: Long, animeUrl: String, episodeUrl: String): JsonObject? =
		readMemo(episodeMemoKey(sourceId, animeUrl, episodeUrl))

	private fun readMemo(key: String): JsonObject? = preferences.getString(key, null)?.let { encoded ->
		runCatching { json.parseToJsonElement(encoded).jsonObject }.getOrNull()
	}

	fun saveEpisodeMemos(sourceId: Long, animeUrl: String, memos: Map<String, JsonObject>) {
		val changed = memos.mapNotNull { (episodeUrl, memo) ->
			val key = episodeMemoKey(sourceId, animeUrl, episodeUrl)
			val value = memo.toString()
			if (preferences.getString(key, null) == value) null else key to value
		}
		if (changed.isEmpty()) return
		preferences.edit {
			for ((key, value) in changed) putString(key, value)
		}
	}

	private fun keyPrefix(sourceId: Long, animeUrl: String): String =
		Hash.sha256("$sourceId\n$animeUrl") + "."

	private fun episodeMemoKey(sourceId: Long, animeUrl: String, episodeUrl: String): String =
		keyPrefix(sourceId, "$animeUrl\n$episodeUrl") + KEY_MEMO

	private companion object {
		const val STORAGE_NAME = "aniyomi_source_metadata"
		const val KEY_MEMO = "memo"
		const val KEY_UPDATE_STRATEGY = "update_strategy"
		const val KEY_INITIALIZED = "initialized"
	}
}
