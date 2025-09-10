package com.mardous.booming.coil.model

import android.net.Uri

class AudioCover(
    val albumId: Long,
    val uri: Uri,
    val path: String,
    val lastModified: Long,
    val isIgnoreMediaStore: Boolean,
    val isUseFolderArt: Boolean,
    val isAlbum: Boolean
) {
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