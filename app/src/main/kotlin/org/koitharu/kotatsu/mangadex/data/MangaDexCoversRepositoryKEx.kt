package org.koitharu.kotatsu.mangadex.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.koitharu.kotatsu.core.network.MangaHttpClient
import org.koitharu.kotatsu.parsers.util.json.getStringOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MangaDexCoversRepositoryKEx @Inject constructor(
	@MangaHttpClient private val okHttpClient: OkHttpClient,
) {

	suspend fun fetchCovers(rawUrlOrMangaId: String): Result<List<MangaDexCoverKEx>> = withContext(Dispatchers.IO) {
		runCatching {
			val titleId = MangaDexUrlUtilKEx.extractTitleId(rawUrlOrMangaId) ?: rawUrlOrMangaId
			if (titleId.isBlank()) {
				error("Invalid MangaDex title ID or URL")
			}

			val covers = mutableListOf<MangaDexCoverKEx>()
			val seenIds = mutableSetOf<String>()
			var offset = 0
			val limit = 100
			var total = Int.MAX_VALUE

			while (offset < total && covers.size < 500) {
				val url = "https://api.mangadex.org/cover".toHttpUrl().newBuilder()
					.addQueryParameter("manga[]", titleId)
					.addQueryParameter("limit", limit.toString())
					.addQueryParameter("offset", offset.toString())
					.addQueryParameter("order[volume]", "asc")
					.build()

				val request = Request.Builder()
					.url(url)
					.header("User-Agent", "DropSauce/1.0")
					.get()
					.build()

				val response = okHttpClient.newCall(request).execute()
				if (!response.isSuccessful) {
					error("MangaDex API request failed with HTTP ${response.code}")
				}

				val bodyString = response.body.string()
				val json = JSONObject(bodyString)
				val resultStatus = json.optString("result")
				if (resultStatus != "ok") {
					error("MangaDex API returned error status: $resultStatus")
				}

				total = json.optInt("total", 0)
				val dataArray = json.optJSONArray("data") ?: break
				if (dataArray.length() == 0) break

				for (i in 0 until dataArray.length()) {
					val item = dataArray.optJSONObject(i) ?: continue
					val id = item.optString("id")
					val attributes = item.optJSONObject("attributes") ?: continue
					val fileName = attributes.optString("fileName")
					if (fileName.isBlank()) continue

					val coverUrl = "https://uploads.mangadex.org/covers/$titleId/$fileName"
					val volume = attributes.getStringOrNull("volume")
					val description = attributes.getStringOrNull("description")
					val locale = attributes.getStringOrNull("locale")

					val coverId = id.ifBlank { fileName }
					if (seenIds.add(coverId)) {
						covers.add(
							MangaDexCoverKEx(
								id = coverId,
								url = coverUrl,
								volume = volume,
								description = description,
								locale = locale,
							)
						)
					}
				}

				offset += limit
			}

			covers
		}
	}
}
