package com.mardous.booming.coil.model

import android.net.Uri
import com.mardous.booming.extensions.media.isArtistNameUnknown

class ArtistImage(
    val id: Long,
    val name: String,
    val coverUri: Uri,
    val isAlbumArtist: Boolean
) {
    val isNameUnknown = name.isArtistNameUnknown()

    override fun toString(): String {
        return buildString {
            append("ArtistImage{")
            append("id=$id,")
            append("name='$name',")
            append("coverUri=$coverUri,")
            append("isAlbumArtist=$isAlbumArtist")
            append("}")
        }
    }
}