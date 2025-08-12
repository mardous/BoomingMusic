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
package com.mardous.booming.ui.screen.info

import android.app.Dialog
import android.os.Bundle
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.core.view.isGone
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mardous.booming.R
import com.mardous.booming.databinding.FragmentSongDetailsBinding
import com.mardous.booming.extensions.create
import com.mardous.booming.extensions.resources.secondaryColor
import com.mardous.booming.extensions.resources.show
import com.mardous.booming.extensions.toHtml
import com.mardous.booming.data.model.Song
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * @author Christians M. A. (mardous)
 */
class SongDetailFragment : DialogFragment() {

    private val navArgs: SongDetailFragmentArgs by navArgs()
    private val viewModel: InfoViewModel by viewModel()

    private val song: Song
        get() = navArgs.extraSong

    private var _binding: FragmentSongDetailsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = FragmentSongDetailsBinding.inflate(layoutInflater)
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.action_details)
            .setView(binding.root)
            .setPositiveButton(R.string.close_action, null)
            .create {
                viewModel.songDetail(requireContext(), song).observe(this) { detail ->
                    binding.progressIndicator.hide()
                    if (detail == SongInfoResult.Empty) {
                        binding.empty.show()
                    } else {
                        // Fill media info
                        fillInfoView(R.id.title, R.string.title, detail.title)
                        fillInfoView(R.id.artist, R.string.artist, detail.artist)
                        fillInfoView(R.id.album, R.string.album, detail.album)
                        fillInfoView(R.id.album_artist, R.string.album_artist, detail.albumArtist)
                        fillInfoView(R.id.track_number, R.string.track, detail.trackNumber)
                        fillInfoView(R.id.disc_number, R.string.disc, detail.discNumber)
                        fillInfoView(R.id.year, R.string.year, detail.albumYear)
                        fillInfoView(R.id.genre, R.string.genre, detail.genre)
                        fillInfoView(R.id.composer, R.string.composer, detail.composer)
                        fillInfoView(R.id.conductor, R.string.conductor, detail.conductor)
                        fillInfoView(R.id.publisher, R.string.publisher, detail.publisher)

                        // Fill playback stats
                        fillInfoView(R.id.played, R.string.played, detail.playCount)
                        fillInfoView(R.id.skipped, R.string.skipped, detail.skipCount)
                        fillInfoView(R.id.last_played, R.string.last_played, detail.lastPlayedDate)

                        // Fill file info
                        fillInfoView(R.id.length, R.string.length, detail.trackLength)
                        fillInfoView(R.id.replay_gain, R.string.replay_gain, detail.replayGain)
                        fillInfoView(R.id.size, R.string.size, detail.fileSize)
                        fillInfoView(R.id.path, R.string.label_file_path, detail.filePath)
                        fillInfoView(R.id.audio_header, R.string.label_audio_header, detail.audioHeader)
                        fillInfoView(R.id.last_modified, R.string.label_last_modified, detail.dateModified)
                        fillInfoView(R.id.comment, R.string.comment, detail.comment)

                        binding.container.show()
                    }
                }
            }
    }

    private fun fillInfoView(@IdRes idRes: Int, titleRes: Int, info: String?) {
        val textView = binding.container.findViewById<TextView>(idRes)
        if (info.isNullOrEmpty()) {
            textView.isGone = true
        } else {
            val hexColor = String.format("#%06X", 0xFFFFFF and secondaryColor())
            val text = "<b><font color=$hexColor>${getString(titleRes)}</font></b>: $info".toHtml()
            textView.setText(titleRes)
            textView.text = text
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}