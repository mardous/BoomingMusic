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

package com.mardous.booming.ui.screen.library.artists

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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil3.request.crossfade
import com.google.android.material.transition.MaterialArcMotion
import com.google.android.material.transition.MaterialContainerTransform
import com.mardous.booming.R
import com.mardous.booming.coil.artistImage
import com.mardous.booming.core.sort.AlbumSortMode
import com.mardous.booming.core.sort.SongSortMode
import com.mardous.booming.core.sort.SortMode
import com.mardous.booming.data.mapper.searchFilter
import com.mardous.booming.data.model.Album
import com.mardous.booming.data.model.Artist
import com.mardous.booming.data.model.Song
import com.mardous.booming.data.remote.lastfm.model.LastFmArtist
import com.mardous.booming.databinding.FragmentArtistDetailBinding
import com.mardous.booming.extensions.applyHorizontalWindowInsets
import com.mardous.booming.extensions.defaultGridColumns
import com.mardous.booming.extensions.dp
import com.mardous.booming.extensions.isLandscape
import com.mardous.booming.extensions.materialSharedAxis
import com.mardous.booming.extensions.media.artistInfo
import com.mardous.booming.extensions.media.displayName
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
import com.mardous.booming.playback.shuffle.OpenShuffleMode
import com.mardous.booming.ui.IAlbumCallback
import com.mardous.booming.ui.IArtistCallback
import com.mardous.booming.ui.ISongCallback
import com.mardous.booming.ui.adapters.HeaderAdapter
import com.mardous.booming.ui.adapters.HorizontalListAdapter
import com.mardous.booming.ui.adapters.SectionHeaderAdapter
import com.mardous.booming.ui.adapters.WikiAdapter
import com.mardous.booming.ui.adapters.album.SimpleAlbumAdapter
import com.mardous.booming.ui.adapters.artist.ArtistAdapter
import com.mardous.booming.ui.adapters.song.SimpleSongAdapter
import com.mardous.booming.ui.component.base.AbsMainActivityFragment
import com.mardous.booming.ui.component.menu.onAlbumsMenu
import com.mardous.booming.ui.component.menu.onArtistMenu
import com.mardous.booming.ui.component.menu.onArtistsMenu
import com.mardous.booming.ui.component.menu.onSongMenu
import com.mardous.booming.ui.component.menu.onSongsMenu
import com.mardous.booming.util.Preferences
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import java.util.Locale

/**
 * @author Christians M. A. (mardous)
 */
class ArtistDetailFragment : AbsMainActivityFragment(R.layout.fragment_artist_detail),
    IAlbumCallback, IArtistCallback, ISongCallback {

    private val arguments by navArgs<ArtistDetailFragmentArgs>()
    private val detailViewModel by viewModel<ArtistDetailViewModel> {
        parametersOf(arguments.artistId, arguments.artistName)
    }

    private var _binding: FragmentArtistDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var headerAdapter: HeaderAdapter
    private lateinit var albumHeaderAdapter: SectionHeaderAdapter
    private lateinit var albumGridAdapter: SimpleAlbumAdapter
    private lateinit var albumHorizontalAdapter: HorizontalListAdapter
    private lateinit var songHeaderAdapter: SectionHeaderAdapter
    private lateinit var songAdapter: SimpleSongAdapter
    private lateinit var similarArtistAdapter: HorizontalListAdapter
    private lateinit var wikiAdapter: WikiAdapter
    private lateinit var concatAdapter: ConcatAdapter

    private var lang: String? = null
    private var biography: String? = null

    private val isAlbumArtist: Boolean
        get() = !arguments.artistName.isNullOrEmpty()

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
        _binding = FragmentArtistDetailBinding.bind(view)
        setSupportActionBar(binding.toolbar, "")
        materialSharedAxis(view, prepareTransition = false)

        view.applyHorizontalWindowInsets()

        binding.appBarLayout.setupStatusBarForeground()

        postponeEnterTransition()
        detailViewModel.getArtistDetail().observe(viewLifecycleOwner) { result ->
            view.doOnPreDraw {
                startPostponedEnterTransition()
            }
            showArtist(result)
        }

        libraryViewModel.getMiniPlayerMargin().observe(viewLifecycleOwner) {
            binding.recyclerView.updatePadding(bottom = it.getWithSpace(16.dp(resources)))
        }

        setupRecyclerView()

        detailViewModel.loadArtistDetail()
    }

    private fun getArtist() = detailViewModel.getArtist()

    private fun createSongAdapter() {
        val itemLayoutRes = if (Preferences.compactArtistSongView) {
            R.layout.item_song
        } else {
            R.layout.item_song_detailed
        }
        songAdapter = SimpleSongAdapter(
            context = requireActivity(),
            songs = getArtist().sortedSongs,
            layoutRes = itemLayoutRes,
            sortMode = SongSortMode.ArtistSongs,
            callback = this
        )
    }

    private fun setupRecyclerView() {
        // Header
        headerAdapter = HeaderAdapter { headerBinding ->
            headerBinding.image.transitionName = if (isAlbumArtist) {
                arguments.artistName
            } else {
                arguments.artistId.toString()
            }
            headerBinding.image.removeHorizontalMarginIfRequired()
            headerBinding.image.artistImage(getArtist()) { crossfade(false) }

            headerBinding.title.text = getArtist().displayName()
            headerBinding.subtitle.text = getArtist().artistInfo(requireContext())

            headerBinding.playAction.setOnClickListener {
                playerViewModel.openQueue(getArtist().sortedSongs, shuffleMode = OpenShuffleMode.Off)
            }
            headerBinding.shuffleAction.setOnClickListener {
                playerViewModel.openAndShuffleQueue(getArtist().sortedSongs)
            }
            headerBinding.searchAction?.setOnClickListener { goToSearch() }
        }

        // Grid albums
        albumHeaderAdapter = SectionHeaderAdapter(getString(R.string.albums_label)) {
            createSortOrderMenu(it, AlbumSortMode.ArtistAlbums)
        }
        albumGridAdapter = SimpleAlbumAdapter(
            requireActivity(),
            getArtist().sortedAlbums,
            R.layout.item_album,
            callback = this
        )

        // Horizontal albums
        val horizontalAlbumAdapter = SimpleAlbumAdapter(
            requireActivity(),
            getArtist().sortedAlbums,
            R.layout.item_image,
            callback = this
        )
        albumHorizontalAdapter = HorizontalListAdapter("", horizontalAlbumAdapter) {
            createSortOrderMenu(it, AlbumSortMode.ArtistAlbums)
        }

        // Songs
        songHeaderAdapter = SectionHeaderAdapter(getString(R.string.songs_label)) {
            createSortOrderMenu(it, SongSortMode.ArtistSongs)
        }
        createSongAdapter()

        // Similar artists
        val similarAdapter = ArtistAdapter(
            activity = requireActivity(),
            dataSet = emptyList(),
            itemLayoutRes = R.layout.item_artist,
            callback = this
        )
        similarArtistAdapter = HorizontalListAdapter(getString(R.string.similar_artists), similarAdapter)

        // Wiki
        wikiAdapter = WikiAdapter()

        val spanCount = defaultGridColumns()
        val layoutManager = GridLayoutManager(requireContext(), spanCount)
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                val adapterAndPosition = concatAdapter.getWrappedAdapterAndPosition(position)
                return if (adapterAndPosition.first == albumGridAdapter && !Preferences.horizontalArtistAlbums) 1 else spanCount
            }
        }

        updateConcatAdapter()
        binding.recyclerView.layoutManager = layoutManager
    }

    private fun updateConcatAdapter() {
        val config = ConcatAdapter.Config.Builder()
            .setStableIdMode(ConcatAdapter.Config.StableIdMode.ISOLATED_STABLE_IDS)
            .build()

        val adapters = mutableListOf<RecyclerView.Adapter<*>>()
        adapters.add(headerAdapter)

        if (Preferences.horizontalArtistAlbums) {
            adapters.add(albumHorizontalAdapter)
        } else {
            adapters.add(albumHeaderAdapter)
            adapters.add(albumGridAdapter)
        }

        adapters.add(songHeaderAdapter)
        adapters.add(songAdapter)
        adapters.add(similarArtistAdapter)
        adapters.add(wikiAdapter)

        concatAdapter = ConcatAdapter(config, adapters)
        if (_binding != null) {
            binding.recyclerView.adapter = concatAdapter
        }
    }

    private fun createSortOrderMenu(view: View, sortMode: SortMode) {
        val popupMenu = PopupMenu(view.context, view).apply {
            sortMode.createMenu(menu, hasSubMenu = false)
            setOnMenuItemClickListener { item ->
                if (sortMode.sortItemSelected(item)) {
                    detailViewModel.loadArtistDetail()
                    true
                } else false
            }
        }
        popupMenu.show()
    }

    private fun showArtist(artist: Artist) {
        if (artist == Artist.empty || artist.songCount == 0) {
            findNavController().navigateUp()
            return
        }

        val songText = plurals(R.plurals.songs, artist.songCount)
        val albumText = plurals(R.plurals.albums, artist.albumCount)

        headerAdapter.notifyItemChanged(0)
        loadBiography(artist.name)

        songHeaderAdapter.updateTitle(songText)
        songAdapter.dataSet = artist.sortedSongs

        albumHeaderAdapter.updateTitle(albumText)
        albumGridAdapter.dataSet = artist.sortedAlbums

        albumHorizontalAdapter.updateTitle(albumText)
        (albumHorizontalAdapter.innerAdapter as SimpleAlbumAdapter).dataSet = artist.sortedAlbums

        updateAlbumsVisibility()

        if (artist.isAlbumArtist) {
            loadSimilarArtists(artist)
        }
    }

    private fun updateAlbumsVisibility() {
        val hasAlbums = getArtist().sortedAlbums.isNotEmpty()
        albumHorizontalAdapter.setVisible(hasAlbums && Preferences.horizontalArtistAlbums)
        albumHeaderAdapter.setVisible(hasAlbums && !Preferences.horizontalArtistAlbums)
    }

    private fun loadBiography(name: String, lang: String? = Locale.getDefault().language) {
        this.biography = null
        this.lang = lang
        detailViewModel.getArtistBio(name, lang, null).observe(viewLifecycleOwner) { lastFmArtist ->
            if (lastFmArtist != null) {
                artistInfo(lastFmArtist)
            }
        }
    }

    private fun artistInfo(lastFmArtist: LastFmArtist?) {
        if (lastFmArtist?.artist?.bio != null) {
            val bioContent = lastFmArtist.artist.bio.content
            if (bioContent != null && bioContent.trim().isNotEmpty()) {
                biography = bioContent
                wikiAdapter.update(getString(R.string.about_x_title, getArtist().name), biography)
            }
        }

        // If the "lang" parameter is set and no biography is given, retry with default language
        if (biography == null && lang != null) {
            loadBiography(getArtist().name, null)
        }
    }

    private fun loadSimilarArtists(artist: Artist) {
        detailViewModel.getSimilarArtists(artist).observe(viewLifecycleOwner) { artists ->
            similarArtists(artists)
        }
    }

    private fun similarArtists(artists: List<Artist>) {
        if (artists.isNotEmpty()) {
            similarArtistAdapter.setVisible(true)
            (similarArtistAdapter.innerAdapter as ArtistAdapter).dataSet = artists
        } else {
            similarArtistAdapter.setVisible(false)
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
        album: Album,
        menuItem: MenuItem,
        sharedElements: Array<Pair<View, String>>?
    ): Boolean {
        return false
    }

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
        artist: Artist,
        menuItem: MenuItem,
        sharedElements: Array<Pair<View, String>>?
    ): Boolean = false

    override fun artistsMenuItemClick(artists: List<Artist>, menuItem: MenuItem) {
        artists.onArtistsMenu(this, menuItem)
    }

    override fun songMenuItemClick(
        song: Song,
        menuItem: MenuItem,
        sharedElements: Array<Pair<View, String>>?
    ): Boolean {
        if (menuItem.itemId == R.id.action_go_to_artist) {
            return true
        }
        return song.onSongMenu(this, menuItem)
    }

    override fun songsMenuItemClick(songs: List<Song>, menuItem: MenuItem) {
        songs.onSongsMenu(this, menuItem)
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_artist_detail, menu)
        if (!isLandscape()) {
            menu.removeItem(R.id.action_search)
        }
        menu.findItem(R.id.action_horizontal_albums)?.isChecked = Preferences.horizontalArtistAlbums
        menu.findItem(R.id.action_ignore_singles)?.isChecked = Preferences.ignoreSingles
        menu.findItem(R.id.action_toggle_compact_song_view)?.isChecked = Preferences.compactArtistSongView
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

            R.id.action_horizontal_albums -> {
                val isChecked = !menuItem.isChecked
                Preferences.horizontalArtistAlbums = isChecked
                menuItem.isChecked = isChecked
                updateAlbumsVisibility()
                updateConcatAdapter()
                true
            }

            R.id.action_ignore_singles -> {
                val isChecked = !menuItem.isChecked
                Preferences.ignoreSingles = isChecked
                menuItem.isChecked = isChecked
                detailViewModel.loadArtistDetail()
                true
            }

            R.id.action_toggle_compact_song_view -> {
                val isChecked = !menuItem.isChecked
                Preferences.compactArtistSongView = isChecked
                menuItem.isChecked = isChecked
                createSongAdapter()
                updateConcatAdapter()
                true
            }

            else -> getArtist().onArtistMenu(this, menuItem)
        }
    }

    private fun goToSearch() {
        findNavController().navigate(R.id.nav_search, searchArgs(getArtist().searchFilter(requireContext())))
    }

    private fun goToPlayInfo() {
        findNavController().navigate(R.id.nav_play_info, playInfoArgs(getArtist()))
    }

    override fun onMediaContentChanged() {
        super.onMediaContentChanged()
        detailViewModel.loadArtistDetail()
    }

    override fun onDestroyView() {
        binding.recyclerView.layoutManager = null
        binding.recyclerView.adapter = null
        super.onDestroyView()
        _binding = null
    }
}