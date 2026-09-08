package com.mardous.booming.core.appwidgets.config

import com.mardous.booming.R
import com.mardous.booming.core.appwidgets.WidgetData
import com.mardous.booming.data.model.ContentType
import com.mardous.booming.playback.library.MediaIDs

enum class SongSource(
    override val label: Int,
    override val requires: WidgetData,
    val contentType: ContentType,
    val mediaId: String
) : WidgetValue {
    Recent(
        R.string.widget_source_recent,
        WidgetData.RecentSongs,
        ContentType.History,
        MediaIDs.RECENT_SONGS
    ),
    Favourites(
        R.string.favorites_label,
        WidgetData.FavouriteSongs,
        ContentType.Favorites,
        MediaIDs.FAVORITES
    ),
    Top(
        R.string.shuffle_mode_most_played,
        WidgetData.TopSongs,
        ContentType.TopTracks,
        MediaIDs.TOP_TRACKS
    )
}
