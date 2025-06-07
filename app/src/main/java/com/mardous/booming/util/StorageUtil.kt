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

package com.mardous.booming.util

import android.annotation.SuppressLint
import android.os.Environment
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import androidx.core.content.getSystemService
import com.mardous.booming.R
import com.mardous.booming.appContext
import com.mardous.booming.extensions.hasR
import com.mardous.booming.model.filesystem.StorageDevice
import com.mardous.booming.recordException
import kotlinx.io.IOException
import java.io.File
import java.lang.reflect.InvocationTargetException

object StorageUtil {

    private val _storageVolumes = mutableListOf<StorageDevice>()

    val storageVolumes: List<StorageDevice>
        get() = _storageVolumes

    init {
        refreshStorageVolumes()
    }

    fun refreshStorageVolumes(): List<StorageDevice> {
        _storageVolumes.clear()
        try {
            val context = appContext()
            val storageManager = context.getSystemService<StorageManager>()
                ?: return emptyList()

            for (sv in storageManager.storageVolumes) {
                if (sv.state != Environment.MEDIA_MOUNTED)
                    continue

                val path = try {
                    sv.getPathCompat()
                } catch (e: Exception) {
                    recordException(e)
                    continue
                }

                val description = sv.getDescription(context) ?: File(path).name
                val icon = if (sv.isRemovable && !sv.isPrimary) {
                    R.drawable.ic_sd_card_24dp
                } else {
                    R.drawable.ic_phone_android_24dp
                }
                _storageVolumes.add(StorageDevice(path, description, icon))
            }
        } catch (t: Throwable) {
            recordException(t)
        }
        return _storageVolumes
    }

    fun getStorageDevice(directory: File): StorageDevice? {
        return try {
            val canonicalPath = directory.canonicalPath
            storageVolumes.firstOrNull { it.file.canonicalPath == canonicalPath }
        } catch (e: IOException) {
            recordException(e)
            null
        }
    }

    @Throws(NoSuchMethodException::class, InvocationTargetException::class, IllegalAccessException::class)
    @SuppressLint("DiscouragedPrivateApi")
    private fun StorageVolume.getPathCompat(): String {
        return if (hasR()) {
            this.directory?.absolutePath ?: throw IllegalStateException("StorageVolume has no directory")
        } else {
            StorageVolume::class.java.getDeclaredMethod("getPath").invoke(this) as String
        }
    }
}
