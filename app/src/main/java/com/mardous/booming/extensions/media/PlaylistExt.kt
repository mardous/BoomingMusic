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

package com.mardous.booming.extensions.media

import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.mardous.booming.R
import com.mardous.booming.database.PlaylistEntity
import com.mardous.booming.service.constants.ServiceEvent

fun PlaylistEntity.isFavorites(context: Context) = playlistName == context.getString(R.string.favorites_label)

fun Context.refreshFavoriteState() {
    LocalBroadcastManager.getInstance(this)
        .sendBroadcast(Intent(ServiceEvent.FAVORITE_STATE_CHANGED))
}