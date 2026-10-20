package org.koitharu.kotatsu.player.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.kanade.tachiyomi.animesource.model.Video
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.koitharu.kotatsu.aniyomi.AniyomiAnimeRepositoryKEx
import org.koitharu.kotatsu.aniyomi.model.AniyomiAnimeSourceKEx
import org.koitharu.kotatsu.core.cache.MemoryContentCache
import org.koitharu.kotatsu.core.model.isAnimeSource
import org.koitharu.kotatsu.core.network.MangaHttpClient
import org.koitharu.kotatsu.core.parser.MangaDataRepository
import org.koitharu.kotatsu.history.data.HistoryRepository
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import javax.inject.Inject

@HiltViewModel
class PlayerViewModelKEx @Inject constructor(
	@ApplicationContext private val context: Context,
	private val dataRepository: MangaDataRepository,
	private val historyRepository: HistoryRepository,
	@MangaHttpClient private val httpClient: OkHttpClient,
	private val contentCache: MemoryContentCache,
) : ViewModel() {

	private var player: ExoPlayer? = null

	private val _isPlaying = MutableStateFlow(false)
	val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

	private val _currentPosition = MutableStateFlow(0L)
	val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

	private val _duration = MutableStateFlow(0L)
	val duration: StateFlow<Long> = _duration.asStateFlow()

	private val _bufferedPosition = MutableStateFlow(0L)
	val bufferedPosition: StateFlow<Long> = _bufferedPosition.asStateFlow()

	private val _videos = MutableStateFlow<List<Video>>(emptyList())
	val videos: StateFlow<List<Video>> = _videos.asStateFlow()

	private val _selectedVideo = MutableStateFlow<Video?>(null)
	val selectedVideo: StateFlow<Video?> = _selectedVideo.asStateFlow()

	private val _speed = MutableStateFlow(1.0f)
	val speed: StateFlow<Float> = _speed.asStateFlow()

	private val _manga = MutableStateFlow<Manga?>(null)
	val manga: StateFlow<Manga?> = _manga.asStateFlow()

	private val _currentChapter = MutableStateFlow<MangaChapter?>(null)
	val currentChapter: StateFlow<MangaChapter?> = _currentChapter.asStateFlow()

	private val _chapters = MutableStateFlow<List<MangaChapter>>(emptyList())
	val chapters: StateFlow<List<MangaChapter>> = _chapters.asStateFlow()

	init {
		viewModelScope.launch {
			while (true) {
				delay(500)
				player?.let { p ->
					_currentPosition.value = p.currentPosition.coerceAtLeast(0L)
					_duration.value = p.duration.coerceAtLeast(0L)
					_bufferedPosition.value = p.bufferedPosition.coerceAtLeast(0L)
					_isPlaying.value = p.isPlaying
				}
			}
		}
	}

	fun initPlayer(manga: Manga, initialChapterId: Long?) {
		_manga.value = manga
		viewModelScope.launch(Dispatchers.IO) {
			val details = if (manga.source is AniyomiAnimeSourceKEx) {
				val repo = AniyomiAnimeRepositoryKEx(manga.source as AniyomiAnimeSourceKEx, contentCache, context)
				runCatching { repo.getDetails(manga) }.getOrDefault(manga)
			} else {
				manga
			}
			_manga.value = details
			val chList = details.chapters.orEmpty()
			_chapters.value = chList

			val chapter = chList.firstOrNull { it.id == initialChapterId }
				?: chList.firstOrNull()

			if (chapter != null) {
				playChapter(chapter)
			}
		}
	}

	fun getOrCreatePlayer(): ExoPlayer {
		return player ?: ExoPlayer.Builder(context).build().also {
			player = it
			it.addListener(object : Player.Listener {
				override fun onIsPlayingChanged(isPlaying: Boolean) {
					_isPlaying.value = isPlaying
				}
			})
		}
	}

	@OptIn(UnstableApi::class)
	fun playChapter(chapter: MangaChapter) {
		_currentChapter.value = chapter
		viewModelScope.launch(Dispatchers.IO) {
			val m = _manga.value ?: return@launch
			val resolvedVideos = if (m.source is AniyomiAnimeSourceKEx) {
				val repo = AniyomiAnimeRepositoryKEx(m.source as AniyomiAnimeSourceKEx, contentCache, context)
				repo.getVideoList(chapter)
			} else {
				listOf(Video(videoUrl = chapter.url, videoTitle = "Local Video"))
			}

			_videos.value = resolvedVideos
			val firstVideo = resolvedVideos.firstOrNull()
			_selectedVideo.value = firstVideo

			if (firstVideo != null) {
				withContext(Dispatchers.Main) {
					playVideo(firstVideo)
				}
			}
		}
	}

	@OptIn(UnstableApi::class)
	fun playVideo(video: Video) {
		_selectedVideo.value = video
		val p = getOrCreatePlayer()

		val videoUrl = video.videoUrl.ifBlank { video.url }
		val uri = Uri.parse(videoUrl)

		val mediaItem = MediaItem.fromUri(uri)

		val dataSourceFactory = if (videoUrl.startsWith("http://") || videoUrl.startsWith("https://")) {
			val clientBuilder = httpClient.newBuilder()
			val headers = video.headers
			if (headers != null) {
				clientBuilder.addInterceptor { chain ->
					val builder = chain.request().newBuilder()
					headers.forEach { pair ->
						builder.header(pair.first, pair.second)
					}
					chain.proceed(builder.build())
				}
			}
			OkHttpDataSource.Factory(clientBuilder.build())
		} else {
			null
		}

		if (dataSourceFactory != null) {
			val mediaSource = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dataSourceFactory)
				.createMediaSource(mediaItem)
			p.setMediaSource(mediaSource)
		} else {
			p.setMediaItem(mediaItem)
		}

		p.prepare()
		p.playWhenReady = true
	}

	fun togglePlayPause() {
		player?.let {
			if (it.isPlaying) it.pause() else it.play()
		}
	}

	fun seekTo(positionMs: Long) {
		player?.seekTo(positionMs)
	}

	fun rewind10() {
		player?.let {
			it.seekTo((it.currentPosition - 10000).coerceAtLeast(0))
		}
	}

	fun forward10() {
		player?.let {
			it.seekTo((it.currentPosition + 10000).coerceAtMost(it.duration))
		}
	}

	fun setSpeed(speed: Float) {
		_speed.value = speed
		player?.playbackParameters = PlaybackParameters(speed)
	}

	override fun onCleared() {
		super.onCleared()
		player?.release()
		player = null
	}
}
