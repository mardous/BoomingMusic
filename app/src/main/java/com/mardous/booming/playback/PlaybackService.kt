package com.mardous.booming.playback

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Process
import android.util.Log
import androidx.annotation.OptIn
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.os.bundleOf
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.TrackSelectionParameters.AudioOffloadPreferences
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.ShuffleOrder.UnshuffledShuffleOrder
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.mp3.Mp3Extractor
import androidx.media3.session.CacheBitmapLoader
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.size.Scale
import coil3.toBitmap
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.mardous.booming.R
import com.mardous.booming.coil.CoilBitmapLoader
import com.mardous.booming.core.appwidgets.BoomingGlanceWidget
import com.mardous.booming.core.appwidgets.state.PlaybackState
import com.mardous.booming.core.appwidgets.state.PlaybackStateDefinition
import com.mardous.booming.core.audio.AudioOutputObserver
import com.mardous.booming.data.local.MediaStoreObserver
import com.mardous.booming.data.local.ReplayGainTagExtractor
import com.mardous.booming.data.local.repository.Repository
import com.mardous.booming.data.model.Song
import com.mardous.booming.extensions.isBluetoothA2dpConnected
import com.mardous.booming.extensions.isBluetoothA2dpDisconnected
import com.mardous.booming.extensions.showToast
import com.mardous.booming.playback.equalizer.EqualizerManager
import com.mardous.booming.playback.library.LibraryProvider
import com.mardous.booming.playback.library.MediaIDs
import com.mardous.booming.playback.processor.BalanceAudioProcessor
import com.mardous.booming.playback.processor.ReplayGainAudioProcessor
import com.mardous.booming.ui.screen.MainActivity
import com.mardous.booming.util.CLEAR_QUEUE_ON_COMPLETION
import com.mardous.booming.util.ENABLE_HISTORY
import com.mardous.booming.util.IGNORE_AUDIO_FOCUS
import com.mardous.booming.util.MP3_INDEX_SEEKING
import com.mardous.booming.util.PAUSE_ON_ZERO_VOLUME
import com.mardous.booming.util.PLAY_ON_STARTUP_MODE
import com.mardous.booming.util.PlayOnStartupMode
import com.mardous.booming.util.Preferences
import com.mardous.booming.util.Preferences.requireString
import com.mardous.booming.util.QUEUE_NEXT_MODE
import com.mardous.booming.util.REWIND_WITH_BACK
import com.mardous.booming.util.SEEK_INTERVAL
import com.mardous.booming.util.STOP_WHEN_CLOSED_FROM_RECENTS
import com.mardous.booming.util.SongPlayCountHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import java.io.ByteArrayOutputStream
import kotlin.random.Random

@OptIn(UnstableApi::class)
class PlaybackService :
    MediaLibraryService(),
    MediaLibrarySession.Callback,
    Player.Listener,
    SharedPreferences.OnSharedPreferenceChangeListener {

    private val serviceScope = CoroutineScope(Job() + Main)
    private val uiHandler = Handler(Looper.getMainLooper())

    private val preferences: SharedPreferences by inject()
    private val sleepTimer: SleepTimer by inject()
    private val equalizerManager: EqualizerManager by inject()
    private val audioOutputObserver: AudioOutputObserver by inject()
    private val repository: Repository by inject()

    private val libraryProvider = LibraryProvider(repository)
    private val songPlayCountHelper = SongPlayCountHelper()
    private val mediaStoreObserver = MediaStoreObserver(uiHandler) {
        mediaSession?.broadcastCustomCommand(
            SessionCommand(Playback.EVENT_MEDIA_CONTENT_CHANGED, Bundle.EMPTY),
            Bundle.EMPTY
        )
    }

    private val playerThread = HandlerThread("Booming-ExoPlayer", Process.THREAD_PRIORITY_AUDIO)
    private val balanceProcessor: BalanceAudioProcessor by inject()
    private val replayGainProcessor: ReplayGainAudioProcessor by inject()

    private lateinit var nm: NotificationManager
    private lateinit var persistentStorage: PersistentStorage
    private lateinit var customCommands: List<CommandButton>
    private lateinit var player: AdvancedForwardingPlayer
    private var mediaSession: MediaLibrarySession? = null

    private var pausedByZeroVolume = false
    private var hasSetUnshuffledOrder = false
    private var stopIndex = -1

    private var widgetUpdateJob: Job? = null

    val isInTransientFocusLoss: Boolean
        get() = player.playbackSuppressionReason == Player.PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS

    val isPlaying: Boolean
        get() = player.isPlaying

    private val shuffleCommand: CommandButton
        get() = if (player.shuffleModeEnabled) {
            customCommands[1]
        } else {
            customCommands[0]
        }

    private val repeatCommand: CommandButton
        get() = when (player.repeatMode) {
            Player.REPEAT_MODE_ALL -> customCommands[3]
            Player.REPEAT_MODE_ONE -> customCommands[4]
            else -> customCommands[2]
        }

    private val pauseOnZeroVolume: Boolean
        get() = preferences.getBoolean(PAUSE_ON_ZERO_VOLUME, false)
    private val sequentialTimeline: Boolean
        get() = preferences.getString(QUEUE_NEXT_MODE, "1") == "1"
    private val handleAudioFocus: Boolean
        get() = preferences.getBoolean(IGNORE_AUDIO_FOCUS, false).not()
    private val maxSeekToPreviousMs: Long
        get() = if (preferences.getBoolean(REWIND_WITH_BACK, true)) REWIND_INSTEAD_PREVIOUS_MILLIS else 0
    private val seekInterval: Long
        get() = preferences.getInt(SEEK_INTERVAL, 10) * 1000L

    override fun onCreate() {
        super.onCreate()
        nm = requireNotNull(getSystemService<NotificationManager>())
        createNotificationChannel()

        customCommands = listOf(
            CommandButton.Builder(CommandButton.ICON_SHUFFLE_OFF)
                .setDisplayName(getString(R.string.shuffle_mode))
                .setPlayerCommand(Player.COMMAND_SET_SHUFFLE_MODE, true)
                .build(),
            CommandButton.Builder(CommandButton.ICON_SHUFFLE_ON)
                .setDisplayName(getString(R.string.shuffle_mode))
                .setPlayerCommand(Player.COMMAND_SET_SHUFFLE_MODE, false)
                .build(),
            CommandButton.Builder(CommandButton.ICON_REPEAT_OFF)
                .setDisplayName(getString(R.string.repeat_mode))
                .setPlayerCommand(Player.COMMAND_SET_REPEAT_MODE, Player.REPEAT_MODE_ALL)
                .build(),
            CommandButton.Builder(CommandButton.ICON_REPEAT_ALL)
                .setDisplayName(getString(R.string.repeat_mode))
                .setPlayerCommand(Player.COMMAND_SET_REPEAT_MODE, Player.REPEAT_MODE_ONE)
                .build(),
            CommandButton.Builder(CommandButton.ICON_REPEAT_ONE)
                .setDisplayName(getString(R.string.repeat_mode))
                .setPlayerCommand(Player.COMMAND_SET_REPEAT_MODE, Player.REPEAT_MODE_OFF)
                .build()
        )

        playerThread.start()
        player = AdvancedForwardingPlayer(
            ExoPlayer.Builder(this)
                .setWakeMode(C.WAKE_MODE_LOCAL)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .setUsage(C.USAGE_MEDIA)
                        .build(), handleAudioFocus
                )
                .setRenderersFactory(
                    object : DefaultRenderersFactory(this) {
                        override fun buildAudioSink(
                            context: Context,
                            enableFloatOutput: Boolean,
                            enableAudioOutputPlaybackParams: Boolean
                        ): AudioSink {
                            return DefaultAudioSink.Builder(this@PlaybackService)
                                .setAudioProcessors(arrayOf(balanceProcessor, replayGainProcessor))
                                .setEnableFloatOutput(enableFloatOutput)
                                .setEnableAudioOutputPlaybackParameters(enableAudioOutputPlaybackParams)
                                .build()
                        }
                    }
                    .setEnableAudioFloatOutput(equalizerManager.audioFloatOutput.value)
                )
                .setMediaSourceFactory(
                    DefaultMediaSourceFactory(
                        this, DefaultExtractorsFactory()
                            .setConstantBitrateSeekingEnabled(true)
                            .also {
                                if (preferences.getBoolean(MP3_INDEX_SEEKING, false)) {
                                    it.setMp3ExtractorFlags(Mp3Extractor.FLAG_ENABLE_INDEX_SEEKING)
                                }
                            }
                    )
                )
                .setSkipSilenceEnabled(equalizerManager.skipSilence.value)
                .setHandleAudioBecomingNoisy(true)
                .setMaxSeekToPreviousPositionMs(maxSeekToPreviousMs)
                .setSeekBackIncrementMs(seekInterval)
                .setSeekForwardIncrementMs(seekInterval)
                .setPlaybackLooper(playerThread.looper)
                .build()
        )

        player.exoPlayer.shuffleOrder = ImprovedShuffleOrder(0, 0, Random.nextLong())
        player.setSequentialTimelineEnabled(sequentialTimeline)
        player.addListener(this)

        mediaSession = with(MediaLibrarySession.Builder(this, player, this)) {
            setId(packageName)
            setSessionActivity(createSessionActivityIntent())
            setBitmapLoader(
                CacheBitmapLoader(CoilBitmapLoader(serviceScope, this@PlaybackService, preferences))
            )
            build()
        }

        setMediaNotificationProvider(
            DefaultMediaNotificationProvider(
                this,
                { _ -> NOTIFICATION_ID },
                CHANNEL_ID,
                R.string.playing_notification_description
            ).apply {
                setSmallIcon(R.drawable.ic_stat_music_playback)
            }
        )

        mediaStoreObserver.init(this)

        persistentStorage = PersistentStorage(this, serviceScope, player)
        persistentStorage.restoreState { items, shuffleOrder ->
            player.setMediaItems(items.mediaItems, items.startIndex, items.startPositionMs)
            player.prepare()
            if (player.shuffleModeEnabled && shuffleOrder != null) {
                player.exoPlayer.shuffleOrder = shuffleOrder
            }
        }

        sleepTimer.addFinishListener { allowPendingQuit ->
            if (player.playWhenReady && player.isPlaying) {
                if (allowPendingQuit) {
                    player.exoPlayer.pauseAtEndOfMediaItems = true
                } else {
                    player.pause()
                }
            }
        }

        preferences.registerOnSharedPreferenceChangeListener(this)
        audioOutputObserver.startObserver()

        prepareEqualizerAndSoundSettings()
        registerReceivers()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if ((!isPlaybackOngoing && !isInTransientFocusLoss) ||
            preferences.getBoolean(STOP_WHEN_CLOSED_FROM_RECENTS, false)) {
            pauseAllPlayersAndStopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (bluetoothConnectedRegistered) {
            unregisterReceiver(bluetoothReceiver)
            bluetoothConnectedRegistered = false
        }
        if (headsetReceiverRegistered) {
            unregisterReceiver(headsetReceiver)
            headsetReceiverRegistered = false
        }
        serviceScope.cancel()
        preferences.unregisterOnSharedPreferenceChangeListener(this)
        audioOutputObserver.stopObserver()
        mediaStoreObserver.stop(this)
        mediaSession?.release()
        player.removeListener(this)
        player.release()
        playerThread.quitSafely()
        equalizerManager.release()
        sleepTimer.release()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE_FAVORITE -> {
                toggleFavorite()
                return START_STICKY
            }
            ACTION_TOGGLE_SHUFFLE -> {
                toggleShuffle()
                return START_STICKY
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaSession
    }

    override fun onConnect(
        session: MediaSession,
        controller: MediaSession.ControllerInfo
    ): MediaSession.ConnectionResult {
        val connectionResult = super.onConnect(session, controller)
        val availableCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS
            .buildUpon()

        availableCommands.add(SessionCommand(Playback.CYCLE_REPEAT, Bundle.EMPTY))
        availableCommands.add(SessionCommand(Playback.TOGGLE_SHUFFLE, Bundle.EMPTY))
        availableCommands.add(SessionCommand(Playback.TOGGLE_FAVORITE, Bundle.EMPTY))
        availableCommands.add(SessionCommand(Playback.RESTORE_PLAYBACK, Bundle.EMPTY))
        availableCommands.add(SessionCommand(Playback.SET_UNSHUFFLED_ORDER, Bundle.EMPTY))
        availableCommands.add(SessionCommand(Playback.SET_STOP_POSITION, Bundle.EMPTY))

        return MediaSession.ConnectionResult.accept(
            availableCommands.build(),
            connectionResult.availablePlayerCommands
        )
    }

    override fun onAudioSessionIdChanged(audioSessionId: Int) {
        equalizerManager.setSessionId(audioSessionId)
    }

    override fun onGetLibraryRoot(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        params: LibraryParams?
    ): ListenableFuture<LibraryResult<MediaItem>> {
        val libraryParams = LibraryParams.Builder()
            .setOffline(true)
            .setRecent(true)
            .setSuggested(false)
            .build()
        val mediaItem = when {
            params?.isRecent == true -> {
                MediaItem.Builder()
                    .setMediaId(MediaIDs.RECENT_SONGS)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                            .setIsBrowsable(true)
                            .setIsPlayable(false)
                            .build()
                    )
                    .build()
            }
            else -> {
                MediaItem.Builder()
                    .setMediaId(MediaIDs.ROOT)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                            .setIsBrowsable(true)
                            .setIsPlayable(false)
                            .build()
                    )
                    .build()
            }
        }
        return Futures.immediateFuture(LibraryResult.ofItem(mediaItem, libraryParams))
    }

    override fun onGetChildren(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        parentId: String,
        page: Int,
        pageSize: Int,
        params: LibraryParams?
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        return serviceScope.future(IO) {
            val result = runCatching {
                libraryProvider.getChildren(this@PlaybackService, parentId)
            }
            if (result.isSuccess) {
                LibraryResult.ofItemList(result.getOrThrow(), params)
            } else {
                LibraryResult.ofError(SessionError.ERROR_UNKNOWN)
            }
        }
    }

    override fun onGetItem(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        mediaId: String
    ): ListenableFuture<LibraryResult<MediaItem>> {
        return serviceScope.future(IO) {
            val mediaItem = runCatching { libraryProvider.getItem(mediaId) }
                .getOrDefault(MediaItem.EMPTY)
            if (mediaItem != MediaItem.EMPTY) {
                LibraryResult.ofItem(mediaItem, null)
            } else {
                LibraryResult.ofError(SessionError.ERROR_IO)
            }
        }
    }

    override fun onSearch(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        query: String,
        params: LibraryParams?
    ): ListenableFuture<LibraryResult<Void>> {
        return serviceScope.future(IO) {
            runCatching { libraryProvider.search(query) }
                .onSuccess { session.notifySearchResultChanged(browser, query, it.size, params) }

            LibraryResult.ofVoid()
        }
    }

    override fun onGetSearchResult(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        query: String,
        page: Int,
        pageSize: Int,
        params: LibraryParams?
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        return Futures.immediateFuture(
            LibraryResult.ofItemList(libraryProvider.searchResult, params)
        )
    }

    override fun onAddMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: List<MediaItem>
    ): ListenableFuture<List<MediaItem>> {
        return serviceScope.future(IO) {
            runCatching { libraryProvider.getMediaItemsForPlayback(mediaItems) }
                .getOrDefault(emptyList())
        }
    }

    override fun onSetMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: List<MediaItem>,
        startIndex: Int,
        startPositionMs: Long
    ): ListenableFuture<MediaItemsWithStartPosition> {
        player.exoPlayer.let { exoPlayer ->
            if (exoPlayer.shuffleOrder !is ImprovedShuffleOrder && !hasSetUnshuffledOrder) {
                exoPlayer.shuffleOrder = ImprovedShuffleOrder(
                    firstIndex = player.currentMediaItemIndex,
                    length = player.mediaItemCount,
                    randomSeed = Random.nextLong()
                )
            }

            (exoPlayer.shuffleOrder as? ImprovedShuffleOrder)
                ?.playerIndex = startIndex

            hasSetUnshuffledOrder = false
        }
        return serviceScope.future(IO) {
            if (mediaSession.isAutomotiveController(controller) ||
                mediaSession.isAutoCompanionController(controller)) {
                runCatching { libraryProvider.getMediaItemsForAAOSPlayback(mediaItems) }
                    .getOrNull()
                    .let {
                        MediaItemsWithStartPosition(
                            it?.first ?: emptyList(),
                            it?.second ?: C.INDEX_UNSET,
                            startPositionMs
                        )
                    }
            } else {
                runCatching { libraryProvider.getMediaItemsForPlayback(mediaItems) }
                    .getOrDefault(emptyList())
                    .let { MediaItemsWithStartPosition(it, startIndex, startPositionMs) }
            }
        }.also { future ->
            future.addListener({
                val result = runCatching { future.get() }.getOrNull()
                if (result != null && result.mediaItems.isNotEmpty()) {
                    this.mediaSession?.broadcastCustomCommand(
                        SessionCommand(Playback.EVENT_PLAYBACK_STARTED, Bundle.EMPTY),
                        Bundle.EMPTY
                    )
                }
            }, ContextCompat.getMainExecutor(this))
        }
    }

    override fun onCustomCommand(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        customCommand: SessionCommand,
        args: Bundle
    ): ListenableFuture<SessionResult> {
        return when (customCommand.customAction) {
            Playback.TOGGLE_SHUFFLE -> {
                toggleShuffle()
                Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            Playback.CYCLE_REPEAT -> {
                cycleRepeat()
                Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            Playback.TOGGLE_FAVORITE -> {
                toggleFavorite()
                Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            Playback.RESTORE_PLAYBACK -> {
                val playOnStartupMode = preferences.requireString(PLAY_ON_STARTUP_MODE, PlayOnStartupMode.NEVER)
                if (playOnStartupMode != PlayOnStartupMode.NEVER) {
                    CallbackToFutureAdapter.getFuture { completer ->
                        persistentStorage.waitForRestoration {
                            if (!player.currentTimeline.isEmpty) {
                                mediaSession?.broadcastCustomCommand(
                                    SessionCommand(
                                        Playback.EVENT_PLAYBACK_RESTORED,
                                        Bundle.EMPTY
                                    ),
                                    Bundle.EMPTY
                                )
                                completer.set(SessionResult(SessionResult.RESULT_SUCCESS))
                            } else {
                                completer.setException(IllegalStateException("Timeline is empty"))
                            }
                        }
                    }
                } else {
                    Futures.immediateFuture(SessionResult(SessionError.ERROR_INVALID_STATE))
                }
            }

            Playback.SET_UNSHUFFLED_ORDER -> {
                hasSetUnshuffledOrder = true
                player.exoPlayer.shuffleOrder = UnshuffledShuffleOrder(player.mediaItemCount)
                Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            Playback.SET_STOP_POSITION -> {
                val newStopIndex = customCommand.customExtras.getInt("index", -1)
                val canceled = newStopIndex > -1 && newStopIndex == stopIndex
                if (canceled) {
                    player.exoPlayer.pauseAtEndOfMediaItems = false
                    stopIndex = -1
                } else if (newStopIndex == player.currentMediaItemIndex) {
                    player.exoPlayer.pauseAtEndOfMediaItems = true
                    stopIndex = -1
                } else {
                    player.exoPlayer.pauseAtEndOfMediaItems = false
                    stopIndex = newStopIndex
                }
                Futures.immediateFuture(
                    SessionResult(SessionResult.RESULT_SUCCESS, bundleOf("canceled" to canceled))
                )
            }

            else -> Futures.immediateFuture(SessionResult(SessionError.ERROR_NOT_SUPPORTED))
        }
    }

    override fun onPlaybackResumption(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        isForPlayback: Boolean
    ): ListenableFuture<MediaItemsWithStartPosition> {
        if (persistentStorage.restorationState.isRestored) {
            return Futures.immediateFailedFuture(IllegalStateException("No MediaItems saved"))
        } else {
            val settableFuture = SettableFuture.create<MediaItemsWithStartPosition>()
            persistentStorage.waitForMediaItems { items, shuffleOrder ->
                if (items.mediaItems.isNotEmpty()) {
                    if (player.shuffleModeEnabled && shuffleOrder != null) {
                        player.exoPlayer.shuffleOrder = shuffleOrder
                    }
                    settableFuture.set(items)
                } else {
                    settableFuture.setException(IllegalStateException("No MediaItems saved"))
                }
            }
            return settableFuture
        }
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        if (player.playbackState == Player.STATE_ENDED &&
            preferences.getBoolean(CLEAR_QUEUE_ON_COMPLETION, false)) {
            player.exoPlayer.clearMediaItems()
        }
        refreshMediaButtonCustomLayout()
    }

    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
        persistentStorage.saveState(true)
    }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        if (reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM) {
            player.exoPlayer.pauseAtEndOfMediaItems = false
            sleepTimer.consumePendingQuit()
            if (stopIndex == player.currentMediaItemIndex) {
                stopIndex = -1
            }
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (!isPlaying) {
            val currentDurationMs = player.mediaMetadata.durationMs ?: 0
            if (currentDurationMs > 0) {
                if (!player.currentTimeline.isEmpty) {
                    persistentStorage.saveState()
                }
            }
        }
        equalizerManager.setSessionIsActive(isPlaying)
        songPlayCountHelper.notifyPlayStateChanged(isPlaying)
        updateWidgets()
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        updateWidgets()
        refreshMediaButtonCustomLayout()
        persistentStorage.saveState()
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        refreshMediaButtonCustomLayout()
        persistentStorage.saveState()
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        val isPlaying = player.isPlaying

        serviceScope.launch(IO) {
            val currentSong = repository.songByMediaItem(mediaItem)
            if (currentSong != Song.emptySong && preferences.getBoolean(ENABLE_HISTORY, true)) {
                repository.upsertSongInHistory(currentSong)
                replayGainProcessor.currentGain = ReplayGainTagExtractor.getReplayGain(currentSong)
            }
            val previousSong = songPlayCountHelper.song
            if (previousSong != Song.emptySong) {
                if (songPlayCountHelper.shouldBumpPlayCount()) {
                    repository.insertOrIncrementPlayCount(
                        song = previousSong,
                        timePlayed = System.currentTimeMillis()
                    )
                } else if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK) {
                    repository.insertOrIncrementSkipCount(previousSong)
                }
            }
            songPlayCountHelper.notifySongChanged(currentSong, isPlaying)
        }

        if (player.currentMediaItemIndex == stopIndex) {
            player.exoPlayer.pauseAtEndOfMediaItems = true
        }

        persistentStorage.saveState()
        updateWidgets(force = true)
    }

    override fun onPlayerError(error: PlaybackException) {
        showToast(getString(R.string.playback_error_code, error.errorCodeName))
    }

    override fun onEvents(player: Player, events: Player.Events) {
        if (events.contains(Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED)
            && !events.contains(Player.EVENT_TIMELINE_CHANGED)) {
            if (player.shuffleModeEnabled && persistentStorage.restorationState.isRestored) {
                this.player.exoPlayer.shuffleOrder = ImprovedShuffleOrder(
                    firstIndex = player.currentMediaItemIndex,
                    length = player.mediaItemCount,
                    randomSeed = Random.nextLong()
                )
            }
        }
    }

    override fun onSharedPreferenceChanged(preferences: SharedPreferences, key: String?) {
        when (key) {
            QUEUE_NEXT_MODE -> {
                player.setSequentialTimelineEnabled(sequentialTimeline)
            }

            ENABLE_HISTORY -> {
                if (!preferences.getBoolean(key, true)) {
                    serviceScope.launch(IO) {
                        repository.clearSongHistory()
                    }
                }
            }

            IGNORE_AUDIO_FOCUS -> {
                player.setAudioAttributes(player.audioAttributes, handleAudioFocus)
            }

            REWIND_WITH_BACK -> {
                player.exoPlayer.setMaxSeekToPreviousPositionMs(maxSeekToPreviousMs)
            }

            SEEK_INTERVAL -> {
                player.exoPlayer.setSeekBackIncrementMs(seekInterval)
                player.exoPlayer.setSeekForwardIncrementMs(seekInterval)
            }
        }
    }

    private fun toggleShuffle() {
        player.shuffleModeEnabled = !player.shuffleModeEnabled
    }

    private fun cycleRepeat() {
        val currentRepeatMode = player.repeatMode
        player.repeatMode = when (currentRepeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    private fun toggleFavorite() = serviceScope.launch {
        val currentMediaItem = player.currentMediaItem
            ?: return@launch

        withContext(IO) {
            val song = repository.songByMediaItem(currentMediaItem)
            repository.toggleFavorite(song)
        }

        updateWidgets()

        refreshMediaButtonCustomLayout()
        mediaSession?.broadcastCustomCommand(
            SessionCommand(Playback.EVENT_FAVORITE_CONTENT_CHANGED, Bundle.EMPTY),
            Bundle.EMPTY
        )
    }

    private suspend fun buildPlaybackState(isForeground: Boolean): PlaybackState {
        val mediaItem = player.currentMediaItem
        val id = mediaItem?.mediaId?.toLongOrNull()
        if (mediaItem == null || id == null) return PlaybackState.empty

        val isPlaying = player.isPlaying
        val isShuffleMode = player.shuffleModeEnabled
        return withContext(IO) {
            val song = repository.songById(id)
            val isFavorite = repository.isSongFavorite(song.id)
            val result = SingletonImageLoader.get(this@PlaybackService).execute(
                ImageRequest.Builder(this@PlaybackService)
                    .data(song)
                    .scale(Scale.FILL)
                    .size(300)
                    .build()
            )
            val artworkData = result.image?.toBitmap(300, 300)?.let { bitmap ->
                val stream = ByteArrayOutputStream()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 80, stream)
                } else {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                }
                stream.toByteArray()
            }
            PlaybackState(
                isForeground = isForeground,
                isPlaying = isPlaying,
                isFavorite = isFavorite,
                isShuffleMode = isShuffleMode,
                currentTitle = song.title,
                currentArtist = song.artistName,
                currentAlbum = song.albumName,
                artworkData = artworkData
            )
        }
    }

    private fun updateWidgets(force: Boolean = false, isForeground: Boolean = isPlaybackOngoing) {
        widgetUpdateJob?.cancel()
        widgetUpdateJob = serviceScope.launch {
            if (!force) delay(WIDGET_UPDATE_DEBOUNCE)

            val state = buildPlaybackState(isForeground)
            updateGlanceWidgets(state)
        }
    }

    private suspend fun updateGlanceWidgets(playbackState: PlaybackState) = withContext(IO) {
        try {
            val glanceManager = GlanceAppWidgetManager(applicationContext)
            val glanceIds = glanceManager.getGlanceIds(BoomingGlanceWidget::class.java)
            if (glanceIds.isNotEmpty()) {
                glanceIds.forEach { id ->
                    updateAppWidgetState(applicationContext, PlaybackStateDefinition, id) {
                        playbackState
                    }
                    BoomingGlanceWidget().update(applicationContext, id)
                }
            }
        } catch (e: Exception) {
            Log.e("PlaybackService", "Couldn't update Glance widgets", e)
        }
    }

    private fun createSessionActivityIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun createNotificationChannel() {
        var notificationChannel = nm.getNotificationChannel(CHANNEL_ID)
        if (notificationChannel == null) {
            notificationChannel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.playing_notification_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.playing_notification_description)
            }
            nm.createNotificationChannel(notificationChannel)
        }
    }

    private fun refreshMediaButtonCustomLayout() {
        val hasTimeline = !player.currentTimeline.isEmpty
        mediaSession?.connectedControllers?.forEach { controllerInfo ->
            if (mediaSession?.isRemoteController(controllerInfo) == true) {
                val buttonLayout = if (hasTimeline) {
                    ImmutableList.of(repeatCommand, shuffleCommand)
                } else {
                    emptyList()
                }
                mediaSession?.setMediaButtonPreferences(controllerInfo, buttonLayout)
            }
        }
    }

    private fun prepareEqualizerAndSoundSettings() {
        serviceScope.launch {
            equalizerManager.initializeEqualizer()
        }
        serviceScope.launch {
            equalizerManager.volumeState.collect { volume ->
                player.volume = volume.currentVolume
                if (pauseOnZeroVolume && persistentStorage.restorationState.isRestored) {
                    // don't handle volume changes until our player is fully restored
                    if (isPlaying && volume.currentVolume <= 0f) {
                        player.pause()
                        pausedByZeroVolume = true
                    } else if (pausedByZeroVolume && volume.currentVolume >= 0.1f) {
                        player.play()
                        pausedByZeroVolume = false
                    }
                }
            }
        }
        serviceScope.launch {
            equalizerManager.audioOffload.collect { audioOffloadingEnabled ->
                player.trackSelectionParameters = player.trackSelectionParameters
                    .buildUpon()
                    .setAudioOffloadPreferences(
                        AudioOffloadPreferences.Builder()
                            .setAudioOffloadMode(
                                if (audioOffloadingEnabled)
                                    AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_ENABLED
                                else AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_DISABLED
                            )
                            .setIsSpeedChangeSupportRequired(true)
                            .build()
                    )
                    .build()
            }
        }
        serviceScope.launch {
            equalizerManager.skipSilence.collect {
                player.exoPlayer.skipSilenceEnabled = it
            }
        }
        serviceScope.launch {
            equalizerManager.tempoState.collect {
                player.playbackParameters = PlaybackParameters(it.speed, it.actualPitch)
            }
        }
        serviceScope.launch {
            audioOutputObserver.audioDevice.collect {
                equalizerManager.setCurrentDevice(it)
            }
        }
    }

    private fun registerReceivers() {
        if (!bluetoothConnectedRegistered) {
            ContextCompat.registerReceiver(this, bluetoothReceiver, bluetoothConnectedIntentFilter,
                ContextCompat.RECEIVER_EXPORTED)
            bluetoothConnectedRegistered = true
        }

        if (!headsetReceiverRegistered) {
            ContextCompat.registerReceiver(this, headsetReceiver, headsetReceiverIntentFilter,
                ContextCompat.RECEIVER_EXPORTED)
            headsetReceiverRegistered = true
        }
    }

    private var bluetoothConnectedRegistered = false
    private val bluetoothConnectedIntentFilter = IntentFilter().apply {
        addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
        addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
        addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
    }
    private val bluetoothReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            when (intent?.action) {
                BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED -> {
                    when (intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1)) {
                        BluetoothA2dp.STATE_CONNECTED -> if (Preferences.isResumeOnConnect(true)) {
                            player.play()
                        }
                        BluetoothA2dp.STATE_DISCONNECTED -> if (Preferences.isPauseOnDisconnect(true)) {
                            player.pause()
                        }
                    }
                }
                BluetoothDevice.ACTION_ACL_CONNECTED ->
                    if (context.isBluetoothA2dpConnected() && Preferences.isResumeOnConnect(true)) {
                        player.play()
                    }
                BluetoothDevice.ACTION_ACL_DISCONNECTED ->
                    if (context.isBluetoothA2dpDisconnected() && Preferences.isPauseOnDisconnect(true)) {
                        player.pause()
                    }
            }
        }
    }

    private var receivedHeadsetConnected = false
    private var headsetReceiverRegistered = false
    private val headsetReceiverIntentFilter = IntentFilter(Intent.ACTION_HEADSET_PLUG)
    private val headsetReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (Intent.ACTION_HEADSET_PLUG == intent.action && !isInitialStickyBroadcast) {
                when (intent.getIntExtra("state", -1)) {
                    0 -> if (Preferences.isPauseOnDisconnect(false)) {
                        player.pause()
                    }
                    // Check whether the current song is empty which means the playing queue hasn't restored yet
                    1 -> if (Preferences.isResumeOnConnect(false)) {
                        if (player.currentMediaItem != null) {
                            player.play()
                        } else {
                            receivedHeadsetConnected = true
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val PACKAGE_NAME = "com.mardous.booming"

        const val ACTION_TOGGLE_SHUFFLE = "$PACKAGE_NAME.action.ACTION_TOGGLE_SHUFFLE"
        const val ACTION_TOGGLE_FAVORITE = "$PACKAGE_NAME.action.ACTION_TOGGLE_FAVORITE"

        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "playing_notification"

        private const val WIDGET_UPDATE_DEBOUNCE = 300L
        private const val REWIND_INSTEAD_PREVIOUS_MILLIS = 5000L
    }
}