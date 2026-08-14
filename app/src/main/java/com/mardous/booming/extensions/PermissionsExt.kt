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

package com.mardous.booming.extensions

import android.Manifest.permission.BLUETOOTH_CONNECT
import android.Manifest.permission.POST_NOTIFICATIONS
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_AUDIO
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE

const val EXTRA_IS_PERMISSION_REQUEST = "is_permission_request"

fun getNecessaryPermissions() = getStoragePermissions() + getNearbyDevicesPermissions()

fun getStoragePermissions() = buildSet {
    if (hasT()) {
        add(READ_MEDIA_AUDIO)
        add(POST_NOTIFICATIONS)
    } else {
        add(READ_EXTERNAL_STORAGE)
    }
    if (!hasR()) {
        add(WRITE_EXTERNAL_STORAGE)
    }
}

fun getImagesPermission() = if (hasT()) setOf(READ_MEDIA_IMAGES) else emptySet()

fun getNearbyDevicesPermissions() = if (hasS()) setOf(BLUETOOTH_CONNECT) else emptySet()