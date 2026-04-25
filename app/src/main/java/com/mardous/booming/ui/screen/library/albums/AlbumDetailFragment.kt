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

package com.mardous.booming.ui.screen.library.albums

import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.doOnPreDraw
import androidx.core.view.updatePadding
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import coil3.request.crossfade
import com.google.android.material.transition.MaterialArcMotion
import com.google.android.material.transition.MaterialContainerTransform
import com.mardous.booming.R
import com.mardous.booming.coil.albumImage
import com.mardous.booming.core.sort.AlbumSortMode
import com.mardous.booming.core.sort.SongSortMode
import com.mardous.booming.core.sort.SortMode
import com.mardous.booming.data.mapper.searchFilter
import com.mardous.booming.data.model.Album
import com.mardous.booming.data.model.Song
import com.mardous.booming.data.remote.lastfm.model.LastFmAlbum
import com.mardous.booming.databinding.FragmentAlbumDetailBinding
import com.mardous.booming.extensions.applyHorizontalWindowInsets
import com.mardous.booming.extensions.dp
import com.mardous.booming.extensions.isLandscape
import com.mardous.booming.extensions.materialSharedAxis
import com.mardous.booming.extensions.media.asReadableDuration
import com.mardous.booming.extensions.media.displayArtistName
import com.mardous.booming.extensions.media.isArtistNameUnknown
import com.mardous.booming.extensions.navigation.albumDetailArgs
import com.mardous.booming.extensions.navigation.artistDetailArgs
import com.mardous.booming.extensions.navigation.asFragmentExtras
import com.mardous.booming.extensions.navigation.playInfoArgs
import com.mardous.booming.extensions.navigation.searchArgs
import com.mardous.booming.extensions.plurals
import com.mardous.booming.extensions.resources.removeHorizontalMarginIfRequired
import com.mardous.booming.extensions.resources.setupStatusBarForeground
import com.mardous.booming.extensions.resources.surfaceColor
import com.mardous.booming.extensions.setSupportActionBar
import com.mardous.booming.extensions.utilities.buildInfoString
import com.mardous.booming.playback.shuffle.OpenShuffleMode
import com.mardous.booming.ui.IAlbumCallback
import com.mardous.booming.ui.ISongCallback
import com.mardous.booming.ui.adapters.HeaderAdapter
import com.mardous.booming.ui.adapters.HorizontalListAdapter
import com.mardous.booming.ui.adapters.SectionHeaderAdapter
import com.mardous.booming.ui.adapters.WikiAdapter
import com.mardous.booming.ui.adapters.album.AlbumAdapter
import com.mardous.booming.ui.adapters.song.SimpleSongAdapter
import com.mardous.booming.ui.component.base.AbsMainActivityFragment
import com.mardous.booming.ui.component.menu.onAlbumMenu
import com.mardous.booming.ui.component.menu.onAlbumsMenu
import com.mardous.booming.ui.component.menu.onSongMenu
import com.mardous.booming.ui.component.menu.onSongsMenu
import com.mardous.booming.util.Preferences
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import java.util.Locale

/**
 * @author Christians M. A. (mardous)
 */
class AlbumDetailFragment : AbsMainActivityFragment(R.layout.fragment_album_detail),
    ISongCallback,
    IAlbumCallback {

    private val arguments by navArgs<AlbumDetailFragmentArgs>()
    private val detailViewModel by viewModel<AlbumDetailViewModel> {
        parametersOf(arguments.albumId)
    }

    private var _binding: FragmentAlbumDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var headerAdapter: HeaderAdapter
    private lateinit var songHeaderAdapter: SectionHeaderAdapter
    private lateinit var simpleSongAdapter: SimpleSongAdapter
    private lateinit var moreAlbumsAdapter: HorizontalListAdapter
    private lateinit var wikiAdapter: WikiAdapter
    private lateinit var concatAdapter: ConcatAdapter

    private var albumArtistExists = false
    private var lang: String? = null
    private var biography: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            drawingViewId = R.id.fragment_container
            scrimColor = Color.TRANSPARENT
            setAllContainerColors(surfaceColor())
            setPathMotion(MaterialArcMotion())
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAlbumDetailBinding.bind(view)
        setSupportActionBar(binding.toolbar, "")
        materialSharedAxis(view, prepareTransition = false)

        view.applyHorizontalWindowInsets()

        binding.appBarLayout.setupStatusBarForeground()

        postponeEnterTransition()
        detailViewModel.getAlbumDetail().observe(viewLifecycleOwner) { album ->
            view.doOnPreDraw {
                startPostponedEnterTransition()
            }
            albumArtistExists = !album.albumArtistName.isNullOrEmpty()
            showAlbum(album)
        }

        libraryViewModel.getMiniPlayerMargin().observe(viewLifecycleOwner) {
            binding.recyclerView.updatePadding(bottom = it.getWithSpace(16.dp(resources)))
        }

        setupRecyclerView()

        detailViewModel.loadAlbumDetail()
    }

    private fun getAlbum(): Album = detailViewModel.getAlbum()

    private fun createSongAdapter() {
        val itemLayoutRes = if (Preferences.compactAlbumSongView) {
            R.layout.item_song
        } else {
            R.layout.item_song_detailed
        }
        simpleSongAdapter = SimpleSongAdapter(
            context = requireActivity(),
            songs = getAlbum().songs,
            layoutRes = itemLayoutRes,
            sortMode = SongSortMode.AlbumSongs,
            callback = this
        )
    }

    private fun setupRecyclerView() {
        headerAdapter = HeaderAdapter { headerBinding ->
            headerBinding.image.transitionName = arguments.albumId.toString()
            headerBinding.image.removeHorizontalMarginIfRequired()
            headerBinding.image.albumImage(getAlbum()) { crossfade(false) }

            headerBinding.title.text = getAlbum().name

            val artistName = if (albumArtistExists) getAlbum().albumArtistName else getAlbum().artistName
            headerBinding.subtitle.setOnClickListener { goToArtist() }
            headerBinding.subtitle.text = buildInfoString(
                artistName?.displayArtistName(),
                getAlbum().year.takeIf { it > 0 },
                getAlbum().duration.asReadableDuration(readableFormat = true).takeIf { Preferences.showAlbumDuration }
            )
            headerBinding.playAction.setOnClickListener {
                playerViewModel.openQueue(getAlbum().songs, shuffleMode = OpenShuffleMode.Off)
            }
            headerBinding.shuffleAction.setOnClickListener {
                playerViewModel.openAndShuffleQueue(getAlbum().songs)
            }
            headerBinding.searchAction?.setOnClickListener { goToSearch() }
        }

        songHeaderAdapter = SectionHeaderAdapter(getString(R.string.songs_label)) {
            createSortOrderMenu(it, SongSortMode.AlbumSongs) {
                detailViewModel.loadAlbumDetail()
            }
        }
        createSongAdapter()

        val albumAdapter = AlbumAdapter(requireActivity(), emptyList(), R.layout.item_image, callback = this)
        moreAlbumsAdapter = HorizontalListAdapter("", albumAdapter) {
            createSortOrderMenu(it, AlbumSortMode.SimilarAlbums) {
                loadSimilarContent(getAlbum())
            }
        }

        wikiAdapter = WikiAdapter()

        updateConcatAdapter()
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun updateConcatAdapter() {
        concatAdapter = ConcatAdapter(
            headerAdapter,
            songHeaderAdapter,
            simpleSongAdapter,
            moreAlbumsAdapter,
            wikiAdapter
        )
        binding.recyclerView.adapter = concatAdapter
    }

    private fun createSortOrderMenu(view: View, sortMode: SortMode, onChanged: () -> Unit) {
        val popupMenu = PopupMenu(view.context, view).apply {
            sortMode.createMenu(menu, hasSubMenu = false)
            setOnMenuItemClickListener { item ->
                if (sortMode.sortItemSelected(item)) {
                    onChanged()
                    true
                } else false
            }
        }
        popupMenu.show()
    }

    private fun showAlbum(album: Album) {
        if (album == Album.empty || album.songs.isEmpty()) {
            findNavController().navigateUp()
            return
        }

        headerAdapter.notifyItemChanged(0)

        val songText = plurals(R.plurals.songs, album.songCount)
        songHeaderAdapter.updateTitle(buildInfoString(
            songText,
            album.songCount.takeIf { it > 1 }?.toString()
        ))

        simpleSongAdapter.dataSet = album.songs
        loadSimilarContent(album)
        loadWiki(album)
    }

    private fun loadSimilarContent(album: Album) {
        detailViewModel.getSimilarAlbums(album).observe(viewLifecycleOwner) {
            moreAlbums(it)
        }
    }

    private fun loadWiki(album: Album, lang: String? = Locale.getDefault().language) {
        this.biography = null
        this.lang = lang
        detailViewModel.getAlbumWiki(album, lang).observe(viewLifecycleOwner) { lastFmAlbum ->
            if (lastFmAlbum != null) {
                aboutAlbum(lastFmAlbum)
            }
        }
    }

    private fun moreAlbums(albums: List<Album>) {
        if (albums.isNotEmpty()) {
            val title = if (getAlbum().isArtistNameUnknown())
                getString(R.string.label_more_from_artist) else getString(
                R.string.label_more_from_x,
                getAlbum().displayArtistName()
            )

            moreAlbumsAdapter.setVisible(true)
            moreAlbumsAdapter.updateTitle(title)
            (moreAlbumsAdapter.innerAdapter as AlbumAdapter).dataSet = albums
        } else {
            moreAlbumsAdapter.setVisible(false)
        }
    }

    private fun aboutAlbum(lastFmAlbum: LastFmAlbum) {
        val albumValue = lastFmAlbum.album
        if (albumValue != null) {
            if (!albumValue.wiki?.content.isNullOrEmpty()) {
                biography = albumValue.wiki.content
                wikiAdapter.update(getString(R.string.about_x_title, getAlbum().name), biography)
            }
        }

        // If the "lang" parameter is set and no biography is given, retry with default language
        if (biography == null && lang != null) {
            loadWiki(getAlbum(), null)
        }
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

    override fun albumClick(album: Album, sharedElements: Array<Pair<View, String>>?) {
        findNavController().navigate(
            R.id.nav_album_detail,
            albumDetailArgs(album.id),
            null,
            sharedElements.asFragmentExtras()
        )
    }

    override fun albumMenuItemClick(
        album: Album,
        menuItem: MenuItem,
        sharedElements: Array<Pair<View, String>>?
    ): Boolean {
        return false
    }

    override fun albumsMenuItemClick(albums: List<Album>, menuItem: MenuItem) {
        albums.onAlbumsMenu(this, menuItem)
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_album_detail, menu)
        if (!isLandscape()) {
            menu.removeItem(R.id.action_search)
        }
        menu.findItem(R.id.action_toggle_compact_song_view)
            ?.isChecked = Preferences.compactAlbumSongView
        menu.findItem(R.id.action_show_album_duration)
            ?.isChecked = Preferences.showAlbumDuration
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            android.R.id.home -> {
                findNavController().navigateUp()
                true
            }

            R.id.action_search -> {
                goToSearch()
                true
            }

            R.id.action_play_info -> {
                goToPlayInfo()
                true
            }

            R.id.action_toggle_compact_song_view -> {
                val isChecked = !menuItem.isChecked
                Preferences.compactAlbumSongView = isChecked
                menuItem.isChecked = isChecked
                createSongAdapter()
                updateConcatAdapter()
                true
            }

            R.id.action_show_album_duration -> {
                val isChecked = !menuItem.isChecked
                Preferences.showAlbumDuration = isChecked
                menuItem.isChecked = isChecked
                detailViewModel.loadAlbumDetail()
                true
            }

            else -> getAlbum().onAlbumMenu(this, menuItem)
        }
    }

    private fun goToArtist() {
        if (albumArtistExists) {
            findNavController().navigate(R.id.nav_artist_detail, artistDetailArgs(-1, getAlbum().albumArtistName))
        } else {
            findNavController().navigate(R.id.nav_artist_detail, artistDetailArgs(getAlbum().artistId, null))
        }
    }

    private fun goToSearch() {
        findNavController().navigate(R.id.nav_search, searchArgs(getAlbum().searchFilter(requireContext())))
    }

    private fun goToPlayInfo() {
        findNavController().navigate(R.id.nav_play_info, playInfoArgs(getAlbum()))
    }

    override fun onMediaContentChanged() {
        super.onMediaContentChanged()
        detailViewModel.loadAlbumDetail()
    }

    override fun onDestroyView() {
        binding.recyclerView.layoutManager = null
        binding.recyclerView.adapter = null
        super.onDestroyView()
        _binding = null
    }
}