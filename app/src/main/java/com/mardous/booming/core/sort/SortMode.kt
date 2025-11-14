package com.mardous.booming.core.sort

import android.content.SharedPreferences
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
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import java.text.Collator
import java.util.Locale

sealed class SortMode(
    id: String,
    private val defaults: Pair<SortKey, Boolean>,
    private val items: List<SortItem>
) : KoinComponent {

    protected val collator: Collator by lazy {
        Collator.getInstance(Locale.getDefault()).apply { strength = Collator.PRIMARY }
    }

    val ignoreArticles: Boolean
        get() =  get<SharedPreferences>().getBoolean("ignore_articles_when_sorting", false)

    private val key = "${id}_sort_order"
    open var selectedKey: SortKey
        get() = get<SharedPreferences>().getSortKey(key, defaults.first)
        protected set(newKey) {
            get<SharedPreferences>().edit { putString(key, newKey.value) }
        }

    private val descending = "{$id}_descending"
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

sealed class SongSortMode(
    id: String,
    defaults: Pair<SortKey, Boolean>,
    items: List<SortItem>
) : SortMode(id, defaults, items) {

    object AllSongs : SongSortMode(
        id = "song",
        defaults = SortKey.AZ to false,
        items = listOf(
            KeySortItem.Title,
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
            KeySortItem.Title,
            KeySortItem.Track,
            KeySortItem.Duration,
            DescendingItem
        )
    )

    object ArtistSongs : SongSortMode(
        id = "artist_song",
        defaults = SortKey.AZ to false,
        items = listOf(
            KeySortItem.Title,
            KeySortItem.Album,
            KeySortItem.Duration,
            KeySortItem.Year,
            KeySortItem.DateAdded,
            DescendingItem
        )
    )

    object GenreSongs : SongSortMode(
        id = "genre_song",
        defaults = SortKey.AZ to false,
        items = listOf(
            KeySortItem.Title,
            KeySortItem.Artist,
            KeySortItem.Album,
            KeySortItem.Duration,
            DescendingItem
        )
    )

    object YearSongs : SongSortMode(
        id = "year_song",
        defaults = SortKey.AZ to false,
        items = listOf(
            KeySortItem.Title,
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
            KeySortItem.Title,
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
        val songs = when (selectedKey) {
            SortKey.AZ -> sortedWith(compareBy(collator) {
                it.title.normalize()
            })

            SortKey.Artist -> sortedWith(compareBy(collator) {
                it.artistName.normalize()
            })

            SortKey.Album -> sortedWith(
                Comparator.comparing<Song, String>({ it.albumName.normalize() }, collator)
                    .thenComparingInt {
                        if (it.trackNumber > 0) it.trackNumber else Int.MAX_VALUE
                    }
            )

            SortKey.Track -> sortedWith(compareBy { it.trackNumber })
            SortKey.Duration -> sortedWith(compareBy { it.duration })
            SortKey.Year -> sortedWith(compareBy { it.year })
            SortKey.DateAdded -> sortedWith(compareBy { it.dateAdded })
            SortKey.DateModified -> sortedWith(compareBy { it.rawDateModified })
            SortKey.FileName -> sortedWith(compareBy { it.fileName })
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
        defaults = SortKey.AZ to false,
        items = listOf(
            KeySortItem.Title,
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
            KeySortItem.Title,
            KeySortItem.Year,
            KeySortItem.SongCount,
            KeySortItem.DateAdded,
            DescendingItem
        )
    )

    object SimilarAlbums : AlbumSortMode(
        id = "similar_album",
        defaults = SortKey.AZ to false,
        items = listOf(
            KeySortItem.Title,
            KeySortItem.Year,
            KeySortItem.SongCount,
            KeySortItem.DateAdded,
            DescendingItem
        )
    )

    fun List<Album>.sorted(): List<Album> {
        val albums = when (selectedKey) {
            SortKey.AZ -> sortedWith(compareBy(collator) {
                it.name.normalize()
            })

            SortKey.Artist -> sortedWith(compareBy(collator) {
                it.albumArtistName().normalize()
            })

            SortKey.Year -> sortedWith(compareBy { it.year })
            SortKey.SongCount -> sortedWith(compareBy { it.songCount })
            SortKey.DateAdded -> sortedWith(compareBy { it.dateAdded })
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
        defaults = SortKey.AZ to false,
        items = listOf(
            KeySortItem.Title,
            KeySortItem.SongCount,
            KeySortItem.AlbumCount,
            DescendingItem
        )
    )

    fun List<Artist>.sorted(): List<Artist> {
        val artists = when (selectedKey) {
            SortKey.AZ -> sortedWith(compareBy(collator) {
                it.name.normalize()
            })
            SortKey.SongCount -> sortedWith(compareBy({ it.songCount }, { it.name.normalize() }))
            SortKey.AlbumCount -> sortedWith(compareBy({ it.albumCount }, { it.name.normalize() }))
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
        defaults = SortKey.AZ to false,
        items = listOf(
            KeySortItem.Title,
            KeySortItem.SongCount,
            DescendingItem
        )
    )

    fun List<Genre>.sorted(): List<Genre> {
        val genres = when (selectedKey) {
            SortKey.AZ -> sortedWith(compareBy(collator) {
                it.name.normalize()
            })

            SortKey.SongCount -> sortedWith(compareBy { it.songCount })
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
            SortKey.SongCount -> sortedWith(compareBy { it.songCount })
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
        defaults = SortKey.AZ to false,
        items = listOf(
            KeySortItem.Title,
            KeySortItem.SongCount,
            DescendingItem
        )
    )

    fun List<PlaylistWithSongs>.sorted(): List<PlaylistWithSongs> {
        val playlists = when (selectedKey) {
            SortKey.AZ -> sortedWith(compareBy { it.playlistEntity.playlistName })
            SortKey.SongCount -> sortedWith(compareBy { it.songCount })
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
        defaults = SortKey.AZ to false,
        items = listOf(
            KeySortItem.Title,
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
            KeySortItem.Title,
            KeySortItem.Track,
            KeySortItem.DateAdded,
            KeySortItem.DateModified,
            KeySortItem.FileName,
            DescendingItem
        )
    )

    fun List<FileSystemItem>.sorted(): List<FileSystemItem> {
        val sortedFolders = filterIsInstance<Folder>().let { folders ->
            when (selectedKey) {
                SortKey.AZ -> folders.sortedWith(compareBy { it.fileName })
                SortKey.SongCount -> folders.sortedWith(compareBy { it.songCount })
                SortKey.DateAdded -> folders.sortedWith(compareBy { it.fileDateAdded })
                SortKey.DateModified -> folders.sortedWith(compareBy { it.fileDateModified })
                else -> folders
            }
        }.let { folders ->
            if (selectedDescending) folders.reversed() else folders
        }

        val sortedSongs = filterIsInstance<Song>().let { songs ->
            when (selectedKey) {
                SortKey.AZ -> songs.sortedWith(compareBy { it.title })
                SortKey.Track -> songs.sortedWith(compareBy {
                    if (it.trackNumber > 0) it.trackNumber.asReadableTrackNumber() else -1
                })
                SortKey.DateAdded -> songs.sortedWith(compareBy { it.fileDateAdded })
                SortKey.DateModified -> songs.sortedWith(compareBy { it.fileDateModified })
                SortKey.FileName -> songs.sortedWith(compareBy { it.fileName })
                else -> songs
            }
        }.let { songs ->
            if (selectedDescending) songs.reversed() else songs
        }

        return sortedFolders + sortedSongs
    }
}