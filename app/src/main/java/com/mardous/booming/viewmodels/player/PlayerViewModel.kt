package com.mardous.booming.viewmodels.player

import android.content.Context
import android.os.Bundle
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.mardous.booming.extensions.media.durationStr
import com.mardous.booming.extensions.media.extraInfo
import com.mardous.booming.fragments.player.PlayerColorScheme
import com.mardous.booming.fragments.player.PlayerColorSchemeMode
import com.mardous.booming.helper.color.MediaNotificationProcessor
import com.mardous.booming.model.Song
import com.mardous.booming.model.SongProvider
import com.mardous.booming.service.constants.SessionCommand
import com.mardous.booming.service.playback.Playback
import com.mardous.booming.service.playback.PlaybackManager
import com.mardous.booming.service.queue.*
import com.mardous.booming.util.PlayOnStartupMode
import com.mardous.booming.util.Preferences
import com.mardous.booming.viewmodels.player.model.MediaEvent
import com.mardous.booming.viewmodels.player.model.PlayerProgress
import com.mardous.booming.viewmodels.player.model.SaveCoverResult
import com.mardous.booming.worker.SaveCoverWorker
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random

@OptIn(FlowPreview::class)
class PlayerViewModel(
    private val queueManager: QueueManager,
    private val playbackManager: PlaybackManager,
    private val saveCoverWorker: SaveCoverWorker
) : ViewModel(), QueueObserver {

    private var mediaController: MediaControllerCompat? = null
    private val transportControls get() = mediaController?.transportControls
    private val controllerCallback = object : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            _isPlayingFlow.value = state?.state == PlaybackStateCompat.STATE_PLAYING
        }

        override fun onSessionEvent(event: String?, extras: Bundle?) {
            val mediaEvent = MediaEvent.fromSessionEvent(event)
            if (mediaEvent != null) {
                submitEvent(mediaEvent)
            }
        }
    }

    val audioSessionId get() = playbackManager.getAudioSessionId()

    val progressFlow = playbackManager.progressFlow.map { progress ->
        PlayerProgress(progress, playbackManager.duration().toLong())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = PlayerProgress.Unspecified
    )
    val songProgress get() = progressFlow.value.progress
    val songDuration get() = progressFlow.value.total

    private val _isPlayingFlow = MutableStateFlow(playbackManager.isPlaying())
    val isPlayingFlow = _isPlayingFlow.asStateFlow()
    val isPlaying get() = isPlayingFlow.value

    private val _repeatModeFlow = MutableStateFlow(queueManager.repeatMode)
    val repeatModeFlow = _repeatModeFlow.asStateFlow()
    val repeatMode get() = repeatModeFlow.value

    private val _shuffleModeFlow = MutableStateFlow(queueManager.shuffleMode)
    val shuffleModeFlow = _shuffleModeFlow.asStateFlow()
    val shuffleMode get() = shuffleModeFlow.value

    private val _currentPositionFlow = MutableStateFlow(queueManager.position)
    val currentPositionFlow = _currentPositionFlow.asStateFlow()
    val currentPosition get() = currentPositionFlow.value

    private val _currentSongFlow = MutableStateFlow(queueManager.currentSong)
    val currentSongFlow = _currentSongFlow.asStateFlow()
    val currentSong get() = currentSongFlow.value

    private val _nextSongFlow = MutableStateFlow(queueManager.nextSong)
    val nextSongFlow = _nextSongFlow.asStateFlow()
    val nextSong get() = nextSongFlow.value

    private val _playingQueueFlow = MutableStateFlow(queueManager.playingQueue)
    val playingQueueFlow = _playingQueueFlow.asStateFlow()
    val playingQueue get() = playingQueueFlow.value

    private val _mediaEventFlow = MutableSharedFlow<MediaEvent>()
    val mediaEvent = _mediaEventFlow.asSharedFlow()

    private val _colorScheme = MutableStateFlow(PlayerColorScheme.Unspecified)
    val colorSchemeFlow = _colorScheme.asStateFlow()
    val colorScheme get() = colorSchemeFlow.value

    private val _extraInfoFlow = MutableStateFlow<String?>(null)
    val extraInfoFlow = _extraInfoFlow.asStateFlow()

    val queueDuration get() = queueManager.getDuration(currentPosition)
    val queueDurationAsString get() = queueDuration.durationStr()

    var pendingQuit: Boolean
        get() = playbackManager.pendingQuit
        set(value) { playbackManager.pendingQuit = value }

    private fun submitEvent(event: MediaEvent) = viewModelScope.launch {
        _mediaEventFlow.emit(event)
    }

    init {
        currentSongFlow.debounce(500)
            .distinctUntilChangedBy { it.id }
            .onEach { song ->
                _extraInfoFlow.value = song.extraInfo(Preferences.nowPlayingExtraInfoList)
            }
            .flowOn(IO)
            .launchIn(viewModelScope)

        queueManager.addObserver(this)
    }

    override fun queueChanged(queue: List<Song>) {
        _playingQueueFlow.value = queue
    }

    override fun queuePositionChanged(position: Int, rePosition: Boolean) {
        _currentPositionFlow.value = position
    }

    override fun repeatModeChanged(repeatMode: Playback.RepeatMode) {
        _repeatModeFlow.value = repeatMode
    }

    override fun shuffleModeChanged(shuffleMode: Playback.ShuffleMode) {
        _shuffleModeFlow.value = shuffleMode
    }

    override fun songChanged(currentSong: Song, nextSong: Song) {
        _currentSongFlow.value = currentSong
        _nextSongFlow.value = nextSong
    }

    fun setMediaController(controller: MediaControllerCompat?) {
        mediaController?.unregisterCallback(controllerCallback)
        mediaController = controller
        controller?.registerCallback(controllerCallback)
    }

    fun toggleFavorite() {
        transportControls?.sendCustomAction(SessionCommand.TOGGLE_FAVORITE, null)
    }

    fun cycleRepeatMode() {
        transportControls?.sendCustomAction(SessionCommand.CYCLE_REPEAT, null)
    }

    fun toggleShuffleMode() {
        transportControls?.sendCustomAction(SessionCommand.TOGGLE_SHUFFLE, null)
    }

    fun togglePlayPause() {
        val currentlyPlaying = _isPlayingFlow.value
        if (currentlyPlaying) {
            transportControls?.pause()
        } else {
            transportControls?.play()
        }
    }

    fun playNext() {
        transportControls?.skipToNext()
    }

    fun playPrevious() {
        transportControls?.skipToPrevious()
    }

    fun fastForward() {
        transportControls?.fastForward()
    }

    fun rewind() {
        transportControls?.rewind()
    }

    fun seekTo(positionMillis: Long) {
        transportControls?.seekTo(positionMillis)
    }

    fun isPlayingSong(song: Song) = song.id == currentSong.id

    fun playSongAt(position: Int, newPlayback: Boolean = false) {
        if (queueManager.isEmpty) return
        transportControls?.sendCustomAction(SessionCommand.PLAY_SONG_AT, bundleOf(
            SessionCommand.Extras.POSITION to position,
            SessionCommand.Extras.IS_NEW_PLAYBACK to newPlayback
        ))
    }

    fun openQueue(
        queue: List<Song>,
        position: Int = 0,
        startPlaying: Boolean = true,
        shuffleMode: Playback.ShuffleMode? = null
    ) = viewModelScope.launch(IO) {
        if (shuffleMode == Playback.ShuffleMode.On) {
            openAndShuffleQueue(queue, startPlaying = true)
        } else {
            val result = queueManager.open(queue, position, shuffleMode)
            when (result) {
                QueueManager.SUCCESS -> if (startPlaying) {
                    playSongAt(queueManager.position, newPlayback = true)
                }
                QueueManager.HANDLED_SOURCE -> {
                    playSongAt(position, newPlayback = true)
                }
            }
        }
    }

    fun openAndShuffleQueue(
        queue: List<Song>,
        startPlaying: Boolean = true
    ) = viewModelScope.launch(IO) {
        val success = queueManager.open(
            queue = queue,
            startPosition = Random.Default.nextInt(queue.size),
            shuffleMode = Playback.ShuffleMode.On
        )
        if (success == QueueManager.SUCCESS && startPlaying) {
            playSongAt(queueManager.position, newPlayback = true)
        }
    }

    fun openShuffle(
        providers: List<SongProvider>,
        mode: GroupShuffleMode,
        sortKey: String? = null
    ) = liveData(IO) {
        val success = queueManager.shuffleUsingProviders(providers, mode, sortKey)
        if (success) {
            playSongAt(queueManager.position, newPlayback = true)
        }
        emit(success)
    }

    fun openSpecialShuffle(songs: List<Song>, mode: SpecialShuffleMode) = liveData(IO) {
        emit(queueManager.specialShuffleQueue(songs, mode))
    }

    fun queueNext(song: Song) = liveData(IO) {
        if (playingQueue.isNotEmpty()) {
            queueManager.queueNext(song)
        } else {
            openQueue(listOf(song), startPlaying = false)
        }
        emit(Unit)
    }

    fun queueNext(songs: List<Song>) = liveData(IO) {
        if (playingQueue.isNotEmpty()) {
            queueManager.queueNext(songs)
        } else {
            openQueue(songs, startPlaying = false)
        }
        emit(songs.size)
    }

    fun enqueue(song: Song, to: Int = -1) = liveData(IO) {
        if (playingQueue.isNotEmpty()) {
            if (to >= 0) {
                queueManager.addSong(to, song)
            } else {
                queueManager.addSong(song)
            }
        } else {
            openQueue(listOf(song), startPlaying = false)
        }
        emit(Unit)
    }

    fun enqueue(songs: List<Song>) = liveData(IO) {
        if (playingQueue.isNotEmpty()) {
            queueManager.addSongs(songs)
        } else {
            openQueue(songs, startPlaying = false)
        }
        emit(songs.size)
    }

    fun shuffleQueue() = viewModelScope.launch(IO) {
        queueManager.shuffleQueue()
    }

    fun clearQueue() = viewModelScope.launch(IO) {
        queueManager.clearQueue()
    }

    fun stopAt(stopPosition: Int): Pair<Song, Boolean> {
        if ((currentPosition..playingQueue.lastIndex).contains(stopPosition)) {
            val stopSong = queueManager.getSongAt(stopPosition)
            if (queueManager.stopPosition == stopPosition) {
                queueManager.stopPosition = NO_POSITION
                return stopSong to false
            } else {
                queueManager.stopPosition = stopPosition
                return stopSong to true
            }
        }
        return Song.emptySong to false
    }

    fun moveSong(from: Int, to: Int) {
        if (playingQueue.indices.contains(from) && playingQueue.indices.contains(to)) {
            queueManager.moveSong(from, to)
        }
    }

    fun moveToNextPosition(fromPosition: Int) {
        val nextPosition = queueManager.getNextPosition(false)
        queueManager.moveSong(fromPosition, nextPosition)
    }

    fun removePosition(position: Int) {
        queueManager.removeSong(position)
    }

    fun restorePlayback() = viewModelScope.launch(IO) {
        if (!isPlaying && Preferences.playOnStartupMode != PlayOnStartupMode.NEVER) {
            if (queueManager.isEmpty) {
                transportControls?.sendCustomAction(SessionCommand.RESTORE_PLAYBACK, null)
            } else {
                transportControls?.play()
            }
        }
    }

    fun loadColorScheme(
        context: Context,
        mode: PlayerColorScheme.Mode,
        mediaColor: MediaNotificationProcessor
    ) = viewModelScope.launch(IO) {
        val currentScheme = colorScheme?.mode?.takeIf { it == PlayerColorSchemeMode.AppTheme }
        if (currentScheme == mode)
            return@launch

        val result = runCatching {
            PlayerColorScheme.autoColorScheme(context, mediaColor, mode)
        }
        if (result.isSuccess) {
            _colorScheme.value = result.getOrThrow()
        } else if (result.isFailure) {
            Log.e(TAG, "Failed to load color scheme", result.exceptionOrNull())
        }
    }

    fun saveCover(song: Song) = liveData {
        emit(SaveCoverResult(true))
        val uri = saveCoverWorker.saveArtwork(song)
        emit(SaveCoverResult(false, uri))
    }

    override fun onCleared() {
        setMediaController(null)
        queueManager.removeObserver(this)
        super.onCleared()
    }

    companion object {
        private const val TAG = "PlayerViewModel"
    }
}