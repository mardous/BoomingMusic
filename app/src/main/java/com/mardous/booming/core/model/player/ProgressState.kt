package com.mardous.booming.core.model.player

import androidx.compose.runtime.Immutable
import com.mardous.booming.extensions.media.asReadableDuration

@Immutable
class ProgressState(val progress: Long, val total: Long) {

    val mayUpdateUI = progress > -1 && total > -1

    val remainingTime: Long = (total - progress).coerceAtLeast(0L)

    val remainingTimeAsString: String
        get() = remainingTime.asReadableDuration()

    val progressAsString: String
        get() = progress.asReadableDuration()

    val totalAsString: String
        get() = total.asReadableDuration()

    companion object {
        val Unspecified = ProgressState(0, 0)
    }
}