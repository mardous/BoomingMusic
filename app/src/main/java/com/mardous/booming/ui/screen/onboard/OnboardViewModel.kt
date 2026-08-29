/*
 * Copyright (c) 2026 Christians Martínez Alvarado
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

package com.mardous.booming.ui.screen.onboard

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mardous.booming.data.local.backup.BackupContent
import com.mardous.booming.data.local.backup.BackupManager
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

enum class RestoreState {
    Idle,
    Restoring,
    Restored
}

class OnboardViewModel(private val backupManager: BackupManager) : ViewModel() {

    private val _restoreState = MutableStateFlow(RestoreState.Idle)
    val restoreState = _restoreState.asStateFlow()

    private val _restoreFailedEvent = Channel<Unit>(Channel.BUFFERED)
    val restoreFailedEvent = _restoreFailedEvent.receiveAsFlow()

    fun restoreBackup(uri: Uri) {
        if (_restoreState.value == RestoreState.Restoring)
            return

        _restoreState.value = RestoreState.Restoring
        viewModelScope.launch {
            val isSuccess = backupManager.restoreBackup(uri, BackupContent.entries)
            if (isSuccess) {
                _restoreState.value = RestoreState.Restored
            } else {
                _restoreState.value = RestoreState.Idle
                _restoreFailedEvent.trySend(Unit)
            }
        }
    }
}
