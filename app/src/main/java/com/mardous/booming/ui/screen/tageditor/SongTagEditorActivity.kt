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
import com.mardous.booming.coil.DEFAULT_SONG_IMAGE
import com.mardous.booming.core.model.task.Result
import com.mardous.booming.data.local.MetadataReader
import com.mardous.booming.data.local.toBitmap
import com.mardous.booming.databinding.TagEditorSongFieldBinding
import com.mardous.booming.extensions.resources.getDrawableCompat
import com.mardous.booming.extensions.resources.getResized
import com.mardous.booming.extensions.showToast
import com.mardous.booming.extensions.webSearch
import com.mardous.booming.ui.component.base.AbsTagEditorActivity
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

/**
 * @author Christians M. A. (mardous)
 */
class SongTagEditorActivity : AbsTagEditorActivity() {

    override val viewModel by viewModel<TagEditorViewModel> {
        parametersOf(getEditTarget())
    }

    private var _songBinding: TagEditorSongFieldBinding? = null
    private val songBinding get() = _songBinding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.artworkResult.observe(this) { picture ->
            setImageBitmap(picture?.toBitmap()?.getResized(1080))
        }
        viewModel.tagResult.observe(this) { tagResult ->
            songBinding.title.setText(tagResult.title)
            songBinding.album.setText(tagResult.album)
            songBinding.artist.setText(tagResult.artist)
            songBinding.albumArtist.setText(tagResult.albumArtist)
            songBinding.composer.setText(tagResult.composer)
            songBinding.conductor.setText(tagResult.conductor)
            songBinding.publisher.setText(tagResult.publisher)
            songBinding.genre.setText(tagResult.genre)
            songBinding.year.setText(tagResult.year)
            songBinding.trackNumber.setText(tagResult.trackNumber)
            songBinding.trackTotal.setText(tagResult.trackTotal)
            songBinding.discNumber.setText(tagResult.discNumber)
            songBinding.discTotal.setText(tagResult.discTotal)
            songBinding.lyrics.setText(tagResult.lyrics)
            songBinding.lyricist.setText(tagResult.lyricist)
            songBinding.comment.setText(tagResult.comment)
        }
        viewModel.loadContent()
    }

    override fun onWrapFieldViews(inflater: LayoutInflater, parent: ViewGroup) {
        _songBinding = TagEditorSongFieldBinding.inflate(inflater, parent, true).apply {
            genreTextInputLayout.setEndIconOnClickListener {
                selectDefaultGenre()
            }
        }
    }

    override fun onDefaultGenreSelection(genre: String) {
        val genreContent = songBinding.genre.text?.toString()
        if (!genreContent.isNullOrBlank()) {
            songBinding.genre.setText(String.format("%s;%s", genreContent, genre))
        } else songBinding.genre.setText(genre)
    }

    override fun getDefaultPlaceholder(): Drawable =
        getDrawableCompat(DEFAULT_SONG_IMAGE)!!

    override fun downloadOnlineImage() {
        val songTitleStr = songBinding.title.text?.toString()
        var albumArtistNameStr = songBinding.albumArtist.text?.toString()
        if (albumArtistNameStr.isNullOrBlank()) {
            albumArtistNameStr = songBinding.artist.text?.toString()
        }
        if (songTitleStr.isNullOrBlank() || albumArtistNameStr.isNullOrBlank()) {
            showToast(resources.getString(R.string.album_or_artist_empty))
            return
        }
        viewModel.getTrackInfo(albumArtistNameStr, songTitleStr).observe(this) { result ->
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
        webSearch(songBinding.title.text.toString(), songBinding.artist.text.toString())
    }

    override fun restoreImage() {
        viewModel.loadArtwork()
    }

    override val propertyMap: Map<String, String?>
        get() = hashMapOf(
            MetadataReader.TITLE to songBinding.title.text?.toString(),
            MetadataReader.ALBUM to songBinding.album.text?.toString(),
            MetadataReader.ARTIST to songBinding.artist.text?.toString(),
            MetadataReader.ALBUM_ARTIST to songBinding.albumArtist.text?.toString(),
            MetadataReader.COMPOSER to songBinding.composer.text?.toString(),
            MetadataReader.PRODUCER to songBinding.conductor.text?.toString(),
            MetadataReader.COPYRIGHT to songBinding.publisher.text?.toString(),
            MetadataReader.GENRE to songBinding.genre.text?.toString(),
            MetadataReader.YEAR to songBinding.year.text?.toString(),
            MetadataReader.TRACK_NUMBER to songBinding.trackNumber.text?.toString(),
            MetadataReader.TRACK_TOTAL to songBinding.trackTotal.text?.toString(),
            MetadataReader.DISC_NUMBER to songBinding.discNumber.text?.toString(),
            MetadataReader.DISC_TOTAL to songBinding.discTotal.text?.toString(),
            MetadataReader.LYRICS to songBinding.lyrics.text?.toString(),
            MetadataReader.LYRICIST to songBinding.lyricist.text?.toString(),
            MetadataReader.COMMENT to songBinding.comment.text?.toString()
        )

    override fun onDestroy() {
        super.onDestroy()
        _songBinding = null
    }
}