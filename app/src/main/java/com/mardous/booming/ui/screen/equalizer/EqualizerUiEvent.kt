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

sealed class EqualizerUiEvent(
    open val success: Boolean,
    @StringRes open val messageRes: Int = 0
) {
    data class Action(
        override val success: Boolean,
        @StringRes override val messageRes: Int = 0,
        val canDismiss: Boolean = true
    ) : EqualizerUiEvent(success, messageRes)

    data class Deletion(
        override val success: Boolean,
        val profileName: String,
        val isAutoEq: Boolean
    ) : EqualizerUiEvent(success)

    data class ExportRequest(
        override val success: Boolean,
        @StringRes override val messageRes: Int = 0,
        val data: Pair<String, String>? = null
    ) : EqualizerUiEvent(success, messageRes)

    data class ExportResult(
        override val success: Boolean,
        @StringRes override val messageRes: Int = 0,
        val isShare: Boolean = false,
        val uri: Uri? = null,
        val mimeType: String? = null
    ) : EqualizerUiEvent(success, messageRes)

    data class ImportRequest(
        override val success: Boolean,
        @StringRes override val messageRes: Int = 0,
        val profiles: List<EqProfile> = emptyList()
    ) : EqualizerUiEvent(success, messageRes)

    data class ImportResult(
        override val success: Boolean,
        @StringRes override val messageRes: Int = 0,
        val count: Int = 0
    ) : EqualizerUiEvent(success, messageRes)

    data class AutoEqSyncResult(
        override val success: Boolean,
        val count: Int = 0
    ) : EqualizerUiEvent(success)

    data class AutoEqImportRequest(
        override val success: Boolean,
        @StringRes override val messageRes: Int = 0,
        val profile: AutoEqProfile? = null
    ) : EqualizerUiEvent(success, messageRes)

    data class BandCountChange(
        override val success: Boolean,
        val bandCount: Int
    ) : EqualizerUiEvent(success)
}
