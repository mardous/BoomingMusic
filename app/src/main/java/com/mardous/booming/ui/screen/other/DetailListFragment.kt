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

package com.mardous.booming.ui.screen.other

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.mardous.booming.R
import com.mardous.booming.data.mapper.lastAddedSearchFilter
import com.mardous.booming.data.mapper.searchFilter
import com.mardous.booming.data.mapper.toSongs
import com.mardous.booming.data.model.Album
import com.mardous.booming.data.model.Artist
import com.mardous.booming.data.model.ContentType
import com.mardous.booming.data.model.Song
import com.mardous.booming.databinding.FragmentDetailListBinding
import com.mardous.booming.extensions.*
import com.mardous.booming.extensions.media.playlistInfo
import com.mardous.booming.extensions.navigation.albumDetailArgs
import com.mardous.booming.extensions.navigation.artistDetailArgs
import com.mardous.booming.extensions.navigation.asFragmentExtras
import com.mardous.booming.extensions.navigation.searchArgs
import com.mardous.booming.extensions.resources.createFastScroller
import com.mardous.booming.extensions.resources.hide
import com.mardous.booming.extensions.utilities.buildInfoString
import com.mardous.booming.service.playback.Playback
import com.mardous.booming.ui.IAlbumCallback
import com.mardous.booming.ui.IArtistCallback
import com.mardous.booming.ui.ISongCallback
import com.mardous.booming.ui.adapters.album.AlbumAdapter
import com.mardous.booming.ui.adapters.artist.ArtistAdapter
import com.mardous.booming.ui.adapters.song.SongAdapter
import com.mardous.booming.ui.component.base.AbsMainActivityFragment
import com.mardous.booming.ui.component.menu.onAlbumsMenu
import com.mardous.booming.ui.component.menu.onArtistsMenu
import com.mardous.booming.ui.component.menu.onSongMenu
import com.mardous.booming.ui.component.menu.onSongsMenu
import com.mardous.booming.util.Preferences

class DetailListFragment : AbsMainActivityFragment(R.layout.fragment_detail_list), ISongCallback, IArtistCallback,
    IAlbumCallback {

    private val args by navArgs<DetailListFragmentArgs>()

    private var _binding: FragmentDetailListBinding? = null
    private val binding get() = _binding!!

    private lateinit var contentType: ContentType

    private var songList: List<Song> = arrayListOf()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        contentType = args.type
        _binding = FragmentDetailListBinding.bind(view)
        materialSharedAxis(view)
        view.applyHorizontalWindowInsets()

        mainActivity.setSupportActionBar(binding.toolbar)
        binding.progressIndicator.hide()

        setupButtons()
        loadContent()
        binding.toolbar.setTitle(contentType.titleRes)
        binding.title.setText(contentType.titleRes)

        libraryViewModel.getMiniPlayerMargin().observe(viewLifecycleOwner) {
            binding.recyclerView.updatePadding(bottom = it.getWithSpace())
        }
    }

    private fun setupButtons() {
        if (contentType == ContentType.Favorites || contentType == ContentType.History ||
            contentType == ContentType.TopTracks || contentType == ContentType.RecentSongs ||
            contentType == ContentType.NotRecentlyPlayed
        ) {
            binding.shuffleAction.setOnClickListener {
                playerViewModel.openQueue(songList, shuffleMode = Playback.ShuffleMode.On)
            }
        } else {
            binding.shuffleAction.hide()
        }
        binding.playAction.setOnClickListener {
            playerViewModel.openQueue(songList, shuffleMode = Playback.ShuffleMode.Off)
        }
    }

    private fun songs(songs: List<Song>, emptyMessageRes: Int = 0) {
        this.songList = songs
        binding.subtitle.text = when (contentType) {
            ContentType.RecentSongs -> buildInfoString(
                Preferences.getLastAddedCutoff(requireContext()).description,
                songs.playlistInfo(requireContext())
            )

            ContentType.History -> buildInfoString(
                Preferences.getHistoryCutoff(requireContext()).description,
                songs.playlistInfo(requireContext())
            )

            ContentType.TopTracks,
            ContentType.Favorites,
            ContentType.NotRecentlyPlayed -> songs.playlistInfo(requireContext())

            else -> null
        }
        if (songs.isEmpty() && emptyMessageRes == 0) {
            findNavController().navigateUp()
        } else {
            binding.empty.isVisible = songs.isEmpty()
        }
    }

    private fun loadContent() {
        when (contentType) {
            ContentType.TopArtists -> loadArtists(ContentType.TopArtists)
            ContentType.RecentArtists -> loadArtists(ContentType.RecentArtists)
            ContentType.TopAlbums -> loadAlbums(ContentType.TopAlbums)
            ContentType.RecentAlbums -> loadAlbums(ContentType.RecentAlbums)
            ContentType.TopTracks -> topPlayed()
            ContentType.History -> loadHistory()
            ContentType.RecentSongs -> lastAddedSongs()
            ContentType.Favorites -> loadFavorite()
            ContentType.NotRecentlyPlayed -> loadNotRecentlyPlayed()
        }
    }

    private fun lastAddedSongs() {
        val songAdapter = songAdapter()
        binding.recyclerView.apply {
            adapter = songAdapter
            layoutManager = linearLayoutManager()
            createFastScroller(disablePopup = true)
        }
        libraryViewModel.lastAddedSongs().observe(viewLifecycleOwner) { songs ->
            songAdapter.dataSet = songs
            songs(songs, R.string.playlist_empty_text)
        }
    }

    private fun topPlayed() {
        val songAdapter = songAdapter()
        binding.recyclerView.apply {
            adapter = songAdapter
            layoutManager = linearLayoutManager()
            createFastScroller(disablePopup = true)
        }
        libraryViewModel.topTracks().observe(viewLifecycleOwner) { songs ->
            songAdapter.dataSet = songs
            songs(songs, R.string.playlist_empty_text)
        }
    }

    private fun loadHistory() {
        val songAdapter = songAdapter()
        binding.recyclerView.apply {
            adapter = songAdapter
            layoutManager = linearLayoutManager()
            createFastScroller(disablePopup = true)
        }
        libraryViewModel.observableHistorySongs().observe(viewLifecycleOwner) { songs ->
            songAdapter.dataSet = songs
            songs(songs, R.string.playlist_empty_text)
        }
    }

    private fun loadNotRecentlyPlayed() {
        val songAdapter = songAdapter()
        binding.recyclerView.apply {
            adapter = songAdapter
            layoutManager = linearLayoutManager()
            createFastScroller(disablePopup = true)
        }
        libraryViewModel.notRecentlyPlayedSongs().observe(viewLifecycleOwner) { songs ->
            songAdapter.dataSet = songs
            songs(songs)
        }
    }

    private fun loadFavorite() {
        val songAdapter = songAdapter()
        binding.recyclerView.apply {
            adapter = songAdapter
            layoutManager = linearLayoutManager()
            createFastScroller(disablePopup = true)
        }
        libraryViewModel.favorites().observe(viewLifecycleOwner) { songEntities ->
            val songs = songEntities.toSongs()
            songAdapter.dataSet = songs
            songs(songs)
        }
    }

    private fun loadArtists(type: ContentType) {
        val artistAdapter = artistAdapter()
        val padding = dip(R.dimen.grid_item_margin)
        binding.recyclerView.apply {
            adapter = artistAdapter
            layoutManager = gridLayoutManager()
            updatePadding(left = padding, right = padding)
        }
        libraryViewModel.artists(type).observe(viewLifecycleOwner) { artists ->
            artistAdapter.dataSet = artists
            songs(artists.flatMap { it.songs })
            // Subtitle won't set automatically for albums and artists
            binding.subtitle.text = plurals(R.plurals.x_artists, artists.size)
        }
    }

    private fun loadAlbums(type: ContentType) {
        val albumAdapter = albumAdapter()
        val padding = dip(R.dimen.grid_item_margin)
        binding.recyclerView.apply {
            adapter = albumAdapter
            layoutManager = gridLayoutManager()
            updatePadding(left = padding, right = padding)
        }
        libraryViewModel.albums(type).observe(viewLifecycleOwner) { albums ->
            albumAdapter.dataSet = albums
            songs(albums.flatMap { it.songs })
            // Subtitle won't set automatically for albums and artists
            binding.subtitle.text = plurals(R.plurals.x_albums, albums.size)
        }
    }

    private fun songAdapter(songs: List<Song> = listOf()): SongAdapter =
        SongAdapter(requireActivity(), songs, R.layout.item_list, callback = this)

    private fun artistAdapter(artists: List<Artist> = listOf()): ArtistAdapter =
        ArtistAdapter(requireActivity(), artists, R.layout.item_grid_circle_single_row, this)

    private fun albumAdapter(albums: List<Album> = listOf()): AlbumAdapter =
        AlbumAdapter(requireActivity(), albums, R.layout.item_grid, callback = this)

    private fun linearLayoutManager(): LinearLayoutManager =
        LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)

    private fun gridLayoutManager(): GridLayoutManager =
        GridLayoutManager(requireContext(), defaultGridColumns(), GridLayoutManager.VERTICAL, false)

    override fun onMediaContentChanged() {
        super.onMediaContentChanged()
        loadContent()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_list_detail, menu)
        if (!contentType.isSearchableContent) {
            menu.removeItem(R.id.action_search)
        }
        menu.findItem(R.id.action_clear_history).isVisible = contentType.isHistoryContent
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            android.R.id.home -> {
                findNavController().navigateUp()
                true
            }

            R.id.action_search -> {
                if (contentType.isFavoriteContent) {
                    libraryViewModel.favoritePlaylist().observe(viewLifecycleOwner) {
                        findNavController().navigate(
                            R.id.nav_search,
                            searchArgs(it.searchFilter(requireContext()))
                        )
                    }
                } else if (contentType.isRecentContent) {
                    findNavController().navigate(
                        R.id.nav_search,
                        searchArgs(lastAddedSearchFilter(requireContext()))
                    )
                }
                true
            }

            R.id.action_clear_history -> {
                libraryViewModel.clearHistory()
                val translationY = -(libraryViewModel.getMiniPlayerMargin().value?.getWithSpace() ?: 0)
                val snackBar = Snackbar.make(binding.root, getString(R.string.history_cleared), Snackbar.LENGTH_LONG)
                val snackBarView = snackBar.view
                snackBarView.translationY = translationY.toFloat()
                snackBar.show()
                true
            }

            else -> songList.onSongsMenu(this, menuItem)
        }
    }

    override fun albumClick(album: Album, sharedElements: Array<Pair<View, String>>?) {
        findNavController().navigate(
            R.id.nav_album_detail,
            albumDetailArgs(album.id),
            null,
            sharedElements.asFragmentExtras()
        )
    }

    override fun albumMenuItemClick(
        album: Album, menuItem: MenuItem, sharedElements: Array<Pair<View, String>>?
    ): Boolean = false

    override fun albumsMenuItemClick(albums: List<Album>, menuItem: MenuItem) {
        albums.onAlbumsMenu(this, menuItem)
    }

    override fun artistClick(artist: Artist, sharedElements: Array<Pair<View, String>>?) {
        findNavController().navigate(
            R.id.nav_artist_detail,
            artistDetailArgs(artist),
            null,
            sharedElements.asFragmentExtras()
        )
    }

    override fun artistMenuItemClick(
        artist: Artist, menuItem: MenuItem, sharedElements: Array<Pair<View, String>>?
    ): Boolean = false

    override fun artistsMenuItemClick(artists: List<Artist>, menuItem: MenuItem) {
        artists.onArtistsMenu(this, menuItem)
    }

    override fun songMenuItemClick(
        song: Song,
        menuItem: MenuItem,
        sharedElements: Array<Pair<View, String>>?
    ): Boolean {
        return song.onSongMenu(this, menuItem)
    }

    override fun songsMenuItemClick(songs: List<Song>, menuItem: MenuItem) {
        songs.onSongsMenu(this, menuItem)
    }
}
