package com.mardous.booming.coil.model

import android.content.ContentResolver
import android.net.Uri
import android.provider.MediaStore.Audio.AudioColumns
import com.mardous.booming.data.local.repository.RealSongRepository

class AudioCover(
    val albumId: Long,
    val uri: Uri,
    val path: String,
    val lastModified: Long,
    val isIgnoreMediaStore: Boolean,
    val isUseFolderArt: Boolean,
    val isAlbum: Boolean
) {

    val isComplete: Boolean = albumId != -1L && path.isNotEmpty()

    constructor(uri: Uri, isIgnoreMediaStore: Boolean, isUseFolderArt: Boolean) :
            this(-1, uri, "", -1, isIgnoreMediaStore, isUseFolderArt, false)

    fun getComplete(contentResolver: ContentResolver): AudioCover {
        val completeCover = if (isComplete) this else {
            val id = uri.path?.substringAfterLast("/", "")
            if (id.isNullOrEmpty() || id == "-1") null else {
                contentResolver.query(
                    RealSongRepository.getAudioContentUri(),
                    arrayOf(AudioColumns.ALBUM_ID, AudioColumns.DATA, AudioColumns.DATE_MODIFIED),
                    "${AudioColumns._ID} = ?",
                    arrayOf(id),
                    null
                )?.use { c ->
                    if (c.moveToFirst()) {
                        val albumId = c.getLong(c.getColumnIndexOrThrow(AudioColumns.ALBUM_ID))
                        val filePath = c.getString(c.getColumnIndexOrThrow(AudioColumns.DATA))
                        val dateModified = c.getLong(c.getColumnIndexOrThrow(AudioColumns.DATE_MODIFIED))
                        AudioCover(
                            albumId = albumId,
                            uri = uri,
                            path = filePath,
                            lastModified = dateModified,
                            isIgnoreMediaStore = isIgnoreMediaStore,
                            isUseFolderArt = isUseFolderArt,
                            isAlbum = isAlbum
                        )
                    } else null
                }
            }
        }
        return requireNotNull(completeCover)
    }

    override fun toString(): String {
        return buildString {
            append("AudioCover{")
            append("albumId=$albumId,")
            append("uri=$uri,")
            append("path='$path',")
            append("lastModified=$lastModified,")
            append("isIgnoreMediaStore=$isIgnoreMediaStore,")
            append("isUseFolderArt=$isUseFolderArt,")
            append("isAlbum=$isAlbum")
            append("}")
        }
    }
}