package com.mardous.booming.core.model.sort

import com.mardous.booming.R

sealed class SortItem(val group: Int, val id: Int, val title: Int)

sealed class KeySortItem(id: Int, title: Int, val key: SortKey)
    : SortItem(group = 0, id = id, title = title) {

    object Title : KeySortItem(
        R.id.action_sort_order_az,
        R.string.sort_order_az,
        SortKey.AZ
    )

    object Album : KeySortItem(
        R.id.action_sort_order_album,
        R.string.sort_order_album,
        SortKey.Album
    )

    object Artist : KeySortItem(
        R.id.action_sort_order_artist,
        R.string.sort_order_artist,
        SortKey.Artist
    )

    object Duration : KeySortItem(
        R.id.action_sort_order_duration,
        R.string.sort_order_duration,
        SortKey.Duration
    )

    object Year : KeySortItem(
        R.id.action_sort_order_year,
        R.string.sort_order_year,
        SortKey.Year
    )

    object Track : KeySortItem(
        R.id.action_sort_order_track_list,
        R.string.sort_order_track_list,
        SortKey.Track
    )

    object SongCount : KeySortItem(
        R.id.action_sort_order_number_of_songs,
        R.string.sort_order_number_of_songs,
        SortKey.SongCount
    )

    object AlbumCount : KeySortItem(
        R.id.action_sort_order_number_of_albums,
        R.string.sort_order_number_of_albums,
        SortKey.AlbumCount
    )

    object DateAdded : KeySortItem(
        R.id.action_sort_order_date_added,
        R.string.sort_order_date_added,
        SortKey.DateAdded
    )

    object DateModified : KeySortItem(
        R.id.action_sort_order_date_modified,
        R.string.sort_order_date_modified,
        SortKey.DateModified
    )

    object FileName : KeySortItem(
        R.id.action_sort_order_file_name,
        R.string.sort_order_file_name,
        SortKey.FileName
    )
}

object DescendingItem : SortItem(
    group = 1,
    id = R.id.action_sort_order_descending,
    title = R.string.sort_order_descending
)