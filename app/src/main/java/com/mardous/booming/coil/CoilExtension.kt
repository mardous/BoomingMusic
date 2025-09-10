package com.mardous.booming.coil

import android.widget.ImageView
import coil3.load
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.error
import coil3.request.placeholder
import com.mardous.booming.R
import com.mardous.booming.data.local.room.PlaylistWithSongs
import com.mardous.booming.data.model.Album
import com.mardous.booming.data.model.Artist
import com.mardous.booming.data.model.Song

val DEFAULT_ARTIST_IMAGE: Int = R.drawable.default_artist_art
val DEFAULT_SONG_IMAGE: Int = R.drawable.default_audio_art
val DEFAULT_ALBUM_IMAGE: Int = R.drawable.default_album_art

private typealias RequestBuilder = ImageRequest.Builder

fun RequestBuilder.onSuccess(receiver: (ImageRequest, SuccessResult) -> Unit) =
    listener(onSuccess = receiver)

fun RequestBuilder.songImage(song: Song?) = data(song)
    .placeholder(DEFAULT_SONG_IMAGE)
    .error(DEFAULT_SONG_IMAGE)

fun RequestBuilder.albumImage(album: Album?) = data(album)
    .placeholder(DEFAULT_ALBUM_IMAGE)
    .error(DEFAULT_ALBUM_IMAGE)

fun RequestBuilder.artistImage(artist: Artist?) = data(artist)
    .placeholder(DEFAULT_ARTIST_IMAGE)
    .error(DEFAULT_ARTIST_IMAGE)

fun RequestBuilder.playlistImage(playlist: PlaylistWithSongs?) = data(playlist)
    .placeholder(DEFAULT_SONG_IMAGE)
    .error(DEFAULT_SONG_IMAGE)

fun ImageView.songImage(
    song: Song?,
    builder: ImageRequest.Builder.() -> Unit = {}
) = load(song) {
    placeholder(DEFAULT_SONG_IMAGE)
    error(DEFAULT_SONG_IMAGE)
    builder()
}

fun ImageView.albumImage(
    album: Album?,
    builder: ImageRequest.Builder.() -> Unit = {}
) = load(album) {
    placeholder(DEFAULT_ALBUM_IMAGE)
    error(DEFAULT_ALBUM_IMAGE)
    builder()
}

fun ImageView.artistImage(
    artist: Artist?,
    builder: ImageRequest.Builder.() -> Unit = {}
) = load(artist) {
    placeholder(DEFAULT_ARTIST_IMAGE)
    error(DEFAULT_ARTIST_IMAGE)
    builder()
}

fun ImageView.playlistImage(
    playlist: PlaylistWithSongs?,
    builder: ImageRequest.Builder.() -> Unit = {}
) = load(playlist) {
    placeholder(DEFAULT_SONG_IMAGE)
    error(DEFAULT_SONG_IMAGE)
    builder()
}
