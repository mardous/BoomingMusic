/*
 * Copyright (c) 2024 Christians Martínez Alvarado
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mardous.booming.extensions.navigation

import android.view.View
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.NavGraph
import androidx.navigation.contains
import androidx.navigation.findNavController
import androidx.navigation.fragment.FragmentNavigator
import androidx.navigation.fragment.FragmentNavigatorExtras
import com.mardous.booming.data.model.Album
import com.mardous.booming.data.model.Artist
import com.mardous.booming.data.model.ContentType
import com.mardous.booming.data.model.Folder
import com.mardous.booming.data.model.Genre
import com.mardous.booming.data.model.Song
import com.mardous.booming.ui.screen.library.albums.AlbumDetailFragmentArgs
import com.mardous.booming.ui.screen.library.artists.ArtistDetailFragmentArgs
import com.mardous.booming.ui.screen.library.folders.FolderDetailFragmentArgs
import com.mardous.booming.ui.screen.library.genres.GenreDetailFragmentArgs
import com.mardous.booming.ui.screen.info.PlayInfoFragmentArgs
import com.mardous.booming.ui.screen.info.SongDetailFragmentArgs
import com.mardous.booming.ui.screen.other.DetailListFragmentArgs
import com.mardous.booming.ui.screen.library.playlists.PlaylistDetailFragmentArgs
import com.mardous.booming.ui.screen.library.search.SearchFragmentArgs
import com.mardous.booming.core.model.*
import com.mardous.booming.data.SearchFilter
import com.mardous.booming.util.Preferences

fun playInfoArgs(album: Album) =
    PlayInfoFragmentArgs.Builder(false, album.id, null)
        .build()
        .toBundle()

fun playInfoArgs(artist: Artist) =
    if (artist.isAlbumArtist) {
        PlayInfoFragmentArgs.Builder(true, -1, artist.name)
            .build()
            .toBundle()
    } else {
        PlayInfoFragmentArgs.Builder(true, artist.id, null)
            .build()
            .toBundle()
    }

fun detailArgs(type: ContentType) =
    DetailListFragmentArgs.Builder(type)
        .build()
        .toBundle()

fun songDetailArgs(song: Song) =
    SongDetailFragmentArgs.Builder(song)
        .build()
        .toBundle()

fun genreDetailArgs(genre: Genre) =
    GenreDetailFragmentArgs.Builder(genre)
        .build()
        .toBundle()

fun folderDetailArgs(folder: Folder) =
    FolderDetailFragmentArgs.Builder(folder.filePath)
        .build()
        .toBundle()

fun playlistDetailArgs(playlistId: Long) =
    PlaylistDetailFragmentArgs.Builder(playlistId)
        .build()
        .toBundle()

fun albumDetailArgs(albumId: Long) =
    AlbumDetailFragmentArgs.Builder(albumId)
        .build()
        .toBundle()

fun artistDetailArgs(artist: Artist) =
    if (artist.isAlbumArtist) artistDetailArgs(-1, artist.name)
    else artistDetailArgs(artist.id, null)

fun artistDetailArgs(album: Album) =
    if (Preferences.onlyAlbumArtists && !album.albumArtistName.isNullOrEmpty())
        artistDetailArgs(-1, album.albumArtistName)
    else artistDetailArgs(album.artistId, null)

fun artistDetailArgs(song: Song) =
    if (Preferences.onlyAlbumArtists && !song.albumArtistName.isNullOrEmpty())
        artistDetailArgs(-1, song.albumArtistName)
    else artistDetailArgs(song.artistId, null)

fun artistDetailArgs(artistId: Long, artistName: String? = null) =
    ArtistDetailFragmentArgs.Builder(artistId, artistName)
        .build()
        .toBundle()

fun searchArgs(filter: SearchFilter? = null, query: String? = null) =
    SearchFragmentArgs.Builder(query, filter)
        .build()
        .toBundle()

fun Fragment.findActivityNavController(@IdRes id: Int): NavController {
    return requireActivity().findNavController(id)
}

fun NavGraph.isValidCategory(id: Int): Boolean {
    return CategoryInfo.Category.entries.any { it.id == id } && this.contains(id)
}

fun <T : View> Array<Pair<T, String>>?.asFragmentExtras(): FragmentNavigator.Extras {
    if (isNullOrEmpty()) {
        return FragmentNavigator.Extras.Builder().build()
    }
    return FragmentNavigatorExtras(*this)
}