package com.mardous.booming.data.model

import androidx.media3.common.C

data class QueuePosition(
    val current: Int,
    private val indicesInTimeline: IntArray
) {

    val previous: Int = current - 1
    val next: Int = current + 1

    fun setCurrentIndex(index: Int) =
        copy(current = getPositionForIndex(index))

    fun getPositionForIndex(index: Int) =
        indicesInTimeline.indexOfFirst { it == index }

    fun getIndexForPosition(position: Int) =
        indicesInTimeline.getOrElse(position) { C.INDEX_UNSET }

    companion object {
        val Undefined = QueuePosition(C.INDEX_UNSET, IntArray(0))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as QueuePosition

        if (current != other.current) return false
        if (previous != other.previous) return false
        if (next != other.next) return false
        if (!indicesInTimeline.contentEquals(other.indicesInTimeline)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = current
        result = 31 * result + previous
        result = 31 * result + next
        result = 31 * result + indicesInTimeline.contentHashCode()
        return result
    }
}