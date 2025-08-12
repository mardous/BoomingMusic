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

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import com.mardous.booming.R
import com.mardous.booming.databinding.TagEditorAlbumFieldBinding
import com.mardous.booming.extensions.glide.DEFAULT_ALBUM_IMAGE
import com.mardous.booming.extensions.resources.getDrawableCompat
import com.mardous.booming.extensions.resources.getResized
import com.mardous.booming.extensions.showToast
import com.mardous.booming.extensions.webSearch
import com.mardous.booming.core.model.task.Result
import com.mardous.booming.data.local.MetadataReader
import com.mardous.booming.data.local.toBitmap
import com.mardous.booming.ui.component.base.AbsTagEditorActivity
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

/**
 * @author Christians M. A. (mardous)
 */
class AlbumTagEditorActivity : AbsTagEditorActivity() {

    override val viewModel by viewModel<TagEditorViewModel> {
        parametersOf(getEditTarget())
    }

    private lateinit var albumBinding: TagEditorAlbumFieldBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.artworkResult.observe(this) { artwork ->
            setImageBitmap(artwork?.toBitmap()?.getResized(1080))
        }
        viewModel.tagResult.observe(this) { tagResult ->
            albumBinding.album.setText(tagResult.album)
            albumBinding.albumArtist.setText(tagResult.albumArtist)
            albumBinding.conductor.setText(tagResult.conductor)
            albumBinding.publisher.setText(tagResult.publisher)
            albumBinding.genre.setText(tagResult.genre)
            albumBinding.year.setText(tagResult.year)
            albumBinding.trackTotal.setText(tagResult.trackTotal)
            albumBinding.discNumber.setText(tagResult.discNumber)
            albumBinding.discTotal.setText(tagResult.discTotal)
        }
        viewModel.loadContent()
    }

    override fun onWrapFieldViews(inflater: LayoutInflater, parent: ViewGroup) {
        albumBinding = TagEditorAlbumFieldBinding.inflate(inflater, parent, true).apply {
            genreTextInputLayout.setEndIconOnClickListener {
                selectDefaultGenre()
            }
        }
    }

    override fun onDefaultGenreSelection(genre: String) {
        val genreContent = albumBinding.genre.text?.toString()
        if (!genreContent.isNullOrBlank()) {
            albumBinding.genre.setText(String.format("%s;%s", genreContent, genre))
        } else albumBinding.genre.setText(genre)
    }

    override fun getDefaultPlaceholder(): Drawable = getDrawableCompat(DEFAULT_ALBUM_IMAGE)!!

    override fun searchLastFMImage() {
        val albumTitleStr = albumBinding.album.text?.toString()
        val albumArtistNameStr = albumBinding.albumArtist.text?.toString()
        if (albumArtistNameStr.isNullOrBlank() || albumTitleStr.isNullOrBlank()) {
            showToast(resources.getString(R.string.album_or_artist_empty))
            return
        }
        viewModel.getAlbumInfo(albumArtistNameStr, albumTitleStr).observe(this) { result ->
            if (result is Result.Success) {
                val url = result.data.imageUrl
                if (!url.isNullOrEmpty()) {
                    loadImageFromUrl(url)
                } else {
                    showToast(R.string.could_not_download_album_cover)
                }
            } else {
                showToast(R.string.could_not_download_album_cover)
            }
        }
    }

    override fun searchOnlineImage() {
        webSearch(albumBinding.album.text.toString(), albumBinding.albumArtist.text.toString())
    }

    override fun restoreImage() {
        viewModel.loadArtwork()
    }

    override val propertyMap: Map<String, String?>
        get() = hashMapOf(
            MetadataReader.ALBUM to albumBinding.album.text?.toString(),
            MetadataReader.ALBUM_ARTIST to albumBinding.albumArtist.text?.toString(),
            MetadataReader.PRODUCER to albumBinding.conductor.text?.toString(),
            MetadataReader.COPYRIGHT to albumBinding.publisher.text?.toString(),
            MetadataReader.GENRE to albumBinding.genre.text?.toString(),
            MetadataReader.YEAR to albumBinding.year.text?.toString(),
            MetadataReader.TRACK_TOTAL to albumBinding.trackTotal.text?.toString(),
            MetadataReader.DISC_NUMBER to albumBinding.discNumber.text?.toString(),
            MetadataReader.DISC_TOTAL to albumBinding.discTotal.text?.toString()
        )
}