package org.koitharu.kotatsu.player.ui

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
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
import org.koitharu.kotatsu.core.cache.MemoryContentCache
import org.koitharu.kotatsu.core.network.MangaHttpClient
import org.koitharu.kotatsu.core.parser.MangaRepository
import org.koitharu.kotatsu.history.data.HistoryRepository
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaChapter
import org.koitharu.kotatsu.parsers.util.runCatchingCancellable
import org.koitharu.kotatsu.player.settings.PlayerPreferencesKEx
import org.koitharu.kotatsu.player.utils.PlayerAudioTrackKEx
import org.koitharu.kotatsu.player.utils.PlayerSubtitleTrackKEx
import org.koitharu.kotatsu.player.utils.SubtitleUtilsKEx
import org.koitharu.kotatsu.player.utils.TrackUtilsKEx
import javax.inject.Inject

@OptIn(UnstableApi::class)
@HiltViewModel
class PlayerViewModelKEx @Inject constructor(
	@ApplicationContext private val context: Context,
	private val mangaRepositoryFactory: MangaRepository.Factory,
	private val historyRepository: HistoryRepository,
	@MangaHttpClient private val httpClient: OkHttpClient,
	private val contentCache: MemoryContentCache,
	val preferences: PlayerPreferencesKEx,
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

	private val _playbackState = MutableStateFlow(Player.STATE_IDLE)
	val playbackState: StateFlow<Int> = _playbackState.asStateFlow()

	private val _errorMessage = MutableStateFlow<String?>(null)
	val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

	private val _videos = MutableStateFlow<List<Video>>(emptyList())
	val videos: StateFlow<List<Video>> = _videos.asStateFlow()

	private val _selectedVideo = MutableStateFlow<Video?>(null)
	val selectedVideo: StateFlow<Video?> = _selectedVideo.asStateFlow()

	private val _audioTracks = MutableStateFlow<List<PlayerAudioTrackKEx>>(emptyList())
	val audioTracks: StateFlow<List<PlayerAudioTrackKEx>> = _audioTracks.asStateFlow()

	private val _selectedAudioTrack = MutableStateFlow<PlayerAudioTrackKEx?>(null)
	val selectedAudioTrack: StateFlow<PlayerAudioTrackKEx?> = _selectedAudioTrack.asStateFlow()

	private val _subtitleTracks = MutableStateFlow<List<PlayerSubtitleTrackKEx>>(emptyList())
	val subtitleTracks: StateFlow<List<PlayerSubtitleTrackKEx>> = _subtitleTracks.asStateFlow()

	private val _selectedSubtitleTrack = MutableStateFlow<PlayerSubtitleTrackKEx?>(null)
	val selectedSubtitleTrack: StateFlow<PlayerSubtitleTrackKEx?> = _selectedSubtitleTrack.asStateFlow()

	private val _externalSubtitles = mutableListOf<PlayerSubtitleTrackKEx>()

	private val _speed = MutableStateFlow(preferences.getSpeed())
	val speed: StateFlow<Float> = _speed.asStateFlow()

	private val _resizeMode = MutableStateFlow(preferences.getResizeMode())
	val resizeMode: StateFlow<Int> = _resizeMode.asStateFlow()

	private val _autoPlayNext = MutableStateFlow(preferences.getAutoPlayNext())
	val autoPlayNext: StateFlow<Boolean> = _autoPlayNext.asStateFlow()

	private val _subtitleOffsetMs = MutableStateFlow(0L)
	val subtitleOffsetMs: StateFlow<Long> = _subtitleOffsetMs.asStateFlow()

	private val _subtitleFontSize = MutableStateFlow(preferences.getSubtitleFontSize())
	val subtitleFontSize: StateFlow<Float> = _subtitleFontSize.asStateFlow()

	private val _subtitleTextColor = MutableStateFlow(preferences.getSubtitleTextColor())
	val subtitleTextColor: StateFlow<Int> = _subtitleTextColor.asStateFlow()

	private val _subtitleBgColor = MutableStateFlow(preferences.getSubtitleBgColor())
	val subtitleBgColor: StateFlow<Int> = _subtitleBgColor.asStateFlow()

	private val _subtitleBold = MutableStateFlow(preferences.getSubtitleBold())
	val subtitleBold: StateFlow<Boolean> = _subtitleBold.asStateFlow()

	private val _currentVolumePercent = MutableStateFlow(0.8f)
	val currentVolumePercent: StateFlow<Float> = _currentVolumePercent.asStateFlow()

	private val _currentBrightnessPercent = MutableStateFlow(0.8f)
	val currentBrightnessPercent: StateFlow<Float> = _currentBrightnessPercent.asStateFlow()

	private val _manga = MutableStateFlow<Manga?>(null)
	val manga: StateFlow<Manga?> = _manga.asStateFlow()

	private val _currentChapter = MutableStateFlow<MangaChapter?>(null)
	val currentChapter: StateFlow<MangaChapter?> = _currentChapter.asStateFlow()

	private val _chapters = MutableStateFlow<List<MangaChapter>>(emptyList())
	val chapters: StateFlow<List<MangaChapter>> = _chapters.asStateFlow()

	private var hasPendingSeek = false
	private var pendingSeekPercent = 0f

	init {
		viewModelScope.launch {
			var tickCount = 0
			while (true) {
				delay(500)
				player?.let { p ->
					val curPos = p.currentPosition.coerceAtLeast(0L)
					val dur = p.duration.coerceAtLeast(0L)
					_currentPosition.value = curPos
					_duration.value = dur
					_bufferedPosition.value = p.bufferedPosition.coerceAtLeast(0L)
					_isPlaying.value = p.isPlaying
					_playbackState.value = p.playbackState

					if (hasPendingSeek && dur > 0L && pendingSeekPercent > 0f) {
						p.seekTo((pendingSeekPercent * dur).toLong())
						hasPendingSeek = false
					}

					tickCount++
					if (tickCount % 4 == 0) {
						saveHistory()
					}
				}
			}
		}
	}

	fun initPlayer(manga: Manga, initialChapterId: Long?) {
		_manga.value = manga
		viewModelScope.launch(Dispatchers.IO) {
			val details = runCatchingCancellable { mangaRepositoryFactory.create(manga.source).getDetails(manga) }.getOrDefault(manga)
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
		return player ?: ExoPlayer.Builder(context).build().also { p ->
			player = p
			p.playbackParameters = PlaybackParameters(_speed.value)
			p.addListener(object : Player.Listener {
				override fun onIsPlayingChanged(isPlaying: Boolean) {
					_isPlaying.value = isPlaying
				}

				override fun onPlaybackStateChanged(state: Int) {
					_playbackState.value = state
					if (state == Player.STATE_READY) {
						_errorMessage.value = null
						refreshTracks()
					} else if (state == Player.STATE_ENDED) {
						saveHistory()
						if (_autoPlayNext.value && hasNextChapter()) {
							playNextChapter()
						}
					}
				}

				override fun onPlayerError(error: PlaybackException) {
					_errorMessage.value = error.localizedMessage ?: "Playback error"
				}

				override fun onTracksChanged(tracks: androidx.media3.common.Tracks) {
					refreshTracks()
				}
			})
		}
	}

	fun playChapter(chapter: MangaChapter) {
		_currentChapter.value = chapter
		_errorMessage.value = null
		viewModelScope.launch(Dispatchers.IO) {
			val m = _manga.value ?: return@launch
			val savedHistory = runCatchingCancellable { historyRepository.getOne(m) }.getOrNull()
			if (savedHistory != null && savedHistory.chapterId == chapter.id && savedHistory.percent > 0f && savedHistory.percent < 0.95f) {
				hasPendingSeek = true
				pendingSeekPercent = savedHistory.percent
			} else {
				hasPendingSeek = false
				pendingSeekPercent = 0f
			}

			val resolvedVideos = listOf(
				Video(
					videoUrl = chapter.url,
					videoTitle = chapter.title.orEmpty().ifBlank { "Episode ${chapter.number}" },
				),
			)

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

	fun playVideo(video: Video) {
		_selectedVideo.value = video
		_errorMessage.value = null
		val p = getOrCreatePlayer()

		val videoUrl = video.videoUrl.ifBlank { video.url }
		val uri = Uri.parse(videoUrl)

		val mediaItemBuilder = MediaItem.Builder().setUri(uri)

		// Attach external subtitles if available
		if (_externalSubtitles.isNotEmpty()) {
			val subConfigs = _externalSubtitles.mapNotNull { track ->
				track.uri?.let { subUri ->
					SubtitleUtilsKEx.createSubtitleConfigFromUri(context, subUri).first
				}
			}
			mediaItemBuilder.setSubtitleConfigurations(subConfigs)
		}

		val mediaItem = mediaItemBuilder.build()

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
			val mediaSource = DefaultMediaSourceFactory(dataSourceFactory).createMediaSource(mediaItem)
			p.setMediaSource(mediaSource)
		} else {
			p.setMediaItem(mediaItem)
		}

		p.prepare()
		p.playWhenReady = true
	}

	fun refreshTracks() {
		val p = player ?: return
		_audioTracks.value = TrackUtilsKEx.getAudioTracks(p)
		_subtitleTracks.value = TrackUtilsKEx.getSubtitleTracks(p, _externalSubtitles)
		_selectedAudioTrack.value = _audioTracks.value.firstOrNull { it.isSelected }
		_selectedSubtitleTrack.value = _subtitleTracks.value.firstOrNull { it.isSelected }
	}

	fun selectAudioTrack(track: PlayerAudioTrackKEx) {
		val p = player ?: return
		TrackUtilsKEx.selectAudioTrack(p, track)
		refreshTracks()
	}

	fun selectSubtitleTrack(track: PlayerSubtitleTrackKEx) {
		val p = player ?: return
		TrackUtilsKEx.selectSubtitleTrack(p, track)
		refreshTracks()
	}

	fun disableSubtitles() {
		val p = player ?: return
		TrackUtilsKEx.disableSubtitles(p)
		refreshTracks()
	}

	fun importSubtitleUri(uri: Uri) {
		val (config, label) = SubtitleUtilsKEx.createSubtitleConfigFromUri(context, uri)
		val externalTrack = PlayerSubtitleTrackKEx(
			id = "ext_${System.currentTimeMillis()}",
			language = "en",
			label = label,
			isSelected = true,
			isExternal = true,
			uri = uri,
			mimeType = config.mimeType,
		)
		_externalSubtitles.add(externalTrack)

		// Re-play current video with updated subtitle tracks
		val curVid = _selectedVideo.value
		if (curVid != null) {
			playVideo(curVid)
		}
	}

	fun togglePlayPause() {
		player?.let {
			if (it.isPlaying) it.pause() else it.play()
		}
	}

	fun play() {
		player?.play()
	}

	fun pause() {
		player?.pause()
	}

	fun hasPreviousChapter(): Boolean {
		val list = _chapters.value
		val current = _currentChapter.value ?: return false
		val index = list.indexOfFirst { it.id == current.id }
		return index > 0
	}

	fun hasNextChapter(): Boolean {
		val list = _chapters.value
		val current = _currentChapter.value ?: return false
		val index = list.indexOfFirst { it.id == current.id }
		return index >= 0 && index < list.size - 1
	}

	fun playPreviousChapter() {
		val list = _chapters.value
		val current = _currentChapter.value ?: return
		val index = list.indexOfFirst { it.id == current.id }
		if (index > 0) {
			playChapter(list[index - 1])
		}
	}

	fun playNextChapter() {
		val list = _chapters.value
		val current = _currentChapter.value ?: return
		val index = list.indexOfFirst { it.id == current.id }
		if (index >= 0 && index < list.size - 1) {
			playChapter(list[index + 1])
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

	fun scrubSeek(deltaMs: Long) {
		player?.let {
			val target = (it.currentPosition + deltaMs).coerceIn(0, it.duration.coerceAtLeast(0))
			it.seekTo(target)
		}
	}

	fun setSpeed(speed: Float) {
		_speed.value = speed
		preferences.setSpeed(speed)
		player?.playbackParameters = PlaybackParameters(speed)
	}

	fun setResizeMode(mode: Int) {
		_resizeMode.value = mode
		preferences.setResizeMode(mode)
	}

	fun setAutoPlayNext(enabled: Boolean) {
		_autoPlayNext.value = enabled
		preferences.setAutoPlayNext(enabled)
	}

	fun setSubtitleOffsetMs(offsetMs: Long) {
		_subtitleOffsetMs.value = offsetMs
	}

	fun setSubtitleFontSize(sizeSp: Float) {
		_subtitleFontSize.value = sizeSp
		preferences.setSubtitleFontSize(sizeSp)
	}

	fun setSubtitleTextColor(color: Int) {
		_subtitleTextColor.value = color
		preferences.setSubtitleTextColor(color)
	}

	fun setSubtitleBgColor(color: Int) {
		_subtitleBgColor.value = color
		preferences.setSubtitleBgColor(color)
	}

	fun setSubtitleBold(bold: Boolean) {
		_subtitleBold.value = bold
		preferences.setSubtitleBold(bold)
	}

	fun setVolumePercent(percent: Float) {
		_currentVolumePercent.value = percent.coerceIn(0f, 1f)
	}

	fun setBrightnessPercent(percent: Float) {
		_currentBrightnessPercent.value = percent.coerceIn(0f, 1f)
	}

	private fun saveHistory() {
		val m = _manga.value ?: return
		val ch = _currentChapter.value ?: return
		val dur = _duration.value
		val pos = _currentPosition.value
		if (dur > 0L) {
			val percent = (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
			viewModelScope.launch(Dispatchers.IO) {
				runCatchingCancellable {
					historyRepository.addOrUpdate(
						manga = m,
						chapterId = ch.id,
						page = 0,
						scroll = 0,
						percent = percent,
						force = false,
					)
				}
			}
		}
	}

	override fun onCleared() {
		saveHistory()
		player?.release()
		player = null
	}
}
