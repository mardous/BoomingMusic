package com.mardous.booming.coil

import android.content.Context
import android.widget.ImageView
import coil3.load
import coil3.request.ImageRequest
import coil3.request.error
import coil3.request.placeholder
import com.mardous.booming.R
import com.mardous.booming.data.local.room.PlaylistWithSongs
import com.mardous.booming.data.model.Album
import com.mardous.booming.data.model.Artist
import com.mardous.booming.data.model.Song
import com.mardous.booming.ui.component.views.getPlaceholderDrawable

val DEFAULT_ARTIST_IMAGE: Int = R.drawable.ic_artist_24dp
val DEFAULT_SONG_IMAGE: Int = R.drawable.ic_music_note_24dp
val DEFAULT_ALBUM_IMAGE: Int = R.drawable.ic_album_24dp
val DEFAULT_PLAYLIST_IMAGE: Int = R.drawable.ic_playlist_play_24dp
val DEFAULT_GENRE_IMAGE: Int = R.drawable.ic_radio_24dp
val DEFAULT_YEAR_IMAGE: Int = R.drawable.ic_event_24dp

private typealias RequestBuilder = ImageRequest.Builder

fun RequestBuilder.placeholderDrawableRes(context: Context, foregroundRes: Int) =
    placeholder(context.getPlaceholderDrawable(foregroundRes))
        .error(context.getPlaceholderDrawable(foregroundRes))

fun ImageView.songImage(
    song: Song?,
    builder: ImageRequest.Builder.() -> Unit = {}
) = load(song) {
    placeholderDrawableRes(context, DEFAULT_SONG_IMAGE)
    builder()
}

fun ImageView.albumImage(
    album: Album?,
    builder: ImageRequest.Builder.() -> Unit = {}
) = load(album) {
    placeholderDrawableRes(context, DEFAULT_ALBUM_IMAGE)
    builder()
}

fun ImageView.artistImage(
    artist: Artist?,
    builder: ImageRequest.Builder.() -> Unit = {}
) = load(artist) {
    placeholderDrawableRes(context, DEFAULT_ARTIST_IMAGE)
    builder()
}

fun ImageView.playlistImage(
    playlist: PlaylistWithSongs?,
    builder: ImageRequest.Builder.() -> Unit = {}
) = load(playlist) {
    placeholderDrawableRes(context, DEFAULT_PLAYLIST_IMAGE)
    builder()
}
