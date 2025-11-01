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

package com.mardous.booming.ui.screen.equalizer

import android.net.Uri
import androidx.annotation.StringRes
import com.mardous.booming.core.model.equalizer.EQPreset

open class PresetOpResult(
    val success: Boolean,
    @StringRes val messageRes: Int = 0,
    val canDismiss: Boolean = true
)

class ExportRequestResult(
    success: Boolean,
    @StringRes messageRes: Int = 0,
    val presets: List<EQPreset> = emptyList(),
    val presetNames: List<String> = emptyList()
) : PresetOpResult(success, messageRes = messageRes)

class PresetExportResult(
    success: Boolean,
    @StringRes messageRes: Int = 0,
    val data: Uri? = null,
    val mimeType: String? = null
) : PresetOpResult(success, messageRes = messageRes)

class ImportRequestResult(
    success: Boolean,
    @StringRes messageRes: Int = 0,
    val presets: List<EQPreset> = emptyList(),
    val presetNames: List<String> = emptyList()
) : PresetOpResult(success, messageRes = messageRes)

class PresetImportResult(
    success: Boolean,
    @StringRes messageRes: Int = 0,
    val imported: Int = 0
) : PresetOpResult(success, messageRes = messageRes)