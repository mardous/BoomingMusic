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

package com.mardous.booming.ui

import android.view.MenuItem
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.mardous.booming.core.model.filesystem.FileSystemItem
import com.mardous.booming.data.local.room.PlaylistWithSongs
import com.mardous.booming.data.model.*

interface ISongCallback {
    fun songMenuItemClick(song: Song, menuItem: MenuItem, sharedElements: Array<Pair<View, String>>?): Boolean
    fun songsMenuItemClick(songs: List<Song>, menuItem: MenuItem)
}

interface IAlbumCallback {
    fun albumClick(album: Album, sharedElements: Array<Pair<View, String>>?)
    fun albumMenuItemClick(album: Album, menuItem: MenuItem, sharedElements: Array<Pair<View, String>>?): Boolean
    fun albumsMenuItemClick(albums: List<Album>, menuItem: MenuItem)
}

interface IArtistCallback {
    fun artistClick(artist: Artist, sharedElements: Array<Pair<View, String>>?)
    fun artistMenuItemClick(artist: Artist, menuItem: MenuItem, sharedElements: Array<Pair<View, String>>?): Boolean
    fun artistsMenuItemClick(artists: List<Artist>, menuItem: MenuItem)
}

interface IGenreCallback {
    fun genreClick(genre: Genre)
}

interface IYearCallback {
    fun yearClick(year: ReleaseYear)
    fun yearMenuItemClick(year: ReleaseYear, menuItem: MenuItem): Boolean
    fun yearsMenuItemClick(selection: List<ReleaseYear>, menuItem: MenuItem): Boolean
}

interface IFileCallback {
    fun fileClick(file: FileSystemItem)
    fun fileMenuItemClick(file: FileSystemItem, menuItem: MenuItem): Boolean
    fun filesMenuItemClick(selection: List<FileSystemItem>, menuItem: MenuItem): Boolean
}

interface IPlaylistCallback {
    fun playlistClick(playlist: PlaylistWithSongs)
    fun playlistMenuItemClick(playlist: PlaylistWithSongs, menuItem: MenuItem): Boolean
    fun playlistsMenuItemClick(playlists: List<PlaylistWithSongs>, menuItem: MenuItem)
}

interface ISearchCallback {
    fun songClick(song: Song, results: List<Any>)
    fun songMenuItemClick(song: Song, menuItem: MenuItem): Boolean
    fun albumClick(album: Album, sharedElements: Array<Pair<View, String>>)
    fun albumMenuItemClick(album: Album, menuItem: MenuItem, sharedElements: Array<Pair<View, String>>): Boolean
    fun artistClick(artist: Artist, sharedElements: Array<Pair<View, String>>)
    fun artistMenuItemClick(artist: Artist, menuItem: MenuItem, sharedElements: Array<Pair<View, String>>): Boolean
    fun playlistClick(playlist: PlaylistWithSongs)
    fun playlistMenuItemClick(playlist: PlaylistWithSongs, menuItem: MenuItem): Boolean
    fun genreClick(genre: Genre)
    fun onMultipleItemAction(menuItem: MenuItem, selection: List<Song>)
}

interface IHomeCallback {
    fun createSuggestionAdapter(suggestion: Suggestion): RecyclerView.Adapter<*>?
    fun suggestionClick(suggestion: Suggestion)
}