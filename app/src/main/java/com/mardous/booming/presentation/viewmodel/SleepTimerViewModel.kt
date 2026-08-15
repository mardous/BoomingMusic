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

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mardous.booming.App
import com.mardous.booming.playback.SleepTimer
import com.mardous.booming.presentation.screen.sleeptimer.SleepTimerEvent
import com.mardous.booming.presentation.screen.sleeptimer.SleepTimerUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val Context.sleepTimerDataStore by preferencesDataStore("sleep_timer")

class SleepTimerViewModel(
    application: Application,
    private val sleepTimer: SleepTimer
) : AndroidViewModel(application) {

    private val _uiState: Flow<SleepTimerUiState> = combine(
        sleepTimer.isRunning,
        sleepTimer.waitingFor,
        application.sleepTimerDataStore.data
    ) { isRunning, waitingFor, prefs ->
        SleepTimerUiState(
            isRunning = isRunning,
            waitingFor = waitingFor,
            isFinishMusic = prefs[Keys.IS_FINISH_MUSIC] ?: false,
            isFadeOut = prefs[Keys.IS_FADE_OUT] ?: false,
            fadeOutDuration = prefs[Keys.FADE_OUT_DURATION] ?: 5f,
            timerValue = prefs[Keys.TIMER_VALUE] ?: 5f
        )
    }

    val uiState = _uiState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SleepTimerUiState(
            isRunning = false,
            waitingFor = null,
            isFinishMusic = false,
            isFadeOut = false,
            fadeOutDuration = 5f,
            timerValue = 5f
        )
    )

    private val _sleepTimerEvent = Channel<SleepTimerEvent>(Channel.BUFFERED)
    val sleepTimerEvent = _sleepTimerEvent.receiveAsFlow()

    fun setTimerState(
        value: Float = uiState.value.timerValue,
        isFinishMusic: Boolean = uiState.value.isFinishMusic,
        isFadeOut: Boolean = uiState.value.isFadeOut,
        fadeOutDuration: Float = uiState.value.fadeOutDuration
    ) = viewModelScope.launch(Dispatchers.IO) {
        getApplication<App>().sleepTimerDataStore.edit {
            it[Keys.TIMER_VALUE] = value
            it[Keys.IS_FINISH_MUSIC] = isFinishMusic
            it[Keys.IS_FADE_OUT] = isFadeOut
            it[Keys.FADE_OUT_DURATION] = fadeOutDuration
        }
    }

    fun startTimer() = viewModelScope.launch(Dispatchers.IO) {
        sleepTimer.set(
            millisInFuture = uiState.value.timerValue.toLong() * 60 * 1000,
            allowPendingQuit = uiState.value.isFinishMusic,
            fadeOut = uiState.value.isFadeOut,
            fadeDuration = uiState.value.fadeOutDuration.toLong() * 1000
        )
        _sleepTimerEvent.send(
            SleepTimerEvent.Set(uiState.value.timerValue.toLong())
        )
    }

    fun cancelTimer() = viewModelScope.launch(Dispatchers.IO) {
        if (sleepTimer.cancel()) {
            _sleepTimerEvent.send(SleepTimerEvent.Canceled)
        }
    }

    private interface Keys {
        companion object {
            val IS_FINISH_MUSIC = booleanPreferencesKey("finish_music")
            val IS_FADE_OUT = booleanPreferencesKey("fade_out")
            val FADE_OUT_DURATION = floatPreferencesKey("fade_out_duration")
            val TIMER_VALUE = floatPreferencesKey("timer_value")
        }
    }
}