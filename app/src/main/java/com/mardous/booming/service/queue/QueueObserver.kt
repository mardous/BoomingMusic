package com.mardous.booming.service.queue

import com.mardous.booming.data.model.Song
import com.mardous.booming.service.playback.Playback

interface QueueObserver {
    fun queueChanged(queue: List<Song>, reason: QueueChangeReason) {}
    fun queuePositionChanged(position: Int, rePosition: Boolean) {}
    fun repeatModeChanged(repeatMode: Playback.RepeatMode) {}
    fun shuffleModeChanged(shuffleMode: Playback.ShuffleMode) {}
    fun songChanged(currentSong: Song, nextSong: Song) {}
}