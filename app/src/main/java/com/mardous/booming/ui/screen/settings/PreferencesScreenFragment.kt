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

import android.content.SharedPreferences
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode
import androidx.core.graphics.drawable.toDrawable
import androidx.core.os.LocaleListCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import coil3.SingletonImageLoader
import com.google.android.material.color.DynamicColors
import com.mardous.booming.BuildConfig
import com.mardous.booming.R
import com.mardous.booming.coil.CoverProvider
import com.mardous.booming.data.local.room.InclExclDao
import com.mardous.booming.extensions.*
import com.mardous.booming.extensions.files.getFormattedFileName
import com.mardous.booming.extensions.navigation.findActivityNavController
import com.mardous.booming.extensions.utilities.dateStr
import com.mardous.booming.extensions.utilities.toEnum
import com.mardous.booming.ui.component.preferences.ProgressIndicatorPreference
import com.mardous.booming.ui.component.preferences.SwitchWithButtonPreference
import com.mardous.booming.ui.component.preferences.ThemePreference
import com.mardous.booming.ui.component.preferences.dialog.*
import com.mardous.booming.ui.dialogs.MultiCheckDialog
import com.mardous.booming.ui.dialogs.library.BlacklistWhitelistDialog
import com.mardous.booming.ui.screen.library.LibraryViewModel
import com.mardous.booming.ui.screen.library.ReloadType
import com.mardous.booming.ui.screen.lyrics.LyricsViewModel
import com.mardous.booming.ui.screen.lyrics.LyricsViewSettings
import com.mardous.booming.ui.screen.update.UpdateSearchResult
import com.mardous.booming.ui.screen.update.UpdateViewModel
import com.mardous.booming.util.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class AppearancePreferencesFragment : PreferenceScreenFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_screen_appearance)
    }
}

class NowPlayingPreferencesFragment : PreferenceScreenFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_screen_now_playing)
    }
}

class LyricsPreferencesFragment : PreferenceScreenFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_screen_lyrics)
    }
}

class PlaybackPreferencesFragment : PreferenceScreenFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_screen_playback)
    }
}

class LibraryPreferencesFragment : PreferenceScreenFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_screen_library)
    }
}

class AdvancedPreferencesFragment : PreferenceScreenFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_screen_advanced)
    }
}

open class PreferenceScreenFragment : PreferenceFragmentCompat(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    private val libraryViewModel: LibraryViewModel by activityViewModel()
    private val lyricsViewModel: LyricsViewModel by activityViewModel()
    private val updateViewModel: UpdateViewModel by activityViewModel()

    private val importFontLauncher: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) {
                lyricsViewModel.importCustomFont(requireContext(), uri)
                    .observe(viewLifecycleOwner) { success ->
                        if (success) {
                            showToast(R.string.font_imported_successfully)
                        } else {
                            showToast(R.string.could_not_import_font)
                        }
                    }
            }
        }

    @OptIn(DelicateCoroutinesApi::class)
    private val createBackupLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/*")) { uri ->
            if (uri != null) {
                GlobalScope.launch {
                    BackupHelper.createBackup(requireContext(), uri)
                }
            }
        }

    @OptIn(DelicateCoroutinesApi::class)
    private val selectBackupLauncher: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { selection ->
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

    private val preferences: SharedPreferences by inject()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        preferences.registerOnSharedPreferenceChangeListener(this)
        libraryViewModel.getMiniPlayerMargin().observe(viewLifecycleOwner) {
            listView.updatePadding(bottom = it.getWithSpace())
        }
        setDivider(Color.TRANSPARENT.toDrawable())
        materialSharedAxis(view)
        preparePreferences()
    }

    fun preparePreferences() {
        findPreference<Preference>("about")?.summary =
            getString(R.string.about_summary, BuildConfig.VERSION_NAME)

        findPreference<ThemePreference>(GENERAL_THEME)?.apply {
            customCallback = object : ThemePreference.Callback {
                override fun onThemeSelected(themeName: String) {
                    Preferences.generalTheme = themeName
                    setDefaultNightMode(Preferences.getDayNightMode(themeName))
                    restartActivity()
                }
            }
        }

        findPreference<Preference>(BLACK_THEME)?.apply {
            setOnPreferenceChangeListener { _, newValue ->
                val themeName = Preferences.getGeneralTheme((newValue as Boolean))
                setDefaultNightMode(Preferences.getDayNightMode(themeName))
                requireActivity().recreate()
                true
            }
        }

        findPreference<Preference>(MATERIAL_YOU)?.apply {
            isVisible = hasS()
            setOnPreferenceChangeListener { _, newValue ->
                val activity = requireActivity()
                if (newValue as Boolean) {
                    DynamicColors.applyToActivityIfAvailable(activity)
                }
                activity.recreate()
                true
            }
        }

        findPreference<Preference>(USE_CUSTOM_FONT)?.setOnPreferenceChangeListener { _, _ ->
            requireActivity().recreate()
            true
        }

        findPreference<Preference>(ADD_EXTRA_CONTROLS)?.isVisible = !resources.isTablet
        onUpdateNowPlayingScreen()
        onUpdateCoverActions()

        findPreference<Preference>(LyricsViewSettings.Key.BLUR_EFFECT)
            ?.isVisible = hasS()

        findPreference<Preference>(LyricsViewSettings.Key.SELECTED_CUSTOM_FONT)
            ?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            importFontLauncher.launch(
                arrayOf("font/ttf", "font/otf", "application/x-font-ttf", "application/x-font-otf")
            )
            true
        }

        findPreference<Preference>("clear_lyrics")
            ?.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            lyricsViewModel.deleteLyrics()
            showToast(R.string.lyrics_cleared)
            true
        }
        onUpdateLyricsPreferences()

        if (!hasR()) {
            findPreference<Preference>(TRASH_MUSIC_FILES)?.isVisible = false
        }

        findPreference<Preference>(LAST_ADDED_CUTOFF)?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, _ ->
                libraryViewModel.forceReload(ReloadType.Suggestions)
                true
            }

        findPreference<SwitchWithButtonPreference>(WHITELIST_ENABLED)?.apply {
            setButtonPressedListener(object : SwitchWithButtonPreference.OnButtonPressedListener {
                override fun onButtonPressed() {
                    showLibraryFolderSelector(InclExclDao.WHITELIST)
                }
            })
        }

        findPreference<SwitchWithButtonPreference>(BLACKLIST_ENABLED)?.apply {
            setButtonPressedListener(object : SwitchWithButtonPreference.OnButtonPressedListener {
                override fun onButtonPressed() {
                    showLibraryFolderSelector(InclExclDao.BLACKLIST)
                }
            })
        }

        findPreference<Preference>(IGNORE_MEDIA_STORE)?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, _ ->
                clearImageLoaderCache()
                true
            }

        findPreference<Preference>(PREFERRED_IMAGE_SIZE)?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, _ ->
                clearImageLoaderCache()
                true
            }

        findPreference<Preference>(USE_FOLDER_ART)?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { _, _ ->
                clearImageLoaderCache()
                true
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

        val updateSearchPreference = findPreference<ProgressIndicatorPreference>("search_for_update")
        if (updateSearchPreference != null) {
            updateSearchPreference.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                updateViewModel.searchForUpdate(true)
                true
            }

            updateViewModel.updateEventObservable.observe(viewLifecycleOwner) {
                val result = it.peekContent()
                when (result.state) {
                    UpdateSearchResult.State.Searching -> {
                        updateSearchPreference.showProgressIndicator()
                        updateSearchPreference.isEnabled = false
                        updateSearchPreference.summary = getString(R.string.checking_please_wait)
                    }

                    UpdateSearchResult.State.Completed,
                    UpdateSearchResult.State.Failed -> {
                        updateSearchState(updateSearchPreference, result.executedAtMillis)
                    }

                    else -> {
                        updateSearchState(updateSearchPreference, Preferences.lastUpdateSearch)
                    }
                }
            }
        }
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        val dialogFragment: DialogFragment? = when (preference) {
            is NowPlayingExtraInfoPreference -> NowPlayingExtraInfoPreferenceDialog()
            is CategoriesPreference -> CategoriesPreferenceDialog()
            is ClearQueueActionPreference -> ClearQueueActionPreferenceDialog()
            is NowPlayingScreenPreference -> NowPlayingScreenPreferenceDialog()
            is ActionOnCoverPreference -> ActionOnCoverPreferenceDialog.newInstance(preference.key, preference.title!!)
            else -> null
        }

        if (dialogFragment != null) {
            dialogFragment.show(childFragmentManager, "androidx.preference.PreferenceFragment.DIALOG")
        } else {
            super.onDisplayPreferenceDialog(preference)
        }
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        val settingsScreen = preference.key.toEnum<SettingsScreen>()
        return when {
            settingsScreen != null -> {
                findNavController().navigate(settingsScreen.navAction)
                true
            }
            preference.key == "about" -> {
                findActivityNavController(R.id.fragment_container).navigate(R.id.nav_about)
                true
            }
            else -> super.onPreferenceTreeClick(preference)
        }
    }

    override fun onSharedPreferenceChanged(preferences: SharedPreferences, key: String?) {
        when (key) {
            NOW_PLAYING_SCREEN -> onUpdateNowPlayingScreen()
            COVER_DOUBLE_TAP_ACTION,
            COVER_LEFT_DOUBLE_TAP_ACTION,
            COVER_RIGHT_DOUBLE_TAP_ACTION,
            COVER_LONG_PRESS_ACTION -> onUpdateCoverActions()
            LyricsViewSettings.Key.BACKGROUND_EFFECT -> onUpdateLyricsPreferences()
        }
    }

    private fun onUpdateNowPlayingScreen() {
        findPreference<Preference>(NOW_PLAYING_SCREEN)?.summary =
            getString(Preferences.nowPlayingScreen.titleRes)
    }

    private fun onUpdateCoverActions() {
        findPreference<Preference>(COVER_SINGLE_TAP_ACTION)?.summary =
            getString(Preferences.coverSingleTapAction.titleRes)

        findPreference<Preference>(COVER_DOUBLE_TAP_ACTION)?.summary =
            getString(Preferences.coverDoubleTapAction.titleRes)

        findPreference<Preference>(COVER_LEFT_DOUBLE_TAP_ACTION)?.summary =
            getString(Preferences.coverLeftDoubleTapAction.titleRes)

        findPreference<Preference>(COVER_RIGHT_DOUBLE_TAP_ACTION)?.summary =
            getString(Preferences.coverRightDoubleTapAction.titleRes)

        findPreference<Preference>(COVER_LONG_PRESS_ACTION)?.summary =
            getString(Preferences.coverLongPressAction.titleRes)
    }

    private fun onUpdateLyricsPreferences() {
        val hasBackgroundEffects =
            preferences.getString(LyricsViewSettings.Key.BACKGROUND_EFFECT, "none") != "none"
        findPreference<Preference>(LyricsViewSettings.Key.SHADOW_EFFECT)
            ?.isEnabled = hasBackgroundEffects
        findPreference<Preference>(LyricsViewSettings.Key.BLUR_EFFECT)
            ?.isEnabled = hasBackgroundEffects
    }

    private fun showLibraryFolderSelector(type: Int) {
        BlacklistWhitelistDialog.newInstance(type).show(childFragmentManager, "LIBRARY_PATHS_PREFERENCE")
    }

    private fun clearImageLoaderCache() = lifecycleScope.launch(Dispatchers.IO) {
        try {
            CoverProvider.clearCache(requireContext())

            val imageLoader = SingletonImageLoader.get(requireContext())
            imageLoader.memoryCache?.clear()
            imageLoader.diskCache?.clear()
        } catch (e: Exception) {
            Log.e("Settings", "Failed to clear image loader cache", e)
        }
    }

    private fun updateSearchState(preference: ProgressIndicatorPreference?, lastUpdateSearch: Long) {
        requestContext {
            preference?.hideProgressIndicator()
            preference?.isEnabled = true
            preference?.summary = getString(R.string.last_update_search_x, it.dateStr(lastUpdateSearch))
        }
    }

    private fun restartActivity() {
        activity?.recreate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        preferences.unregisterOnSharedPreferenceChangeListener(this)
    }
}