package eu.kanade.tachiyomi.torrentutils

import eu.kanade.tachiyomi.torrentutils.model.TorrentFile
import eu.kanade.tachiyomi.torrentutils.model.TorrentInfo
import kotlinx.coroutines.runBlocking

object TorrentUtils {

    suspend fun getTorrentInfo(
        url: String,
        title: String,
    ): TorrentInfo {
        val hash = if (url.startsWith("magnet:", ignoreCase = true)) {
            val match = Regex("""xt=urn:btih:([a-zA-Z0-9]+)""").find(url)
            match?.groupValues?.get(1) ?: url.hashCode().toString()
        } else {
            url.hashCode().toString()
        }

        val file = TorrentFile(
            path = title,
            indexFile = 0,
            size = -1L,
            torrentHash = hash,
            trackers = emptyList(),
        )

        return TorrentInfo(
            title = title,
            files = listOf(file),
            hash = hash,
            size = -1L,
            trackers = emptyList(),
        )
    }

    @Deprecated(
        message = "This overload of getTorrentInfo exists only for binary compatibility with extensions targeting" +
            " other forks where getTorrentInfo was not a suspend function",
        level = DeprecationLevel.HIDDEN,
    )
    @JvmName("getTorrentInfo")
    fun blockingShimForGetTorrentInfo(
        url: String,
        title: String,
    ): TorrentInfo {
        return runBlocking {
            getTorrentInfo(url, title)
        }
    }
}
