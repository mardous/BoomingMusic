package com.mardous.booming.ui.component.base

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
import coil3.Image
import coil3.SingletonImageLoader
import coil3.dispose
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.target.Target
import coil3.toBitmap
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mardous.booming.data.local.EditTarget
import com.mardous.booming.databinding.ActivityTagEditorBinding
import com.mardous.booming.extensions.*
import com.mardous.booming.extensions.resources.setupStatusBarForeground
import com.mardous.booming.ui.screen.tageditor.TagEditorViewModel
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
        IntentCompat.getParcelableExtra(intent, EXTRA_TARGET, EditTarget::class.java) ?: EditTarget.Companion.Empty

    protected abstract val placeholderDrawable: Drawable

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

    override fun onDestroy() {
        binding.image.dispose()
        super.onDestroy()
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

    protected abstract fun downloadOnlineImage()

    protected abstract fun searchOnlineImage()

    protected abstract fun restoreImage()

    protected fun startImagePicker() {
        imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    protected open fun selectImage() {
        val items = arrayOf(
            getString(com.mardous.booming.R.string.download_image),
            getString(com.mardous.booming.R.string.pick_from_local_storage),
            getString(com.mardous.booming.R.string.web_search),
            getString(com.mardous.booming.R.string.restore_default),
            getString(com.mardous.booming.R.string.remove_cover)
        )
        MaterialAlertDialogBuilder(this)
            .setTitle(com.mardous.booming.R.string.update_image)
            .setItems(items) { _: DialogInterface, which: Int ->
                when (which) {
                    0 -> downloadOnlineImage()
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
        SingletonImageLoader.get(this).enqueue(
            ImageRequest.Builder(this)
                .data(url)
                .crossfade(false)
                .target(imageViewTarget)
                .build()
        )
    }

    protected open fun loadImageFromFile(selectedFileUri: Uri) {
        SingletonImageLoader.get(this).enqueue(
            ImageRequest.Builder(this)
                .data(selectedFileUri)
                .crossfade(false)
                .memoryCachePolicy(CachePolicy.READ_ONLY)
                .target(imageViewTarget)
                .build()
        )
    }

    private val imageViewTarget: Target by lazy {
        object : coil3.target.ImageViewTarget(binding.image) {
            override fun onError(error: Image?) {
                super.onError(error)
                setImageBitmap(null)
            }

            override fun onSuccess(result: Image) {
                super.onSuccess(result)
                val albumArtBitmap = result.toBitmap()
                setImageBitmap(albumArtBitmap)
                viewModel.setPictureBitmap(albumArtBitmap)
            }
        }
    }

    protected fun setImageBitmap(bitmap: Bitmap?) {
        if (bitmap == null) {
            binding.image.setImageDrawable(placeholderDrawable)
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
                showToast(com.mardous.booming.R.string.saving_changes)
            } else {
                binding.actionSave.show()
                if (result.isSuccess) {
                    showToast(com.mardous.booming.R.string.changes_saved_successfully)
                } else {
                    showToast(com.mardous.booming.R.string.could_not_save_some_changes)
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

    companion object {
        const val EXTRA_TARGET = "extra_target"
    }
}