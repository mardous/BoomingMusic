package com.mardous.booming.viewmodels.player

import android.content.Context
import android.os.Bundle
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.lifecycle.*
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
import com.mardous.booming.service.queue.GroupShuffleMode
import com.mardous.booming.service.queue.QueueManager
import com.mardous.booming.service.queue.SpecialShuffleMode
import com.mardous.booming.service.queue.StopPosition
import com.mardous.booming.util.PlayOnStartupMode
import com.mardous.booming.util.Preferences
import com.mardous.booming.viewmodels.player.model.MediaEvent
import com.mardous.booming.viewmodels.player.model.PlayerProgress
import com.mardous.booming.viewmodels.player.model.SaveCoverResult
import com.mardous.booming.worker.SaveCoverWorker
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random

@OptIn(FlowPreview::class)
class PlayerViewModel(
    private val queueManager: QueueManager,
    private val playbackManager: PlaybackManager,
    private val saveCoverWorker: SaveCoverWorker
) : ViewModel() {

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

    private val _isPlayingFlow = MutableStateFlow(false)
    val isPlayingFlow = _isPlayingFlow.asStateFlow()
    val isPlaying get() = isPlayingFlow.value

    val repeatModeFlow = queueManager.repeatModeFlow
    val repeatMode get() = repeatModeFlow.value

    val shuffleModeFlow = queueManager.shuffleModeFlow
    val shuffleMode get() = shuffleModeFlow.value

    val currentPositionFlow = queueManager.positionFlow
    val currentPosition get() = currentPositionFlow.value.value

    val currentSongFlow = queueManager.currentSongFlow
    val currentSong get() = currentSongFlow.value

    val nextSongFlow = queueManager.nextSongFlow
    val nextSong get() = nextSongFlow.value

    val playingQueueFlow = queueManager.playingQueueFlow
    val playingQueue get() = playingQueueFlow.value

    val queueDuration get() = queueManager.getDuration(currentPosition)
    val queueDurationAsString get() = queueDuration.durationStr()

    private val _mediaEventFlow = MutableSharedFlow<MediaEvent>(
        replay = 1,
        extraBufferCapacity = 32,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val mediaEvent = _mediaEventFlow.asSharedFlow()

    private val _colorScheme = MutableLiveData<PlayerColorScheme>()
    val colorSchemeObservable: LiveData<PlayerColorScheme> = _colorScheme
    val colorSchemeFlow = _colorScheme.asFlow()
    val colorScheme get() = colorSchemeObservable.value

    private val _extraInfoFlow = MutableStateFlow<String?>(null)
    val extraInfoFlow = _extraInfoFlow.asStateFlow()

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
        transportControls?.sendCustomAction(SessionCommand.TOGGLE_SHUFFLE, null)
    }

    fun toggleShuffleMode() {
        transportControls?.sendCustomAction(SessionCommand.CYCLE_REPEAT, null)
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

    fun playSongAt(position: Int) {
        queueManager.playSongAt(position)
    }

    fun openQueue(
        queue: List<Song>,
        position: Int = 0,
        startPlaying: Boolean = true,
        shuffleMode: Playback.ShuffleMode? = null
    ) = viewModelScope.launch(IO) {
        queueManager.open(queue, position, startPlaying, shuffleMode)
    }

    fun openAndShuffleQueue(
        queue: List<Song>,
        startPlaying: Boolean = true,
        shuffleMode: Playback.ShuffleMode? = null
    ) = viewModelScope.launch(IO) {
        queueManager.open(queue, Random.Default.nextInt(queue.size), startPlaying, shuffleMode)
    }

    fun openShuffle(
        providers: List<SongProvider>,
        mode: GroupShuffleMode,
        sortKey: String? = null
    ) = liveData(IO) {
        emit(queueManager.shuffleUsingProviders(providers, mode, sortKey))
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

    fun stopAt(stopPosition: Int) = liveData(IO) {
        if ((currentPosition..playingQueue.lastIndex).contains(stopPosition)) {
            var canceled = false
            if (queueManager.stopPosition == stopPosition) {
                queueManager.setStopPosition(StopPosition.INFINITE, fromUser = true)
                canceled = true
            } else {
                queueManager.setStopPosition(stopPosition, fromUser = true)
            }
            emit(!canceled)
        }
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
            _colorScheme.postValue(result.getOrThrow())
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
        mediaController?.unregisterCallback(controllerCallback)
        super.onCleared()
    }

    companion object {
        private const val TAG = "PlayerViewModel"
    }
}