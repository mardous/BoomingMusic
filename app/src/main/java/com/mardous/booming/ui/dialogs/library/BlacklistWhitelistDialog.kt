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

package com.mardous.booming.ui.dialogs.library

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mardous.booming.R
import com.mardous.booming.data.local.room.InclExclDao
import com.mardous.booming.data.local.room.InclExclEntity
import com.mardous.booming.databinding.DialogProgressRecyclerViewBinding
import com.mardous.booming.extensions.create
import com.mardous.booming.extensions.extraNotNull
import com.mardous.booming.extensions.files.getCanonicalPathSafe
import com.mardous.booming.extensions.withArgs
import com.mardous.booming.ui.adapters.SimpleItemAdapter
import com.mardous.booming.ui.dialogs.library.FolderChooserDialog.FolderCallback
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import java.io.File

/**
 * @author Christians M. A. (mardous)
 */
class BlacklistWhitelistDialog : DialogFragment(), FolderCallback, SimpleItemAdapter.Callback<String> {

    private val listType: Int by extraNotNull(EXTRA_TYPE)

    private var _binding: DialogProgressRecyclerViewBinding? = null
    private val binding get() = _binding!!

    private val inclExclDao: InclExclDao by inject<InclExclDao>()
    private val isBlacklist: Boolean
        get() = listType == InclExclDao.BLACKLIST

    private var adapter: SimpleItemAdapter<String>? = null
    private var paths = listOf<String>()

    @SuppressLint("CheckResult")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val folderChooserDialog = childFragmentManager.findFragmentByTag("FOLDER_CHOOSER") as FolderChooserDialog?
        folderChooserDialog?.setCallback(this)
        _binding = DialogProgressRecyclerViewBinding.inflate(layoutInflater)
        adapter = SimpleItemAdapter(R.layout.item_folder, callback = this)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        val titleRes = if (isBlacklist) {
            R.string.blacklist_title
        } else {
            R.string.whitelist_title
        }
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(titleRes)
            .setView(binding.root)
            .setNegativeButton(R.string.close_action, null)
            .setNeutralButton(R.string.clear_action, null)
            .create { dialog ->
                dialog.getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener {
                    showClearDialog()
                }
                refreshData()
            }
    }

    override fun bindData(itemView: View, position: Int, item: String): Boolean {
        if (position == 0) {
            val textView = itemView.findViewById<TextView>(android.R.id.text1)
            textView.text = item
            textView.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_add_24dp, 0, 0, 0)
            return true
        }
        return false
    }

    override fun itemClick(itemView: View, position: Int, item: String) {
        if (position == 0) {
            FolderChooserDialog().also { it.setCallback(this) }
                .show(childFragmentManager, "FOLDER_CHOOSER")
        } else {
            val messageRes = if (isBlacklist) R.string.remove_from_blacklist
            else R.string.remove_from_whitelist
            MaterialAlertDialogBuilder(requireContext())
                .setMessage(messageRes)
                .setPositiveButton(R.string.remove_action) { _: DialogInterface, _: Int ->
                    lifecycleScope.launch(IO) {
                        inclExclDao.deletePath(InclExclEntity(item, listType))
                        refreshData()
                    }
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    override fun onFolderSelection(dialog: FolderChooserDialog, folder: File) {
        lifecycleScope.launch(IO) {
            inclExclDao.insertPath(InclExclEntity(folder.getCanonicalPathSafe(), listType))
            refreshData()
        }
    }

    private fun showClearDialog() {
        if (paths.isEmpty()) return

        val messageRes = if (isBlacklist) R.string.do_you_want_to_clear_the_blacklist else R.string.do_you_want_to_clear_the_whitelist
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(messageRes)
            .setPositiveButton(R.string.clear_action) { _: DialogInterface, _: Int ->
                lifecycleScope.launch(IO) {
                    inclExclDao.clearPaths(listType)
                    refreshData()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun refreshData() {
        lifecycleScope.launch(IO) {
            paths = inclExclDao.getPaths(listType).map { it.path }
            withContext(Main) {
                _binding?.progressIndicator?.isVisible = false
                adapter?.items = arrayListOf(getString(R.string.add_directory)).also {
                    it.addAll(paths)
                }
            }
        }
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
        adapter = null
    }

    companion object {
        private const val EXTRA_TYPE = "type"

        fun newInstance(listType: Int): BlacklistWhitelistDialog {
            return BlacklistWhitelistDialog().withArgs {
                putInt(EXTRA_TYPE, listType)
            }
        }
    }
}