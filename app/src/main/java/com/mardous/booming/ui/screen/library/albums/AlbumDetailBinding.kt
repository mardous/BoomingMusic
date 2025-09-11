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

import com.mardous.booming.databinding.FragmentAlbumDetailBinding

/**
 * @author Christians M. A. (mardous)
 */
class AlbumDetailBinding(binding: FragmentAlbumDetailBinding) {
    val appBarLayout = binding.appBarLayout
    val toolbar = binding.toolbar
    val image = binding.image
    val albumTitle = binding.albumTitle
    val albumText = binding.albumText
    val playAction = binding.playAction
    val shuffleAction = binding.shuffleAction
    val searchAction = binding.searchAction
    val songTitle = binding.fragmentAlbumContent.songTitle
    val songSortOrder = binding.fragmentAlbumContent.songSortOrder
    val songRecyclerView = binding.fragmentAlbumContent.recyclerView
    val similarAlbumTitle = binding.fragmentAlbumContent.moreTitle
    val similarAlbumSortOrder = binding.fragmentAlbumContent.similarAlbumSortOrder
    val similarAlbumRecyclerView = binding.fragmentAlbumContent.moreRecyclerView
    val wikiTitle = binding.fragmentAlbumContent.wikiTitle
    val wiki = binding.fragmentAlbumContent.wiki
    val container = binding.container
}