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

package com.mardous.booming.extensions.files

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.mardous.booming.R
import com.mardous.booming.data.model.Song
import com.mardous.booming.extensions.showToast
import java.io.File

private const val TAG = "SAF"

fun File.isSAFRequired(): Boolean = !canWrite()

fun Song.isSAFRequiredForSong(): Boolean = data.isSAFRequiredForPath()

fun String.isSAFRequiredForPath(): Boolean = File(this).isSAFRequired()

fun List<Song>.isSAFRequiredForSongs(): Boolean = any { it.isSAFRequiredForSong() }

fun Context.saveTreeUri(uri: Uri?): Uri? {
    if (uri == null) return null
    contentResolver.takePersistableUriPermission(
        uri,
        Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
    )
    return uri
}

fun Context.getWritablePersistedUris(): List<Uri> =
    contentResolver.persistedUriPermissions
        .filter { it.isWritePermission }
        .map { it.uri }

fun Context.isSAFAccessGranted(): Boolean = getWritablePersistedUris().isNotEmpty()

fun Context.findSAFDocument(path: String): Uri? {
    val segments = path.split("/").toMutableList()
    return getWritablePersistedUris().firstNotNullOfOrNull { root ->
        DocumentFile.fromTreeUri(this, root)?.findDocument(segments)
    }
}

private fun DocumentFile.findDocument(segments: MutableList<String>): Uri? {
    for (file in listFiles()) {
        val name = file.name ?: continue
        val index = segments.indexOf(name)
        if (index == -1) continue

        if (file.isDirectory) {
            val remaining = segments.toMutableList()
            remaining.removeAt(index)
            return file.findDocument(remaining)
        }

        if (file.isFile && index == segments.lastIndex) {
            return file.uri
        }
    }
    return null
}

fun Context.deleteUsingSAF(path: String): Boolean {
    if (!path.isSAFRequiredForPath()) {
        return try {
            File(path).delete()
        } catch (e: Exception) {
            Log.e(TAG, "delete: Error deleting file $path", e)
            false
        }
    }

    val uri = findSAFDocument(path)
    if (uri == null) {
        showToast(getString(R.string.saf_error_uri))
        Log.e(TAG, "delete: Can't get SAF URI")
        return false
    }

    return try {
        DocumentsContract.deleteDocument(contentResolver, uri)
    } catch (e: Exception) {
        showToast(getString(R.string.saf_delete_failed, e.localizedMessage))
        Log.e(TAG, "delete: Failed to delete a file descriptor provided by SAF", e)
        false
    }
}
