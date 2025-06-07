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

package com.mardous.booming.dialogs.library

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_AUDIO
import android.app.Dialog
import android.content.DialogInterface
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle
import android.os.Environment.getExternalStorageDirectory
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat.checkSelfPermission
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mardous.booming.R
import com.mardous.booming.adapters.SimpleItemAdapter
import com.mardous.booming.databinding.DialogRecyclerViewBinding
import com.mardous.booming.extensions.create
import com.mardous.booming.extensions.hasT
import com.mardous.booming.extensions.resources.useLinearLayout
import com.mardous.booming.util.StorageUtil
import java.io.File
import java.io.IOException

/**
 * @author Christians M. A. (mardous)
 */
class FolderChooserDialog : DialogFragment(), SimpleItemAdapter.Callback<String> {

    private var initialPath: String = getExternalStorageDirectory().absolutePath
    private var parentFolder: File? = null
    private var parentContents: Array<File>? = null
    private var canGoUp = false
    private var callback: FolderCallback? = null

    private var _binding: DialogRecyclerViewBinding? = null
    private val binding get() = requireNotNull(_binding)

    private var adapter: SimpleItemAdapter<String>? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if (hasT()) {
            if (checkSelfPermission(requireActivity(), READ_MEDIA_AUDIO) != PERMISSION_GRANTED) {
                return permissionErrorDialog()
            }
        } else {
            if (checkSelfPermission(requireActivity(), READ_EXTERNAL_STORAGE) != PERMISSION_GRANTED) {
                return permissionErrorDialog()
            }
        }

        val mSavedInstanceState = savedInstanceState ?: Bundle()
        if (!mSavedInstanceState.containsKey("current_path")) {
            mSavedInstanceState.putString("current_path", initialPath)
        }

        parentFolder = File(mSavedInstanceState.getString("current_path", File.pathSeparator))
        checkIfCanGoUp()
        parentContents = listFiles()

        _binding = DialogRecyclerViewBinding.inflate(layoutInflater)
        adapter = SimpleItemAdapter(
            R.layout.item_folder,
            items = getSelectionContents(),
            callback = this
        )
        binding.recyclerView.useLinearLayout()
        binding.recyclerView.adapter = adapter
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(getTitle(parentFolder))
            .setView(binding.root)
            .setPositiveButton(R.string.add_action, null)
            .setNegativeButton(android.R.string.cancel, null)
            .create { dialog ->
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                    callback?.onFolderSelection(this, parentFolder!!)
                    dialog.dismiss()
                }
            }
    }

    private fun permissionErrorDialog(): AlertDialog =
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.permissions_denied)
            .setMessage(R.string.permission_external_storage_denied)
            .setPositiveButton(android.R.string.ok, null)
            .create()

    override fun bindData(itemView: View, position: Int, item: String): Boolean {
        val textView = itemView.findViewById<TextView>(android.R.id.text1)
        try {
            if (isEmulatedDir(parentFolder)) {
                val folder = parentContents?.getOrNull(if (canGoUp) position - 1 else position)
                if (folder != null) {
                    val device = StorageUtil.getStorageDevice(folder)
                    if (device != null) {
                        textView.text = device.fileName
                        textView.setCompoundDrawablesRelativeWithIntrinsicBounds(device.iconRes, 0, 0, 0)
                        return true
                    }
                }
            }
        } catch (_: IOException) {}
        textView.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_folder_24dp, 0, 0, 0)
        return false
    }

    override fun itemClick(itemView: View, position: Int, item: String) {
        onSelection(position)
    }

    private fun onSelection(i: Int) {
        if (canGoUp && i == 0) {
            parentFolder = parentFolder?.parentFile
            checkIfCanGoUp()
        } else {
            parentFolder = parentContents?.getOrNull(if (canGoUp) i - 1 else i)
            canGoUp = true
        }
        reload()
    }

    private fun getSelectionContents(): List<String> {
        return if (parentContents == null) {
            if (canGoUp) listOf("..") else listOf()
        } else {
            val results = ArrayList<String>(parentContents!!.size + if (canGoUp) 1 else 0)
            if (canGoUp) {
                results.add("..")
            }
            results.also {
                it.addAll(parentContents!!.map { file -> file.name })
            }
        }
    }

    private fun listFiles(): Array<File>? {
        if (isEmulatedDir(parentFolder)) {
            return StorageUtil.storageVolumes.map { it.file }.toTypedArray()
        }
        val results = mutableListOf<File>()
        parentFolder?.listFiles()?.let { files ->
            files.forEach { file -> if (file.isDirectory) results.add(file) }
            return results.sortedBy { it.name }.toTypedArray()
        }
        return null
    }

    private fun checkIfCanGoUp() {
        if (isEmulatedDir(parentFolder)) {
            canGoUp = false
            return
        }
        canGoUp = parentFolder?.parentFile != null
    }

    private fun isEmulatedDir(folder: File?): Boolean {
        if (folder == null)
            return false

        return StorageUtil.storageVolumes.any {
            it.file.parent == folder.canonicalPath
        }
    }

    private fun reload() {
        parentContents = listFiles()
        if (isEmulatedDir(parentFolder)) {
            dialog?.setTitle(R.string.select_storage_title)
        } else {
            dialog?.setTitle(getTitle(parentFolder))
        }
        adapter?.items = getSelectionContents()
    }

    private fun getTitle(folder: File?): CharSequence? {
        val device = folder?.let {
            StorageUtil.getStorageDevice(it)
        }
        return device?.fileName ?: folder?.name
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("current_path", parentFolder?.absolutePath)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun setCallback(callback: FolderCallback) {
        this.callback = callback
    }

    interface FolderCallback {
        fun onFolderSelection(dialog: FolderChooserDialog, folder: File)
    }
}