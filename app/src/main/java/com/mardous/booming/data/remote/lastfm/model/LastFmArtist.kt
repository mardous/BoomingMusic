package com.mardous.booming.data.remote.lastfm.model

import kotlinx.serialization.Serializable

@Serializable
class LastFmArtist(val artist: Artist?) {
    @Serializable
    class Artist(val bio: Bio?)

    @Serializable
    class Bio(val content: String?)
}