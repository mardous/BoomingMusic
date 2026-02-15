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
import com.mardous.booming.core.model.equalizer.EqProfile
import com.mardous.booming.core.model.equalizer.autoeq.AutoEqProfile

open class ProfileOpResult(
    val success: Boolean,
    @StringRes val messageRes: Int = 0,
    val canDismiss: Boolean = true
)

class ProfileDeletionResult(
    success: Boolean,
    val profileName: String,
    val autoEqProfile: Boolean
) : ProfileOpResult(success, messageRes = 0)

class ProfileExportRequest(
    success: Boolean,
    @StringRes messageRes: Int = 0,
    val profileExportData: Pair<String, String>? = null
) : ProfileOpResult(success, messageRes = messageRes)

class ProfileExportResult(
    success: Boolean,
    @StringRes messageRes: Int = 0,
    val isShareRequest: Boolean = false,
    val data: Uri? = null,
    val mimeType: String? = null
) : ProfileOpResult(success, messageRes = messageRes)

class ProfileImportRequest(
    success: Boolean,
    @StringRes messageRes: Int = 0,
    val profiles: List<EqProfile> = emptyList()
) : ProfileOpResult(success, messageRes = messageRes)

class ProfileImportResult(
    success: Boolean,
    @StringRes messageRes: Int = 0,
    val imported: Int = 0
) : ProfileOpResult(success, messageRes = messageRes)

class AutoEqImportRequest(
    success: Boolean,
    @StringRes messageRes: Int = 0,
    val profile: AutoEqProfile? = null
) : ProfileOpResult(success, messageRes = messageRes)