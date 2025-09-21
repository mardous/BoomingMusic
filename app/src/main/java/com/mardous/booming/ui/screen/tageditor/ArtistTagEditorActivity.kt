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

package com.mardous.booming.ui.screen.tageditor

import android.content.DialogInterface
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.toBitmap
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mardous.booming.R
import com.mardous.booming.coil.DEFAULT_ARTIST_IMAGE
import com.mardous.booming.data.local.MetadataReader
import com.mardous.booming.databinding.TagEditorArtistFieldBinding
import com.mardous.booming.extensions.webSearch
import com.mardous.booming.ui.component.base.AbsTagEditorActivity
import com.mardous.booming.ui.component.views.getPlaceholderDrawable
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

/**
 * @author Christians M. A. (mardous)
 */
class ArtistTagEditorActivity : AbsTagEditorActivity() {

    override val viewModel by viewModel<TagEditorViewModel> {
        parametersOf(getEditTarget())
    }

    override val placeholderDrawable: Drawable by lazy {
        getPlaceholderDrawable(DEFAULT_ARTIST_IMAGE)
    }

    private lateinit var artistBinding: TagEditorArtistFieldBinding

    private var albumArtistName: String? = null
    private var artistName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.image.setImageDrawable(placeholderDrawable)
        viewModel.tagResult.observe(this) {
            artistName = it.artist
            albumArtistName = it.albumArtist
            artistBinding.albumArtist.setText(it.albumArtist)
            artistBinding.genre.setText(it.genre)
            artistBinding.discTotal.setText(it.discTotal)
        }
        viewModel.loadContent()
        loadCurrentArtistImage()
    }

    override fun onWrapFieldViews(inflater: LayoutInflater, parent: ViewGroup) {
        artistBinding = TagEditorArtistFieldBinding.inflate(inflater, parent, true).apply {
            genreTextInputLayout.setEndIconOnClickListener {
                selectDefaultGenre()
            }
        }
    }

    override fun onDefaultGenreSelection(genre: String) {
        val genreContent = artistBinding.genre.text?.toString()
        if (!genreContent.isNullOrBlank()) {
            artistBinding.genre.setText(String.format("%s;%s", genreContent, genre))
        } else artistBinding.genre.setText(genre)
    }

    override fun selectImage() {
        val items = arrayOf(
            getString(R.string.set_artist_image),
            getString(R.string.web_search),
            getString(R.string.reset_artist_image)
        )
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.update_image)
            .setItems(items) { _: DialogInterface, which: Int ->
                when (which) {
                    0 -> startImagePicker()
                    1 -> searchOnlineImage()
                    2 -> deleteImage()
                }
            }
            .show()
    }

    override fun downloadOnlineImage() {}

    override fun searchOnlineImage() {
        webSearch(artistName)
    }

    override fun restoreImage() {}

    override fun deleteImage() {
        viewModel.resetArtistImage().observe(this) { isSuccess ->
            if (isSuccess) {
                loadCurrentArtistImage()
            }
        }
    }

    private fun loadCurrentArtistImage() {
        viewModel.requestArtist().observe(this) { artist ->
            SingletonImageLoader.get(this)
                .enqueue(
                    ImageRequest.Builder(this)
                        .data(artist)
                        .listener(
                            onError = { _, _ ->
                                setImageBitmap(null)
                            },
                            onSuccess = { _, result ->
                                setImageBitmap(result.image.toBitmap())
                            }
                        )
                        .build()
                )
        }
    }

    override fun loadImageFromFile(selectedFileUri: Uri) {
        viewModel.setArtistImage(selectedFileUri).observe(this) {
            loadCurrentArtistImage()
        }
    }

    override val propertyMap: Map<String, String?>
        get() = buildMap {
            val albumArtist = artistBinding.albumArtist.text?.toString()
            val isSingleSong = viewModel.uris.size == 1
            if (albumArtist.isNullOrEmpty() && !artistName.equals(albumArtistName, true) && isSingleSong) {
                put(MetadataReader.ARTIST, albumArtist)
            }
            put(MetadataReader.ALBUM_ARTIST, albumArtist)
            put(MetadataReader.GENRE, artistBinding.genre.text?.toString())
            put(MetadataReader.DISC_TOTAL, artistBinding.discTotal.text?.toString())
        }
}