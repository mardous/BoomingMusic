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
import android.app.NotificationManager
import android.appwidget.AppWidgetManager
import android.bluetooth.BluetoothDevice
import android.content.*
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.*
import android.os.PowerManager.WakeLock
import android.provider.MediaStore
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.annotation.RequiresApi
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.core.content.edit
import androidx.core.content.getSystemService
import androidx.core.util.Predicate
import androidx.localbroadcastmanager.content.LocalBroadcastManager
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
import com.mardous.booming.audio.SoundSettings
import com.mardous.booming.database.toPlayCount
import com.mardous.booming.extensions.*
import com.mardous.booming.extensions.glide.getSongGlideModel
import com.mardous.booming.extensions.glide.songOptions
import com.mardous.booming.extensions.media.asQueueItems
import com.mardous.booming.extensions.media.displayArtistName
import com.mardous.booming.extensions.media.durationStr
import com.mardous.booming.extensions.media.isArtistNameUnknown
import com.mardous.booming.extensions.utilities.buildInfoString
import com.mardous.booming.glide.transformation.BlurTransformation
import com.mardous.booming.misc.ReplayGainTagExtractor
import com.mardous.booming.model.Playlist
import com.mardous.booming.model.Song
import com.mardous.booming.providers.databases.HistoryStore
import com.mardous.booming.providers.databases.PlaybackQueueStore
import com.mardous.booming.providers.databases.SongPlayCountStore
import com.mardous.booming.repository.Repository
import com.mardous.booming.service.constants.ServiceAction
import com.mardous.booming.service.constants.ServiceEvent
import com.mardous.booming.service.equalizer.EqualizerManager
import com.mardous.booming.service.notification.PlayingNotification
import com.mardous.booming.service.notification.PlayingNotificationClassic
import com.mardous.booming.service.notification.PlayingNotificationImpl24
import com.mardous.booming.service.playback.Playback
import com.mardous.booming.service.playback.Playback.PlaybackCallbacks
import com.mardous.booming.service.playback.PlaybackManager
import com.mardous.booming.service.queue.SmartPlayingQueue
import com.mardous.booming.service.queue.toQueueSongs
import com.mardous.booming.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import org.koin.android.ext.android.inject
import org.koin.java.KoinJavaComponent.get
import kotlin.math.log10
import kotlin.math.min
import kotlin.random.Random.Default.nextInt

class MusicService : MediaBrowserServiceCompat(), PlaybackCallbacks, OnSharedPreferenceChangeListener {

    private val mBinder: IBinder = MusicBinder()

    private val serviceScope = CoroutineScope(Job() + Main)
    private val repository by inject<Repository>()

    private val appWidgetBig = AppWidgetBig.instance
    private val appWidgetSimple = AppWidgetSimple.instance
    private val appWidgetSmall = AppWidgetSmall.instance

    private lateinit var playingQueue: SmartPlayingQueue
    private var queuesRestored = false
    private var playbackRestored = false
    private var needsToRestorePlayback = false

    private val sharedPreferences: SharedPreferences by inject()
    private val equalizerManager: EqualizerManager by inject()
    private val soundSettings: SoundSettings by inject()
    private lateinit var playbackManager: PlaybackManager
    private var mediaSession: MediaSessionCompat? = null
    private var playingNotification: PlayingNotification? = null
    private var notificationManager: NotificationManager? = null
    private var playerHandler: Handler? = null

    private var musicPlayerHandlerThread: HandlerThread? = null
    private var throttledSeekHandler: ThrottledSeekHandler? = null

    private lateinit var mediaStoreObserver: MediaStoreObserver
    private var notHandledMetaChangedForCurrentTrack = false
    private var uiThreadHandler: Handler? = null
    private var wakeLock: WakeLock? = null

    // Android Auto
    private var musicProvider = get<AutoMusicProvider>(AutoMusicProvider::class.java)
    private var packageValidator: PackageValidator? = null

    private val songPlayCountHelper = SongPlayCountHelper()

    private val updateFavoriteReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ServiceEvent.FAVORITE_STATE_CHANGED) {
                updateFavoriteState()
            }
        }
    }

    private var bluetoothConnectedRegistered = false
    private val bluetoothConnectedIntentFilter = IntentFilter().apply {
        addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
        addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
    }
    private val bluetoothReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            when (intent?.action) {
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
                        if (getCurrentSong() != Song.emptySong) {
                            play()
                        } else {
                            receivedHeadsetConnected = true
                        }
                    }
                }
            }
        }
    }

    var pendingQuit = false

    override fun onCreate() {
        super.onCreate()
        val powerManager = getSystemService<PowerManager>()
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, javaClass.name)
            wakeLock!!.setReferenceCounted(false)
        }

        playingQueue = SmartPlayingQueue(this, sharedPreferences, serviceScope, Preferences.queueNextSequentially)

        // Set up PlaybackHandler.
        musicPlayerHandlerThread = HandlerThread("PlaybackHandler", Process.THREAD_PRIORITY_BACKGROUND)
        musicPlayerHandlerThread!!.start()
        playerHandler = Handler(musicPlayerHandlerThread!!.looper)
        playbackManager = PlaybackManager(this, equalizerManager, soundSettings, serviceScope)
        playbackManager.setCallbacks(this)
        setupMediaSession()

        // Create the UI-thread handler.
        uiThreadHandler = Handler(Looper.getMainLooper())
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(widgetIntentReceiver, IntentFilter(ServiceAction.ACTION_APP_WIDGET_UPDATE))
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(updateFavoriteReceiver, IntentFilter(ServiceEvent.FAVORITE_STATE_CHANGED))
        sessionToken = mediaSession?.sessionToken
        notificationManager = getSystemService()
        initNotification()

        mediaStoreObserver = MediaStoreObserver(this, playerHandler!!)
        throttledSeekHandler = ThrottledSeekHandler(this, playerHandler!!)
        contentResolver.registerContentObserver(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, mediaStoreObserver
        )
        contentResolver.registerContentObserver(
            MediaStore.Audio.Media.INTERNAL_CONTENT_URI, true, mediaStoreObserver
        )
        restoreState()

        serviceScope.launch(IO) {
            equalizerManager.initializeEqualizer()
        }

        musicProvider.setMusicService(this)
        packageValidator = PackageValidator(this, R.xml.allowed_media_browser_callers)

        LocalBroadcastManager.getInstance(this)
            .sendBroadcast(Intent("${ServiceEvent.BOOMING_PACKAGE_NAME}.BOOMING_MUSIC_SERVICE_CREATED"))
        registerHeadsetEvents()
        registerBluetoothConnected()
        registerBecomingNoisyReceiver()

        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onBind(intent: Intent?): IBinder? {
        // For Android auto, need to call super, or onGetRoot won't be called.
        return if (intent != null && "android.media.browse.MediaBrowserService" == intent.action) {
            super.onBind(intent)
        } else mBinder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        if (!isPlaying && !playbackManager.mayResume()) {
            stopSelf()
        }
        return true
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
        var resultSent = false
        serviceScope.launch(IO) {
            result.sendResult(musicProvider.getChildren(parentId, resources))
            resultSent = true
        }
        if (!resultSent) {
            result.detach()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null && intent.action != null) {
            serviceScope.launch {
                restoreQueuesAndPositionIfNecessary()
                when (intent.action) {
                    ServiceAction.ACTION_TOGGLE_PAUSE -> if (isPlaying) {
                        pause()
                    } else {
                        play()
                    }

                    ServiceAction.ACTION_PAUSE -> pause()
                    ServiceAction.ACTION_PLAY -> play()
                    ServiceAction.ACTION_PLAY_PLAYLIST -> playFromPlaylist(intent)
                    ServiceAction.ACTION_PREVIOUS -> back(true)
                    ServiceAction.ACTION_NEXT -> playNextSong(true)
                    ServiceAction.ACTION_STOP,
                    ServiceAction.ACTION_QUIT -> {
                        pendingQuit = false
                        playingQueue.stopPosition = -1
                        quit()
                    }

                    ServiceAction.ACTION_PENDING_QUIT -> {
                        pendingQuit = isPlaying
                        if (!pendingQuit) {
                            playingQueue.stopPosition = -1
                            quit()
                        }
                    }
                    ServiceAction.ACTION_TOGGLE_FAVORITE -> toggleFavorite()
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(widgetIntentReceiver)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(updateFavoriteReceiver)
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
        mediaSession?.isActive = false
        quit()
        releaseResources()
        serviceScope.cancel()
        contentResolver.unregisterContentObserver(mediaStoreObserver)
        wakeLock?.release()
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        LocalBroadcastManager.getInstance(this)
            .sendBroadcast(Intent("${ServiceEvent.BOOMING_PACKAGE_NAME}.BOOMING_MUSIC_SERVICE_DESTROYED"))
    }

    private fun playFromPlaylist(intent: Intent) {
        val playlist =
            IntentCompat.getParcelableExtra(intent, ServiceAction.Extras.EXTRA_PLAYLIST, Playlist::class.java)
        val shuffleMode = intent.getIntExtra(ServiceAction.Extras.EXTRA_SHUFFLE_MODE, getShuffleMode())
        if (playlist != null) {
            serviceScope.launch(IO) {
                val playlistSongs = playlist.getSongs()
                if (playlistSongs.isNotEmpty()) {
                    if (shuffleMode == Playback.ShuffleMode.ON) {
                        val startPosition = nextInt(playlistSongs.size)
                        openQueue(playlistSongs, startPosition, true)
                        setShuffleMode(shuffleMode)
                    } else {
                        openQueue(playlistSongs, 0, true)
                    }
                } else {
                    showToast(R.string.playlist_empty_text)
                }
            }
        } else {
            showToast(R.string.playlist_empty_text)
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

    override fun onTaskRemoved(rootIntent: Intent) {
        if (!isPlaying && !playbackManager.mayResume()) {
            quit()
        }
        super.onTaskRemoved(rootIntent)
    }

    private fun savePosition() {
        sharedPreferences.edit { putInt(SAVED_QUEUE_POSITION, getPosition()) }
    }

    internal fun savePositionInTrack() {
        sharedPreferences.edit { putInt(SAVED_POSITION_IN_TRACK, getSongProgressMillis()) }
    }

    private fun saveState() {
        saveQueues()
        savePosition()
        savePositionInTrack()
    }

    private fun saveQueues() {
        playingQueue.saveQueues()
    }

    fun restoreState(completion: () -> Unit = {}) {
        playingQueue.repeatMode = sharedPreferences.getInt(SAVED_REPEAT_MODE, Playback.RepeatMode.OFF)
        playingQueue.shuffleMode = sharedPreferences.getInt(SAVED_SHUFFLE_MODE, Playback.ShuffleMode.OFF)
        handleAndSendChangeInternal(ServiceEvent.REPEAT_MODE_CHANGED)
        handleAndSendChangeInternal(ServiceEvent.SHUFFLE_MODE_CHANGED)
        serviceScope.launch {
            restoreQueuesAndPositionIfNecessary()
            completion()
        }
    }

    internal suspend fun restoreQueuesAndPositionIfNecessary() {
        if (!queuesRestored && playingQueue.isEmpty) {
            withContext(IO) {
                val restoredQueue = PlaybackQueueStore.getInstance(this@MusicService).savedPlayingQueue
                val restoredOriginalQueue = PlaybackQueueStore.getInstance(this@MusicService).savedOriginalPlayingQueue
                val restoredPosition = sharedPreferences.getInt(SAVED_QUEUE_POSITION, -1)
                val restoredPositionInTrack = sharedPreferences.getInt(SAVED_POSITION_IN_TRACK, -1)
                if (restoredQueue.isNotEmpty() && restoredQueue.size == restoredOriginalQueue.size && restoredPosition != -1) {
                    playingQueue.originalPlayingQueue = ArrayList(restoredOriginalQueue.toQueueSongs())
                    playingQueue.playingQueue = ArrayList(restoredQueue.toQueueSongs())
                    playingQueue.position = restoredPosition
                    withContext(Main) {
                        openCurrent {
                            prepareNext()
                            if (restoredPositionInTrack > 0) {
                                seek(restoredPositionInTrack)
                            }
                            notHandledMetaChangedForCurrentTrack = true
                            sendChangeInternal(ServiceEvent.META_CHANGED)

                            if (needsToRestorePlayback || receivedHeadsetConnected) {
                                play()
                                if (receivedHeadsetConnected) {
                                    receivedHeadsetConnected = false
                                }
                            }
                        }
                    }

                    sendChangeInternal(ServiceEvent.QUEUE_CHANGED)
                    mediaSession?.setQueueTitle(getString(R.string.playing_queue_label))
                    mediaSession?.setQueue(playingQueue.playingQueue.asQueueItems())
                } else {
                    needsToRestorePlayback = false
                }
            }
            queuesRestored = true
        }
    }

    fun quit() {
        pause()
        stopForegroundAndNotification()

        //force to updateMetadata play count if necessary
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

    private fun releaseResources() {
        playerHandler?.removeCallbacksAndMessages(null)
        musicPlayerHandlerThread?.quitSafely()
        playbackManager.release()
        mediaSession?.release()
    }

    private var isForeground = false

    @SuppressLint("InlinedApi")
    private fun startForegroundOrNotify() {
        if (playingNotification != null && getCurrentSong().id != -1L) {
            if (isForeground && !isPlaying) {
                // This makes the notification dismissible
                // We can't call stopForeground(false) on A12 though, which may result in crashes
                // when we call startForeground after that e.g. when Alarm goes off,
                if (!hasS()) {
                    ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
                    isForeground = false
                }
            }
            if (!isForeground && isPlaying) {
                // Specify that this is a media service, if supported.
                ServiceCompat.startForeground(
                    this,
                    PlayingNotification.NOTIFICATION_ID,
                    playingNotification!!.build(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )

                isForeground = true
            } else {
                // If we are already in foreground just updateMetadata the notification
                notificationManager?.notify(PlayingNotification.NOTIFICATION_ID, playingNotification!!.build())
            }
        } else {
            stopForegroundAndNotification()
        }
    }

    private fun stopForegroundAndNotification() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        notificationManager?.cancel(PlayingNotification.NOTIFICATION_ID)
        isForeground = false
    }

    val isPlaying: Boolean
        get() = playbackManager.isPlaying()

    private val playbackSpeed: Float
        get() = playbackManager.getPlaybackSpeed()

    fun getPosition() = playingQueue.position

    fun setPosition(position: Int) {
        openTrackAndPrepareNextAt(position) { success ->
            if (success) {
                notifyChange(ServiceEvent.PLAY_STATE_CHANGED)
            }
        }
    }

    fun playNextSong(force: Boolean) {
        if (isLastTrack && playingQueue.repeatMode == Playback.RepeatMode.OFF) {
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
        playSongOrSetPositionAt(getNextPosition(force), !force)
    }

    @Synchronized
    private fun openTrackAndPrepareNextAt(position: Int, completion: (success: Boolean) -> Unit) {
        playingQueue.setPositionTo(position)
        openCurrent { success ->
            completion(success)
            if (success) prepareNextImpl()
            notifyChange(ServiceEvent.META_CHANGED)
            notHandledMetaChangedForCurrentTrack = false
        }
    }

    @Synchronized
    private fun openCurrent(completion: (success: Boolean) -> Unit) {
        playbackManager.setDataSource(getTrackUri(getCurrentSong())) { success ->
            completion(success)
        }
    }

    private fun prepareNext() {
        prepareNextImpl()
    }

    @Synchronized
    private fun prepareNextImpl() {
        try {
            val nextPosition = getNextPosition(false)
            if (nextPosition == playingQueue.stopPosition) {
                playbackManager.setNextDataSource(null)
            } else {
                playbackManager.setNextDataSource(getTrackUri(getSongAt(nextPosition)))
            }
            playingQueue.nextPosition = nextPosition
        } catch (_: Exception) {
        }
    }

    fun toggleFavorite() {
        serviceScope.launch {
            MusicUtil.toggleFavorite(getCurrentSong())
            LocalBroadcastManager.getInstance(this@MusicService)
                .sendBroadcast(Intent(ServiceEvent.FAVORITE_STATE_CHANGED))
        }
    }

    fun isCurrentFavorite(completion: (isFavorite: Boolean) -> Unit) {
        serviceScope.launch {
            val isFavorite = MusicUtil.isFavorite(getCurrentSong())
            completion(isFavorite)
        }
    }

    private fun updateFavoriteState() {
        isCurrentFavorite { isFavorite ->
            if (!isForeground) {
                playingNotification?.updateMetadata(getCurrentSong()) {
                    playingNotification?.setPlaying(isPlaying)
                    playingNotification?.updateFavorite(isFavorite)
                    startForegroundOrNotify()
                }
            } else {
                playingNotification?.updateFavorite(isFavorite)
                startForegroundOrNotify()
            }

        }
    }

    private fun initNotification() {
        playingNotification = if (Preferences.classicNotification) {
            PlayingNotificationClassic.from(this, notificationManager!!)
        } else {
            PlayingNotificationImpl24.from(this, notificationManager!!, mediaSession!!)
        }
    }

    internal fun updateMediaSessionPlaybackState() {
        val stateBuilder = PlaybackStateCompat.Builder()
            .setActions(MEDIA_SESSION_ACTIONS)
            .setState(
                if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                getSongProgressMillis().toLong(),
                playbackSpeed
            )
        setCustomAction(stateBuilder)
        mediaSession?.setPlaybackState(stateBuilder.build())
    }

    @SuppressLint("CheckResult")
    private fun updateMediaSessionMetadata(onCompletion: () -> Unit) {
        val song = getCurrentSong()
        if (song.id == -1L) {
            mediaSession?.setMetadata(null)
            return
        }
        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, song.albumName)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, song.albumArtistName)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.displayArtistName())
            .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, song.trackNumber.toLong())
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, song.duration)
            .putLong(MediaMetadataCompat.METADATA_KEY_YEAR, song.year.toLong())
            .putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, getPlayingQueue().size.toLong())

        // If we're in car mode, don't wait for the artwork to load before setting session metadata.
        if (resources.isCarMode) {
            mediaSession?.setMetadata(metadata.build())
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
                        mediaSession?.setMetadata(metadata.build())
                        onCompletion()
                    }

                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap?>?) {
                        metadata.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, resource)
                        mediaSession?.setMetadata(metadata.build())
                        onCompletion()
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {}
                })
            }
        } else {
            mediaSession?.setMetadata(metadata.build())
            onCompletion()
        }
    }

    private fun updateNotification() {
        if (playingNotification != null && getCurrentSong().id != -1L) {
            stopForegroundAndNotification()
            initNotification()
        }
    }

    fun runOnUiThread(runnable: Runnable?) {
        uiThreadHandler?.post(runnable!!)
    }

    fun getCurrentSong() = playingQueue.currentSong

    fun getSongAt(position: Int) = playingQueue.getSongAt(position)

    fun getNextPosition(force: Boolean) = playingQueue.getNextPosition(force)

    fun getQueueDurationInfo(): String? {
        val playingQueue = getPlayingQueue()
        if (playingQueue.isEmpty()) {
            return null
        }
        return getQueueDurationMillis(getPosition()).durationStr()
    }

    fun getQueuePositionInfo(): String =
        buildInfoString((getPosition() + 1).toString(), getPlayingQueue().size.toString(), delimiter = "/")

    fun getNextSong(): Song = getSongAt(getNextPosition(false))

    fun getNextSongInfo(context: Context): String {
        val nextSong = getNextSong()
        return if (!nextSong.isArtistNameUnknown()) {
            context.getString(R.string.next_song_x_by_artist_x, nextSong.title, nextSong.displayArtistName())
        } else context.getString(R.string.next_song_x, nextSong.title)
    }

    fun getQueueInfo(context: Context): String =
        buildInfoString(getQueuePositionInfo(), getNextSongInfo(context))

    private val isFirstTrack: Boolean
        get() = playingQueue.isFirstTrack

    private val isLastTrack: Boolean
        get() = playingQueue.isLastTrack

    fun getPlayingQueue() = playingQueue.playingQueue

    fun getRepeatMode() = playingQueue.repeatMode

    fun setRepeatMode(mode: Int) {
        playingQueue.setRepeatMode(mode) {
            prepareNext()
            handleAndSendChangeInternal(ServiceEvent.REPEAT_MODE_CHANGED)
        }
    }

    fun restorePlayback() {
        if (!playbackRestored) {
            if (queuesRestored) {
                play()
            } else {
                needsToRestorePlayback = true
            }
            playbackRestored = true
        }
    }

    fun openQueue(queue: List<Song>, startPosition: Int, startPlaying: Boolean) {
        playingQueue.open(queue, startPosition) { position ->
            if (startPlaying) {
                playSongAt(position)
            } else {
                setPosition(position)
            }
            notifyChange(ServiceEvent.QUEUE_CHANGED)
        }
    }

    fun playNext(song: Song) {
        playingQueue.playNext(song)
        notifyChange(ServiceEvent.QUEUE_CHANGED)
    }

    fun playNext(songs: List<Song>) {
        playingQueue.playNext(songs)
        notifyChange(ServiceEvent.QUEUE_CHANGED)
    }

    fun addSong(position: Int, song: Song) {
        playingQueue.addSong(position, song)
        notifyChange(ServiceEvent.QUEUE_CHANGED)
    }

    fun addSong(song: Song) {
        playingQueue.addSong(song)
        notifyChange(ServiceEvent.QUEUE_CHANGED)
    }

    fun addSongs(position: Int, songs: List<Song>) {
        playingQueue.addSongs(position, songs)
        notifyChange(ServiceEvent.QUEUE_CHANGED)
    }

    fun addSongs(songs: List<Song>) {
        playingQueue.addSongs(songs)
        notifyChange(ServiceEvent.QUEUE_CHANGED)
    }

    fun removeSong(position: Int) {
        playingQueue.removeSong(position)
        notifyChange(ServiceEvent.QUEUE_CHANGED)
    }

    fun removeSong(song: Song) {
        playingQueue.removeSong(song)
        notifyChange(ServiceEvent.QUEUE_CHANGED)
    }

    fun removeSongs(songs: List<Song>) {
        playingQueue.removeSongs(songs)
        notifyChange(ServiceEvent.QUEUE_CHANGED)
    }

    fun moveSong(from: Int, to: Int) {
        playingQueue.moveSong(from, to)
        notifyChange(ServiceEvent.QUEUE_CHANGED)
    }

    fun shuffleQueue() {
        playingQueue.shuffleQueue {
            notifyChange(ServiceEvent.QUEUE_CHANGED)
        }
    }

    fun clearQueue() {
        playingQueue.clearQueue()
        setPosition(-1)
        notifyChange(ServiceEvent.QUEUE_CHANGED)
    }

    fun playSongAt(position: Int) {
        // Every chromecast method needs to run on main thread, or you are greeted with IllegalStateException
        // So it will use Main dispatcher
        // And by using Default dispatcher for local playback we are reduce the burden of main thread
        serviceScope.launch(Default) {
            openTrackAndPrepareNextAt(position) { success ->
                if (success) {
                    play()
                } else {
                    runOnUiThread {
                        if (playingQueue.isEmpty) {
                            showToast(R.string.empty_play_queue)
                        } else {
                            showToast(R.string.unplayable_file)
                        }
                    }
                }
            }
        }
    }

    fun pause() {
        playbackManager.pause {
            notifyChange(ServiceEvent.PLAY_STATE_CHANGED)
        }
    }

    @Synchronized
    fun play() {
        playbackManager.play { playSongAt(getPosition()) }
        if (notHandledMetaChangedForCurrentTrack) {
            handleChangeInternal(ServiceEvent.META_CHANGED)
            notHandledMetaChangedForCurrentTrack = false
        }
        notifyChange(ServiceEvent.PLAY_STATE_CHANGED)
    }

    fun playPreviousSong(force: Boolean) {
        if (isFirstTrack && playingQueue.repeatMode == Playback.RepeatMode.OFF) {
            showToast(R.string.list_start)
            return
        }
        playSongOrSetPositionAt(getPreviousPosition(force))
    }

    fun back(force: Boolean) {
        if (Preferences.rewindWithBack && getSongProgressMillis() > REWIND_INSTEAD_PREVIOUS_MILLIS) {
            seek(0)
        } else {
            playPreviousSong(force)
        }
    }

    private fun getPreviousPosition(force: Boolean) = playingQueue.getPreviousPosition(force)

    fun getSongProgressMillis(): Int = playbackManager.getSongProgressMillis()

    fun getSongDurationMillis(): Int = playbackManager.getSongDurationMillis()

    fun getQueueDurationMillis(position: Int) = playingQueue.getDuration(position)

    @Synchronized
    fun seek(millis: Int) {
        try {
            playbackManager.seek(millis)
            throttledSeekHandler?.notifySeek()
        } catch (_: Exception) {
        }
    }

    fun cycleRepeatMode() {
        when (getRepeatMode()) {
            Playback.RepeatMode.OFF -> setRepeatMode(Playback.RepeatMode.ALL)
            Playback.RepeatMode.ALL -> setRepeatMode(Playback.RepeatMode.CURRENT)
            else -> setRepeatMode(Playback.RepeatMode.OFF)
        }
    }

    fun toggleShuffle() {
        if (getShuffleMode() == Playback.ShuffleMode.OFF) {
            setShuffleMode(Playback.ShuffleMode.ON)
        } else {
            setShuffleMode(Playback.ShuffleMode.OFF)
        }
    }

    fun getShuffleMode() = playingQueue.shuffleMode

    fun setShuffleMode(mode: Int) {
        playingQueue.setShuffleMode(mode) {
            handleAndSendChangeInternal(ServiceEvent.SHUFFLE_MODE_CHANGED)
            notifyChange(ServiceEvent.QUEUE_CHANGED)
        }
    }

    private fun isAutoPlay(): Boolean {
        return isPlaying || Preferences.autoPlayOnSkip
    }

    private fun playSongOrSetPositionAt(position: Int, forcePlay: Boolean = false) {
        if (isAutoPlay() || forcePlay) {
            playSongAt(position)
        } else {
            setPosition(position)
        }
    }

    private fun notifyChange(what: String) {
        handleAndSendChangeInternal(what)
    }

    internal fun handleAndSendChangeInternal(what: String) {
        handleChangeInternal(what)
        sendChangeInternal(what)
    }

    private fun sendChangeInternal(what: String) {
        LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(what))
        appWidgetBig.notifyChange(this, what)
        appWidgetSimple.notifyChange(this, what)
        appWidgetSmall.notifyChange(this, what)
    }

    private fun handleChangeInternal(what: String) {
        when (what) {
            ServiceEvent.PLAY_STATE_CHANGED -> {
                updateMediaSessionPlaybackState()
                val isPlaying = isPlaying
                if (!isPlaying) {
                    if (getSongDurationMillis() > 0) {
                        savePositionInTrack()
                    }
                }
                songPlayCountHelper.notifyPlayStateChanged(isPlaying)
                playingNotification?.setPlaying(isPlaying)
                startForegroundOrNotify()
            }

            ServiceEvent.META_CHANGED -> {
                if (playingQueue.stopPosition < getPosition()) {
                    setStopPosition(-1)
                }
                // We must call updateMediaSessionPlaybackState after the load of album art is completed
                // if we are loading it, or it won't be updated in the notification
                updateMediaSessionMetadata {
                    updateMediaSessionPlaybackState()
                    isCurrentFavorite { isFavorite ->
                        playingNotification?.updateMetadata(getCurrentSong()) {
                            playingNotification?.updateFavorite(isFavorite)
                            startForegroundOrNotify()
                        }
                    }
                }
                savePosition()
                savePositionInTrack()
                serviceScope.launch(IO) {
                    val currentSong = getCurrentSong()
                    HistoryStore.getInstance(this@MusicService).addSongId(currentSong.id)
                    repository.upsertSongInHistory(currentSong)
                    songPlayCountHelper.notifySongChanged(currentSong, isPlaying)
                    applyReplayGain(currentSong)
                }
            }

            ServiceEvent.QUEUE_CHANGED -> {
                playingQueue.stopPosition = -1
                mediaSession?.setQueueTitle(getString(R.string.playing_queue_label))
                mediaSession?.setQueue(playingQueue.playingQueue.asQueueItems())
                updateMediaSessionMetadata(::updateMediaSessionPlaybackState) // because playing queue size might have changed
                saveState()
                if (playingQueue.playingQueue.isNotEmpty()) {
                    prepareNext()
                } else {
                    stopForegroundAndNotification()
                }
            }
        }
    }

    private fun setupMediaSession() {
        mediaSession = MediaSessionCompat(this, BuildConfig.APPLICATION_ID)
        mediaSession!!.isActive = true
        mediaSession!!.setCallback(MediaSessionCallback(this, serviceScope))
    }

    private fun setCustomAction(stateBuilder: PlaybackStateCompat.Builder) {
        var repeatIcon = R.drawable.ic_repeat_24dp // REPEAT_MODE_NONE
        if (getRepeatMode() == Playback.RepeatMode.CURRENT) {
            repeatIcon = R.drawable.ic_repeat_one_on_24dp
        } else if (getRepeatMode() == Playback.RepeatMode.ALL) {
            repeatIcon = R.drawable.ic_repeat_on_24dp
        }
        stateBuilder.addCustomAction(
            PlaybackStateCompat.CustomAction.Builder(
                CYCLE_REPEAT, getString(R.string.action_cycle_repeat), repeatIcon
            ).build()
        )

        val shuffleIcon = if (getShuffleMode() == Playback.ShuffleMode.ON)
            R.drawable.ic_shuffle_on_24dp else R.drawable.ic_shuffle_24dp
        stateBuilder.addCustomAction(
            PlaybackStateCompat.CustomAction.Builder(
                TOGGLE_SHUFFLE, getString(R.string.action_toggle_shuffle), shuffleIcon
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

    fun getPlaybackManager(): PlaybackManager = playbackManager

    fun getAudioSessionId(): Int = playbackManager.getAudioSessionId()

    @RequiresApi(Build.VERSION_CODES.P)
    fun getRoutedDevice(): AudioDeviceInfo? = playbackManager.getRoutedDevice()

    private fun applyReplayGain(song: Song) {
        val mode = Preferences.replayGainSourceMode
        if (mode != ReplayGainSourceMode.MODE_NONE) {
            val gainValues = ReplayGainTagExtractor.getReplayGain(song)
            var adjustDB = 0.0f
            var peak = 1.0f

            val rgTrack: Float = gainValues.rgTrack
            val rgAlbum: Float = gainValues.rgAlbum
            val rgpTrack: Float = gainValues.peakTrack
            val rgpAlbum: Float = gainValues.peakAlbum

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

    private fun getTrackUri(song: Song): String {
        return song.mediaStoreUri.toString()
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
                playingQueue.setSequentialMode(Preferences.queueNextSequentially)
            }

            PLAYBACK_SPEED -> {
                updateMediaSessionPlaybackState()
            }

            GAPLESS_PLAYBACK -> {
                playbackManager.isGaplessPlayback = Preferences.gaplessPlayback
                if (playbackManager.isGaplessPlayback) {
                    prepareNext()
                } else {
                    playbackManager.setNextDataSource(null)
                }
                playbackManager.updateBalance()
            }

            REPLAYGAIN_SOURCE_MODE,
            REPLAYGAIN_PREAMP_WITH_TAG,
            REPLAYGAIN_PREAMP_WITHOUT_TAG -> serviceScope.launch(IO) {
                applyReplayGain(getCurrentSong())
            }

            ALBUM_ART_ON_LOCK_SCREEN,
            BLURRED_ALBUM_ART -> {
                updateMediaSessionMetadata(::updateMediaSessionPlaybackState)
            }

            CLASSIC_NOTIFICATION -> {
                updateNotification()
                playingNotification?.updateMetadata(getCurrentSong()) {
                    playingNotification?.setPlaying(isPlaying)
                    startForegroundOrNotify()
                }
            }

            COLORED_NOTIFICATION,
            NOTIFICATION_EXTRA_TEXT_LINE,
            NOTIFICATION_PRIORITY -> {
                playingNotification?.updateMetadata(getCurrentSong()) {
                    playingNotification?.setPlaying(isPlaying)
                    startForegroundOrNotify()
                }
            }
        }
    }

    override fun onTrackWentToNext() {
        bumpPlayCount()
        if (checkShouldStop(false) || (playingQueue.repeatMode == Playback.RepeatMode.OFF && isLastTrack)) {
            playbackManager.setNextDataSource(null)
            pause()
            seek(0)
            if (checkShouldStop(true)) {
                playingQueue.setPositionToNext()
                notifyChange(ServiceEvent.META_CHANGED)
                quit()
            }
        } else {
            playingQueue.setPositionToNext()
            prepareNextImpl()
            notifyChange(ServiceEvent.META_CHANGED)
            playbackManager.updateBalance()
            playbackManager.updateTempo()
        }
    }

    override fun onTrackEnded() {
        acquireWakeLock()

        // bump play count before anything else
        bumpPlayCount()
        // if there is a timer finished, don't continue
        if ((playingQueue.repeatMode == Playback.RepeatMode.OFF && isLastTrack) || checkShouldStop(false)) {
            notifyChange(ServiceEvent.PLAY_STATE_CHANGED)
            seek(0)
            if (checkShouldStop(true)) {
                quit()
            }
        } else {
            playNextSong(false)
        }
        releaseWakeLock()
    }

    override fun onPlayStateChanged() {
        notifyChange(ServiceEvent.PLAY_STATE_CHANGED)
    }

    private fun checkShouldStop(resetState: Boolean): Boolean {
        if (pendingQuit || playingQueue.stopPosition == getPosition()) {
            if (resetState) {
                pendingQuit = false
                playingQueue.stopPosition = -1
            }
            return true
        }
        return false
    }

    val stopPosition: Int
        get() = playingQueue.stopPosition

    fun setStopPosition(position: Int) {
        playingQueue.stopPosition = position
        if (playbackManager.isGaplessPlayback) {
            prepareNext()
        }
    }

    inner class MusicBinder : Binder() {
        val service: MusicService
            get() = this@MusicService
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
        internal const val CYCLE_REPEAT = ServiceEvent.BOOMING_PACKAGE_NAME + ".cyclerepeat"
        internal const val TOGGLE_SHUFFLE = ServiceEvent.BOOMING_PACKAGE_NAME + ".toggleshuffle"
        internal const val TOGGLE_FAVORITE = ServiceEvent.BOOMING_PACKAGE_NAME + ".togglefavorite"

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