package com.mardous.booming.ui.screen.player

import android.content.Context
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.mardous.booming.core.model.shuffle.GroupShuffleMode
import com.mardous.booming.core.model.shuffle.SpecialShuffleMode
import com.mardous.booming.extensions.isNightMode
import com.mardous.booming.extensions.media.durationStr
import com.mardous.booming.extensions.media.extraInfo
import com.mardous.booming.util.color.MediaNotificationProcessor
import com.mardous.booming.data.model.Song
import com.mardous.booming.data.SongProvider
import com.mardous.booming.service.MusicServiceConnection
import com.mardous.booming.service.constants.SessionCommand
import com.mardous.booming.service.playback.Playback
import com.mardous.booming.service.playback.PlaybackManager
import com.mardous.booming.service.queue.*
import com.mardous.booming.util.PlayOnStartupMode
import com.mardous.booming.util.Preferences
import com.mardous.booming.core.model.shuffle.ShuffleOperationState
import com.mardous.booming.data.local.AlbumCoverSaver
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.random.Random

@OptIn(FlowPreview::class)
class PlayerViewModel(
    private val serviceConnection: MusicServiceConnection,
    private val queueManager: QueueManager,
    private val playbackManager: PlaybackManager,
    private val albumCoverSaver: AlbumCoverSaver
) : ViewModel(), QueueObserver {

    private val transportControls get() = serviceConnection.transportControls
    val mediaEvent = serviceConnection.mediaSessionEvent
    val isConnected = serviceConnection.isConnected

    // Prevent concurrent shuffle actions
    private val shuffleMutex = Mutex()

    val isPlayingFlow = serviceConnection.playbackState.map { stateCompat ->
        stateCompat.state == PlaybackStateCompat.STATE_PLAYING
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = serviceConnection.currentPlaybackState == PlaybackStateCompat.STATE_PLAYING
    )
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

    private val _colorScheme = MutableStateFlow(PlayerColorScheme.Unspecified)
    val colorSchemeFlow = _colorScheme.asStateFlow()
    val colorScheme get() = colorSchemeFlow.value

    private val _shuffleOperationState = MutableStateFlow(ShuffleOperationState())
    val shuffleOperationState = _shuffleOperationState.asStateFlow()

    private val _extraInfoFlow = MutableStateFlow<String?>(null)
    val extraInfoFlow = _extraInfoFlow.asStateFlow()

    val queueDuration get() = queueManager.getDuration(currentPosition)
    val queueDurationAsString get() = queueDuration.durationStr()

    val currentProgressFlow = playbackManager.progressFlow
    val currentProgress get() = currentProgressFlow.value

    val totalDurationFlow = playbackManager.durationFlow
    val totalDuration get() = totalDurationFlow.value

    val audioSessionId get() = playbackManager.getAudioSessionId()

    var pendingQuit: Boolean
        get() = playbackManager.pendingQuit
        set(value) { playbackManager.pendingQuit = value }

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

    override fun queueChanged(queue: List<Song>, reason: QueueChangeReason) {
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
        if (isPlaying) {
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
        if (queue.isNotEmpty()) {
            val success = queueManager.open(
                queue = queue,
                startPosition = Random.Default.nextInt(queue.size),
                shuffleMode = Playback.ShuffleMode.On
            )
            if (success == QueueManager.SUCCESS && startPlaying) {
                playSongAt(queueManager.position, newPlayback = true)
            }
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

    fun openSpecialShuffle(songs: List<Song>, mode: SpecialShuffleMode) = viewModelScope.launch {
        if (shuffleOperationState.value.isIdle) {
            _shuffleOperationState.value = ShuffleOperationState(mode, ShuffleOperationState.Status.InProgress)
            val success = withContext(IO) {
                queueManager.specialShuffleQueue(songs, mode)
            }
            if (success) {
                playSongAt(queueManager.position, newPlayback = true)
            }
            _shuffleOperationState.value = ShuffleOperationState()
        }
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
        shuffleMutex.withLock {
            queueManager.shuffleQueue()
        }
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
            transportControls?.sendCustomAction(SessionCommand.RESTORE_PLAYBACK, null)
        }
    }

    fun loadColorScheme(
        context: Context,
        mode: PlayerColorScheme.Mode,
        mediaColor: MediaNotificationProcessor
    ) = viewModelScope.launch(IO) {
        val currentScheme = colorScheme.mode.takeIf { it == PlayerColorSchemeMode.AppTheme }
        if (currentScheme == mode && colorScheme.isDark == context.isNightMode)
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
        val uri = albumCoverSaver.saveArtwork(song)
        emit(SaveCoverResult(false, uri))
    }

    override fun onCleared() {
        queueManager.removeObserver(this)
        super.onCleared()
    }

    companion object {
        private const val TAG = "PlayerViewModel"
    }
}