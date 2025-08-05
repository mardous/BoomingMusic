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

package com.mardous.booming.fragments.lyrics

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipboardManager
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Process
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult
import androidx.core.app.ShareCompat
import androidx.core.content.getSystemService
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mardous.booming.R
import com.mardous.booming.databinding.DialogLyricsSelectorBinding
import com.mardous.booming.databinding.DialogSongSearchBinding
import com.mardous.booming.databinding.FragmentLyricsEditorBinding
import com.mardous.booming.extensions.*
import com.mardous.booming.extensions.glide.getDefaultGlideTransition
import com.mardous.booming.extensions.glide.getSongGlideModel
import com.mardous.booming.extensions.glide.songOptions
import com.mardous.booming.extensions.media.albumArtistName
import com.mardous.booming.extensions.media.displayArtistName
import com.mardous.booming.extensions.media.isArtistNameUnknown
import com.mardous.booming.extensions.resources.animateToggle
import com.mardous.booming.extensions.resources.requestInputMethod
import com.mardous.booming.fragments.base.AbsMainActivityFragment
import com.mardous.booming.http.Result
import com.mardous.booming.lyrics.LyricsSource
import com.mardous.booming.model.DownloadedLyrics
import com.mardous.booming.model.Song
import com.mardous.booming.viewmodels.lyrics.LyricsViewModel
import com.mardous.booming.viewmodels.lyrics.model.LyricsResult
import org.koin.androidx.viewmodel.ext.android.activityViewModel

/**
 * @author Christians M. A. (mardous)
 */
class LyricsEditorFragment : AbsMainActivityFragment(R.layout.fragment_lyrics_editor) {

    private val lyricsViewModel: LyricsViewModel by activityViewModel()
    private val args: LyricsEditorFragmentArgs by navArgs()
    private val song get() = args.extraSong

    private var _binding: FragmentLyricsEditorBinding? = null
    private val binding get() = _binding!!

    private lateinit var permissionLauncher: ActivityResultLauncher<IntentSenderRequest>
    private lateinit var importLyricsLauncher: ActivityResultLauncher<Array<String>>

    private var lyricsResult: LyricsResult? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLyricsEditorBinding.bind(view)

        materialSharedAxis(view)
        view.applyWindowInsets(left = true, right = true, bottom = true)
        setSupportActionBar(binding.toolbar)
        permissionLauncher = registerForActivityResult(StartIntentSenderForResult()) {
            if (it.resultCode != Activity.RESULT_OK) {
                findNavController().navigateUp()
            }
        }
        importLyricsLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { data: Uri? ->
            data?.let { importLyrics(song, it) }
        }

        binding.search.setOnClickListener { searchLyrics() }
        binding.download.setOnClickListener { downloadLyrics() }
        binding.selectAll.setOnClickListener { selectAllInActiveInput() }
        binding.paste.setOnClickListener { pasteFromClipboard() }
        binding.save.setOnClickListener { saveLyrics() }
        binding.title.text = song.title
        binding.text.text = song.displayArtistName()

        Glide.with(this)
            .asBitmap()
            .load(song.getSongGlideModel())
            .songOptions(song)
            .transition(getDefaultGlideTransition())
            .into(binding.image)

        binding.save.isEnabled = false
        binding.progressIndicator.show()

        lyricsViewModel.getAllLyrics(song, fromEditor = true).observe(viewLifecycleOwner) {
            setLyricsResult(it)
        }
    }

    override fun onStart() {
        super.onStart()
        lyricsViewModel.getWritableUris(song).observe(viewLifecycleOwner) { uris ->
            requestWritePermissions(uris)
        }
    }

    private fun requestWritePermissions(uris: Collection<Uri>) = requestContext {
        if (uris.isNotEmpty() && hasR()) {
            val contentResolver = it.contentResolver
            val missingPerms = uris.filterNot { uri ->
                it.checkUriPermission(
                    uri, Process.myPid(), Process.myUid(), Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                ) == PackageManager.PERMISSION_GRANTED
            }
            if (missingPerms.isNotEmpty()) {
                val pendingIntent = MediaStore.createWriteRequest(contentResolver, missingPerms)
                permissionLauncher.launch(IntentSenderRequest.Builder(pendingIntent).build())
            }
        }
    }

    private fun setLyricsResult(result: LyricsResult) {
        binding.progressIndicator.hide()

        binding.embeddedButton.isEnabled = true
        binding.externalButton.isEnabled = true

        binding.plainInput.setText(result.plainLyrics.content)
        binding.syncedInput.setText(result.syncedLyrics.content?.rawText)

        if (!result.loading) {
            this.lyricsResult = result
            if (result.plainLyrics.isEditable) {
                binding.plainInput.doOnTextChanged { text, _, _, _ ->
                    binding.save.isEnabled = true
                }
            }
            if (result.syncedLyrics.isEditable) {
                binding.syncedInput.doOnTextChanged { text, _, _, _ ->
                    binding.save.isEnabled = true
                }
            }
            result.sources.forEach {
                requireView().findViewById<Button>(it.applicableButtonId)?.setText(it.titleRes)
            }
            binding.toggleGroup.addOnButtonCheckedListener { group, checkedId, isChecked ->
                applyCheckedButtonState(result, checkedId, isChecked)
            }
            applyCheckedButtonState(result, binding.toggleGroup.checkedButtonId, true)
        }
    }

    private fun applyCheckedButtonState(lyrics: LyricsResult, checkedId: Int, isChecked: Boolean) {
        val source = lyrics.sources.first { it.applicableButtonId == checkedId }
        showLyricsInput(source, isChecked)

        val button = binding.toggleGroup.findViewById<Button>(checkedId)
        if (!isChecked) return

        if (source.tooltipKey.isNotEmpty()) {
            val balloon = createBoomingMusicBalloon(source.tooltipKey) {
                setDismissWhenClicked(true)
                setText(getString(source.descriptionRes))
            }
            if (isLandscape()) {
                balloon?.showAlignTop(button)
            } else {
                balloon?.showAlignBottom(button)
            }
        }
    }

    private fun showLyricsInput(source: LyricsSource, isChecked: Boolean) {
        binding.plainInput.clearFocus()
        binding.syncedInput.clearFocus()
        if (source.isExternalSource) {
            binding.plainInputLayout.isGone = isChecked
            binding.syncedInputLayout.isVisible = isChecked
        } else {
            binding.syncedInputLayout.isGone = isChecked
            binding.plainInputLayout.isVisible = isChecked
        }
        activity?.hideSoftKeyboard()
    }

    private fun searchLyrics() {
        showSongTagsInput(song, R.string.search_lyrics) { title: String, artist: String ->
            val searchSuffix = getString(R.string.lyrics).lowercase()
            if (artist.isArtistNameUnknown()) {
                context?.webSearch(title, searchSuffix)
            } else {
                context?.webSearch(artist, title, searchSuffix)
            }
        }
    }

    private fun downloadLyrics() {
        if (!requireContext().isOnline(false)) {
            MaterialAlertDialogBuilder(requireContext())
                .setMessage(R.string.connection_unavailable)
                .setNegativeButton(android.R.string.cancel, null)
                .show()
            return
        }

        showSongTagsInput(song, R.string.download_lyrics) { title: String, artist: String ->
            lyricsViewModel.getOnlineLyrics(song, title, artist).observe(viewLifecycleOwner) { result ->
                if (result is Result.Loading) {
                    binding.download.isEnabled = false
                    binding.progressIndicator.show()
                } else {
                    binding.download.isEnabled = true
                    binding.progressIndicator.hide()
                    if (result is Result.Success) {
                        val downloadedLyrics = result.data
                        if (downloadedLyrics.hasMultiOptions) {
                            showLyricsSelector(downloadedLyrics)
                        } else if (!downloadedLyrics.syncedLyrics.isNullOrEmpty()) {
                            binding.syncedInput.setText(downloadedLyrics.syncedLyrics)
                            binding.toggleGroup.check(R.id.externalButton)
                        } else if (!downloadedLyrics.plainLyrics.isNullOrEmpty()) {
                            binding.plainInput.setText(downloadedLyrics.plainLyrics)
                            binding.toggleGroup.check(R.id.embeddedButton)
                        } else {
                            downloadError()
                        }
                    } else {
                        downloadError()
                    }
                }
            }
        }
    }

    private fun selectAllInActiveInput() {
        val input = if (binding.syncedInputLayout.isVisible) {
            binding.syncedInput
        } else {
            binding.plainInput
        }
        input.setSelection(0, input.text?.length ?: 0)
        input.requestInputMethod()
    }

    private fun pasteFromClipboard() {
        val clipboard = requireContext().getSystemService<ClipboardManager>()
        val clip = clipboard?.primaryClip?.getItemAt(0)?.text ?: return

        val input = if (binding.syncedInputLayout.isVisible) {
            binding.syncedInput
        } else {
            binding.plainInput
        }
        input.setText(clip)
    }

    private fun showSongTagsInput(song: Song, titleRes: Int, onInput: (title: String, artistName: String) -> Unit) {
        val dialogBinding = DialogSongSearchBinding.inflate(layoutInflater)
        dialogBinding.titleInput.setText(song.title)
        dialogBinding.artistInput.setText(song.albumArtistName())
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(titleRes)
            .setView(dialogBinding.root)
            .setPositiveButton(android.R.string.ok) { _: DialogInterface, _: Int ->
                val title = dialogBinding.titleInput.text?.toString()
                val artist = dialogBinding.artistInput.text?.toString()
                if (title.isNullOrBlank() || artist.isNullOrBlank()) {
                    showToast(R.string.album_or_artist_empty)
                } else {
                    onInput(title, artist)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showLyricsSelector(downloadedLyrics: DownloadedLyrics) {
        val dialogBinding = DialogLyricsSelectorBinding.inflate(layoutInflater)
        dialogBinding.plainLyrics.setOnClickListener { dialogBinding.plainLyricsCheck.animateToggle() }
        dialogBinding.syncedLyrics.setOnClickListener { dialogBinding.syncedLyricsCheck.animateToggle() }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.choose_lyrics)
            .setView(dialogBinding.root)
            .setPositiveButton(android.R.string.ok) { _: DialogInterface, _: Int ->
                if (dialogBinding.plainLyricsCheck.isChecked) {
                    binding.plainInput.setText(downloadedLyrics.plainLyrics ?: "")
                }
                if (dialogBinding.syncedLyricsCheck.isChecked) {
                    binding.syncedInput.setText(downloadedLyrics.syncedLyrics ?: "")
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun downloadError() {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(R.string.cannot_download_lyrics)
            .setPositiveButton(R.string.yes) { _: DialogInterface, _: Int ->
                searchLyrics()
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    private fun saveLyrics() {
        lyricsViewModel.saveLyrics(
            song = song,
            plainLyrics = lyricsResult?.plainLyrics?.edit(binding.plainInput.text?.toString()),
            syncedLyrics = lyricsResult?.syncedLyrics?.edit(binding.syncedInput.text?.toString())
        ).observe(viewLifecycleOwner) { saveResult ->
            if (saveResult.hasChanged) {
                if (saveResult.isSuccess) {
                    showToast(R.string.lyrics_were_updated_successfully, Toast.LENGTH_SHORT)
                } else {
                    showToast(R.string.failed_to_update_lyrics, Toast.LENGTH_SHORT)
                }
            } else {
                showToast(R.string.there_are_no_changes_to_save)
            }
        }
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_lyrics, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean =
        when (menuItem.itemId) {
            android.R.id.home -> {
                findNavController().navigateUp()
                true
            }

            R.id.action_import_lyrics -> {
                importSyncedLyrics(song)
                true
            }

            R.id.action_share_lyrics -> {
                shareSyncedLyrics(song)
                true
            }

            else -> false
        }

    private fun shareSyncedLyrics(song: Song) {
        lyricsViewModel.shareSyncedLyrics(song).observe(viewLifecycleOwner) {
            if (it == null) {
                showToast(R.string.no_synced_lyrics_found, Toast.LENGTH_SHORT)
            } else {
                startActivity(
                    ShareCompat.IntentBuilder(requireContext())
                        .setType(MIME_TYPE_APPLICATION)
                        .setChooserTitle(R.string.action_share_synchronized_lyrics)
                        .setStream(it)
                        .createChooserIntent()
                )
            }
        }
    }

    private fun importSyncedLyrics(song: Song) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.action_import_synchronized_lyrics)
            .setMessage(
                getString(R.string.do_you_want_to_import_lrc_lyrics_for_song_x, song.title).toHtml()
            )
            .setPositiveButton(R.string.yes) { _: DialogInterface, _: Int ->
                try {
                    importLyricsLauncher.launch(arrayOf("application/*"))
                    showToast(R.string.select_a_file_containing_synchronized_lyrics)
                } catch (_: ActivityNotFoundException) {
                }
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    private fun importLyrics(song: Song, data: Uri) {
        lyricsViewModel.importLyrics(song, data)
            .observe(viewLifecycleOwner) { isSuccess ->
                if (isSuccess) {
                    showToast(getString(R.string.import_lyrics_for_song_x, song.title))
                } else {
                    showToast(getString(R.string.could_not_import_lyrics_for_song_x, song.title))
                }
            }
    }

    override fun onDestroyView() {
        binding.toggleGroup.clearOnButtonCheckedListeners()
        super.onDestroyView()
        _binding = null
    }
}