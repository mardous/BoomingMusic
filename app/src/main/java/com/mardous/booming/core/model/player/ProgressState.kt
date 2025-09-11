package com.mardous.booming.core.model.player

import androidx.compose.runtime.Immutable
import com.mardous.booming.extensions.media.durationStr

@Immutable
class ProgressState(val progress: Long, val total: Long) {

    val mayUpdateUI = progress > -1 && total > -1

    val remainingTime: Long = (total - progress).coerceAtLeast(0L)

    val remainingTimeAsString: String
        get() = remainingTime.durationStr()

    val progressAsString: String
        get() = progress.durationStr()

    val totalAsString: String
        get() = total.durationStr()

    companion object {
        val Unspecified = ProgressState(0, 0)
    }
}