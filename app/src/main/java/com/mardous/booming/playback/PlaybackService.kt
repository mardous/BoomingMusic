package com.mardous.booming.playback

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.*
import android.os.*
import androidx.annotation.OptIn
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.core.os.bundleOf
import androidx.core.os.postDelayed
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.media3.common.*
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.source.ShuffleOrder.UnshuffledShuffleOrder
import androidx.media3.session.*
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.mardous.booming.R
import com.mardous.booming.coil.CoilBitmapLoader
import com.mardous.booming.core.appwidgets.AppWidgetBig
import com.mardous.booming.core.appwidgets.AppWidgetSimple
import com.mardous.booming.core.appwidgets.AppWidgetSmall
import com.mardous.booming.core.audio.SoundSettings
import com.mardous.booming.data.local.MediaStoreObserver
import com.mardous.booming.data.local.ReplayGainMode
import com.mardous.booming.data.local.ReplayGainTagExtractor
import com.mardous.booming.data.local.repository.Repository
import com.mardous.booming.data.model.ContentType
import com.mardous.booming.data.model.Song
import com.mardous.booming.extensions.isBluetoothA2dpConnected
import com.mardous.booming.extensions.isBluetoothA2dpDisconnected
import com.mardous.booming.extensions.showToast
import com.mardous.booming.playback.equalizer.EqualizerManager
import com.mardous.booming.playback.equalizer.EqualizerSession
import com.mardous.booming.playback.library.LibraryProvider
import com.mardous.booming.playback.library.MediaIDs
import com.mardous.booming.playback.processor.BalanceAudioProcessor
import com.mardous.booming.playback.processor.ReplayGainAudioProcessor
import com.mardous.booming.ui.screen.MainActivity
import com.mardous.booming.util.*
import com.mardous.booming.util.Preferences.requireString
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.guava.future
import org.koin.android.ext.android.inject
import kotlin.random.Random

@OptIn(UnstableApi::class)
class PlaybackService :
    MediaLibraryService(),
    MediaLibrarySession.Callback,
    Player.Listener,
    SharedPreferences.OnSharedPreferenceChangeListener{

    private val serviceScope = CoroutineScope(Job() + Main)
    private val uiHandler = Handler(Looper.getMainLooper())

    private val appWidgetBig = AppWidgetBig.instance
    private val appWidgetSimple = AppWidgetSimple.instance
    private val appWidgetSmall = AppWidgetSmall.instance

    private val preferences: SharedPreferences by inject()
    private val sleepTimer: SleepTimer by inject()
    private val equalizerManager: EqualizerManager by inject()
    private val soundSettings: SoundSettings by inject()
    private val repository: Repository by inject()

    private val libraryProvider = LibraryProvider(repository)
    private val songPlayCountHelper = SongPlayCountHelper()
    private val mediaStoreObserver = MediaStoreObserver(uiHandler) {
        mediaSession?.broadcastCustomCommand(
            SessionCommand(Playback.EVENT_MEDIA_CONTENT_CHANGED, Bundle.EMPTY),
            Bundle.EMPTY
        )
    }

    private lateinit var persistentStorage: PersistentStorage

    private val pendingStartCommands = mutableListOf<Intent>()
    private var delayedShutdownHandler: Handler? = null
    private val playerThread = HandlerThread("Booming-ExoPlayer", Process.THREAD_PRIORITY_AUDIO)
    private lateinit var notificationProvider: DefaultMediaNotificationProvider
    private var mediaSession: MediaLibrarySession? = null
    private lateinit var player: AdvancedForwardingPlayer
    private lateinit var customCommands: List<CommandButton>

    private val balanceProcessor = BalanceAudioProcessor()
    private val replayGainProcessor = ReplayGainAudioProcessor(ReplayGainMode.Off)

    private var willSetUnshuffledOrder = false
    private var stopIndex = -1

    val isInTransientFocusLoss: Boolean
        get() = player.playbackSuppressionReason == Player.PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS

    val isPlaying: Boolean
        get() = player.isPlaying

    var currentSong = Song.emptySong
        private set(value) {
            field = value
            updateWidgets()
        }

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
        delayedShutdownHandler = Handler(Looper.getMainLooper())

        customCommands = listOf(
            CommandButton.Builder(CommandButton.ICON_SHUFFLE_OFF)
                .setDisplayName(getString(R.string.shuffle_mode))
                .setSessionCommand(SessionCommand(Playback.SHUFFLE_ON, Bundle.EMPTY))
                .build(),
            CommandButton.Builder(CommandButton.ICON_SHUFFLE_ON)
                .setDisplayName(getString(R.string.shuffle_mode))
                .setSessionCommand(SessionCommand(Playback.SHUFFLE_OFF, Bundle.EMPTY))
                .build(),
            CommandButton.Builder(CommandButton.ICON_REPEAT_OFF)
                .setDisplayName(getString(R.string.repeat_mode))
                .setSessionCommand(SessionCommand(Playback.REPEAT_ALL, Bundle.EMPTY))
                .build(),
            CommandButton.Builder(CommandButton.ICON_REPEAT_ALL)
                .setDisplayName(getString(R.string.repeat_mode))
                .setSessionCommand(SessionCommand(Playback.REPEAT_ONE, Bundle.EMPTY))
                .build(),
            CommandButton.Builder(CommandButton.ICON_REPEAT_ONE)
                .setDisplayName(getString(R.string.repeat_mode))
                .setSessionCommand(SessionCommand(Playback.REPEAT_OFF, Bundle.EMPTY))
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
                            enableAudioTrackPlaybackParams: Boolean
                        ): AudioSink? {
                            return DefaultAudioSink.Builder(this@PlaybackService)
                                .setAudioProcessors(arrayOf(replayGainProcessor, balanceProcessor))
                                .setEnableFloatOutput(enableFloatOutput)
                                .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                                .build()
                        }
                    }
                    .setEnableAudioFloatOutput(soundSettings.audioFloatOutputFlow.value)
                    .setEnableAudioTrackPlaybackParams(true)
                )
                .setSkipSilenceEnabled(soundSettings.skipSilenceFlow.value)
                .setHandleAudioBecomingNoisy(true)
                .setMaxSeekToPreviousPositionMs(maxSeekToPreviousMs)
                .setSeekBackIncrementMs(seekInterval)
                .setSeekForwardIncrementMs(seekInterval)
                .setPlaybackLooper(playerThread.looper)
                .build()
        )

        player.setSequentialTimelineEnabled(sequentialTimeline)
        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                // See: https://github.com/androidx/media/issues/244
                this@PlaybackService.onAudioSessionIdChanged(audioSessionId)
            }
        })
        player.addListener(this)

        notificationProvider = BoomingNotificationProvider(
            this,
            NOTIFICATION_ID,
            CHANNEL_ID,
            R.string.playing_notification_description
        )

        mediaSession = with(MediaLibrarySession.Builder(this, player, this)) {
            setId(packageName)
            setSessionActivity(createSessionActivityIntent())
            setBitmapLoader(CoilBitmapLoader(this@PlaybackService, preferences))
            setMediaNotificationProvider(notificationProvider)
            build()
        }

        mediaStoreObserver.init(this)

        persistentStorage = PersistentStorage(this, serviceScope, player)
        persistentStorage.restoreState { items, shuffleOrder ->
            player.setMediaItems(items.mediaItems, items.startIndex, items.startPositionMs)
            player.prepare()
            if (player.shuffleModeEnabled && shuffleOrder != null) {
                player.exoPlayer.shuffleOrder = shuffleOrder
            }
            if (!player.currentTimeline.isEmpty) {
                pendingStartCommands.forEach { command -> processCommand(command) }
                pendingStartCommands.clear()
            } else {
                stopForeground(STOP_FOREGROUND_REMOVE)
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

        prepareEqualizerAndSoundSettings()
        registerReceivers()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!isPlaybackOngoing || preferences.getBoolean(STOP_WHEN_CLOSED_FROM_RECENTS, false)) {
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
        LocalBroadcastManager.getInstance(this).unregisterReceiver(widgetIntentReceiver)
        serviceScope.cancel()
        preferences.unregisterOnSharedPreferenceChangeListener(this)
        mediaStoreObserver.stop(this)
        mediaSession?.release()
        player.removeListener(this)
        player.release()
        playerThread.quitSafely()
        equalizerManager.release()
        sleepTimer.release()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null && !isPlaying && isInTransientFocusLoss) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else if (intent != null) {
            // Cancel any pending shutdown
            delayedShutdownHandler?.removeCallbacksAndMessages(null)
            if (persistentStorage.restorationState.isRestored) {
                if (player.currentTimeline.isEmpty) {
                    startForegroundWithPendingMode(
                        R.string.playback_stopped_title,
                        R.string.empty_play_queue
                    )
                    postDelayedShutdown()
                } else {
                    processCommand(intent)
                }
            } else {
                startForegroundWithPendingMode(
                    R.string.preparing_playback_title,
                    R.string.restoring_playback_state
                )
                pendingStartCommands.add(intent)
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

        if (session.isRemoteController(controller)) {
            for (command in customCommands) {
                command.sessionCommand?.let {
                    availableCommands.add(it)
                }
            }
        }

        availableCommands.add(SessionCommand(Playback.CYCLE_REPEAT, Bundle.EMPTY))
        availableCommands.add(SessionCommand(Playback.TOGGLE_SHUFFLE, Bundle.EMPTY))
        availableCommands.add(SessionCommand(Playback.TOGGLE_FAVORITE, Bundle.EMPTY))
        availableCommands.add(SessionCommand(Playback.SET_UNSHUFFLED_ORDER, Bundle.EMPTY))
        availableCommands.add(SessionCommand(Playback.SET_STOP_POSITION, Bundle.EMPTY))

        return MediaSession.ConnectionResult.accept(
            availableCommands.build(),
            connectionResult.availablePlayerCommands
        )
    }

    override fun onAudioSessionIdChanged(audioSessionId: Int) {
        equalizerManager.audioSessionId = audioSessionId
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

    override fun onAddMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: List<MediaItem>
    ): ListenableFuture<List<MediaItem>> {
        if (mediaSession.isRemoteController(controller)) {
            return serviceScope.future(IO) {
                val result = runCatching {
                    libraryProvider.getMediaItemsForPlayback(mediaItems)
                }
                result.getOrDefault(emptyList())
            }
        }
        return super.onAddMediaItems(mediaSession, controller, mediaItems)
    }

    override fun onSetMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: List<MediaItem>,
        startIndex: Int,
        startPositionMs: Long
    ): ListenableFuture<MediaItemsWithStartPosition> {
        val mediaItemsWithStartPosition = MediaItemsWithStartPosition(mediaItems, startIndex, startPositionMs)
        val future = Futures.immediateFuture(mediaItemsWithStartPosition)
        future.addListener({
            if (mediaItems.isNotEmpty()) {
                this.mediaSession?.broadcastCustomCommand(
                    SessionCommand(Playback.EVENT_PLAYBACK_STARTED, Bundle.EMPTY),
                    Bundle.EMPTY
                )
                if (player.shuffleModeEnabled) {
                    player.exoPlayer.shuffleOrder = ImprovedShuffleOrder(
                        firstIndex = startIndex,
                        length = mediaItems.size,
                        randomSeed = Random.nextLong()
                    )
                }
            }
        }, ContextCompat.getMainExecutor(this))
        return future
    }

    override fun onCustomCommand(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        customCommand: SessionCommand,
        args: Bundle
    ): ListenableFuture<SessionResult> {
        return when (customCommand.customAction) {
            Playback.TOGGLE_SHUFFLE -> {
                player.shuffleModeEnabled = !player.shuffleModeEnabled
                Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            Playback.SHUFFLE_OFF -> {
                player.shuffleModeEnabled = false
                Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            Playback.SHUFFLE_ON -> {
                player.shuffleModeEnabled = true
                Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            Playback.CYCLE_REPEAT -> {
                val currentRepeatMode = player.repeatMode
                player.repeatMode = when (currentRepeatMode) {
                    Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                    Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                    else -> Player.REPEAT_MODE_OFF
                }
                Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            Playback.REPEAT_OFF -> {
                player.repeatMode = Player.REPEAT_MODE_OFF
                Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            Playback.REPEAT_ALL -> {
                player.repeatMode = Player.REPEAT_MODE_ALL
                Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }

            Playback.REPEAT_ONE -> {
                player.repeatMode = Player.REPEAT_MODE_ONE
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
                val length = customCommand.customExtras.getInt("length")
                if (length > 0) {
                    willSetUnshuffledOrder = true
                    player.shuffleModeEnabled = true
                    player.exoPlayer.shuffleOrder = UnshuffledShuffleOrder(length)
                    Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                } else {
                    Futures.immediateFuture(SessionResult(SessionError.ERROR_BAD_VALUE))
                }
            }

            Playback.SET_STOP_POSITION -> {
                val newStopIndex = customCommand.customExtras.getInt("index", -1)
                val canceled = newStopIndex > -1 && newStopIndex == stopIndex
                if (canceled) {
                    player.exoPlayer.pauseAtEndOfMediaItems = false
                    stopIndex = -1
                } else if (stopIndex == player.currentMediaItemIndex) {
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
        controller: MediaSession.ControllerInfo
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
        if (isPlaying) {
            if (equalizerManager.eqState.isEnabled) {
                //Shutdown any existing external audio sessions
                equalizerManager.closeAudioEffectSession(EqualizerSession.SESSION_EXTERNAL)
                //Start internal equalizer session (will only turn on if enabled)
                equalizerManager.openAudioEffectSession(EqualizerSession.SESSION_INTERNAL)
            } else {
                equalizerManager.openAudioEffectSession(EqualizerSession.SESSION_EXTERNAL)
            }
        } else {
            equalizerManager.closeAudioEffectSession(EqualizerSession.SESSION_EXTERNAL)
            val currentDurationMs = player.mediaMetadata.durationMs ?: 0
            if (currentDurationMs > 0) {
                if (!player.currentTimeline.isEmpty) {
                    persistentStorage.saveState()
                }
            }
        }
        songPlayCountHelper.notifyPlayStateChanged(isPlaying)
        updateWidgets()
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        if (persistentStorage.restorationState.isRestored) {
            // Assign a new ShuffleOrder only if the state is
            // fully restored to avoid inconsistencies.
            if (shuffleModeEnabled) {
                if (willSetUnshuffledOrder) {
                    willSetUnshuffledOrder = false
                } else {
                    player.exoPlayer.shuffleOrder = ImprovedShuffleOrder(
                        player.currentMediaItemIndex,
                        player.mediaItemCount,
                        Random.nextLong()
                    )
                }
            }
        }
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
            currentSong = repository.songByMediaItem(mediaItem)
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
    }

    override fun onPlayerError(error: PlaybackException) {
        showToast(getString(R.string.playback_error_code, error.errorCodeName))
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
                player.maxSeekToPreviousPosition = maxSeekToPreviousMs
            }

            SEEK_INTERVAL -> {
                player.seekBackIncrement = seekInterval
                player.seekForwardIncrement = seekInterval
            }
        }
    }

    private fun processCommand(command: Intent) {
        when (command.action) {
            ACTION_TOGGLE_PAUSE -> if (isPlaying) {
                player.pause()
            } else {
                player.play()
            }
            ACTION_PLAY_PLAYLIST -> playFromPlaylist(command)
            ACTION_PREVIOUS -> player.seekToPrevious()
            ACTION_NEXT -> player.seekToNext()
        }
    }

    private fun playFromPlaylist(intent: Intent) = serviceScope.launch(IO) {
        val contentType = IntentCompat.getSerializableExtra(intent, EXTRA_CONTENT_TYPE, ContentType::class.java)
        val songs = when (contentType) {
            ContentType.RecentSongs -> repository.recentSongs()
            ContentType.TopTracks -> repository.playCountSongs()
            else -> repository.allSongs()
        }
        withContext(Main) {
            if (songs.isNotEmpty()) {
                player.shuffleModeEnabled = intent.getBooleanExtra(EXTRA_SHUFFLE_MODE, false)
                player.setMediaItems(songs.toMediaItems())
                player.prepare()
                player.play()
            } else {
                showToast(R.string.playlist_empty_text)
            }
        }
    }

    private fun toggleFavorite() = serviceScope.launch {
        val currentMediaItem = player.currentMediaItem
        if (currentMediaItem == null) return@launch

        withContext(IO) {
            val song = repository.songByMediaItem(currentMediaItem)
            repository.toggleFavorite(song)
        }

        refreshMediaButtonCustomLayout()
        mediaSession?.broadcastCustomCommand(
            SessionCommand(Playback.EVENT_FAVORITE_CONTENT_CHANGED, Bundle.EMPTY),
            Bundle.EMPTY
        )
    }

    private fun updateWidgets() {
        uiHandler.post {
            appWidgetBig.notifyChange(this)
            appWidgetSimple.notifyChange(this)
            appWidgetSmall.notifyChange(this)
        }
    }

    private fun createSessionActivityIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun startForegroundWithPendingMode(titleRes: Int, messageRes: Int) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(titleRes))
            .setContentText(getString(messageRes))
            .setSmallIcon(R.drawable.ic_stat_music_playback)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(createSessionActivityIntent())
            .setShowWhen(false)
            .setSilent(true)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun postDelayedShutdown(delayInSeconds: Long = 15) {
        delayedShutdownHandler?.removeCallbacksAndMessages(null)
        delayedShutdownHandler?.postDelayed(delayInSeconds * 1000) {
            if (!isPlaybackOngoing) {
                pauseAllPlayersAndStopSelf()
            }
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
        serviceScope.launch(IO) {
            equalizerManager.initializeEqualizer()
        }
        serviceScope.launch {
            soundSettings.skipSilenceFlow.collect {
                player.exoPlayer.skipSilenceEnabled = it
            }
        }
        serviceScope.launch {
            soundSettings.replayGainStateFlow.collect {
                replayGainProcessor.mode = it.value.mode
                replayGainProcessor.preAmpGain = it.value.preamp
                replayGainProcessor.preAmpGainWithoutTag = it.value.preampWithoutGain
            }
        }
        serviceScope.launch {
            soundSettings.tempoFlow.collect {
                player.playbackParameters = player.playbackParameters
                    .withSpeed(it.value.speed)
                    .withPitch(it.value.actualPitch)
            }
        }
        serviceScope.launch {
            soundSettings.balanceFlow.collect {
                balanceProcessor.setBalance(it.value.left, it.value.right)
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
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(widgetIntentReceiver, IntentFilter(ACTION_APP_WIDGET_UPDATE))
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

    private val widgetIntentReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val command = intent.getStringExtra(EXTRA_APP_WIDGET_NAME) ?: return
            val ids = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
            when (command) {
                AppWidgetBig.NAME -> {
                    appWidgetBig.performUpdate(this@PlaybackService, ids)
                }
                AppWidgetSimple.NAME -> {
                    appWidgetSimple.performUpdate(this@PlaybackService, ids)
                }
                AppWidgetSmall.NAME -> {
                    appWidgetSmall.performUpdate(this@PlaybackService, ids)
                }
            }
        }
    }

    companion object {
        private const val PACKAGE_NAME = "com.mardous.booming"

        const val ACTION_APP_WIDGET_UPDATE = "$PACKAGE_NAME.action.app_widget_update"
        const val ACTION_TOGGLE_PAUSE = "$PACKAGE_NAME.action.toggle_pause"
        const val ACTION_PLAY_PLAYLIST = "$PACKAGE_NAME.action.play.playlist"
        const val ACTION_PREVIOUS = "$PACKAGE_NAME.booming.action.previous"
        const val ACTION_NEXT = "$PACKAGE_NAME.action.next"

        const val EXTRA_APP_WIDGET_NAME = "$PACKAGE_NAME.extra.app_widget_name"
        const val EXTRA_CONTENT_TYPE = "$PACKAGE_NAME.extra.content_type"
        const val EXTRA_SHUFFLE_MODE = "$PACKAGE_NAME.extra.shuffle_mode"

        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "playing_notification"

        private const val REWIND_INSTEAD_PREVIOUS_MILLIS = 5000L
    }
}