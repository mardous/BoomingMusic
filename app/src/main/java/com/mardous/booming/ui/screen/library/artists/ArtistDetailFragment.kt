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
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.core.view.updatePaddingRelative
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil3.request.crossfade
import com.google.android.material.transition.MaterialArcMotion
import com.google.android.material.transition.MaterialContainerTransform
import com.mardous.booming.R
import com.mardous.booming.coil.artistImage
import com.mardous.booming.core.model.task.Result
import com.mardous.booming.core.sort.AlbumSortMode
import com.mardous.booming.core.sort.SongSortMode
import com.mardous.booming.core.sort.SortMode
import com.mardous.booming.data.mapper.searchFilter
import com.mardous.booming.data.model.Album
import com.mardous.booming.data.model.Artist
import com.mardous.booming.data.model.Song
import com.mardous.booming.data.remote.lastfm.model.LastFmArtist
import com.mardous.booming.databinding.FragmentArtistDetailBinding
import com.mardous.booming.extensions.*
import com.mardous.booming.extensions.media.artistInfo
import com.mardous.booming.extensions.media.displayName
import com.mardous.booming.extensions.navigation.*
import com.mardous.booming.extensions.resources.*
import com.mardous.booming.service.playback.Playback
import com.mardous.booming.ui.IAlbumCallback
import com.mardous.booming.ui.IArtistCallback
import com.mardous.booming.ui.ISongCallback
import com.mardous.booming.ui.adapters.album.AlbumAdapter
import com.mardous.booming.ui.adapters.album.SimpleAlbumAdapter
import com.mardous.booming.ui.adapters.artist.ArtistAdapter
import com.mardous.booming.ui.adapters.song.SimpleSongAdapter
import com.mardous.booming.ui.component.base.AbsMainActivityFragment
import com.mardous.booming.ui.component.menu.*
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

    private var _binding: ArtistDetailBinding? = null
    private val binding get() = _binding!!

    private var lang: String? = null
    private var biography: String? = null

    private lateinit var songAdapter: SimpleSongAdapter
    private lateinit var albumAdapter: AlbumAdapter

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
        _binding = ArtistDetailBinding(FragmentArtistDetailBinding.bind(view))
        setSupportActionBar(binding.toolbar, "")
        materialSharedAxis(view, prepareTransition = false)

        view.applyHorizontalWindowInsets()

        binding.appBarLayout.setupStatusBarForeground()
        binding.image.removeHorizontalMarginIfRequired()
        binding.image.transitionName = if (isAlbumArtist) arguments.artistName
        else arguments.artistId.toString()

        postponeEnterTransition()
        detailViewModel.getArtistDetail().observe(viewLifecycleOwner) { result ->
            view.doOnPreDraw {
                startPostponedEnterTransition()
            }
            showArtist(result)
        }

        libraryViewModel.getMiniPlayerMargin().observe(viewLifecycleOwner) {
            binding.container.updatePadding(bottom = it.getWithSpace(16.dp(resources)))
        }

        setupRecyclerView()
        setupSortOrder()

        binding.playAction.setOnClickListener {
            playerViewModel.openQueue(getArtist().sortedSongs, shuffleMode = Playback.ShuffleMode.Off)
        }
        binding.shuffleAction.setOnClickListener {
            playerViewModel.openQueue(getArtist().sortedSongs, shuffleMode = Playback.ShuffleMode.On)
        }
        binding.searchAction?.setOnClickListener {
            goToSearch()
        }

        binding.biography.apply {
            setOnClickListener {
                maxLines = (if (maxLines == 4) Integer.MAX_VALUE else 4)
            }
        }

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
        binding.songRecyclerView.safeUpdateWithRetry { adapter = songAdapter }
    }

    private fun setupRecyclerView() {
        setupAlbumGrid()
        binding.albumRecyclerView.apply {
            itemAnimator = DefaultItemAnimator()
            destroyOnDetach()
        }
        binding.songRecyclerView.apply {
            layoutManager = LinearLayoutManager(this.context)
            itemAnimator = DefaultItemAnimator()
            destroyOnDetach()
        }
        createSongAdapter()
    }

    private fun setupSortOrder() {
        createSortOrderMenu(binding.songSortOrder, SongSortMode.ArtistSongs)
        createSortOrderMenu(binding.albumSortOrder, AlbumSortMode.ArtistAlbums)
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
        view.setOnClickListener { popupMenu.show() }
    }

    private fun setupAlbumGrid() {
        val horizontalAlbums = Preferences.horizontalArtistAlbums
        val padding = if (horizontalAlbums) {
            dip(R.dimen.grid_item_horizontal_margin)
        } else {
            dip(R.dimen.grid_item_album_margin)
        }
        binding.albumRecyclerView.safeUpdateWithRetry {
            updatePaddingRelative(start = padding, end = padding)
            layoutManager = createAlbumLayout(horizontalAlbums)
            adapter = createAlbumAdapter(horizontalAlbums)
        }
    }

    private fun createAlbumLayout(horizontal: Boolean): RecyclerView.LayoutManager {
        return if (horizontal) {
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        } else {
            GridLayoutManager(requireContext(), defaultGridColumns())
        }
    }

    private fun createAlbumAdapter(horizontal: Boolean): AlbumAdapter {
        albumAdapter = SimpleAlbumAdapter(
            requireActivity(),
            getArtist().sortedAlbums,
            if (horizontal) R.layout.item_image else R.layout.item_album,
            callback = this
        )
        return albumAdapter
    }

    private fun showArtist(artist: Artist) {
        if (artist.songCount == 0) {
            findNavController().navigateUp()
            return
        }

        binding.image.artistImage(artist) { crossfade(false) }

        if (requireContext().isAllowedToDownloadMetadata()) {
            loadBiography(artist.name)
        }
        binding.artistTitle.text = artist.displayName()
        binding.artistText.text = artist.artistInfo(requireContext())

        val songText = plurals(R.plurals.songs, artist.songCount)
        val albumText = plurals(R.plurals.albums, artist.albumCount)
        binding.songTitle.text = songText
        binding.albumTitle.text = albumText
        songAdapter.dataSet = artist.sortedSongs

        val albums = artist.sortedAlbums
        albumAdapter.dataSet = artist.sortedAlbums
        binding.albumTitle.isVisible = albums.isNotEmpty()
        binding.albumSortOrder.isVisible = albums.isNotEmpty()
        binding.albumRecyclerView.isVisible = albums.isNotEmpty()

        if (artist.isAlbumArtist) {
            loadSimilarArtists(artist)
        }
    }

    private fun loadBiography(name: String, lang: String? = Locale.getDefault().language) {
        this.biography = null
        this.lang = lang
        detailViewModel.getArtistBio(name, lang, null).observe(viewLifecycleOwner) { result ->
            if (result is Result.Success) {
                artistInfo(result.data)
            }
        }
    }


    private fun artistInfo(lastFmArtist: LastFmArtist?) {
        if (lastFmArtist?.artist?.bio != null) {
            val bioContent = lastFmArtist.artist.bio.content
            if (bioContent != null && bioContent.trim().isNotEmpty()) {
                biography = bioContent
                val biographyView = binding.biography
                biographyView.show()
                biographyView.setMarkdownText(bioContent)
                val biographyTitleView = binding.biographyTitle
                biographyTitleView.text = getString(R.string.about_x_title, getArtist().name)
                biographyTitleView.show()
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
            binding.similarArtistTitle.isVisible = true
            binding.similarArtistRecyclerView.apply {
                isVisible = true
                layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                adapter = ArtistAdapter(requireActivity(), artists, R.layout.item_artist, this@ArtistDetailFragment)
                destroyOnDetach()
            }
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
                setupAlbumGrid()
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
        _binding?.albumRecyclerView?.layoutManager = null
        _binding?.albumRecyclerView?.adapter = null
        _binding?.songRecyclerView?.layoutManager = null
        _binding?.songRecyclerView?.adapter = null
        _binding?.similarArtistRecyclerView?.layoutManager = null
        _binding?.similarArtistRecyclerView?.adapter = null
        super.onDestroyView()
        _binding = null
    }
}