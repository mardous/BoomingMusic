/*
 * Copyright (c) 2026 Christians Martínez Alvarado
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

package com.mardous.booming.playback

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.mardous.booming.util.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.time.Duration.Companion.milliseconds

@OptIn(UnstableApi::class)
class CrossfadeManager(
    private val context: Context,
    private val primaryPlayer: AdvancedForwardingPlayer,
    private val scope: CoroutineScope
) {
    private var secondaryExoPlayer: ExoPlayer? = null
    private var isCrossfading = false
    private var lastCrossfadedFromIndex = -1

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            lastCrossfadedFromIndex = -1
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            lastCrossfadedFromIndex = -1
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            secondaryExoPlayer?.let { secondary ->
                if (isPlaying) {
                    if (!secondary.isPlaying && secondary.playbackState != Player.STATE_ENDED) {
                        secondary.play()
                    }
                } else {
                    if (secondary.isPlaying) {
                        secondary.pause()
                    }
                }
            }
        }
    }

    init {
        primaryPlayer.addListener(playerListener)
        scope.launch {
            while (true) {
                delay(100.milliseconds)
                if (Preferences.enableCrossfade && primaryPlayer.isPlaying && !isCrossfading) {
                    val duration = primaryPlayer.duration
                    val position = primaryPlayer.currentPosition
                    val fadeDuration = Preferences.crossfadeDuration

                    if (duration != C.TIME_UNSET && duration > (fadeDuration + 1000L)) {
                        val remaining = duration - position
                        val currentIndex = primaryPlayer.currentMediaItemIndex
                        val nextIndex = primaryPlayer.nextMediaItemIndex

                        if (remaining <= fadeDuration && nextIndex != C.INDEX_UNSET && currentIndex != lastCrossfadedFromIndex) {
                            lastCrossfadedFromIndex = currentIndex
                            executeCrossfade(nextIndex, fadeDuration)
                        }
                    }
                }
            }
        }
    }

    private fun executeCrossfade(nextIndex: Int, fadeDuration: Long) {
        isCrossfading = true
        val currentItem = primaryPlayer.getMediaItemAt(primaryPlayer.currentMediaItemIndex)

        scope.launch {
            // Secondary player (Player 2) takes over playing Track 1 without requesting AudioFocus,
            // preventing AudioFocus ducking or audio drop when Player 1 advances.
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build()

            val secondary = ExoPlayer.Builder(context)
                .setAudioAttributes(audioAttributes, false)
                .build().also {
                    secondaryExoPlayer = it
                    it.setMediaItem(currentItem)
                    it.prepare()
                    it.seekTo(primaryPlayer.currentPosition)
                    it.volume = 1f
                    it.playWhenReady = primaryPlayer.isPlaying
                }

            // Wait until Secondary player is fully READY and outputting Track 1 audio at 1f
            var waited = 0
            while ((secondary.playbackState != Player.STATE_READY || !secondary.isPlaying) && waited < 1000 && isCrossfading) {
                delay(10.milliseconds)
                waited += 10
            }

            // Quietly advance Primary player (Player 1) to Track 2 at 0L at 0f volume.
            // Screen details update immediately to Track 2 at 0s, and Track 2 plays continuously without restart.
            primaryPlayer.volume = 0f
            primaryPlayer.seekTo(nextIndex, 0L)
            primaryPlayer.play()

            val steps = 30
            val interval = fadeDuration / steps
            val delta = 1f / steps
            var progress = 0f

            // Equal-power crossfade:
            // Secondary (Track 1) fades cos(progress * PI/2) -> 1f to 0f
            // Primary (Track 2) fades sin(progress * PI/2) -> 0f to 1f
            while (progress < 1f && isCrossfading) {
                if (primaryPlayer.isPlaying) {
                    progress = (progress + delta).coerceAtMost(1f)
                    val angle = progress * (Math.PI / 2.0)
                    val secVol = cos(angle).toFloat().coerceIn(0f, 1f)
                    val primVol = sin(angle).toFloat().coerceIn(0f, 1f)

                    if (secondary.playbackState != Player.STATE_ENDED) {
                        secondary.volume = secVol
                    }
                    primaryPlayer.volume = primVol
                }
                delay(interval.milliseconds)
            }

            // Cleanup: Primary player is ALREADY playing Track 2 smoothly at ~3s with volume 1f.
            // NO RESTART OR SECONDARY SEEK OCCURS!
            primaryPlayer.volume = 1f
            secondary.stop()
            secondary.release()
            secondaryExoPlayer = null
            isCrossfading = false
        }
    }

    fun release() {
        primaryPlayer.removeListener(playerListener)
        isCrossfading = false
        secondaryExoPlayer?.release()
        secondaryExoPlayer = null
    }
}