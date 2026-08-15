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

package com.mardous.booming.presentation.viewmodel

import android.app.DownloadManager
import android.content.Context
import android.util.Log
import androidx.core.content.getSystemService
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.mardous.booming.core.model.task.Event
import com.mardous.booming.data.remote.github.GitHubService
import com.mardous.booming.data.remote.github.model.GitHubRelease
import com.mardous.booming.ui.screen.update.UpdateSearchResult
import com.mardous.booming.util.Preferences
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date

class UpdateViewModel(private val updateService: GitHubService): ViewModel() {

    private val ioHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e("UpdateViewModel", "Update search failed!", throwable)
    }

    private val _updateSearch = MutableLiveData(UpdateSearchResult())
    val updateEventObservable = _updateSearch.map { Event(it) }
    val updateEvent get() = updateEventObservable.value

    val latestRelease get() = updateEvent?.peekContent()?.data

    fun searchForUpdate(fromUser: Boolean, allowExperimental: Boolean = Preferences.experimentalUpdates) =
        viewModelScope.launch(Dispatchers.IO) {
            val current = updateEvent?.peekContent() ?: UpdateSearchResult(executedAtMillis = Preferences.lastUpdateSearch)
            if (current.shouldStartNewSearchFor(fromUser, allowExperimental)) {
                _updateSearch.postValue(
                    current.copy(
                        state = UpdateSearchResult.State.Searching,
                        wasFromUser = fromUser,
                        wasExperimentalQuery = allowExperimental
                    )
                )

                val result = runCatching {
                    updateService.latestRelease(allowExperimental = allowExperimental)
                }
                val executedAtMillis = Date().time.also {
                    Preferences.lastUpdateSearch = it
                }
                val newState = if (result.isSuccess) {
                    UpdateSearchResult(
                        state = UpdateSearchResult.State.Completed,
                        data = result.getOrThrow(),
                        executedAtMillis = executedAtMillis,
                        wasFromUser = fromUser,
                        wasExperimentalQuery = allowExperimental
                    )
                } else {
                    UpdateSearchResult(
                        state = UpdateSearchResult.State.Failed,
                        data = null,
                        executedAtMillis = executedAtMillis,
                        wasFromUser = fromUser,
                        wasExperimentalQuery = allowExperimental
                    )
                }
                _updateSearch.postValue(newState)
            }
        }

    fun downloadUpdate(context: Context, release: GitHubRelease) =
        viewModelScope.launch(Dispatchers.IO + ioHandler) {
            val downloadRequest = release.getDownloadRequest(context)
            if (downloadRequest != null) {
                val downloadManager = context.getSystemService<DownloadManager>()
                if (downloadManager != null) {
                    val lastUpdateId = Preferences.lastUpdateId
                    if (lastUpdateId != -1L) {
                        downloadManager.remove(lastUpdateId)
                    }
                    Preferences.lastUpdateId = downloadManager.enqueue(downloadRequest)
                }
            }
        }
}