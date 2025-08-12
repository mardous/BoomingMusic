package com.mardous.booming.core.model.shuffle

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