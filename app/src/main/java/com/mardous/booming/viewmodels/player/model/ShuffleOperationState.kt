package com.mardous.booming.viewmodels.player.model

import com.mardous.booming.service.queue.SpecialShuffleMode

data class ShuffleOperationState(
    val mode: SpecialShuffleMode? = null,
    val status: Status = Status.Idle
) {
    val isIdle: Boolean
        get() = mode == null && status == Status.Idle

    enum class Status {
        Idle, InProgress
    }
}
