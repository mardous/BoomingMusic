package com.mardous.booming.core.sort

import android.content.SharedPreferences
import android.icu.text.Collator
import android.view.Menu
import android.view.MenuItem
import androidx.core.content.edit
import com.mardous.booming.R
import com.mardous.booming.core.model.filesystem.FileSystemItem
import com.mardous.booming.core.model.sort.DescendingItem
import com.mardous.booming.core.model.sort.KeySortItem
import com.mardous.booming.core.model.sort.SortItem
import com.mardous.booming.core.model.sort.SortKey
import com.mardous.booming.data.local.room.PlaylistWithSongs
import com.mardous.booming.data.model.*
import com.mardous.booming.extensions.media.albumArtistName
import com.mardous.booming.extensions.media.asReadableTrackNumber
import com.mardous.booming.extensions.media.normalizeForSorting
import com.mardous.booming.util.Preferences
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import java.util.Locale

sealed class SortMode(
    id: String,
    private val defaults: Pair<SortKey, Boolean>,
    private val items: List<SortItem>
) : KoinComponent {

    protected val collator: Collator
        get() = sortingCollator()

    val ignoreArticles: Boolean
        get() = Preferences.ignoreArticlesWhenSorting

    private val key = "${id}_sort_order"
    open var selectedKey: SortKey
        get() = get<SharedPreferences>().getSortKey(key, defaults.first)
        protected set(newKey) {
            get<SharedPreferences>().edit { putString(key, newKey.value) }
        }

    private val descending = "${id}_descending"
    open var selectedDescending: Boolean
        get() = get<SharedPreferences>().getBoolean(descending, defaults.second)
        protected set(newDescending) {
            get<SharedPreferences>().edit { putBoolean(descending, newDescending) }
        }

    fun createMenu(menu: Menu, hasSubMenu: Boolean = true) {
        if (items.isEmpty()) return

        val subMenuItem = menu.findItem(R.id.action_sort_order)
        val sortMenu = if (hasSubMenu) {
            if (subMenuItem != null) {
                subMenuItem.subMenu
            } else {
                menu.addSubMenu(Menu.NONE, R.id.action_sort_order, Menu.NONE, R.string.action_sort_order)
            } ?: return
        } else menu

        sortMenu.clear()
        items.forEachIndexed { index, item ->
            sortMenu.add(item.group, item.id, index, item.title)
        }

        sortMenu.setGroupCheckable(0, true, true)
        prepareMenu(sortMenu)
    }

    fun prepareMenu(menu: Menu) {
        if (items.isEmpty()) return

        val menu = menu.findItem(R.id.action_sort_order)?.subMenu ?: menu
        items.forEach {
            when (it) {
                is KeySortItem -> if (it.key == selectedKey) menu.findItem(it.id)?.isChecked = true
                is DescendingItem -> menu.findItem(it.id)?.apply {
                    isCheckable = true
                    isChecked = selectedDescending
                }
            }
        }
    }

    fun sortItemSelected(menuItem: MenuItem): Boolean {
        if (items.isEmpty()) return false

        return when(val selectedItem = items.find { it.id == menuItem.itemId }) {
            is KeySortItem -> {
                menuItem.isChecked = true
                selectedKey = selectedItem.key
                true
            }
            is DescendingItem -> {
                menuItem.isChecked = !menuItem.isChecked
                selectedDescending = menuItem.isChecked
                true
            }
            else -> false
        }
    }

    protected fun String.normalize(language: String = Locale.getDefault().language): String {
        return normalizeForSorting(ignoreArticles, language)
    }

    private fun SharedPreferences.getSortKey(key: String, default: SortKey): SortKey {
        val value = getString(key, null)
        return SortKey.entries.firstOrNull { it.value == value } ?: default
    }
}

private fun <T, K : Comparable<K>> List<T>.sortedWithTiebreak(
    tiebreak: Comparator<T>,
    selector: (T) -> K
): List<T> = sortedWith(compareBy(selector).then(tiebreak))

sealed class SongSortMode(
    id: String,
    defaults: Pair<SortKey, Boolean>,
    items: List<SortItem>
) : SortMode(id, defaults, items) {

    object AllSongs : SongSortMode(
        id = "song",
        defaults = SortKey.Name to false,
        items = listOf(
            KeySortItem.Name,
            KeySortItem.Artist,
            KeySortItem.Album,
            KeySortItem.Duration,
            KeySortItem.Year,
            KeySortItem.DateAdded,
            KeySortItem.DateModified,
            KeySortItem.FileName,
            DescendingItem
        )
    )

    object AlbumSongs : SongSortMode(
        id = "album_song",
        defaults = SortKey.Track to false,
        items = listOf(
            KeySortItem.Name,
            KeySortItem.Track,
            KeySortItem.Duration,
            DescendingItem
        )
    )

    object ArtistSongs : SongSortMode(
        id = "artist_song",
        defaults = SortKey.Name to false,
        items = listOf(
            KeySortItem.Name,
            KeySortItem.Album,
            KeySortItem.Duration,
            KeySortItem.Year,
            KeySortItem.DateAdded,
            DescendingItem
        )
    )

    object GenreSongs : SongSortMode(
        id = "genre_song",
        defaults = SortKey.Name to false,
        items = listOf(
            KeySortItem.Name,
            KeySortItem.Artist,
            KeySortItem.Album,
            KeySortItem.Duration,
            DescendingItem
        )
    )

    object YearSongs : SongSortMode(
        id = "year_song",
        defaults = SortKey.Name to false,
        items = listOf(
            KeySortItem.Name,
            KeySortItem.Artist,
            KeySortItem.Album,
            KeySortItem.Duration,
            DescendingItem
        )
    )

    object FolderSongs : SongSortMode(
        id = "folder_song",
        defaults = SortKey.DateAdded to true,
        items = listOf(
            KeySortItem.Name,
            KeySortItem.Artist,
            KeySortItem.Album,
            KeySortItem.Duration,
            KeySortItem.DateAdded,
            KeySortItem.DateModified,
            KeySortItem.FileName,
            DescendingItem
        )
    )

    class Dynamic(
        override var selectedKey: SortKey,
        override var selectedDescending: Boolean = false,
        items: List<SortItem> = emptyList()
    ) : SongSortMode("dynamic_song", selectedKey to selectedDescending, items)

    fun List<Song>.sorted(): List<Song> {
        val byTitle: Comparator<Song> = compareBy(collator) { it.title.normalize() }
        val songs = when (selectedKey) {
            SortKey.Name -> sortedWith(byTitle)

            SortKey.Artist -> sortedWith(
                compareBy<Song, String>(collator) { it.artistName.normalize() }.then(byTitle)
            )

            SortKey.Album -> sortedWith(
                compareBy<Song, String>(collator) { it.albumName.normalize() }
                    .thenBy { if (it.trackNumber > 0) it.trackNumber else Int.MAX_VALUE }
            )

            SortKey.Track -> sortedWithTiebreak(byTitle) { it.trackNumber }
            SortKey.Duration -> sortedWithTiebreak(byTitle) { it.duration }
            SortKey.Year -> sortedWithTiebreak(byTitle) { it.year }
            SortKey.DateAdded -> sortedWithTiebreak(byTitle) { it.dateAdded }
            SortKey.DateModified -> sortedWithTiebreak(byTitle) { it.rawDateModified }
            SortKey.FileName -> sortedWith(compareBy(collator) { it.fileName })
            else -> this
        }
        return if (selectedDescending) songs.reversed() else songs
    }
}

sealed class AlbumSortMode(
    id: String,
    defaults: Pair<SortKey, Boolean>,
    items: List<SortItem>
) : SortMode(id, defaults, items) {

    object AllAlbums : AlbumSortMode(
        id = "album",
        defaults = SortKey.Name to false,
        items = listOf(
            KeySortItem.Name,
            KeySortItem.Artist,
            KeySortItem.Year,
            KeySortItem.SongCount,
            KeySortItem.DateAdded,
            DescendingItem
        )
    )

    object ArtistAlbums : AlbumSortMode(
        id = "artist_album",
        defaults = SortKey.Year to true,
        items = listOf(
            KeySortItem.Name,
            KeySortItem.Year,
            KeySortItem.SongCount,
            KeySortItem.DateAdded,
            DescendingItem
        )
    )

    object SimilarAlbums : AlbumSortMode(
        id = "similar_album",
        defaults = SortKey.Name to false,
        items = listOf(
            KeySortItem.Name,
            KeySortItem.Year,
            KeySortItem.SongCount,
            KeySortItem.DateAdded,
            DescendingItem
        )
    )

    fun List<Album>.sorted(): List<Album> {
        val byName: Comparator<Album> = compareBy(collator) { it.name.normalize() }
        val albums = when (selectedKey) {
            SortKey.Name -> sortedWith(byName)

            SortKey.Artist -> sortedWith(
                compareBy<Album, String>(collator) { it.albumArtistName().normalize() }.then(byName)
            )

            SortKey.Year -> sortedWithTiebreak(byName) { it.year }
            SortKey.SongCount -> sortedWithTiebreak(byName) { it.songCount }
            SortKey.DateAdded -> sortedWithTiebreak(byName) { it.dateAdded }
            else -> this
        }
        return if (selectedDescending) albums.reversed() else albums
    }
}

sealed class ArtistSortMode(
    id: String,
    defaults: Pair<SortKey, Boolean>,
    items: List<SortItem>
) : SortMode(id, defaults, items) {

    object AllArtists : ArtistSortMode(
        id = "artist",
        defaults = SortKey.Name to false,
        items = listOf(
            KeySortItem.Name,
            KeySortItem.SongCount,
            KeySortItem.AlbumCount,
            DescendingItem
        )
    )

    fun List<Artist>.sorted(): List<Artist> {
        val byName: Comparator<Artist> = compareBy(collator) { it.name.normalize() }
        val artists = when (selectedKey) {
            SortKey.Name -> sortedWith(byName)
            SortKey.SongCount -> sortedWithTiebreak(byName) { it.songCount }
            SortKey.AlbumCount -> sortedWithTiebreak(byName) { it.albumCount }
            else -> this
        }
        return if (selectedDescending) artists.reversed() else artists
    }
}

sealed class GenreSortMode(
    id: String,
    defaults: Pair<SortKey, Boolean>,
    items: List<SortItem>
) : SortMode(id, defaults, items) {

    object AllGenres : GenreSortMode(
        id = "genre",
        defaults = SortKey.Name to false,
        items = listOf(
            KeySortItem.Name,
            KeySortItem.SongCount,
            DescendingItem
        )
    )

    fun List<Genre>.sorted(): List<Genre> {
        val byName: Comparator<Genre> = compareBy(collator) { it.name.normalize() }
        val genres = when (selectedKey) {
            SortKey.Name -> sortedWith(byName)
            SortKey.SongCount -> sortedWithTiebreak(byName) { it.songCount }
            else -> this
        }
        return if (selectedDescending) genres.reversed() else genres
    }
}

sealed class YearSortMode(
    id: String,
    defaults: Pair<SortKey, Boolean>,
    items: List<SortItem>
) : SortMode(id, defaults, items) {

    object AllYears : YearSortMode(
        id = "year",
        defaults = SortKey.Year to false,
        items = listOf(
            KeySortItem.Year,
            KeySortItem.SongCount,
            DescendingItem
        )
    )

    fun List<ReleaseYear>.sorted(): List<ReleaseYear> {
        val years = when (selectedKey) {
            SortKey.Year -> sortedWith(compareBy { it.year })
            SortKey.SongCount -> sortedWithTiebreak(compareBy { it.year }) { it.songCount }
            else -> this
        }
        return if (selectedDescending) years.reversed() else years
    }
}

sealed class PlaylistSortMode(
    id: String,
    defaults: Pair<SortKey, Boolean>,
    items: List<SortItem>
) : SortMode(id, defaults, items) {

    object AllPlaylists : PlaylistSortMode(
        id = "playlist",
        defaults = SortKey.Name to false,
        items = listOf(
            KeySortItem.Name,
            KeySortItem.SongCount,
            DescendingItem
        )
    )

    fun List<PlaylistWithSongs>.sorted(): List<PlaylistWithSongs> {
        val byName: Comparator<PlaylistWithSongs> = compareBy(collator) {
            it.playlistEntity.playlistName.normalize()
        }
        val playlists = when (selectedKey) {
            SortKey.Name -> sortedWith(byName)
            SortKey.SongCount -> sortedWithTiebreak(byName) { it.songCount }
            else -> this
        }
        return if (selectedDescending) playlists.reversed() else playlists
    }
}

sealed class FileSortMode(
    id: String,
    defaults: Pair<SortKey, Boolean>,
    items: List<SortItem>
) : SortMode(id, defaults, items) {

    object AllFolders : FileSortMode(
        id = "folder",
        defaults = SortKey.Name to false,
        items = listOf(
            KeySortItem.Name,
            KeySortItem.SongCount,
            KeySortItem.DateAdded,
            KeySortItem.DateModified,
            DescendingItem
        )
    )

    object AllFiles : FileSortMode(
        id = "file",
        defaults = SortKey.FileName to false,
        items = listOf(
            KeySortItem.Name,
            KeySortItem.Track,
            KeySortItem.DateAdded,
            KeySortItem.DateModified,
            KeySortItem.FileName,
            DescendingItem
        )
    )

    fun List<FileSystemItem>.sorted(): List<FileSystemItem> {
        val byFolderName: Comparator<Folder> = compareBy(collator) { it.fileName }
        val byTitle: Comparator<Song> = compareBy(collator) { it.title.normalize() }

        val sortedFolders = filterIsInstance<Folder>().let { folders ->
            when (selectedKey) {
                SortKey.Name -> folders.sortedWith(byFolderName)
                SortKey.SongCount -> folders.sortedWithTiebreak(byFolderName) { it.songCount }
                SortKey.DateAdded -> folders.sortedWithTiebreak(byFolderName) { it.fileDateAdded }
                SortKey.DateModified -> folders.sortedWithTiebreak(byFolderName) { it.fileDateModified }
                else -> folders
            }
        }.let { folders ->
            if (selectedDescending) folders.reversed() else folders
        }

        val sortedSongs = filterIsInstance<Song>().let { songs ->
            when (selectedKey) {
                SortKey.Name -> songs.sortedWith(byTitle)
                SortKey.Track -> songs.sortedWithTiebreak(byTitle) {
                    if (it.trackNumber > 0) it.trackNumber.asReadableTrackNumber() else -1
                }
                SortKey.DateAdded -> songs.sortedWithTiebreak(byTitle) { it.fileDateAdded }
                SortKey.DateModified -> songs.sortedWithTiebreak(byTitle) { it.fileDateModified }
                SortKey.FileName -> songs.sortedWith(compareBy(collator) { it.fileName })
                else -> songs
            }
        }.let { songs ->
            if (selectedDescending) songs.reversed() else songs
        }

        return sortedFolders + sortedSongs
    }
}
