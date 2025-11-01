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

package com.mardous.booming.data.local

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.util.Log
import androidx.annotation.WorkerThread
import com.mardous.booming.data.local.repository.RealSongRepository.Companion.getAudioContentUri
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

/**
 * @author Christians M. A. (mardous)
 */
class MediaQueryDispatcher(val uri: Uri = getAudioContentUri()) : KoinComponent {

    private var projection: Array<String>? = null
    private var selection: String? = null
    private var selectionArguments: Array<String>? = null
    private var sortOrder: String? = null

    fun withColumns(vararg projection: String): MediaQueryDispatcher =
        apply { this.projection = arrayOf(*projection) }

    fun setProjection(projection: Array<String>?): MediaQueryDispatcher =
        apply { this.projection = projection }

    fun setSelection(selection: String?): MediaQueryDispatcher =
        apply { this.selection = selection }

    fun setSelectionArguments(selectionArguments: Array<String>?): MediaQueryDispatcher =
        apply { this.selectionArguments = selectionArguments }

    fun setSortOrder(sortOrder: String?): MediaQueryDispatcher =
        apply { this.sortOrder = sortOrder }

    fun addSelection(selection: String?, mode: String = "AND"): MediaQueryDispatcher =
        apply {
            if (!selection.isNullOrBlank()) {
                if (this.selection.isNullOrEmpty()) {
                    this.selection = selection
                } else this.selection += " $mode $selection"
            }
        }

    fun addArguments(vararg newArguments: String): MediaQueryDispatcher =
        apply {
            if (newArguments.isNotEmpty()) {
                val currentSelectionArguments = this.selectionArguments
                if (currentSelectionArguments.isNullOrEmpty()) {
                    this.selectionArguments = arrayOf(*newArguments)
                } else this.selectionArguments = currentSelectionArguments + newArguments
            }
        }

    @WorkerThread
    fun dispatch(): Cursor? {
        try {
            return get<ContentResolver>().query(uri, projection, selection, selectionArguments, sortOrder)
        } catch (e: IllegalArgumentException) {
            Log.e("QueryDispatcher", "Couldn't dispatch media query", e)
        }
        return null
    }
}