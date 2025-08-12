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

package com.mardous.booming.ui.dialogs.playlists

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mardous.booming.R
import com.mardous.booming.databinding.DialogProgressRecyclerViewBinding
import com.mardous.booming.extensions.create
import com.mardous.booming.extensions.media.getSpannedTitles
import com.mardous.booming.extensions.requestContext
import com.mardous.booming.extensions.showToast
import com.mardous.booming.ui.screen.library.LibraryViewModel
import com.mardous.booming.ui.screen.library.ImportablePlaylistResult
import org.koin.androidx.viewmodel.ext.android.activityViewModel

/**
 * @author Christians M. A. (mardous)
 */
class ImportPlaylistDialog : DialogFragment() {

    private var binding: DialogProgressRecyclerViewBinding? = null
    private val viewModel: LibraryViewModel by activityViewModel()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .also {
                binding = DialogProgressRecyclerViewBinding.inflate(layoutInflater.cloneInContext(it.context)).apply {
                    recyclerView.apply {
                        layoutManager = LinearLayoutManager(requireContext())
                        setHasFixedSize(true)
                    }
                }
            }
            .setTitle(R.string.action_import_playlist)
            .setView(binding!!.root)
            .setNegativeButton(android.R.string.cancel, null)
            .create {
                viewModel.getDevicePlaylists().observe(this) { result ->
                    if (result.isEmpty()) {
                        showToast(R.string.there_are_no_importable_playlists)
                        dismiss()
                    } else requestContext {
                        binding?.progressIndicator?.hide()
                        binding?.recyclerView?.adapter = Adapter(it, result)
                    }
                }
            }
    }

    private fun playlistSelected(selected: ImportablePlaylistResult) {
        val items = selected.songs.getSpannedTitles(requireContext())
        val checked = MutableList(items.size) { index -> index }
        val checkedItems = BooleanArray(checked.size) { true }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.select_songs_title)
            .setMultiChoiceItems(items.toTypedArray(), checkedItems) { _: DialogInterface, i: Int, b: Boolean ->
                if (b) checked.add(i) else checked.remove(i)
            }
            .setPositiveButton(R.string.import_action) { _: DialogInterface, _: Int ->
                val importSongs = selected.songs.filterIndexed { index, _ -> checked.contains(index) }
                if (importSongs.isEmpty()) {
                    showToast(getString(R.string.playlist_x_not_imported, selected.playlistName))
                } else {
                    viewModel.importPlaylist(requireContext(), selected).observe(this) { result ->
                        showToast(result.resultMessage)
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private inner class Adapter(private val context: Context, private val playlists: List<ImportablePlaylistResult>) :
        RecyclerView.Adapter<Adapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
            val icon: ImageView = itemView.findViewById(R.id.icon_view)
            val title: TextView = itemView.findViewById(R.id.title)
            val text: TextView = itemView.findViewById(R.id.text)

            init {
                text.isVisible = false
                icon.setImageResource(R.drawable.ic_queue_music_24dp)

                itemView.setOnClickListener(this)
            }

            override fun onClick(view: View?) {
                playlistSelected(playlists[bindingAdapterPosition])
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_dialog_list, parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.title.text = playlists[position].playlistName
        }

        override fun getItemCount(): Int = playlists.size
    }
}