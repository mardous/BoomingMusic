package com.mardous.booming.data.remote.lastfm.model

import kotlinx.serialization.Serializable

@Serializable
class LastFmAlbum(val album: Album?) {
    @Serializable
    class Album(val wiki: Wiki?)

    @Serializable
    class Wiki(val content: String?)
}