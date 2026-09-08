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

package com.mardous.booming.core.model.queue

import androidx.media3.common.C

/**
 * Manages the translation between visual queue positions and internal Media3 timeline indices.
 *
 * In Media3, the [androidx.media3.common.Timeline] maintains a fixed structural order of items, 
 * referred to as **indices**. However, when shuffle mode is enabled, the visual order in which 
 * items are presented to the user—the **position**—no longer matches their structural index.
 *
 * This class provides a centralized way to map between these two coordinate systems:
 *
 * 1. **Index**: The structural, internal index of a MediaItem in the [androidx.media3.common.Timeline].
 *    This typically only changes when items are added, removed, or moved structurally.
 * 2. **Position**: The dynamic, visual rank of a MediaItem in the current playback queue as seen 
 *    by the user. This changes when shuffle mode is toggled or the shuffle order is randomized.
 *
 * ### Example Scenario
 * Suppose we have a timeline with items: `[Song A (0), Song B (1), Song C (2)]`.
 * If shuffle mode is enabled, the queue might look like: `[Song B (1), Song C (2), Song A (0)]`.
 *
 * - The **Position 0** in the UI corresponds to **Index 1** (Song B).
 * - The **Index 0** (Song A) is now at **Position 2** in the UI.
 *
 * Through [getIndexForPosition], this class maps a visual rank to its timeline reference.
 * Through [getPositionForIndex], it maps a timeline reference back to its visual rank.
 *
 * @property current The current visual position in the queue.
 * @property indicesInTimeline An array where each element at `position` stores the corresponding structural `index`.
 */
data class QueuePosition(
    val current: Int,
    private val indicesInTimeline: IntArray
) {
    /** The visual position of the item immediately preceding the [current] one. */
    val previous: Int = current - 1
    
    /** The visual position of the item immediately following the [current] one. */
    val next: Int = current + 1

    /**
     * Creates a copy of this [QueuePosition] with the [current] position updated to 
     * match where the given [index] is located visually.
     * 
     * @param index The structural index to target.
     * @return A new [QueuePosition] instance.
     */
    fun setCurrentIndex(index: Int) =
        copy(current = getPositionForIndex(index))

    /**
     * Maps a structural timeline index to its current visual position in the queue.
     * 
     * @param index The internal timeline index.
     * @return The visual position (0-based), or -1 if the index is not in the queue.
     */
    fun getPositionForIndex(index: Int) =
        indicesInTimeline.indexOfFirst { it == index }

    /**
     * Maps a visual queue position to its corresponding structural timeline index.
     * 
     * @param position The visual rank in the queue.
     * @return The timeline index, or [C.INDEX_UNSET] if the position is out of bounds.
     */
    fun getIndexForPosition(position: Int) =
        indicesInTimeline.getOrElse(position) { C.INDEX_UNSET }

    companion object {
        /** Represents a position in an empty or uninitialized queue. */
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