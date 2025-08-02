/*
 * Copyright (c) 2024 Christians Martínez Alvarado
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mardous.booming.service

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.*
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.net.Uri
import android.os.*
import android.os.PowerManager.WakeLock
import android.provider.MediaStore
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.session.PlaybackStateCompat.CustomAction
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.core.content.getSystemService
import androidx.core.os.postDelayed
import androidx.core.util.Predicate
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.media.AudioAttributesCompat
import androidx.media.AudioFocusRequestCompat
import androidx.media.AudioManagerCompat
import androidx.media.MediaBrowserServiceCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.mardous.booming.BuildConfig
import com.mardous.booming.R
import com.mardous.booming.androidauto.AutoMediaIDHelper
import com.mardous.booming.androidauto.AutoMusicProvider
import com.mardous.booming.androidauto.PackageValidator
import com.mardous.booming.appwidgets.AppWidgetBig
import com.mardous.booming.appwidgets.AppWidgetSimple
import com.mardous.booming.appwidgets.AppWidgetSmall
import com.mardous.booming.database.fromHistoryToSongs
import com.mardous.booming.database.toPlayCount
import com.mardous.booming.database.toSongs
import com.mardous.booming.extensions.*
import com.mardous.booming.extensions.glide.getSongGlideModel
import com.mardous.booming.extensions.glide.songOptions
import com.mardous.booming.extensions.media.displayArtistName
import com.mardous.booming.extensions.media.indexOfSong
import com.mardous.booming.extensions.media.isArtistNameUnknown
import com.mardous.booming.extensions.utilities.buildInfoString
import com.mardous.booming.glide.transformation.BlurTransformation
import com.mardous.booming.helper.UriSongResolver
import com.mardous.booming.model.ContentType
import com.mardous.booming.model.Song
import com.mardous.booming.providers.databases.HistoryStore
import com.mardous.booming.providers.databases.SongPlayCountStore
import com.mardous.booming.repository.Repository
import com.mardous.booming.service.constants.ServiceAction
import com.mardous.booming.service.constants.ServiceAction.Extras.Companion.EXTRA_CONTENT_TYPE
import com.mardous.booming.service.constants.ServiceAction.Extras.Companion.EXTRA_SHUFFLE_MODE
import com.mardous.booming.service.constants.SessionCommand
import com.mardous.booming.service.constants.SessionEvent
import com.mardous.booming.service.equalizer.EqualizerManager
import com.mardous.booming.service.notification.PlayingNotificationManager
import com.mardous.booming.service.playback.Playback
import com.mardous.booming.service.playback.Playback.PlaybackCallbacks
import com.mardous.booming.service.playback.PlaybackManager
import com.mardous.booming.service.queue.NO_POSITION
import com.mardous.booming.service.queue.QueueChangeReason
import com.mardous.booming.service.queue.QueueManager
import com.mardous.booming.service.queue.QueueObserver
import com.mardous.booming.taglib.ReplayGainTagExtractor
import com.mardous.booming.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import org.koin.android.ext.android.inject
import org.koin.java.KoinJavaComponent.get
import kotlin.math.log10
import kotlin.math.min
import kotlin.random.Random

class MusicService : MediaBrowserServiceCompat(), PlaybackCallbacks, QueueObserver,
    AudioManager.OnAudioFocusChangeListener, OnSharedPreferenceChangeListener {

    private val serviceScope = CoroutineScope(Job() + Main)

    private val repository by inject<Repository>()
    private val uriSongResolver by inject<UriSongResolver>()
    private val queueManager by inject<QueueManager>()
    private val playbackManager by inject<PlaybackManager>()

    private val appWidgetBig = AppWidgetBig.instance
    private val appWidgetSimple = AppWidgetSimple.instance
    private val appWidgetSmall = AppWidgetSmall.instance

    private var playbackRestored = false
    private var needsToRestorePlayback = false
    private var trackEndedByCrossfade = false
    private var mayResumeOnFocusGain = false

    private val sharedPreferences: SharedPreferences by inject()
    private val equalizerManager: EqualizerManager by inject()

    private var foregroundNotificationHandler: Handler? = null
    private var delayedShutdownHandler: Handler? = null
    private var musicPlayerHandlerThread: HandlerThread? = null
    private var playerHandler: Handler? = null
    private var uiThreadHandler: Handler? = null
    private var throttledSeekHandler: ThrottledSeekHandler? = null
    private var wakeLock: WakeLock? = null

    private lateinit var playingNotificationManager: PlayingNotificationManager
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var persistentStorage: PersistentStorage
    private lateinit var mediaStoreObserver: MediaStoreObserver

    // Android Auto
    private var musicProvider = get<AutoMusicProvider>(AutoMusicProvider::class.java)
    private var packageValidator: PackageValidator? = null

    private val pendingStartCommands = mutableListOf<Intent>()
    private val songPlayCountHelper = SongPlayCountHelper()

    private val audioManager by lazy { getSystemService<AudioManager>() }
    private val audioFocusRequest: AudioFocusRequestCompat =
        AudioFocusRequestCompat.Builder(AudioManagerCompat.AUDIOFOCUS_GAIN)
            .setOnAudioFocusChangeListener(this)
            .setAudioAttributes(
                AudioAttributesCompat.Builder()
                    .setUsage(AudioAttributesCompat.USAGE_MEDIA)
                    .setContentType(AudioAttributesCompat.CONTENT_TYPE_MUSIC)
                    .build()
            ).build()

    val isPlaying get() = playbackManager.isPlaying()
    val currentSongProgress get() = playbackManager.position()
    val currentSongDuration get() = playbackManager.duration()

    private val playbackSpeed get() = playbackManager.getPlaybackSpeed()
    private val shuffleMode get() = queueManager.shuffleMode
    private val repeatMode get() = queueManager.repeatMode
    private val isFirstTrack get() = queueManager.isFirstTrack
    private val isLastTrack get() = queueManager.isLastTrack
    private val playingQueue get() = queueManager.playingQueue
    private val currentPosition get() = queueManager.position

    val currentSong get() = queueManager.currentSong

    override fun onCreate() {
        super.onCreate()
        val powerManager = getSystemService<PowerManager>()
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, javaClass.name)
            wakeLock!!.setReferenceCounted(false)
        }

        // Set up PlaybackHandler.
        musicPlayerHandlerThread = HandlerThread("PlaybackHandler", Process.THREAD_PRIORITY_BACKGROUND)
        musicPlayerHandlerThread!!.start()
        playerHandler = Handler(musicPlayerHandlerThread!!.looper)

        foregroundNotificationHandler = Handler(Looper.getMainLooper())
        delayedShutdownHandler = Handler(Looper.getMainLooper())

        persistentStorage = PersistentStorage(this, serviceScope)

        queueManager.addObserver(this)
        playbackManager.initialize(this, serviceScope)
        setupMediaSession()

        // Create the UI-thread handler.
        uiThreadHandler = Handler(Looper.getMainLooper())
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(widgetIntentReceiver, IntentFilter(ServiceAction.ACTION_APP_WIDGET_UPDATE))

        sessionToken = mediaSession.sessionToken
        playingNotificationManager = PlayingNotificationManager(this, mediaSession, playbackManager, queueManager)

        mediaStoreObserver = MediaStoreObserver(this, playerHandler!!)
        throttledSeekHandler = ThrottledSeekHandler(this, persistentStorage, playerHandler!!)
        contentResolver.registerContentObserver(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, mediaStoreObserver
        )
        contentResolver.registerContentObserver(
            MediaStore.Audio.Media.INTERNAL_CONTENT_URI, true, mediaStoreObserver
        )

        restoreState()

        packageValidator = PackageValidator(this, R.xml.allowed_media_browser_callers)

        registerHeadsetEvents()
        registerBluetoothConnected()
        registerBecomingNoisyReceiver()

        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null && !playbackManager.isPlaying() && !mayResumeOnFocusGain) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else if (intent != null) {
            // Cancel any pending shutdown
            delayedShutdownHandler?.removeCallbacksAndMessages(null)
            if (persistentStorage.restorationState.isRestored) {
                if (queueManager.isEmpty) {
                    playingNotificationManager.startForeground(this) {
                        displayEmptyQueueNotification()
                    }
                    postDelayedShutdown(10)
                } else {
                    playingNotificationManager.startForeground(this) {
                        displayPlayingNotification()
                    }
                }
                processCommand(intent)
            } else {
                playingNotificationManager.startForeground(this) {
                    displayPendingNotification()
                }
                pendingStartCommands.add(intent)
            }
        }
        return START_NOT_STICKY
    }

    override fun onUnbind(intent: Intent?): Boolean {
        if (!isPlaying && !mayResumeOnFocusGain) {
            stopSelf()
        }
        return super.onUnbind(intent)
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? {
        // Check origin to ensure we're not allowing any arbitrary app to browse app contents
        if (!packageValidator!!.isKnownCaller(clientPackageName, clientUid)) {
            return null
        }

        // System UI query (Android 11+)
        val isSystemMediaQuery = Predicate { hints: Bundle? ->
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q || hints == null) {
                false
            } else hints.getBoolean(BrowserRoot.EXTRA_RECENT) ||
                    hints.getBoolean(BrowserRoot.EXTRA_SUGGESTED) ||
                    hints.getBoolean(BrowserRoot.EXTRA_OFFLINE)
        }
        return if (isSystemMediaQuery.test(rootHints)) {
            // By returning null, we explicitly disable support for content discovery/suggestions
            null
        } else {
            BrowserRoot(AutoMediaIDHelper.MEDIA_ID_ROOT, null)
        }
    }

    override fun onLoadChildren(parentId: String, result: Result<List<MediaBrowserCompat.MediaItem>>) {
        result.detach()
        serviceScope.launch(IO) {
            val children = runCatching { musicProvider.getChildren(parentId, resources) }
            result.sendResult(children.getOrElse { emptyList() })
        }
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        if (!isPlaying && !mayResumeOnFocusGain) {
            quit()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(widgetIntentReceiver)
        if (headsetReceiverRegistered) {
            unregisterReceiver(headsetReceiver)
            headsetReceiverRegistered = false
        }
        if (bluetoothConnectedRegistered) {
            unregisterReceiver(bluetoothReceiver)
            bluetoothConnectedRegistered = false
        }
        if (becomingNoisyReceiverRegistered) {
            unregisterReceiver(becomingNoisyReceiver)
            becomingNoisyReceiverRegistered = false
        }
        quit()
        serviceScope.cancel()
        abandonFocus()
        playerHandler?.removeCallbacksAndMessages(null)
        foregroundNotificationHandler?.removeCallbacksAndMessages(null)
        delayedShutdownHandler?.removeCallbacksAndMessages(null)
        musicPlayerHandlerThread?.quitSafely()
        playbackManager.release()
        mediaSession.isActive = false
        mediaSession.release()
        wakeLock?.release()
        queueManager.removeObserver(this)
        contentResolver.unregisterContentObserver(mediaStoreObserver)
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun queueChanged(queue: List<Song>, reason: QueueChangeReason) {
        mediaSession.setQueueTitle(getString(R.string.playing_queue_label))
        mediaSession.setQueue(queueManager.getMediaSessionQueue())
        updateMediaSessionMetadata(::updateMediaSessionPlaybackState) // because playing queue size might have changed
        persistentStorage.saveState()
        if (queue.isNotEmpty()) {
            prepareNext()
        } else {
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
    }

    override fun queuePositionChanged(position: Int, rePosition: Boolean) {
        if (rePosition) {
            prepareSongAt(position)
        } else if (position == NO_POSITION) {
            openCurrentAndPrepareNext()
        }
    }

    override fun repeatModeChanged(repeatMode: Playback.RepeatMode) {
        persistentStorage.saveRepeatMode(repeatMode)
        mediaSession.setRepeatMode(repeatMode.value)
        prepareNext()
    }

    override fun shuffleModeChanged(shuffleMode: Playback.ShuffleMode) {
        persistentStorage.saveShuffleMode(shuffleMode)
        mediaSession.setShuffleMode(shuffleMode.value)
    }

    override fun songChanged(currentSong: Song, nextSong: Song) {
        // We must call updateMediaSessionPlaybackState after the load of album art is completed
        // if we are loading it, or it won't be updated in the notification
        updateMediaSessionMetadata {
            updateMediaSessionPlaybackState()
            isCurrentFavorite { isFavorite ->
                playingNotificationManager.displayPlayingNotification(song = currentSong, isFavorite = isFavorite)
            }
        }
        updateWidgets()
        persistentStorage.savePosition()
        persistentStorage.savePositionInTrack()
        serviceScope.launch(IO) {
            HistoryStore.getInstance(this@MusicService).addSongId(currentSong.id)
            repository.upsertSongInHistory(currentSong)
            songPlayCountHelper.notifySongChanged(currentSong, isPlaying)
            applyReplayGain(currentSong)
        }
        playbackManager.setCrossFadeNextDataSource(nextSong)
    }

    override fun onTrackWentToNext() {
        bumpPlayCount()
        if (checkShouldStop(false) || (queueManager.repeatMode == Playback.RepeatMode.Off && isLastTrack)) {
            playbackManager.setNextDataSource(null)
            pause(true)
            seek(0, false)
            if (checkShouldStop(true)) {
                queueManager.setPositionToNext()
                quit()
            }
        } else {
            queueManager.setPositionToNext()
            prepareNext()
            playbackManager.updateBalance()
            playbackManager.updateTempo()
        }
    }

    override fun onTrackEnded() {
        acquireWakeLock()

        // bump play count before anything else
        bumpPlayCount()

        // Always advance queue and prepare next, regardless of how track ended
        queueManager.setPositionToNext()
        prepareNext()

        // if there is a timer finished, don't continue
        if ((queueManager.repeatMode == Playback.RepeatMode.Off && isLastTrack) || checkShouldStop(false)) {
            pause()
            seek(0, false)
            if (checkShouldStop(true)) {
                quit()
            }
        }

        releaseWakeLock()
    }

    override fun onTrackEndedWithCrossFade() {
        trackEndedByCrossfade = true
        onTrackEnded()
    }

    override fun onPlayStateChanged() {
        // We use the foreground notification handler here to slightly delay the call to stopForeground().
        // This appears to be necessary in order to allow our notification to become dismissable if pause() is called via onStartCommand() to this service.
        // Presumably, there is an issue in calling stopForeground() too soon after startForeground() which causes the notification to be stuck in the 'ongoing' state and not able to be dismissed.
        //
        // This behavior is highly inspired by S2 Music Player: https://github.com/timusus/Shuttle2

        foregroundNotificationHandler?.removeCallbacksAndMessages(null)
        delayedShutdownHandler?.removeCallbacksAndMessages(null)

        updateMediaSessionPlaybackState()
        updateWidgets()

        val isPlaying = playbackManager.isPlaying()
        if (isPlaying) {
            playingNotificationManager.startForeground(this) {
                displayPlayingNotification(true)
            }
        } else {
            playingNotificationManager.displayPlayingNotification(false)
            if (!mayResumeOnFocusGain) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    foregroundNotificationHandler?.postDelayed(150) {
                        stopForeground(STOP_FOREGROUND_DETACH)
                    }
                    postDelayedShutdown()
                }
            }
            if (currentSongDuration > 0) {
                persistentStorage.savePositionInTrack()
            }
        }

        songPlayCountHelper.notifyPlayStateChanged(isPlaying)
    }

    private fun processCommand(command: Intent) {
        when (command.action) {
            ServiceAction.ACTION_TOGGLE_PAUSE -> if (isPlaying) {
                pause()
            } else {
                play()
            }

            ServiceAction.ACTION_PAUSE -> pause()
            ServiceAction.ACTION_PLAY -> play()
            ServiceAction.ACTION_PLAY_PLAYLIST -> playFromPlaylist(command)
            ServiceAction.ACTION_PREVIOUS -> back(true)
            ServiceAction.ACTION_NEXT -> playNextSong(true)
            ServiceAction.ACTION_STOP,
            ServiceAction.ACTION_QUIT -> {
                playbackManager.pendingQuit = false
                queueManager.stopPosition = NO_POSITION
                quit()
            }

            ServiceAction.ACTION_PENDING_QUIT -> {
                playbackManager.pendingQuit = isPlaying
                if (!playbackManager.pendingQuit) {
                    queueManager.stopPosition = NO_POSITION
                    quit()
                }
            }
            ServiceAction.ACTION_TOGGLE_FAVORITE -> toggleFavorite()
        }
    }

    private fun playFromPlaylist(intent: Intent) {
        val contentType =
            IntentCompat.getSerializableExtra(intent, EXTRA_CONTENT_TYPE, ContentType::class.java)
        val shuffleMode =
            IntentCompat.getSerializableExtra(intent, EXTRA_SHUFFLE_MODE, Playback.ShuffleMode::class.java)

        serviceScope.launch(IO) {
            val songs = when (contentType) {
                ContentType.RecentSongs -> repository.recentSongs()
                ContentType.TopTracks -> repository.topPlayedSongs()
                else -> repository.allSongs()
            }
            if (songs.isNotEmpty()) {
                if (shuffleMode == Playback.ShuffleMode.On) {
                    val startPosition = Random.nextInt(songs.size)
                    openQueue(songs, startPosition, true, shuffleMode)
                } else {
                    openQueue(songs, 0, true)
                }
            } else {
                showToast(R.string.playlist_empty_text)
            }
        }
    }

    private fun prepareRestoredPlayback(restoredPositionInTrack: Int) {
        openCurrent { success ->
            prepareNext()
            if (restoredPositionInTrack > 0) {
                seek(restoredPositionInTrack)
            }
            if (needsToRestorePlayback || receivedHeadsetConnected) {
                play()
                if (success && needsToRestorePlayback) {
                    mediaSession.sendSessionEvent(SessionEvent.PLAYBACK_RESTORED, null)
                }
                receivedHeadsetConnected = false
                needsToRestorePlayback = false
            }
        }
    }

    private fun acquireWakeLock() {
        wakeLock?.acquire(30000)
    }

    private fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) {
            wakeLock!!.release()
        }
    }

    // Inspired by S2 Music Player: https://github.com/timusus/Shuttle2
    private fun postDelayedShutdown(delayInSeconds: Long = 15) {
        delayedShutdownHandler?.removeCallbacksAndMessages(null)
        delayedShutdownHandler?.postDelayed(delayInSeconds * 1000) {
            if (!isPlaying && !mayResumeOnFocusGain) {
                if (queueManager.isEmpty) {
                    playingNotificationManager.cancelNotification()
                }
                stopSelf()
            }
        }
    }

    private fun restoreState() = serviceScope.launch {
        equalizerManager.initializeEqualizer()
        persistentStorage.restoreState()
        persistentStorage.restoreQueue { restored, restoredPositionInTrack ->
            if (restored) {
                prepareRestoredPlayback(restoredPositionInTrack)
                pendingStartCommands.forEach { pendingCommand ->
                    processCommand(pendingCommand)
                }
                pendingStartCommands.clear()
            } else {
                stopForeground(STOP_FOREGROUND_REMOVE)
                playingNotificationManager.displayEmptyQueueNotification()
                postDelayedShutdown()
            }
        }
    }

    fun quit() {
        pause()
        stopForeground(STOP_FOREGROUND_REMOVE)

        //force to update play count if necessary
        bumpPlayCount()

        stopSelf()
    }

    private fun registerBluetoothConnected() {
        if (!bluetoothConnectedRegistered) {
            ContextCompat.registerReceiver(this, bluetoothReceiver, bluetoothConnectedIntentFilter,
                ContextCompat.RECEIVER_EXPORTED)
            bluetoothConnectedRegistered = true
        }
    }

    private fun registerHeadsetEvents() {
        if (!headsetReceiverRegistered) {
            ContextCompat.registerReceiver(this, headsetReceiver, headsetReceiverIntentFilter,
                ContextCompat.RECEIVER_EXPORTED)
            headsetReceiverRegistered = true
        }
    }

    private fun registerBecomingNoisyReceiver() {
        if (!becomingNoisyReceiverRegistered) {
            ContextCompat.registerReceiver(
                this,
                becomingNoisyReceiver,
                becomingNoisyReceiverIntentFilter,
                ContextCompat.RECEIVER_EXPORTED
            )
            becomingNoisyReceiverRegistered = true
        }
    }

    fun playNextSong(force: Boolean) {
        if (isLastTrack && queueManager.repeatMode == Playback.RepeatMode.Off) {
            showToast(R.string.list_end)
            return
        }
        // Try to bump play/skip count only if the user is skipping manually (force == true).
        if (force && !bumpPlayCount()) {
            val currentSong = songPlayCountHelper.song
            if (currentSong != Song.emptySong) {
                serviceScope.launch(IO) {
                    val playCountEntity = repository.findSongInPlayCount(currentSong.id)
                        ?.apply { skipCount += 1 } ?: currentSong.toPlayCount(skipCount = 1)

                    repository.upsertSongInPlayCount(playCountEntity)
                }
            }
        }
        playOrPrepareSongAt(getNextPosition(force), !force)
    }

    private fun openCurrentAndPrepareNext(completion: (success: Boolean) -> Unit = {}) {
        openCurrent { success ->
            completion(success)
            if (success) {
                prepareNext()
            }
        }
    }

    private fun openCurrent(completion: (success: Boolean) -> Unit) {
        val force = if (!trackEndedByCrossfade) {
            true
        } else {
            trackEndedByCrossfade = false
            false
        }
        playbackManager.setDataSource(currentSong, force) { success ->
            completion(success)
        }
    }

    private fun prepareNext() {
        try {
            val nextPosition = getNextPosition(false)
            if (nextPosition == queueManager.stopPosition) {
                playbackManager.setNextDataSource(null)
            } else {
                playbackManager.setNextDataSource(getSongAt(nextPosition))
            }
            queueManager.nextPosition = nextPosition
        } catch (_: Exception) {
        }
    }

    private fun toggleFavorite() = serviceScope.launch {
        val song = currentSong
        val isFavorite = withContext(IO) {
            repository.toggleFavorite(song)
        }
        playingNotificationManager.displayPlayingNotification(song = song, isFavorite = isFavorite)
        mediaSession.sendSessionEvent(SessionEvent.FAVORITE_CONTENT_CHANGED, null)
    }

    private fun isCurrentFavorite(completion: (isFavorite: Boolean) -> Unit) = serviceScope.launch {
        val isFavorite = withContext(IO) {
            repository.isSongFavorite(currentSong.id)
        }
        completion(isFavorite)
    }

    internal fun updateMediaSessionPlaybackState() {
        val stateBuilder = PlaybackStateCompat.Builder()
            .setActions(MEDIA_SESSION_ACTIONS)
            .setState(
                if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                currentSongProgress.toLong(),
                playbackSpeed
            )
        setCustomAction(stateBuilder)
        mediaSession.setPlaybackState(stateBuilder.build())
    }

    @SuppressLint("CheckResult")
    private fun updateMediaSessionMetadata(onCompletion: () -> Unit) {
        val song = currentSong
        if (song.id == -1L) {
            mediaSession.setMetadata(null)
            return
        }
        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, song.albumName)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, song.albumArtistName)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.displayArtistName())
            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, song.mediaStoreUri.toString())
            .putString(MediaMetadataCompat.METADATA_KEY_GENRE, song.genreName)
            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, song.id.toString())
            .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, song.trackNumber.toLong())
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, song.duration)
            .putLong(MediaMetadataCompat.METADATA_KEY_YEAR, song.year.toLong())
            .putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, playingQueue.size.toLong())

        // If we're in car mode, don't wait for the artwork to load before setting session metadata.
        if (resources.isCarMode) {
            mediaSession.setMetadata(metadata.build())
        }

        // We must send the album art in METADATA_KEY_ALBUM_ART key on A13+ or
        // else album art is blurry in notification
        if (Preferences.albumArtOnLockscreenAllowed || resources.isCarMode || hasT()) {
            val request = Glide.with(this)
                .asBitmap()
                .songOptions(song)
                .load(song.getSongGlideModel())

            if (Preferences.blurredAlbumArtAllowed) {
                request.transform(BlurTransformation.Builder(this).build())
            }
            runOnUiThread {
                request.into(object : CustomTarget<Bitmap?>(SIZE_ORIGINAL, SIZE_ORIGINAL) {
                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        super.onLoadFailed(errorDrawable)
                        metadata.putBitmap(
                            MediaMetadataCompat.METADATA_KEY_ALBUM_ART,
                            BitmapFactory.decodeResource(resources, R.drawable.default_audio_art)
                        )
                        mediaSession.setMetadata(metadata.build())
                        onCompletion()
                    }

                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap?>?) {
                        metadata.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, resource)
                        mediaSession.setMetadata(metadata.build())
                        onCompletion()
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {}
                })
            }
        } else {
            mediaSession.setMetadata(metadata.build())
            onCompletion()
        }
    }

    fun runOnUiThread(runnable: Runnable?) {
        uiThreadHandler?.post(runnable!!)
    }

    fun getSongAt(position: Int) = queueManager.getSongAt(position)

    fun getNextPosition(force: Boolean) = queueManager.getNextPosition(force)

    fun getQueueInfo(context: Context): String {
        val position = (currentPosition + 1).toString()
        val queueSize = playingQueue.size.toString()
        val positionInfo = String.format("%s/%s", position, queueSize)

        val nextSong = getSongAt(getNextPosition(false))
        val nextSongInfo = if (!nextSong.isArtistNameUnknown()) {
            context.getString(R.string.next_song_x_by_artist_x, nextSong.title, nextSong.displayArtistName())
        } else {
            context.getString(R.string.next_song_x, nextSong.title)
        }
        return buildInfoString(positionInfo, nextSongInfo)
    }

    private fun restorePlayback() {
        if (!playbackRestored) {
            if (persistentStorage.restorationState.isRestored) {
                play()
                mediaSession.sendSessionEvent(SessionEvent.PLAYBACK_RESTORED, null)
            } else {
                needsToRestorePlayback = true
            }
            playbackRestored = true
        }
    }

    private fun openQueue(
        queue: List<Song>,
        startPosition: Int,
        startPlaying: Boolean,
        shuffleMode: Playback.ShuffleMode = Playback.ShuffleMode.Off
    ) = serviceScope.launch(IO) {
        val result = queueManager.open(queue, startPosition, shuffleMode)
        val position = if (result == QueueManager.HANDLED_SOURCE) startPosition else queueManager.position
        if (result != QueueManager.EMPTY_SOURCE && startPlaying) {
            playSongAt(position)
        } else {
            prepareSongAt(position)
        }
    }

    private fun requestFocus(): Boolean {
        return audioManager?.let {
            AudioManagerCompat.requestAudioFocus(it, audioFocusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } ?: false
    }

    private fun abandonFocus() {
        audioManager?.let {
            AudioManagerCompat.abandonAudioFocusRequest(it, audioFocusRequest)
        }
    }

    private fun pause(force: Boolean = false) {
        playbackManager.pause(force)
    }

    private fun play(force: Boolean = false) {
        if (!requestFocus()) {
            showToast(R.string.audio_focus_denied)
            return
        }
        playbackManager.play(force, serviceScope) {
            playSongAt(currentPosition)
        }
    }

    private fun playSongAt(position: Int, newPlayback: Boolean = false) {
        prepareSongAt(position) { success ->
            if (success) {
                play()
                if (newPlayback) {
                    mediaSession.sendSessionEvent(SessionEvent.PLAYBACK_STARTED, null)
                }
            } else {
                val message = if (queueManager.isEmpty) {
                    getString(R.string.empty_play_queue)
                } else {
                    getString(R.string.unplayable_file)
                }
                showToast(message)
            }
        }
    }

    private fun playPreviousSong(force: Boolean) {
        if (isFirstTrack && queueManager.repeatMode == Playback.RepeatMode.Off) {
            showToast(R.string.list_start)
            return
        }
        playOrPrepareSongAt(getPreviousPosition(force))
    }

    private fun back(force: Boolean) {
        if (Preferences.rewindWithBack && currentSongProgress > REWIND_INSTEAD_PREVIOUS_MILLIS) {
            seek(0)
        } else {
            playPreviousSong(force)
        }
    }

    private fun getPreviousPosition(force: Boolean) = queueManager.getPreviousPosition(force)

    @Synchronized
    private fun seek(millis: Int, force: Boolean = true) {
        try {
            playbackManager.seek(millis, force)
            throttledSeekHandler?.notifySeek()
        } catch (_: Exception) {
        }
    }

    private fun playOrPrepareSongAt(position: Int, forcePlay: Boolean = false) {
        if (isPlaying || Preferences.autoPlayOnSkip || forcePlay) {
            playSongAt(position)
        } else {
            prepareSongAt(position)
        }
    }

    private fun prepareSongAt(position: Int, completion: (Boolean) -> Unit = {}) {
        queueManager.setPosition(position)
        openCurrentAndPrepareNext(completion)
    }

    private fun updateWidgets() {
        appWidgetBig.notifyChange(this)
        appWidgetSimple.notifyChange(this)
        appWidgetSmall.notifyChange(this)
    }

    internal fun mediaStoreChanged() = serviceScope.launch {
        mediaSession.sendSessionEvent(SessionEvent.MEDIA_CONTENT_CHANGED, null)
    }

    private fun setupMediaSession() {
        val activityPi = packageManager?.getLaunchIntentForPackage(packageName)?.let { intent ->
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        }
        mediaSession = MediaSessionCompat(this, BuildConfig.APPLICATION_ID).apply {
            isActive = true
            setSessionActivity(activityPi)
            setCallback(mediaSessionCallback)
        }
    }

    private fun setCustomAction(stateBuilder: PlaybackStateCompat.Builder) {
        var repeatIcon = R.drawable.ic_repeat_24dp // REPEAT_MODE_NONE
        if (repeatMode == Playback.RepeatMode.One) {
            repeatIcon = R.drawable.ic_repeat_one_on_24dp
        } else if (repeatMode == Playback.RepeatMode.All) {
            repeatIcon = R.drawable.ic_repeat_on_24dp
        }
        val shuffleIcon = if (shuffleMode == Playback.ShuffleMode.On) {
            R.drawable.ic_shuffle_on_24dp
        } else {
            R.drawable.ic_shuffle_24dp
        }
        stateBuilder.addCustomAction(
            CustomAction.Builder(
                SessionCommand.CYCLE_REPEAT,
                getString(R.string.action_cycle_repeat),
                repeatIcon
            ).build()
        )
        stateBuilder.addCustomAction(
            CustomAction.Builder(
                SessionCommand.TOGGLE_SHUFFLE,
                getString(R.string.action_toggle_shuffle),
                shuffleIcon
            ).build()
        )
    }

    private fun bumpPlayCount(): Boolean {
        if (songPlayCountHelper.shouldBumpPlayCount()) {
            val lastSong = songPlayCountHelper.song
            if (lastSong != Song.emptySong) {
                val currentTime = System.currentTimeMillis()
                serviceScope.launch(IO) {
                    val playCountEntity = repository.findSongInPlayCount(lastSong.id)?.apply {
                        timePlayed = currentTime
                        playCount += 1
                    } ?: lastSong.toPlayCount(timePlayed = currentTime, playCount = 1)

                    repository.upsertSongInPlayCount(playCountEntity)
                }
                SongPlayCountStore.getInstance(this).bumpPlayCount(lastSong.id)
            }
            return true
        }
        return false
    }

    private fun applyReplayGain(song: Song) {
        val mode = Preferences.replayGainSourceMode
        if (song != Song.emptySong && mode != ReplayGainSourceMode.MODE_NONE) {
            val rg = ReplayGainTagExtractor.getReplayGain(song.mediaStoreUri)
            var adjustDB = 0.0f
            var peak = 1.0f

            val rgTrack: Float = rg.trackGain
            val rgAlbum: Float = rg.albumGain
            val rgpTrack: Float = rg.trackPeak
            val rgpAlbum: Float = rg.albumPeak

            if (mode == ReplayGainSourceMode.MODE_ALBUM) {
                adjustDB = (if (rgTrack == 0.0f) adjustDB else rgTrack)
                adjustDB = (if (rgAlbum == 0.0f) adjustDB else rgAlbum)
                peak = (if (rgpTrack == 1.0f) peak else rgpTrack)
                peak = (if (rgpAlbum == 1.0f) peak else rgpAlbum)
            } else if (mode == ReplayGainSourceMode.MODE_TRACK) {
                adjustDB = (if (rgAlbum == 0.0f) adjustDB else rgAlbum)
                adjustDB = (if (rgTrack == 0.0f) adjustDB else rgTrack)
                peak = (if (rgpAlbum == 1.0f) peak else rgpAlbum)
                peak = (if (rgpTrack == 1.0f) peak else rgpTrack)
            }

            if (adjustDB == 0f) {
                adjustDB = Preferences.getReplayGainValue(false)
            } else {
                adjustDB += Preferences.getReplayGainValue(true)

                val peakDB = -20.0f * (log10(peak.toDouble()).toFloat())
                adjustDB = min(adjustDB.toDouble(), peakDB.toDouble()).toFloat()
            }

            playbackManager.setReplayGain(adjustDB)
        } else {
            playbackManager.setReplayGain(Float.NaN)
        }
    }

    private fun restorePlaybackState(wasPlaying: Boolean, progress: Int) {
        playbackManager.setCallbacks(this)
        openCurrentAndPrepareNext { success ->
            if (success) {
                seek(progress)
                if (wasPlaying) {
                    play()
                } else {
                    pause()
                }
            }
        }
    }

    override fun onSharedPreferenceChanged(preferences: SharedPreferences, key: String?) {
        when (key) {
            ENABLE_HISTORY ->
                if (!Preferences.historyEnabled) {
                    serviceScope.launch(IO) {
                        repository.clearSongHistory()
                    }
                }

            QUEUE_NEXT_MODE -> {
                queueManager.setSequentialMode(Preferences.queueNextSequentially)
            }

            PLAYBACK_SPEED -> {
                updateMediaSessionPlaybackState()
            }

            CROSSFADE_DURATION -> {
                val progress = currentSongProgress
                val wasPlaying = isPlaying

                val crossFadeDuration = Preferences.crossFadeDuration
                if (playbackManager.maybeSwitchToCrossFade(crossFadeDuration)) {
                    restorePlaybackState(wasPlaying, progress)
                } else {
                    playbackManager.setCrossFadeDuration(crossFadeDuration)
                }
            }

            GAPLESS_PLAYBACK -> {
                playbackManager.gaplessPlayback = Preferences.gaplessPlayback
                if (playbackManager.gaplessPlayback) {
                    prepareNext()
                } else {
                    playbackManager.setNextDataSource(null)
                }
                playbackManager.updateBalance()
            }

            REPLAYGAIN_SOURCE_MODE,
            REPLAYGAIN_PREAMP_WITH_TAG,
            REPLAYGAIN_PREAMP_WITHOUT_TAG -> serviceScope.launch(IO) {
                applyReplayGain(currentSong)
            }

            ALBUM_ART_ON_LOCK_SCREEN,
            BLURRED_ALBUM_ART -> {
                updateMediaSessionMetadata(::updateMediaSessionPlaybackState)
            }

            CLASSIC_NOTIFICATION -> {
                playingNotificationManager.recreateNotification(this)
                playingNotificationManager.startForeground(this) {
                    displayPlayingNotification()
                }
            }

            COLORED_NOTIFICATION,
            NOTIFICATION_EXTRA_TEXT_LINE,
            NOTIFICATION_PRIORITY -> {
                playingNotificationManager.displayPlayingNotification()
            }
        }
    }

    private fun checkShouldStop(resetState: Boolean): Boolean {
        if (playbackManager.pendingQuit || queueManager.isStopPosition) {
            if (resetState) {
                playbackManager.pendingQuit = false
                queueManager.stopPosition = NO_POSITION
            }
            return true
        }
        return false
    }

    private val mediaSessionCallback = object : MediaSessionCompat.Callback() {
        override fun onPrepare() {
            super.onPrepare()
            if (currentSong == Song.emptySong) {
                restoreState()
            }
        }

        override fun onPlay() {
            super.onPlay()
            if (currentSong != Song.emptySong) {
                play()
            }
        }

        override fun onPause() {
            super.onPause()
            pause()
        }

        override fun onSkipToNext() {
            super.onSkipToNext()
            playNextSong(true)
        }

        override fun onFastForward() {
            super.onFastForward()
            val currentPosition = currentSongProgress
            val songDuration = currentSongDuration
            seek((currentPosition + (Preferences.seekInterval * 1000))
                .coerceAtMost(songDuration))
        }

        override fun onRewind() {
            super.onRewind()
            val currentPosition = currentSongProgress
            seek((currentPosition - (Preferences.seekInterval * 1000))
                .coerceAtLeast(0))
        }

        override fun onSkipToPrevious() {
            super.onSkipToPrevious()
            back(true)
        }

        override fun onStop() {
            quit()
        }

        override fun onSeekTo(pos: Long) {
            seek(pos.toInt())
        }

        override fun onCustomAction(action: String, extras: Bundle) {
            when (action) {
                SessionCommand.RESTORE_PLAYBACK -> {
                    restorePlayback()
                }

                SessionCommand.CYCLE_REPEAT -> {
                    when (repeatMode) {
                        Playback.RepeatMode.Off -> queueManager.setRepeatMode(Playback.RepeatMode.All)
                        Playback.RepeatMode.All -> queueManager.setRepeatMode(Playback.RepeatMode.One)
                        else -> queueManager.setRepeatMode(Playback.RepeatMode.Off)
                    }
                    updateMediaSessionPlaybackState()
                }

                SessionCommand.TOGGLE_SHUFFLE -> {
                    if (shuffleMode == Playback.ShuffleMode.Off) {
                        queueManager.setShuffleMode(Playback.ShuffleMode.On)
                    } else {
                        queueManager.setShuffleMode(Playback.ShuffleMode.Off)
                    }
                    updateMediaSessionPlaybackState()
                }

                SessionCommand.TOGGLE_FAVORITE -> {
                    toggleFavorite()
                }

                SessionCommand.PLAY_SONG_AT -> {
                    val position = extras.getInt(SessionCommand.Extras.POSITION, -1)
                    if (position >= 0) {
                        playSongAt(position, extras.getBoolean(SessionCommand.Extras.IS_NEW_PLAYBACK))
                    }
                }

                else -> Log.d("MediaSession", "Unsupported action: $action")
            }
        }

        override fun onPlayFromMediaId(mediaId: String, extras: Bundle) {
            super.onPlayFromMediaId(mediaId, extras)

            val musicId = AutoMediaIDHelper.extractMusicID(mediaId)
            val itemId = musicId?.toLong() ?: -1

            val songs = ArrayList<Song>()

            serviceScope.launch(IO) {
                when (val category = AutoMediaIDHelper.extractCategory(mediaId)) {
                    AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_ALBUM -> {
                        val album = repository.albumById(itemId)
                        songs.addAll(album.songs)
                        openQueue(songs, 0, true)
                    }

                    AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_ARTIST -> {
                        val artist = repository.artistById(itemId)
                        songs.addAll(artist.songs)
                        openQueue(songs, 0, true)
                    }

                    AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_PLAYLIST -> {
                        val playlist = repository.playlistWithSongs(itemId)
                        songs.addAll(playlist.songs.toSongs())
                        openQueue(songs, 0, true)
                    }

                    AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_HISTORY,
                    AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_TOP_TRACKS,
                    AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_QUEUE -> {
                        val tracks: List<Song> = when (category) {
                            AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_HISTORY -> {
                                repository.historySongs().fromHistoryToSongs()
                            }

                            AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_TOP_TRACKS -> {
                                repository.topPlayedSongs()
                            }

                            else -> queueManager.playingQueue
                        }
                        songs.addAll(tracks)
                        var songIndex = tracks.indexOfSong(itemId)
                        if (songIndex == -1) {
                            songIndex = 0
                        }
                        openQueue(songs, songIndex, true)
                    }

                    AutoMediaIDHelper.MEDIA_ID_MUSICS_BY_SHUFFLE -> {
                        val allSongs = repository.allSongs().shuffled()
                        openQueue(allSongs, 0, true, Playback.ShuffleMode.On)
                    }
                }
            }
        }

        @Suppress("DEPRECATION")
        override fun onPlayFromSearch(query: String, extras: Bundle) {
            serviceScope.launch(IO) {
                val songs = ArrayList<Song>()
                if (query.isEmpty()) {
                    songs.addAll(repository.allSongs())
                } else {
                    // Build a queue based on songs that match "query" or "extras" param
                    val mediaFocus = extras.getString(MediaStore.EXTRA_MEDIA_FOCUS)
                    when (mediaFocus) {
                        MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE -> {
                            val artistQuery = extras.getString(MediaStore.EXTRA_MEDIA_ARTIST)
                            if (artistQuery != null) {
                                val artists = repository.searchArtists(artistQuery)
                                if (artists.isNotEmpty()) {
                                    songs.addAll(artists.first().songs)
                                }
                            }
                        }
                        MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE -> {
                            val albumQuery = extras.getString(MediaStore.EXTRA_MEDIA_ALBUM)
                            if (albumQuery != null) {
                                val albums = repository.searchAlbums(albumQuery)
                                if (albums.isNotEmpty()) {
                                    songs.addAll(albums.first().songs)
                                }
                            }
                        }
                        MediaStore.Audio.Playlists.ENTRY_CONTENT_TYPE -> {
                            val playlistQuery = extras.getString(MediaStore.EXTRA_MEDIA_PLAYLIST)
                            if (playlistQuery != null) {
                                val playlists = repository.searchPlaylists(playlistQuery)
                                if (playlists.isNotEmpty()) {
                                    songs.addAll(playlists.first().songs.toSongs())
                                }
                            }
                        }
                        MediaStore.Audio.Genres.ENTRY_CONTENT_TYPE -> {
                            val genresQuery = extras.getString(MediaStore.EXTRA_MEDIA_GENRE)
                            if (genresQuery != null) {
                                val genres = repository.searchGenres(genresQuery)
                                if (genres.isNotEmpty()) {
                                    songs.addAll(genres.flatMap { repository.songsByGenre(it.id) })
                                }
                            }
                        }
                    }
                }

                // Search by title
                if (songs.isEmpty()) {
                    songs.addAll(repository.searchSongs(query))
                }

                openQueue(songs, 0, true)
            }
        }

        override fun onPlayFromUri(uri: Uri, extras: Bundle) {
            serviceScope.launch(IO) {
                val songs = uriSongResolver.resolve(uri)
                if (songs.isNotEmpty()) {
                    openQueue(songs, 0, true)
                }
            }
        }
    }

    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (!isPlaying && mayResumeOnFocusGain) {
                    play()
                    mayResumeOnFocusGain = false
                }
            }

            AudioManager.AUDIOFOCUS_LOSS -> {
                // Lost focus for an unbounded amount of time: stop playback and release media playback
                if (!Preferences.ignoreAudioFocus) {
                    val force = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    // Starting with Android 12, the system automatically fades out
                    // the output when focus is lost; we simply pause without fading
                    // out on our own.
                    pause(force)
                }
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media playback because playback
                // is likely to resume
                if (Preferences.pauseOnTransientFocusLoss) {
                    val wasPlaying = isPlaying
                    pause(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                    mayResumeOnFocusGain = wasPlaying
                }
            }
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
                            play()
                        }
                        BluetoothA2dp.STATE_DISCONNECTED -> if (Preferences.isPauseOnDisconnect(true)) {
                            pause()
                        }
                    }
                }
                BluetoothDevice.ACTION_ACL_CONNECTED ->
                    if (context.isBluetoothA2dpConnected() && Preferences.isResumeOnConnect(true)) { play() }
                BluetoothDevice.ACTION_ACL_DISCONNECTED ->
                    if (context.isBluetoothA2dpDisconnected() && Preferences.isPauseOnDisconnect(true)) { pause() }
            }
        }
    }

    private var becomingNoisyReceiverRegistered = false
    private val becomingNoisyReceiverIntentFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
    private val becomingNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                if (Preferences.isPauseOnDisconnect(false)) {
                    pause()
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
                        pause()
                    }
                    // Check whether the current song is empty which means the playing queue hasn't restored yet
                    1 -> if (Preferences.isResumeOnConnect(false)) {
                        if (currentSong != Song.emptySong) {
                            play()
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
            val command = intent.getStringExtra(ServiceAction.Extras.EXTRA_APP_WIDGET_NAME) ?: return
            val ids = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
            when (command) {
                AppWidgetBig.NAME -> {
                    appWidgetBig.performUpdate(this@MusicService, ids)
                }
                AppWidgetSimple.NAME -> {
                    appWidgetSimple.performUpdate(this@MusicService, ids)
                }
                AppWidgetSmall.NAME -> {
                    appWidgetSmall.performUpdate(this@MusicService, ids)
                }
            }
        }
    }

    companion object {
        private const val REWIND_INSTEAD_PREVIOUS_MILLIS = 5000
        private const val MEDIA_SESSION_ACTIONS = (PlaybackStateCompat.ACTION_PLAY
                or PlaybackStateCompat.ACTION_PAUSE
                or PlaybackStateCompat.ACTION_PLAY_PAUSE
                or PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH
                or PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
                or PlaybackStateCompat.ACTION_PLAY_FROM_URI
                or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                or PlaybackStateCompat.ACTION_STOP
                or PlaybackStateCompat.ACTION_SEEK_TO)
    }
}