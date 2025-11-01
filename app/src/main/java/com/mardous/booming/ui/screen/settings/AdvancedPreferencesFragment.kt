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

package com.mardous.booming.ui.screen.settings

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.preference.Preference
import com.mardous.booming.R
import com.mardous.booming.extensions.files.getFormattedFileName
import com.mardous.booming.ui.dialogs.MultiCheckDialog
import com.mardous.booming.util.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * @author Christians M. A. (mardous)
 */
@OptIn(DelicateCoroutinesApi::class)
class AdvancedPreferencesFragment : PreferencesScreenFragment() {

    private lateinit var createBackupLauncher: ActivityResultLauncher<String>
    private lateinit var selectBackupLauncher: ActivityResultLauncher<Array<String>>

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_screen_advanced)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        createBackupLauncher =
            registerForActivityResult(ActivityResultContracts.CreateDocument("application/*")) { destination ->
                backupData(destination)
            }
        selectBackupLauncher =
            registerForActivityResult(ActivityResultContracts.OpenDocument()) { selection ->
                restoreData(selection)
            }

        findPreference<Preference>(LANGUAGE_NAME)?.setOnPreferenceChangeListener { _, newValue ->
            val languageTag = (newValue as? String)
            if (languageTag == null || languageTag == "auto") {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
            } else {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag))
            }
            true
        }

        findPreference<Preference>(BACKUP_DATA)?.setOnPreferenceClickListener {
            createBackupLauncher.launch(
                getFormattedFileName(
                    "Backup",
                    BackupHelper.BACKUP_EXTENSION
                )
            )
            true
        }

        findPreference<Preference>(RESTORE_DATA)?.setOnPreferenceClickListener {
            selectBackupLauncher.launch(arrayOf("application/*"))
            true
        }
    }

    private fun backupData(destination: Uri?) {
        if (destination != null) {
            GlobalScope.launch {
                BackupHelper.createBackup(requireContext(), destination)
            }
        }
    }

    private fun restoreData(selection: Uri?) {
        if (selection != null) {
            val items = BackupContent.entries.map {
                getString(it.titleRes)
            }
            val multiCheckDialog = MultiCheckDialog.Builder(requireContext())
                .title(R.string.select_content_to_restore)
                .items(items)
                .createDialog { _, whichPos, _ ->
                    val content = BackupContent.entries.filterIndexed { i, _ ->
                        whichPos.contains(i)
                    }
                    GlobalScope.launch {
                        BackupHelper.restoreBackup(requireContext(), selection, content)
                    }
                    true
                }
            multiCheckDialog.show(childFragmentManager, "RESTORE_DIALOG")
        }
    }
}