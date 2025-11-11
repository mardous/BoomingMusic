package com.mardous.booming.data.mapper

import android.content.Context
import android.provider.MediaStore
import com.mardous.booming.R
import com.mardous.booming.data.SearchFilter
import com.mardous.booming.data.local.room.PlaylistEntity
import com.mardous.booming.data.local.search.BasicSearchFilter
import com.mardous.booming.data.local.search.LastAddedSearchFilter
import com.mardous.booming.data.local.search.SmartSearchFilter
import com.mardous.booming.data.model.*
import com.mardous.booming.data.model.search.FilterSelection
import com.mardous.booming.data.model.search.SearchQuery
import com.mardous.booming.extensions.media.displayName

fun Album.searchFilter(context: Context) =
    SmartSearchFilter(
        context.getString(R.string.search_album_x_label, name), null,
        FilterSelection(
            SearchQuery.FilterMode.Songs,
            MediaStore.Audio.AudioColumns.TITLE,
            MediaStore.Audio.AudioColumns.ALBUM_ID + "=?",
            id.toString()
        )
    )

fun Artist.searchFilter(context: Context): SmartSearchFilter {
    return if (isAlbumArtist) {
        SmartSearchFilter(
            context.getString(R.string.search_artist_x_label, displayName()), null,
            FilterSelection(
                SearchQuery.FilterMode.Songs,
                MediaStore.Audio.AudioColumns.TITLE,
                "${MediaStore.Audio.AudioColumns.ALBUM_ARTIST}=?",
                name
            ),
            FilterSelection(
                SearchQuery.FilterMode.Albums,
                MediaStore.Audio.AudioColumns.ALBUM,
                "${MediaStore.Audio.AudioColumns.ALBUM_ARTIST}=?",
                name
            )
        )
    } else {
        SmartSearchFilter(
            context.getString(R.string.search_artist_x_label, displayName()), null,
            FilterSelection(
                SearchQuery.FilterMode.Songs,
                MediaStore.Audio.AudioColumns.TITLE,
                "${MediaStore.Audio.AudioColumns.ARTIST_ID}=?",
                id.toString()
            ),
            FilterSelection(
                SearchQuery.FilterMode.Albums,
                MediaStore.Audio.AudioColumns.ALBUM,
                "${MediaStore.Audio.AudioColumns.ARTIST_ID}=?",
                id.toString()
            )
        )
    }
}

fun Folder.searchFilter(context: Context): SearchFilter =
    BasicSearchFilter(
        context.getString(R.string.search_in_folder_x, fileName),
        BasicSearchFilter.Argument(filePath, BasicSearchFilter.Argument.FOLDER)
    )

fun Genre.searchFilter(context: Context): SearchFilter =
    BasicSearchFilter(
        context.getString(R.string.search_from_x_label, name),
        BasicSearchFilter.Argument(id, BasicSearchFilter.Argument.GENRE)
    )

fun ReleaseYear.searchFilter(context: Context): SearchFilter =
    BasicSearchFilter(
        context.getString(R.string.search_year_x_label, name),
        BasicSearchFilter.Argument(year, BasicSearchFilter.Argument.YEAR)
    )

fun PlaylistEntity.searchFilter(context: Context): SearchFilter =
    BasicSearchFilter(
        context.getString(R.string.search_list_x_label, playlistName),
        BasicSearchFilter.Argument(playListId, BasicSearchFilter.Argument.PLAYLIST)
    )

fun lastAddedSearchFilter(context: Context): SearchFilter =
    LastAddedSearchFilter(context.getString(R.string.search_last_added))