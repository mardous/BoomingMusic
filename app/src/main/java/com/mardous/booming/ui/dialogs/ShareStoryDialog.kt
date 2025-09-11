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

package com.mardous.booming.ui.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
import androidx.core.os.BundleCompat
import androidx.core.view.drawToBitmap
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.ImageViewTarget
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mardous.booming.R
import com.mardous.booming.data.local.MediaStoreWriter
import com.mardous.booming.data.model.Song
import com.mardous.booming.databinding.DialogShareToStoriesBinding
import com.mardous.booming.extensions.*
import com.mardous.booming.extensions.glide.getSongGlideModel
import com.mardous.booming.extensions.glide.songOptions
import com.mardous.booming.extensions.media.displayArtistName
import com.mardous.booming.extensions.media.isArtistNameUnknown
import com.mardous.booming.extensions.resources.toJPG
import com.mardous.booming.util.FileUtil
import org.koin.android.ext.android.inject
import java.util.Locale

class ShareStoryDialog : DialogFragment() {

    private var _binding: DialogShareToStoriesBinding? = null
    private val binding get() = _binding!!

    private val mediaStoreWriter: MediaStoreWriter by inject()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val song = BundleCompat.getParcelable(requireArguments(), EXTRA_SONG, Song::class.java)!!
        _binding = DialogShareToStoriesBinding.inflate(layoutInflater)
        binding.songTitle.text = song.title
        if (song.isArtistNameUnknown()) {
            binding.songArtist.isVisible = false
        } else {
            binding.songArtist.text = song.displayArtistName()
        }
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.share_to_stories)
            .setView(binding.root)
            .setPositiveButton(R.string.action_share) { _: DialogInterface, _: Int ->
                shareStory()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create { dialog ->
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).isEnabled = false
                Glide.with(this)
                    .asBitmap()
                    .songOptions(song)
                    .load(song.getSongGlideModel())
                    .centerCrop()
                    .into(object : ImageViewTarget<Bitmap>(binding.image) {
                        override fun setResource(resource: Bitmap?) {
                            dialog.getButton(DialogInterface.BUTTON_POSITIVE).isEnabled = true
                            if (resource == null) {
                                view.setImageResource(R.drawable.default_audio_art)
                            } else {
                                view.setImageBitmap(resource)
                            }
                        }
                    })
            }
    }

    private fun shareStory() {
        val imageName = String.format(Locale.getDefault(), "Story_%d.jpg", System.currentTimeMillis())
        val mediaStoreRequest = MediaStoreWriter.Request.forImage(imageName, BOOMING_STORIES_DIR_NAME, STORY_MIME_TYPE)

        val result = mediaStoreWriter.toMediaStore(EXTERNAL_CONTENT_URI, mediaStoreRequest) { os ->
            binding.root.drawToBitmap(Bitmap.Config.ARGB_8888).toJPG(stream = os)
        }

        when (result.resultCode) {
            MediaStoreWriter.Result.Code.SUCCESS -> sendData(result.uri)
            MediaStoreWriter.Result.Code.NO_SCOPED_STORAGE -> {
                val file = mediaStoreWriter.toFile(FileUtil.imagesDirectory(BOOMING_STORIES_DIR_NAME), imageName) { os ->
                    binding.root.drawToBitmap(Bitmap.Config.ARGB_8888).toJPG(stream = os)
                }
                if (file != null) {
                    MediaScannerConnection.scanFile(requireContext(), arrayOf(file.absolutePath), arrayOf(STORY_MIME_TYPE)) { _: String, uri: Uri ->
                        sendData(uri)
                    }
                } else {
                    showError()
                }
            }

            else -> showError()
        }
    }

    private fun sendData(uri: Uri?) {
        if (uri == null) {
            showError()
        } else {
            val feedIntent = Intent(Intent.ACTION_SEND).apply {
                type = MIME_TYPE_IMAGE
                putExtra(Intent.EXTRA_STREAM, uri)
            }
            startActivity(feedIntent.toChooser(getString(R.string.share_to_stories)))
        }
    }

    private fun showError() {
        context?.showToast(R.string.could_not_create_the_story)
    }

    companion object {
        private const val BOOMING_STORIES_DIR_NAME = "Booming Design"
        private const val STORY_MIME_TYPE = "image/jpeg"

        fun create(song: Song) = ShareStoryDialog().withArgs {
            putParcelable(EXTRA_SONG, song)
        }
    }
}