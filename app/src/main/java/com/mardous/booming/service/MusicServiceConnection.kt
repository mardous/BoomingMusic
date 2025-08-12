package com.mardous.booming.service

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.mardous.booming.core.model.MediaEvent
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class MusicServiceConnection(context: Context, serviceComponent: ComponentName) {

    private val _mediaSessionEvent = MutableSharedFlow<MediaEvent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val mediaSessionEvent = _mediaSessionEvent.asSharedFlow()

    private val _playbackState = MutableStateFlow(EMPTY_PLAYBACK_STATE)
    val playbackState = _playbackState.asStateFlow()
    val currentPlaybackState get() = playbackState.value.state

    private val _isConnected = MutableStateFlow(false)
    val isConnected = _isConnected.asStateFlow()

    private val mediaBrowserConnectionCallback = MediaBrowserConnectionCallback(context)
    private val mediaBrowser = MediaBrowserCompat(
        context,
        serviceComponent,
        mediaBrowserConnectionCallback,
        null
    ).apply { connect() }

    private val mediaControllerCallback = MediaControllerCallback()
    private var _mediaController: MediaControllerCompat? = null

    val transportControls: MediaControllerCompat.TransportControls?
        get() = _mediaController?.transportControls

    private inner class MediaBrowserConnectionCallback(private val context: Context) :
        MediaBrowserCompat.ConnectionCallback() {

        override fun onConnected() {
            // Get a MediaController for the MediaSession.
            _mediaController = MediaControllerCompat(context, mediaBrowser.sessionToken).apply {
                registerCallback(mediaControllerCallback)
            }
            _isConnected.value = true
        }

        override fun onConnectionSuspended() {
            _mediaController = null
            _isConnected.value = false
        }

        override fun onConnectionFailed() {
            _mediaController = null
            _isConnected.value = false
        }
    }

    private inner class MediaControllerCallback : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            _playbackState.value = (state ?: EMPTY_PLAYBACK_STATE)
        }

        override fun onSessionReady() {
            if (playbackState.value == EMPTY_PLAYBACK_STATE) {
                _playbackState.value = _mediaController?.playbackState ?: EMPTY_PLAYBACK_STATE
            }
        }

        override fun onSessionEvent(event: String?, extras: Bundle?) {
            val mediaEvent = MediaEvent.fromSessionEvent(event)
            if (mediaEvent != null) {
                _mediaSessionEvent.tryEmit(mediaEvent)
            }
        }

        override fun onSessionDestroyed() {
            mediaBrowserConnectionCallback.onConnectionSuspended()
        }
    }

    companion object {
        private val EMPTY_PLAYBACK_STATE = PlaybackStateCompat.Builder()
            .setState(PlaybackStateCompat.STATE_NONE, 0, 0f)
            .build()
    }
}