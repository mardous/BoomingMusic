package com.mardous.booming.viewmodels.player.model

import androidx.compose.runtime.Immutable
import com.mardous.booming.extensions.media.durationStr

@Immutable
class PlayerProgress(val progress: Long, val total: Long) {

    val mayUpdateUI = progress > -1 && total > -1

    val remainingTime: Long = (total - progress).coerceAtLeast(0L)

    val remainingTimeAsString: String
        get() = remainingTime.durationStr()

    val progressAsString: String
        get() = progress.durationStr()

    val totalAsString: String
        get() = total.durationStr()

    companion object {
        val Unspecified = PlayerProgress(0, 0)
    }
}