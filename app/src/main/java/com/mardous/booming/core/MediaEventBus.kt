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

package com.mardous.booming.core

import com.mardous.booming.core.model.MediaEvent
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * A shared event bus for multimedia-related events across the application.
 *
 * This bus allows different components (ViewModels, Fragments, etc.) to post and subscribe
 * to [MediaEvent]s without being directly coupled to each other.
 */
class MediaEventBus {
    private val _eventFlow = MutableSharedFlow<MediaEvent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val eventFlow = _eventFlow.asSharedFlow()

    /**
     * Posts a new [MediaEvent] to the bus.
     *
     * @param event The event to broadcast.
     */
    fun postEvent(event: MediaEvent) {
        _eventFlow.tryEmit(event)
    }
}
