package com.mardous.booming.ui.screen.player

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.mardous.booming.core.model.MediaEvent
import com.mardous.booming.core.model.PaletteColor
import com.mardous.booming.core.model.shuffle.GroupShuffleMode
import com.mardous.booming.core.model.shuffle.ShuffleOperationState
import com.mardous.booming.core.model.shuffle.SpecialShuffleMode
import com.mardous.booming.core.sort.SongSortMode
import com.mardous.booming.data.SongProvider
import com.mardous.booming.data.local.AlbumCoverSaver
import com.mardous.booming.data.local.MetadataReader
import com.mardous.booming.data.local.repository.Repository
import com.mardous.booming.data.model.QueuePosition
import com.mardous.booming.data.model.Song
import com.mardous.booming.extensions.files.toAudioFile
import com.mardous.booming.extensions.isNightMode
import com.mardous.booming.extensions.utilities.DEFAULT_INFO_DELIMITER
import com.mardous.booming.playback.Playback
import com.mardous.booming.playback.getQueueItems
import com.mardous.booming.playback.progress.ProgressObserver
import com.mardous.booming.playback.shuffle.OpenShuffleMode
import com.mardous.booming.playback.shuffle.ShuffleManager
import com.mardous.booming.playback.toMediaItems
import com.mardous.booming.util.Preferences
import com.mardous.booming.util.REMEMBER_SHUFFLE_MODE
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

const val QUEUE_DEBOUNCE = 100L

@OptIn(FlowPreview::class)
@androidx.annotation.OptIn(UnstableApi::class)
class PlayerViewModel(
    private val preferences: SharedPreferences,
    private val repository: Repository,
    private val albumCoverSaver: AlbumCoverSaver
) : ViewModel(), Player.Listener {

    private val queueMutex = Mutex()
    private val progressObserver = ProgressObserver(intervalMs = 100)
    private val shuffleManager = ShuffleManager()
    private var mediaController: MediaController? = null

    private val _mediaEvent = MutableSharedFlow<MediaEvent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val mediaEvent = _mediaEvent.asSharedFlow()

    private val _isPlayingFlow = MutableStateFlow(false)
    val isPlayingFlow = _isPlayingFlow.asStateFlow()
    val isPlaying get() = _isPlayingFlow.value

    private val _progressFlow = MutableStateFlow(C.TIME_UNSET)
    val progressFlow = _progressFlow.asStateFlow()
    val progress get() = progressFlow.value

    private val _durationFlow = MutableStateFlow(C.TIME_UNSET)
    val durationFlow = _durationFlow.asStateFlow()
    val duration get() = durationFlow.value

    private val _repeatModeFlow = MutableStateFlow(REPEAT_MODE_OFF)
    val repeatModeFlow = _repeatModeFlow.asStateFlow()
    val repeatMode get() = repeatModeFlow.value

    private val _shuffleModeFlow = MutableStateFlow(false)
    val shuffleModeFlow = _shuffleModeFlow.asStateFlow()
    val shuffleModeEnabled get() = shuffleModeFlow.value

    private val _queueFlow = MutableStateFlow(emptyList<Song>())
    val queueFlow = _queueFlow.asStateFlow()
    val queue get() = queueFlow.value

    private val _positionFlow = MutableStateFlow(QueuePosition.Undefined)
    val positionFlow = _positionFlow.asStateFlow()
    val position get() = positionFlow.value

    private val _currentSongFlow = MutableStateFlow(Song.emptySong)
    val currentSongFlow = _currentSongFlow.asStateFlow()
    val currentSong get() = currentSongFlow.value

    private val _nextSongFlow = MutableStateFlow(Song.emptySong)
    val nextSongFlow = _nextSongFlow.asStateFlow()
    val nextSong get() = nextSongFlow.value

    private val _colorScheme = MutableStateFlow(PlayerColorScheme.Unspecified)
    val colorSchemeFlow = _colorScheme.asStateFlow()
    val colorScheme get() = colorSchemeFlow.value

    private val _shuffleOperationState = MutableStateFlow(ShuffleOperationState())
    val shuffleOperationState = _shuffleOperationState.asStateFlow()

    private val _extraInfoFlow = MutableStateFlow<String?>(null)
    val extraInfoFlow = _extraInfoFlow.asStateFlow()

    private val internalJobs = mutableListOf<Job>()

    override fun onCleared() {
        progressObserver.stop()
        cancelInternalJobs()
        super.onCleared()
    }

    fun setMediaController(mediaController: MediaController?) {
        if (this.mediaController == mediaController) return

        this.mediaController = mediaController
        cancelInternalJobs()

        if (mediaController != null) {
            _isPlayingFlow.value = mediaController.isPlaying
            _repeatModeFlow.value = mediaController.repeatMode
            _shuffleModeFlow.value = mediaController.shuffleModeEnabled

            if (progress == C.TIME_UNSET || duration == C.TIME_UNSET) {
                _progressFlow.value = mediaController.contentPosition
                _durationFlow.value = mediaController.contentDuration
            }

            onGenerateQueue(mediaController)

            internalJobs += mediaEvent
                .filter { it == MediaEvent.MediaContentChanged }
                .debounce(500)
                .onEach { event -> onGenerateQueue(mediaController) }
                .launchIn(viewModelScope)

            internalJobs += combine(queueFlow, positionFlow)
            { queue, position -> Pair(queue, position) }
                .debounce(100)
                .onEach { (queue, position) ->
                    _currentSongFlow.value = queue.getOrElse(position.current) { Song.emptySong }
                    _nextSongFlow.value = queue.getOrElse(position.next) { Song.emptySong }
                }
                .launchIn(viewModelScope)

            internalJobs += currentSongFlow
                .debounce(500)
                .distinctUntilChangedBy { it.id }
                .onEach { song -> onGenerateExtraInfo(song) }
                .launchIn(viewModelScope)

            internalJobs += isPlayingFlow
                .onEach { isPlaying -> onSetIsPlaying(isPlaying) }
                .launchIn(viewModelScope)
        }
    }

    fun submitEvent(mediaEvent: MediaEvent) {
        _mediaEvent.tryEmit(mediaEvent)
    }

    private fun cancelInternalJobs() {
        internalJobs.forEach { it.cancel() }
        internalJobs.clear()
    }

    private fun onSetIsPlaying(isPlaying: Boolean) {
        if (isPlaying) {
            progressObserver.start {
                mediaController?.let { controller ->
                    _progressFlow.value = controller.contentPosition
                    _durationFlow.value = controller.contentDuration
                }
            }
        } else {
            progressObserver.stop()
        }
    }

    private fun onGenerateQueue(
        player: Player,
        timeline: Timeline = player.currentTimeline
    ) = viewModelScope.launch {
        queueMutex.withLock {
            // If the timeline is empty, reset the queue and exit early.
            if (timeline.isEmpty) {
                _queueFlow.value = emptyList()
                return@launch
            }

            // Capture the player's current state.
            val shuffle = player.shuffleModeEnabled
            val playerIndex = player.currentMediaItemIndex

            val queueItems = player.getQueueItems(shuffle)
            val indicesInTimeline = queueItems.map { it.indexInTimeline }.toIntArray()
            val queuePosition = QueuePosition(
                current = indicesInTimeline.indexOf(playerIndex),
                indicesInTimeline = indicesInTimeline
            )

            // Retrieve existing songs for the given MediaItems and detect missing ones.
            val (songs, missingMediaItems) = withContext(IO) {
                repository.songsByMediaItems(queueItems.map { it.mediaItem })
            }

            // Build a set of IDs representing missing (deleted) MediaItems.
            val missingIds = missingMediaItems.mapTo(HashSet()) { it.mediaId }
            if (missingIds.isNotEmpty()) {
                // Identify contiguous ranges of missing items to remove them in grouped batches.
                val ranges = mutableListOf<IntRange>()
                var start = -1

                for (i in queueItems.indices) {
                    val missing = queueItems[i].mediaItem.mediaId in missingIds
                    if (missing && start == -1) {
                        // Beginning of a new missing range.
                        start = i
                    } else if (!missing && start != -1) {
                        // End of the current missing range.
                        ranges += (start until i)
                        start = -1
                    }
                }

                // If the last range extends to the end of the list, close it.
                if (start != -1) ranges += (start until queueItems.size)

                // Remove ranges in reverse order to avoid index shifting issues.
                for (range in ranges.asReversed()) {
                    player.removeMediaItems(range.first, range.last + 1)
                }
            }

            // Update the queue with the valid songs and current positions.
            _queueFlow.value = songs
            _positionFlow.value = queuePosition
        }
    }

    private fun onGenerateExtraInfo(song: Song) = viewModelScope.launch(IO) {
        val metadataReader = MetadataReader(song.uri)
        val audioFile = File(song.data).toAudioFile()
        _extraInfoFlow.value = if (Preferences.displayExtraInfo) {
            Preferences.nowPlayingExtraInfoList
                .filter { it.isEnabled }
                .mapNotNull {
                    it.info.toReadableFormat(metadataReader, audioFile?.audioHeader)
                }
                .joinToString(separator = DEFAULT_INFO_DELIMITER)
        } else null
    }

    override fun onEvents(player: Player, events: Player.Events) {
        val isPlayStateEvent = events.containsAny(
            Player.EVENT_PLAYBACK_STATE_CHANGED,
            Player.EVENT_IS_PLAYING_CHANGED,
            Player.EVENT_PLAY_WHEN_READY_CHANGED
        )
        if (isPlayStateEvent) {
            _isPlayingFlow.value = player.playWhenReady && player.isPlaying
            if (player.playbackState == Player.STATE_READY && !player.playWhenReady) {
                _progressFlow.value = player.contentPosition
                _durationFlow.value = player.contentDuration
            }
        }
        if (events.contains(Player.EVENT_REPEAT_MODE_CHANGED)) {
            _repeatModeFlow.value = player.repeatMode
        }
        if (events.contains(Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED)) {
            if (!events.contains(Player.EVENT_TIMELINE_CHANGED)) {
                onGenerateQueue(player)
            }
            _shuffleModeFlow.value = player.shuffleModeEnabled
        }
        if (events.contains(Player.EVENT_POSITION_DISCONTINUITY)) {
            if (!events.contains(Player.EVENT_TIMELINE_CHANGED)) {
                _positionFlow.value = position.setCurrentIndex(player.currentMediaItemIndex)
            }
            if (!player.playWhenReady) {
                _progressFlow.value = player.contentPosition
                _durationFlow.value = player.contentDuration
            }
        }
        if (events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)) {
            if (!events.contains(Player.EVENT_TIMELINE_CHANGED)) {
                _positionFlow.value = position.setCurrentIndex(player.currentMediaItemIndex)
            }
        }
        if (events.contains(Player.EVENT_TIMELINE_CHANGED)) {
            onGenerateQueue(player)
        }
    }

    fun toggleFavorite() {
        mediaController?.sendCustomCommand(SessionCommand(Playback.TOGGLE_FAVORITE, Bundle.EMPTY), Bundle.EMPTY)
    }

    fun cycleRepeatMode() {
        mediaController?.sendCustomCommand(SessionCommand(Playback.CYCLE_REPEAT, Bundle.EMPTY), Bundle.EMPTY)
    }

    fun toggleShuffleMode() {
        mediaController?.sendCustomCommand(SessionCommand(Playback.TOGGLE_SHUFFLE, Bundle.EMPTY), Bundle.EMPTY)
    }

    fun togglePlayPause() {
        if (isPlaying) {
            mediaController?.pause()
        } else {
            mediaController?.play()
        }
    }

    fun seekToNext() {
        mediaController?.seekToNext()
    }

    fun seekToPrevious() {
        mediaController?.seekToPrevious()
    }

    fun seekForward() {
        mediaController?.seekForward()
    }

    fun seekBack() {
        mediaController?.seekBack()
    }

    fun seekTo(positionMillis: Long) {
        mediaController?.seekTo(positionMillis)
    }

    fun generateExtraInfo() {
        onGenerateExtraInfo(currentSong)
    }

    fun playSongAt(newPosition: Int) {
        mediaController?.let { controller ->
            if (controller.playbackState == Player.STATE_READY) {
                if (!controller.currentTimeline.isEmpty) {
                    controller.seekToDefaultPosition(position.getIndexForPosition(newPosition))
                }
            }
        }
    }

    fun playMediaId(mediaId: String, shuffleMode: Boolean = false) {
        mediaController?.let { controller ->
            controller.shuffleModeEnabled = shuffleMode
            controller.setMediaItem(
                MediaItem.Builder()
                    .setMediaId(mediaId)
                    .build(),
                true
            )
            controller.prepare()
            controller.play()
        }
    }

    fun openQueue(
        queue: List<Song>,
        position: Int = 0,
        startPlaying: Boolean = true,
        shuffleMode: OpenShuffleMode = OpenShuffleMode.Remember
    ) = viewModelScope.launch {
        mediaController?.let { controller ->
            var shuffleModeEnabled = controller.shuffleModeEnabled
            if (!preferences.getBoolean(REMEMBER_SHUFFLE_MODE, true)) {
                shuffleModeEnabled = false
            }
            val mediaItems = withContext(IO) { queue.toMediaItems() }
            val shuffleMode = when (shuffleMode) {
                OpenShuffleMode.On -> true
                OpenShuffleMode.Off -> false
                OpenShuffleMode.Remember -> shuffleModeEnabled
            }
            if (mediaItems.isNotEmpty()) {
                controller.shuffleModeEnabled = shuffleMode
                controller.setMediaItems(mediaItems, position, C.TIME_UNSET)
                controller.playWhenReady = startPlaying
                controller.prepare()
            }
        }
    }

    fun openAndShuffleQueue(queue: List<Song>) = viewModelScope.launch {
        mediaController?.let { controller ->
            val mediaItems = withContext(IO) { queue.toMediaItems() }
            if (mediaItems.isNotEmpty()) {
                controller.shuffleModeEnabled = true
                controller.setMediaItems(mediaItems, true)
                controller.prepare()
                controller.play()
            }
        }
    }

    fun openShuffle(
        providers: List<SongProvider>,
        mode: GroupShuffleMode,
        sortMode: SongSortMode
    ) = liveData {
        val mediaItems = withContext(IO) {
            shuffleManager.shuffleByProvider(providers, mode, sortMode).toMediaItems()
        }
        if (mediaItems.isNotEmpty()) {
            mediaController?.let { controller ->
                controller.shuffleModeEnabled = true
                val resultFuture = controller.sendCustomCommand(
                    SessionCommand(Playback.SET_UNSHUFFLED_ORDER, Bundle.EMPTY),
                    Bundle.EMPTY
                )
                val result = runCatching { resultFuture.await() }
                    .getOrDefault(SessionResult(SessionError.ERROR_UNKNOWN))
                if (result.resultCode == SessionResult.RESULT_SUCCESS) {
                    controller.setMediaItems(mediaItems)
                    controller.prepare()
                    controller.play()
                }
            }
            emit(true)
        } else {
            emit(false)
        }
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    fun openSpecialShuffle(songs: List<Song>, mode: SpecialShuffleMode) = viewModelScope.launch {
        if (shuffleOperationState.value.isIdle) {
            _shuffleOperationState.value = ShuffleOperationState(mode, ShuffleOperationState.Status.InProgress)
            val mediaItems = withContext(IO) {
                shuffleManager.applySmartShuffle(songs, mode).toMediaItems()
            }
            if (mediaItems.isNotEmpty()) {
                mediaController?.let { controller ->
                    controller.shuffleModeEnabled = true
                    val resultFuture = controller.sendCustomCommand(
                        SessionCommand(Playback.SET_UNSHUFFLED_ORDER, Bundle.EMPTY),
                        Bundle.EMPTY
                    )
                    val result = runCatching { resultFuture.await() }
                        .getOrDefault(SessionResult(SessionError.ERROR_UNKNOWN))
                    if (result.resultCode == SessionResult.RESULT_SUCCESS) {
                        controller.setMediaItems(mediaItems, true)
                        controller.prepare()
                        controller.play()
                    }
                }
            }
            _shuffleOperationState.value = ShuffleOperationState()
        }
    }

    fun queueNext(song: Song) {
        mediaController?.let { controller ->
            if (controller.currentTimeline.isEmpty) {
                openQueue(listOf(song), startPlaying = false)
            } else {
                var nextIndex = position.getIndexForPosition(position.next)
                if (nextIndex == C.INDEX_UNSET) {
                    nextIndex = controller.mediaItemCount
                }
                controller.addMediaItem(nextIndex, song.toMediaItem())
            }
        }
    }

    fun queueNext(songs: List<Song>) {
        mediaController?.let { controller ->
            if (controller.currentTimeline.isEmpty) {
                openQueue(songs, startPlaying = false)
            } else {
                var nextIndex = position.getIndexForPosition(position.next)
                if (nextIndex == C.INDEX_UNSET) {
                    nextIndex = controller.mediaItemCount
                }
                controller.addMediaItems(nextIndex, songs.toMediaItems())
            }
        }
    }

    fun enqueue(song: Song, toPosition: Int = -1) {
        mediaController?.let { controller ->
            if (controller.currentTimeline.isEmpty) {
                openQueue(listOf(song), startPlaying = false)
            } else {
                val toIndex = position.getIndexForPosition(toPosition)
                if (toPosition >= 0 && toIndex >= 0) {
                    controller.addMediaItem(toIndex, song.toMediaItem())
                } else {
                    controller.addMediaItem(song.toMediaItem())
                }
            }
        }
    }

    fun enqueue(songs: List<Song>) {
        mediaController?.let { controller ->
            if (controller.currentTimeline.isEmpty) {
                openQueue(songs, startPlaying = false)
            } else {
                controller.addMediaItems(songs.toMediaItems())
            }
        }
    }

    fun clearQueue() {
        mediaController?.clearMediaItems()
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    fun stopAt(stopPosition: Int) = liveData {
        mediaController?.let { controller ->
            if (stopPosition >= 0 && stopPosition < controller.mediaItemCount) {
                val stopIndex = position.getIndexForPosition(stopPosition)
                val mediaItem = controller.getMediaItemAt(stopIndex)
                val resultFuture = controller.sendCustomCommand(
                    SessionCommand(Playback.SET_STOP_POSITION, bundleOf("index" to stopIndex)),
                    Bundle.EMPTY
                )
                val result = runCatching { resultFuture.await() }
                    .getOrDefault(SessionResult(SessionError.ERROR_UNKNOWN))
                if (result.resultCode == SessionResult.RESULT_SUCCESS) {
                    val canceled = result.extras.getBoolean("canceled", false)
                    emit(mediaItem.mediaMetadata.title to canceled)
                } else {
                    emit(null to false)
                }
            }
        }
    }

    fun moveSong(fromPosition: Int, toPosition: Int) {
        mediaController?.moveMediaItem(
            position.getIndexForPosition(fromPosition),
            position.getIndexForPosition(toPosition)
        )
    }

    fun moveToNextPosition(fromPosition: Int) {
        moveSong(fromPosition, position.next)
    }

    fun removePosition(positionToRemove: Int) {
        mediaController?.removeMediaItem(position.getIndexForPosition(positionToRemove))
    }

    fun restorePlayback() = viewModelScope.launch {
        mediaController?.let { controller ->
            val resultFuture = controller.sendCustomCommand(
                SessionCommand(Playback.RESTORE_PLAYBACK, Bundle.EMPTY),
                Bundle.EMPTY
            )
            val result = runCatching { resultFuture.await() }
                .getOrDefault(SessionResult(SessionError.ERROR_UNKNOWN))
            if (result.resultCode == SessionResult.RESULT_SUCCESS) {
                controller.playWhenReady = true
            }
        }
    }

    fun generatePlayerScheme(
        context: Context,
        mode: PlayerColorScheme.Mode,
        color: PaletteColor
    ) = viewModelScope.launch(Dispatchers.Default) {
        val currentScheme = colorScheme.mode.takeIf { it == PlayerColorSchemeMode.AppTheme }
        if (currentScheme == mode && colorScheme.isDark == context.isNightMode)
            return@launch

        val result = runCatching {
            PlayerColorScheme.autoColorScheme(context, color, mode)
        }
        if (result.isSuccess) {
            _colorScheme.value = result.getOrThrow()
        } else if (result.isFailure) {
            Log.e(TAG, "Failed to load color scheme", result.exceptionOrNull())
        }
    }

    fun saveCover(song: Song) = liveData(IO) {
        emit(SaveCoverResult(true))
        val uri = albumCoverSaver.saveArtwork(song)
        emit(SaveCoverResult(false, uri))
    }

    companion object {
        private const val TAG = "PlayerViewModel"
    }
}