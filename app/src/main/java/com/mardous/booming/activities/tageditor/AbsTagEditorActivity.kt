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

package com.mardous.booming.activities.tageditor

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.IntentCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.ImageViewTarget
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mardous.booming.R
import com.mardous.booming.activities.base.AbsBaseActivity
import com.mardous.booming.databinding.ActivityTagEditorBinding
import com.mardous.booming.extensions.*
import com.mardous.booming.extensions.resources.getResized
import com.mardous.booming.extensions.resources.setupStatusBarForeground
import com.mardous.booming.taglib.EditTarget
import com.mardous.booming.viewmodels.tageditor.TagEditorViewModel
import org.jaudiotagger.tag.reference.GenreTypes

/**
 * @author Christians M. A. (mardous)
 */
abstract class AbsTagEditorActivity : AbsBaseActivity(),
    View.OnClickListener {

    protected abstract val viewModel: TagEditorViewModel

    protected lateinit var binding: ActivityTagEditorBinding
    private lateinit var imagePickerLauncher: ActivityResultLauncher<PickVisualMediaRequest>
    private lateinit var writeRequestLauncher: ActivityResultLauncher<IntentSenderRequest>

    protected fun getEditTarget() =
        IntentCompat.getParcelableExtra(intent, EXTRA_TARGET, EditTarget::class.java) ?: EditTarget.Empty

    @SuppressLint("InlinedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        imagePickerLauncher =
            registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { result ->
                if (result != null) {
                    loadImageFromFile(result)
                }
            }
        writeRequestLauncher =
            registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
                if (it.resultCode == RESULT_OK) {
                    writeTags()
                }
            }

        binding = ActivityTagEditorBinding.inflate(layoutInflater)
        onWrapFieldViews(layoutInflater, binding.editorFieldContainer)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        binding.root.applyScrollableContentInsets(binding.container)
        binding.actionSave.applyBottomWindowInsets()

        binding.image.setOnClickListener(this@AbsTagEditorActivity)
        binding.actionSave.setOnClickListener(this@AbsTagEditorActivity)
        binding.appBar.setupStatusBarForeground()
    }

    override fun onClick(view: View) {
        when (view) {
            binding.image -> selectImage()
            binding.actionSave -> save()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    protected abstract fun onWrapFieldViews(inflater: LayoutInflater, parent: ViewGroup)

    protected open fun onDefaultGenreSelection(genre: String) {}

    protected abstract fun searchLastFMImage()

    protected abstract fun searchOnlineImage()

    protected abstract fun restoreImage()

    protected fun startImagePicker() {
        imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    protected open fun selectImage() {
        val items = arrayOf(
            getString(R.string.download_image),
            getString(R.string.pick_from_local_storage),
            getString(R.string.web_search),
            getString(R.string.restore_default),
            getString(R.string.remove_cover)
        )
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.update_image)
            .setItems(items) { _: DialogInterface, which: Int ->
                when (which) {
                    0 -> searchLastFMImage()
                    1 -> startImagePicker()
                    2 -> searchOnlineImage()
                    3 -> restoreImage()
                    4 -> deleteImage()
                }
            }
            .show()
    }

    protected open fun deleteImage() {
        setImageBitmap(null)
        viewModel.setPictureDeleted(true)
    }

    protected open fun loadImageFromUrl(url: String?) {
        Glide.with(this).asBitmap()
            .load(url)
            .dontAnimate()
            .error(getDefaultPlaceholder())
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .into(simpleGlideTarget)
    }

    protected open fun loadImageFromFile(selectedFileUri: Uri) {
        Glide.with(this).asBitmap()
            .load(selectedFileUri)
            .dontAnimate()
            .error(getDefaultPlaceholder())
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .into(simpleGlideTarget)
    }

    private val simpleGlideTarget: ImageViewTarget<Bitmap> by lazy {
        object : ImageViewTarget<Bitmap>(binding.image) {
            override fun setResource(resource: Bitmap?) {
                if (resource != null) {
                    val albumArtBitmap = resource.getResized(2048)
                    setImageBitmap(albumArtBitmap)
                    viewModel.setPictureBitmap(albumArtBitmap)
                } else {
                    setImageBitmap(null)
                }
            }
        }
    }

    protected fun setImageBitmap(bitmap: Bitmap?) {
        if (bitmap == null) {
            binding.image.setImageDrawable(getDefaultPlaceholder())
        } else {
            binding.image.setImageBitmap(bitmap)
        }
    }

    protected open fun save() {
        hideSoftKeyboard()
        if (hasR()) {
            val pendingIntent = MediaStore.createWriteRequest(contentResolver, viewModel.uris)
            writeRequestLauncher.launch(IntentSenderRequest.Builder(pendingIntent).build())
        } else {
            writeTags()
        }
    }

    private fun writeTags() {
        viewModel.write(this, propertyMap).observe(this) { result ->
            if (result.isLoading) {
                binding.actionSave.hide()
                showToast(R.string.saving_changes)
            } else {
                binding.actionSave.show()
                if (result.isSuccess) {
                    showToast(R.string.changes_saved_successfully)
                } else {
                    showToast(R.string.could_not_save_some_changes)
                }
            }
        }
    }

    protected abstract val propertyMap: Map<String, String?>

    private val defaultGenreSelector: Dialog by lazy {
        val titles = GenreTypes.getInstanceOf()
            .idToValueMap.values.sorted().toTypedArray()

        MaterialAlertDialogBuilder(this)
            .setItems(titles) { _: DialogInterface, i: Int -> onDefaultGenreSelection(titles[i]) }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }

    protected fun selectDefaultGenre() {
        defaultGenreSelector.show()
    }

    protected abstract fun getDefaultPlaceholder(): Drawable

    companion object {
        const val EXTRA_TARGET = "extra_target"
    }
}