/*
 * Copyright (c) 2024 Christians Martínez Alvarado
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

package com.mardous.booming.core.model.action

import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import com.mardous.booming.R

enum class QueueQuickAction(
    @IdRes val menuItemId: Int,
    @DrawableRes val iconRes: Int,
    @StringRes val titleRes: Int
) {
    Save(R.id.action_save_playing_queue, R.drawable.ic_save_24dp, R.string.save_queue),
    Clear(R.id.action_clear_playing_queue, R.drawable.ic_clear_all_24dp, R.string.clear_queue),
    Shuffle(R.id.action_shuffle_queue, R.drawable.ic_shuffle_24dp, R.string.shuffle_queue),
    ShowCurrentTrack(R.id.action_move_to_current_track, R.drawable.ic_queue_next_24dp, R.string.action_move_to_current_track);
}