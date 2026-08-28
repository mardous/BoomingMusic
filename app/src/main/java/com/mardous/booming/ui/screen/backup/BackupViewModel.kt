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

package com.mardous.booming.ui.screen.backup

import android.content.ContentResolver
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mardous.booming.data.local.backup.BackupContent
import com.mardous.booming.data.local.backup.BackupFile
import com.mardous.booming.data.local.backup.BackupFileWithMetadata
import com.mardous.booming.data.local.backup.BackupManager
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch

sealed interface BackupsState {
    data object NoFolderSelected : BackupsState
    data object Loading : BackupsState
    data class Success(
        val backups: List<BackupFile>,
        val isInBackupOperation: Boolean = false
    ) : BackupsState
    data class Error(val message: String?) : BackupsState
}

class BackupViewModel(
    private val contentResolver: ContentResolver,
    private val preferences: SharedPreferences,
    private val backupManager: BackupManager
) : ViewModel() {

    private val _state = MutableStateFlow<BackupsState>(BackupsState.Loading)
    val state = _state.asStateFlow()

    private val _createBackupEvent = Channel<Boolean>(Channel.BUFFERED)
    val createBackupEvent = _createBackupEvent.receiveAsFlow()

    private val _backupInfoEvent = Channel<BackupFileWithMetadata?>(Channel.BUFFERED)
    val backupInfoEvent = _backupInfoEvent.receiveAsFlow()

    private val _restoreBackupEvent = Channel<Boolean>(Channel.BUFFERED)
    val restoreBackupEvent = _restoreBackupEvent.receiveAsFlow()

    private val _shareBackupEvent = Channel<Uri?>(Channel.BUFFERED)
    val shareBackupEvent = _shareBackupEvent.receiveAsFlow()

    private val _deleteBackupEvent = Channel<Boolean>(Channel.BUFFERED)
    val deleteBackupEvent = _deleteBackupEvent.receiveAsFlow()

    private val _backupDirectory = MutableStateFlow(getBackupDirectory())
    val backupDirectory = _backupDirectory.asStateFlow()

    init {
        _backupDirectory.onEach { loadBackupsFromDirectory(it) }
            .launchIn(viewModelScope)
    }

    private fun getBackupDirectory(): Uri {
        var backupDirectory = preferences.getString("backup_directory", null)
            ?.toUri() ?: Uri.EMPTY
        if (backupDirectory != Uri.EMPTY) {
            val hasUriPermissions = contentResolver.persistedUriPermissions.any {
                it.uri == backupDirectory  && it.isWritePermission
            }
            if (!hasUriPermissions) {
                backupDirectory = Uri.EMPTY
                preferences.edit { remove("backup_directory") }
            }
        }
        return backupDirectory
    }

    fun setBackupDirectory(uri: Uri?) {
        if (uri != null && uri != backupDirectory.value) {
            try {
                if (DocumentsContract.isTreeUri(uri)) {
                    val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    contentResolver.takePersistableUriPermission(uri, flags)
                    preferences.edit { putString("backup_directory", uri.toString()) }
                }
                _backupDirectory.value = uri
            } catch (e: Exception) {
                Log.e("BackupViewModel", "setBackupDirectory: ${e.message}", e)
            }
        }
    }

    private fun loadBackupsFromDirectory(
        directoryUri: Uri = backupDirectory.value
    ) = viewModelScope.launch {
        if (directoryUri == Uri.EMPTY) {
            _state.value = BackupsState.NoFolderSelected
        } else {
            _state.value = BackupsState.Loading
            try {
                val backups = backupManager.getBackupsInDirectory(directoryUri)
                _state.value = BackupsState.Success(backups)
            } catch (e: Exception) {
                Log.e("BackupViewModel", "loadBackupsFromDirectory: ${e.message}", e)
                _state.value = BackupsState.Error(e.message)
            }
        }
    }

    fun loadBackupInfo(uri: Uri?) = viewModelScope.launch {
        if (uri == null) return@launch
        val backupInfo = backupManager.getBackupFileInfo(uri)
        _backupInfoEvent.trySend(backupInfo)
    }

    fun createBackup(name: String, backupContents: List<BackupContent>) = viewModelScope.launch {
        val newState = _state.updateAndGet {
            if (it is BackupsState.Success) {
                it.copy(isInBackupOperation = true)
            } else it
        }
        if (newState is BackupsState.Success) {
            val success = backupManager.createBackup(backupDirectory.value, name, backupContents)
            if (success) loadBackupsFromDirectory()
            _createBackupEvent.trySend(success)
            _state.update {
                if (it is BackupsState.Success) it.copy(isInBackupOperation = false) else it
            }
        }
    }

    fun shareBackup(backupFile: BackupFile) = viewModelScope.launch {
        val newState = _state.updateAndGet {
            if (it is BackupsState.Success) {
                it.copy(isInBackupOperation = true)
            } else it
        }
        if (newState is BackupsState.Success) {
            val uri = backupManager.createShareUriForBackup(backupFile.uri)
            _shareBackupEvent.trySend(uri)
            _state.update {
                if (it is BackupsState.Success) it.copy(isInBackupOperation = false) else it
            }
        }
    }

    fun deleteBackup(backupFile: BackupFile) = viewModelScope.launch {
        val newState = _state.updateAndGet {
            if (it is BackupsState.Success) {
                it.copy(isInBackupOperation = true)
            } else it
        }
        if (newState is BackupsState.Success) {
            val success = backupManager.deleteBackup(backupFile.uri)
            if (success) loadBackupsFromDirectory()
            _deleteBackupEvent.trySend(success)
            _state.update {
                if (it is BackupsState.Success) it.copy(isInBackupOperation = false) else it
            }
        }
    }

    fun restoreBackup(uri: Uri, contents: List<BackupContent>) = viewModelScope.launch {
        val newState = _state.updateAndGet {
            if (it is BackupsState.Success) {
                it.copy(isInBackupOperation = true)
            } else it
        }
        if (newState is BackupsState.Success) {
            val success = backupManager.restoreBackup(uri, contents)
            _restoreBackupEvent.trySend(success)
            _state.update {
                if (it is BackupsState.Success) it.copy(isInBackupOperation = false) else it
            }
        }
    }
}
