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

import com.mardous.booming.databinding.FragmentArtistDetailBinding

/**
 * @author Christians M. A. (mardous)
 */
class ArtistDetailBinding(binding: FragmentArtistDetailBinding) {
    val appBarLayout = binding.appBarLayout
    val toolbar = binding.toolbar
    val image = binding.image
    val artistTitle = binding.artistTitle
    val artistText = binding.artistText
    val playAction = binding.playAction
    val shuffleAction = binding.shuffleAction
    val searchAction = binding.searchAction
    val songTitle = binding.fragmentArtistContent.songTitle
    val songSortOrder = binding.fragmentArtistContent.songSortOrder
    val songRecyclerView = binding.fragmentArtistContent.recyclerView
    val albumTitle = binding.fragmentArtistContent.albumTitle
    val albumSortOrder = binding.fragmentArtistContent.albumSortOrder
    val albumRecyclerView = binding.fragmentArtistContent.albumRecyclerView
    val similarArtistTitle = binding.fragmentArtistContent.similarArtistTitle
    val similarArtistRecyclerView = binding.fragmentArtistContent.similarRecyclerView
    val biographyTitle = binding.fragmentArtistContent.biographyTitle
    val biography = binding.fragmentArtistContent.biography
    val container = binding.container
}