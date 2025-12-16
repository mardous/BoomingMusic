package com.mardous.booming.ui.component.compose

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_SURFACE_VIEW
import androidx.media3.ui.compose.modifiers.resizeWithContentScale
import androidx.media3.ui.compose.state.rememberPresentationState

@Composable
fun VideoPlayerScreen(context: Context, videoUrl: String, modifier: Modifier = Modifier) {
    var player by remember { mutableStateOf<Player?>(null) }

    LifecycleStartEffect(Unit) {
        player = initializePlayer(context, videoUrl)
        onStopOrDispose {
            player?.apply { release() }
            player = null
        }
    }

    player?.let { VideoPlayer(player = it, modifier = modifier) }
}

@Composable
private fun VideoPlayer(player: Player, modifier: Modifier = Modifier) {
    val presentationState = rememberPresentationState(player)

    Box(modifier) {
        // Always leave PlayerSurface to be part of the Compose tree because it will be initialised in
        // the process. If this composable is guarded by some condition, it might never become visible
        // because the Player will not emit the relevant event, e.g. the first frame being ready.
        PlayerSurface(
            player = player,
            surfaceType = SURFACE_TYPE_SURFACE_VIEW,
            modifier = Modifier.resizeWithContentScale(
                contentScale = ContentScale.Crop,
                sourceSizeDp = presentationState.videoSizeDp
            )
        )

        AnimatedVisibility(
            visible = presentationState.coverSurface,
            enter = fadeIn(tween(1000)),
            exit = fadeOut(tween(1000))
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(MaterialTheme.colorScheme.surface)
            )
        }
    }
}

private fun initializePlayer(context: Context, videoUrl: String): Player =
    ExoPlayer.Builder(context).build().apply {
        setMediaItem(MediaItem.fromUri(videoUrl.toUri()))
        repeatMode = Player.REPEAT_MODE_ONE
        playWhenReady = true
        volume = 0f
        prepare()
    }