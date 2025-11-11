package com.mardous.booming.core.model.shuffle

import com.mardous.booming.R

enum class SpecialShuffleMode(val iconRes: Int, val titleRes: Int, val descriptionRes: Int) {
    PureRandom(
        R.drawable.ic_shuffle_24dp,
        R.string.shuffle_mode_pure_random,
        R.string.shuffle_mode_pure_random_description
    ),
    MostPlayed(
        R.drawable.ic_trending_up_24dp,
        R.string.shuffle_mode_most_played,
        R.string.shuffle_mode_most_played_description
    ),
    MostPlayedArtists(
        R.drawable.ic_artist_24dp,
        R.string.shuffle_mode_most_played_artists,
        R.string.shuffle_mode_most_played_artists_description
    ),
    MostPlayedAlbums(
        R.drawable.ic_album_24dp,
        R.string.shuffle_mode_most_played_albums,
        R.string.shuffle_mode_most_played_albums_description
    ),
    FavoriteSongs(
        R.drawable.ic_favorite_outline_24dp,
        R.string.shuffle_mode_favorite_songs,
        R.string.shuffle_mode_favorite_songs_description
    ),
    ForgottenSongs(
        R.drawable.ic_trending_down_24dp,
        R.string.shuffle_mode_forgotten_songs,
        R.string.shuffle_mode_forgotten_songs_description
    ),
    RecentlyAdded(
        R.drawable.ic_library_add_24dp,
        R.string.shuffle_mode_recently_added,
        R.string.shuffle_mode_recently_added_description
    ),
    Combined(
        R.drawable.ic_shuffle_on_24dp,
        R.string.shuffle_mode_combined,
        R.string.shuffle_mode_combined_description
    )
}