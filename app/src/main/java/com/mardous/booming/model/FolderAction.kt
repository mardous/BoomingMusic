/*
 * Copyright (c) 2025 Christians Martínez Alvarado
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

package com.mardous.booming.model

import androidx.annotation.IdRes
import com.mardous.booming.R

enum class FolderAction(@IdRes val id: Int, val preferenceValue: String) {
    Play(R.id.action_play, "play"),
    ShufflePlay(R.id.action_shuffle_play, "shuffle_play"),
    QueueNext(R.id.action_play_next, "queue_next"),
    Queue(R.id.action_add_to_playing_queue, "queue"),
    AddToPlaylist(R.id.action_add_to_playlist, "add_to_playlist"),
    Delete(R.id.action_delete_from_device, "delete")
}

fun Collection<FolderAction>.isPresent(id: Int) = any { it.id == id }