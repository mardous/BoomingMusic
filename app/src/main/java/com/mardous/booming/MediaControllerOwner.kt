package com.mardous.booming

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.*
import com.google.common.util.concurrent.ListenableFuture
import com.mardous.booming.core.model.task.Event
import com.mardous.booming.playback.PlaybackService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalAtomicApi::class)
class MediaControllerOwner(
    private val context: Context,
    private var listener: MediaController.Listener?
) : MediaController.Listener, DefaultLifecycleObserver {

    private val currentState = AtomicReference(State.Idle)

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    private val _isConnected = MutableStateFlow(Event(false))
    val isConnected = _isConnected.asStateFlow()

    val state: State
        get() = currentState.load()

    enum class State {
        Idle, Connected, Connecting, Disconnected
    }

    fun get(): MediaController? = controller

    fun attachTo(lifecycleOwner: LifecycleOwner) {
        val lifecycle = lifecycleOwner.lifecycle
        if (lifecycle.currentState == Lifecycle.State.DESTROYED)
            return

        lifecycle.addObserver(this)
    }

    private fun getSafeListener() = if (state == State.Disconnected) null else listener

    fun addPlayerListener(listener: Player.Listener, lifecycle: Lifecycle) {
        if (state < State.Disconnected && lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
            controller?.addListener(listener)
            lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    controller?.removeListener(listener)
                }
            })
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        val movedToConnectingState = currentState.compareAndSet(State.Idle, State.Connecting) ||
                currentState.compareAndSet(State.Disconnected, State.Connecting)

        if (!movedToConnectingState) return

        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken)
            .setListener(this)
            .buildAsync()

        controllerFuture?.addListener({
            try {
                val instance = controllerFuture?.get()
                if (instance != null) {
                    if (currentState.compareAndSet(State.Connecting, State.Connected)) {
                        controller = instance
                        _isConnected.value = Event(true)
                    } else {
                        instance.release()
                    }
                }
            } catch (e: Exception) {
                Log.e("MediaControllerOwner", "Error connecting to the MediaSession", e)
                currentState.store(State.Disconnected)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    override fun onStop(owner: LifecycleOwner) {
        val oldState = currentState.exchange(State.Disconnected)
        if (oldState < State.Disconnected) {
            controller?.release()
            controller = null
            controllerFuture?.cancel(true)
            controllerFuture = null
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        listener = null
    }

    override fun onDisconnected(controller: MediaController) {
        if (state != State.Disconnected) return

        _isConnected.value = Event(false)
        listener?.onDisconnected(controller)
    }

    override fun onCustomLayoutChanged(controller: MediaController, layout: List<CommandButton>) {
        getSafeListener()?.onCustomLayoutChanged(controller, layout)
    }

    @UnstableApi
    override fun onMediaButtonPreferencesChanged(
        controller: MediaController,
        mediaButtonPreferences: List<CommandButton>
    ) {
        getSafeListener()?.onMediaButtonPreferencesChanged(controller, mediaButtonPreferences)
    }

    override fun onAvailableSessionCommandsChanged(
        controller: MediaController,
        commands: SessionCommands
    ) {
        getSafeListener()?.onAvailableSessionCommandsChanged(controller, commands)
    }

    override fun onCustomCommand(
        controller: MediaController,
        command: SessionCommand,
        args: Bundle
    ): ListenableFuture<SessionResult> {
        return getSafeListener()?.onCustomCommand(controller, command, args)
            ?: super.onCustomCommand(controller, command, args)
    }

    override fun onExtrasChanged(controller: MediaController, extras: Bundle) {
        getSafeListener()?.onExtrasChanged(controller, extras)
    }

    @UnstableApi
    override fun onSessionActivityChanged(
        controller: MediaController,
        sessionActivity: PendingIntent?
    ) {
        getSafeListener()?.onSessionActivityChanged(controller, sessionActivity)
    }

    @UnstableApi
    override fun onError(controller: MediaController, sessionError: SessionError) {
        getSafeListener()?.onError(controller, sessionError)
    }
}
