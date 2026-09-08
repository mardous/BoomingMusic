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

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mardous.booming.R
import com.mardous.booming.core.audio.AudioOutputObserver
import com.mardous.booming.core.model.audiodevice.AudioDeviceType
import com.mardous.booming.core.model.equalizer.CompressorState
import com.mardous.booming.core.model.equalizer.EqEngineMode
import com.mardous.booming.core.model.equalizer.EqProfile
import com.mardous.booming.core.model.equalizer.LimiterState
import com.mardous.booming.core.model.equalizer.autoeq.AutoEqProfile
import com.mardous.booming.core.model.equalizer.autoeq.AutoEqSyncState
import com.mardous.booming.data.local.MediaStoreWriter
import com.mardous.booming.data.local.room.AutoEqEntity
import com.mardous.booming.data.model.replaygain.ReplayGainMode
import com.mardous.booming.extensions.MIME_TYPE_APPLICATION
import com.mardous.booming.extensions.files.getFileProviderUri
import com.mardous.booming.extensions.resolveActivity
import com.mardous.booming.extensions.showToast
import com.mardous.booming.playback.equalizer.EqualizerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

class EqualizerViewModel(
    private val equalizerManager: EqualizerManager,
    private val audioOutputObserver: AudioOutputObserver,
    private val mediaStoreWriter: MediaStoreWriter
) : ViewModel() {

    val eqState = equalizerManager.eqState
    val eqBandCapabilities = equalizerManager.bandCapabilities
    val currentProfile = equalizerManager.eqCurrentProfile
    val bassBoostState = equalizerManager.bassBoostState
    val virtualizerState = equalizerManager.virtualizerState
    val loudnessGainState = equalizerManager.loudnessGainState
    val compressorState = equalizerManager.compressorState
    val limiterState = equalizerManager.limiterState
    val balanceState = equalizerManager.balanceState
    val tempoState = equalizerManager.tempoState
    val replayGainState = equalizerManager.replayGainState
    val bitPerfectAudio = equalizerManager.bitPerfectAudio
    val audioOffload = equalizerManager.audioOffload
    val audioFloatOutput = equalizerManager.audioFloatOutput
    val skipSilence = equalizerManager.skipSilence
    val volumeState = equalizerManager.volumeState
    val audioDevice = audioOutputObserver.audioDevice
    val bitPerfectState = audioOutputObserver.bitPerfectState

    val autoEqProfiles = equalizerManager.autoEqProfiles

    val eqProfiles = combine(
        equalizerManager.eqProfiles,
        equalizerManager.eqCustomProfile
    ) { profiles, custom -> profiles + custom }

    val eqBands = combine(eqState, eqBandCapabilities, currentProfile) { state, bandCapabilities, profile ->
        bandCapabilities.getBands(profile, state.preferredBandCount)
    }

    private val _autoEqSyncState = MutableStateFlow<AutoEqSyncState>(AutoEqSyncState.Idle)
    val autoEqSyncState: StateFlow<AutoEqSyncState> = _autoEqSyncState.asStateFlow()

    private val _autoEqSearchQuery = MutableStateFlow("")
    private var autoEqSyncJob: Job? = null

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val autoEqSearchState = _autoEqSearchQuery
        .debounce(300.milliseconds)
        .mapLatest { query ->
            if (query.isBlank()) {
                emptyList()
            } else {
                equalizerManager.searchAutoEqHeadphones(query)
            }
        }
        .flowOn(Dispatchers.IO)

    private val _uiEvent = Channel<EqualizerUiEvent>(Channel.BUFFERED)
    val uiEvent: Flow<EqualizerUiEvent> = _uiEvent.receiveAsFlow()

    fun setEqualizerState(isEnabled: Boolean) {
        viewModelScope.launch {
            equalizerManager.setEqualizerState(eqState.value.copy(enabled = isEnabled))
        }
    }

    fun setEngineMode(mode: EqEngineMode) = viewModelScope.launch {
        equalizerManager.setEngineMode(mode)
    }

    fun setLoudnessGain(
        enabled: Boolean = loudnessGainState.value.enabled,
        gain: Float = loudnessGainState.value.gainInDb
    ) = viewModelScope.launch {
        equalizerManager.setLoudnessGain(
            loudnessGainState.value.copy(enabled = enabled, gainInDb = gain)
        )
    }

    fun setBassBoost(
        enabled: Boolean = bassBoostState.value.enabled,
        strength: Float = bassBoostState.value.strength
    ) = viewModelScope.launch {
        equalizerManager.setBassBoost(
            bassBoostState.value.copy(enabled = enabled, strength = strength)
        )
    }

    fun setVirtualizer(
        enabled: Boolean = virtualizerState.value.enabled,
        strength: Float = virtualizerState.value.strength
    ) = viewModelScope.launch {
        equalizerManager.setVirtualizer(
            virtualizerState.value.copy(enabled = enabled, strength = strength)
        )
    }

    fun setBandCount(bandCount: Int) = viewModelScope.launch {
        _uiEvent.send(
            EqualizerUiEvent.BandCountChange(
                success = equalizerManager.setBandCount(bandCount),
                bandCount = bandCount
            )
        )
    }

    fun setEqualizerProfile(profile: EqProfile) = viewModelScope.launch {
        equalizerManager.setCurrentProfile(profile)
    }

    fun setAutoEqProfile(profile: AutoEqProfile) = viewModelScope.launch {
        equalizerManager.setAutoEqProfile(profile)
    }

    fun setCustomProfileBandGain(band: Int, gain: Float) = viewModelScope.launch {
        equalizerManager.setCustomProfileBandGain(band, gain)
    }

    fun setEnableBitPerfect(enabled: Boolean) = viewModelScope.launch {
        equalizerManager.setEnableBitPerfect(enabled)
    }

    fun setEnableAudioOffload(enabled: Boolean) = viewModelScope.launch {
        equalizerManager.setEnableAudioOffload(enabled)
    }

    fun setEnableAudioFloatOutput(enabled: Boolean) = viewModelScope.launch {
        equalizerManager.setEnableAudioFloatOutput(enabled)
    }

    fun setEnableSkipSilences(enabled: Boolean) = viewModelScope.launch {
        equalizerManager.setEnableSkipSilence(enabled)
    }

    fun setVolume(volume: Float) = viewModelScope.launch {
        equalizerManager.setVolume(volume)
    }

    fun setBalance(center: Float) = viewModelScope.launch {
        equalizerManager.setBalance(balanceState.value.copy(center = center))
    }

    fun setTempo(
        speed: Float = tempoState.value.speed,
        pitch: Float = tempoState.value.pitch,
        isFixedPitch: Boolean = tempoState.value.isFixedPitch
    ) = viewModelScope.launch {
        equalizerManager.setTempo(
            tempoState.value.copy(speed = speed, pitch = pitch, isFixedPitch = isFixedPitch)
        )
    }

    fun setReplayGain(
        mode: ReplayGainMode = replayGainState.value.mode,
        preamp: Float = replayGainState.value.preamp,
        preampWithoutGain: Float = replayGainState.value.preampWithoutGain
    ) =
        viewModelScope.launch {
            equalizerManager.setReplayGain(
                replayGainState.value.copy(
                    mode = mode,
                    preamp = preamp,
                    preampWithoutGain = preampWithoutGain
                )
            )
        }

    fun setCompressor(
        enabled: Boolean = compressorState.value.enabled,
        attackTimeMs: Float = compressorState.value.attackTimeMs,
        releaseTimeMs: Float = compressorState.value.releaseTimeMs,
        kneeWidth: Float = compressorState.value.kneeWidth,
        noiseGateThreshold: Float = compressorState.value.noiseGateThreshold,
        preGain: Float = compressorState.value.preGain,
        postGain: Float = compressorState.value.postGain,
        ratio: Float = compressorState.value.ratio,
        expanderRatio: Float = compressorState.value.expanderRatio,
        threshold: Float = compressorState.value.threshold
    ) = viewModelScope.launch {
        equalizerManager.setCompressor(
            compressorState.value.copy(
                enabled = enabled,
                attackTimeMs = attackTimeMs,
                releaseTimeMs = releaseTimeMs,
                kneeWidth = kneeWidth,
                noiseGateThreshold = noiseGateThreshold,
                preGain = preGain,
                postGain = postGain,
                ratio = ratio,
                expanderRatio = expanderRatio,
                threshold = threshold
            )
        )
    }

    fun resetCompressor() = viewModelScope.launch {
        equalizerManager.setCompressor(CompressorState.Unspecified)
    }

    fun setLimiter(
        enabled: Boolean = limiterState.value.enabled,
        attackTimeMs: Float = limiterState.value.attackTimeMs,
        releaseTimeMs: Float = limiterState.value.releaseTimeMs,
        postGain: Float = limiterState.value.postGain,
        ratio: Float = limiterState.value.ratio,
        threshold: Float = limiterState.value.threshold
    ) = viewModelScope.launch {
        equalizerManager.setLimiter(
            limiterState.value.copy(
                enabled = enabled,
                attackTimeMs = attackTimeMs,
                releaseTimeMs = releaseTimeMs,
                postGain = postGain,
                ratio = ratio,
                threshold = threshold
            )
        )
    }

    fun resetLimiter() = viewModelScope.launch {
        equalizerManager.setLimiter(LimiterState.Unspecified)
    }

    fun setProMode(proMode: Boolean) = viewModelScope.launch {
        equalizerManager.setProMode(proMode)
    }

    fun setRemoteAutoEqProfile(entity: AutoEqEntity) = viewModelScope.launch {
        val success = equalizerManager.setRemoteAutoEqProfile(entity) == true
        _uiEvent.send(
            EqualizerUiEvent.Action(
                success = success,
                messageRes =
                    if (success) R.string.autoeq_profile_imported_successfully else R.string.no_profile_imported
            )
        )
    }

    fun searchAutoEq(query: String) = viewModelScope.launch {
        _autoEqSearchQuery.value = query
    }

    fun syncAutoEqDatabase(fetchRemote: Boolean = true) {
        if (fetchRemote) {
            autoEqSyncJob?.cancel()
            autoEqSyncJob = equalizerManager.syncAutoEqDatabase()
                .onEach {
                    _autoEqSyncState.value = it
                    if (it is AutoEqSyncState.Success) {
                        _uiEvent.send(EqualizerUiEvent.AutoEqSyncResult(true, it.count))
                    } else if (it is AutoEqSyncState.Error) {
                        _uiEvent.send(EqualizerUiEvent.AutoEqSyncResult(false))
                    }
                }
                .launchIn(viewModelScope)
        } else {
            viewModelScope.launch {
                _autoEqSyncState.update { currentState ->
                    if (currentState is AutoEqSyncState.Idle) {
                        val remoteProfilesCount = equalizerManager.getRemoteAutoEqProfilesCount()
                        if (remoteProfilesCount == 0) {
                            AutoEqSyncState.Idle
                        } else {
                            AutoEqSyncState.Success(remoteProfilesCount)
                        }
                    } else currentState
                }
            }
        }
    }

    fun showOutputDeviceSelector(context: Context) {
        audioOutputObserver.showOutputDeviceSelector(context)
    }

    fun saveProfile(
        profileName: String,
        canReplace: Boolean,
        associatedDevices: Set<AudioDeviceType>
    ) = viewModelScope.launch(Dispatchers.IO) {
        val result = if (!canReplace && !equalizerManager.isProfileNameAvailable(profileName)) {
            EqualizerUiEvent.Action(false, R.string.that_name_is_already_in_use, canDismiss = false)
        } else {
            val newProfile = equalizerManager.getNewProfileFromCustom(profileName, associatedDevices)
            if (equalizerManager.addProfile(newProfile, canReplace, useProfile = true)) {
                EqualizerUiEvent.Action(true, R.string.profile_saved_successfully)
            } else {
                EqualizerUiEvent.Action(false, R.string.the_profile_could_not_be_saved)
            }
        }
        _uiEvent.send(result)
    }

    fun editProfile(
        profile: EqProfile,
        newName: String?,
        newAssociations: Set<AudioDeviceType>
    ) = viewModelScope.launch(Dispatchers.IO) {
        val result = if (newName.isNullOrBlank()) {
            EqualizerUiEvent.Action(false, canDismiss = false)
        } else {
            if (equalizerManager.editProfile(profile, newName, newAssociations.toSet())) {
                EqualizerUiEvent.Action(true, R.string.profile_saved_successfully)
            } else {
                EqualizerUiEvent.Action(false, R.string.the_profile_could_not_be_saved)
            }
        }
        _uiEvent.send(result)
    }

    fun deleteProfile(
        context: Context,
        profile: EqProfile
    ) = viewModelScope.launch(Dispatchers.IO) {
        _uiEvent.send(
            EqualizerUiEvent.Deletion(
                success = equalizerManager.removeProfile(profile),
                profileName = profile.getName(context),
                isAutoEq = false
            )
        )
    }

    fun deleteAutoEqProfile(
        profile: AutoEqProfile
    ) = viewModelScope.launch(Dispatchers.IO) {
        _uiEvent.send(
            EqualizerUiEvent.Deletion(
                success = equalizerManager.deleteAutoEqProfile(profile),
                profileName = profile.name,
                isAutoEq = true
            )
        )
    }

    fun generateExportData(profiles: List<EqProfile>) = viewModelScope.launch(Dispatchers.IO) {
        val exportName = equalizerManager.getNewExportName()
        val exportContent = runCatching { Json.encodeToString(profiles) }.getOrNull()
        val result = if (exportName.isNotEmpty() && !exportContent.isNullOrEmpty()) {
            EqualizerUiEvent.ExportRequest(success = true, data = Pair(exportName, exportContent))
        } else {
            EqualizerUiEvent.ExportRequest(false)
        }
        _uiEvent.send(result)
    }

    fun exportConfiguration(data: Uri?, content: String?) = viewModelScope.launch(Dispatchers.IO) {
        val result = if (data == null || content.isNullOrEmpty()) {
            EqualizerUiEvent.ExportResult(false)
        } else {
            val result = runCatching {
                mediaStoreWriter.toContentResolver(null, data) { stream ->
                    if (content.isNotEmpty()) {
                        stream.bufferedWriter().use { bfw -> bfw.write(content) }
                        true
                    } else false
                }
            }

            if (result.isFailure || result.getOrThrow().resultCode == MediaStoreWriter.Result.Code.ERROR) {
                EqualizerUiEvent.ExportResult(false, R.string.an_unexpected_error_occurred)
            } else {
                EqualizerUiEvent.ExportResult(
                    success = true,
                    messageRes = R.string.profiles_exported_successfully,
                    uri = data,
                    mimeType = MIME_TYPE_APPLICATION
                )
            }
        }
        _uiEvent.send(result)
    }

    fun requestImport(uri: Uri?) = viewModelScope.launch(Dispatchers.IO) {
        if (uri == null) {
            _uiEvent.send(EqualizerUiEvent.ImportRequest(false, R.string.there_is_nothing_to_import))
            return@launch
        }

        val result = equalizerManager.parseProfilesFromUri(uri)
        val profiles = result.getOrNull()
        if (result.isFailure || profiles == null) {
            _uiEvent.send(EqualizerUiEvent.ImportRequest(false, R.string.there_is_nothing_to_import))
        } else {
            _uiEvent.send(EqualizerUiEvent.ImportRequest(true, profiles = profiles))
        }
    }

    fun requestAutoEqImport(uri: Uri?) = viewModelScope.launch(Dispatchers.IO) {
        if (uri == null) {
            _uiEvent.send(EqualizerUiEvent.AutoEqImportRequest(false, R.string.there_is_nothing_to_import))
            return@launch
        }

        val result = equalizerManager.parseAutoEqProfileFromUri(uri)
        val profile = result.getOrNull()
        if (result.isFailure || profile == null) {
            Log.e("EqualizerViewModel", "AutoEq profile parsing failed!", result.exceptionOrNull())
            _uiEvent.send(EqualizerUiEvent.AutoEqImportRequest(false, R.string.there_is_nothing_to_import))
        } else {
            _uiEvent.send(EqualizerUiEvent.AutoEqImportRequest(true, profile = profile))
        }
    }

    fun importProfiles(profiles: List<EqProfile>) = viewModelScope.launch(Dispatchers.IO) {
        val result = if (profiles.isNotEmpty()) {
            EqualizerUiEvent.ImportResult(true, count = equalizerManager.importProfiles(profiles))
        } else {
            EqualizerUiEvent.ImportResult(false, R.string.no_profile_imported)
        }
        _uiEvent.send(result)
    }

    fun importAutoEqProfile(
        profile: AutoEqProfile,
        profileName: String,
        canReplace: Boolean
    ) = viewModelScope.launch(Dispatchers.IO) {
        val result = if (!canReplace && !equalizerManager.isAutoEqProfileNameAvailable(profileName)) {
            EqualizerUiEvent.Action(false, R.string.that_name_is_already_in_use, canDismiss = false)
        } else {
            if (equalizerManager.importAutoEqProfile(profile, profileName, canReplace)) {
                EqualizerUiEvent.Action(true, R.string.autoeq_profile_imported_successfully)
            } else {
                EqualizerUiEvent.Action(false, R.string.no_profile_imported)
            }
        }
        _uiEvent.send(result)
    }

    fun shareProfiles(
        context: Context,
        profiles: List<EqProfile>
    ) = viewModelScope.launch(Dispatchers.IO) {
        val result = if (profiles.isNotEmpty()) {
            val exportsDir = context.externalCacheDir?.resolve("exports")
            if (exportsDir == null || (!exportsDir.exists() && !exportsDir.mkdirs())) {
                EqualizerUiEvent.ExportResult(false, R.string.an_unexpected_error_occurred)
            } else {
                val name = equalizerManager.getNewExportName()
                val result = runCatching {
                    File(exportsDir, name)
                        .also { it.writeText(Json.encodeToString(profiles)) }
                        .getFileProviderUri(context)
                }
                if (result.isSuccess) {
                    EqualizerUiEvent.ExportResult(
                        success = true,
                        isShare = true,
                        uri = result.getOrThrow(),
                        mimeType = MIME_TYPE_APPLICATION
                    )
                } else {
                    EqualizerUiEvent.ExportResult(
                        success = false,
                        isShare = true,
                        messageRes = R.string.an_unexpected_error_occurred
                    )
                }
            }
        } else {
            EqualizerUiEvent.ExportResult(false)
        }
        _uiEvent.send(result)
    }

    fun resetEqualizer() = viewModelScope.launch(Dispatchers.IO) {
        equalizerManager.resetConfiguration()
    }

    fun hasSystemEqualizer(context: Context): Boolean {
        val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
        return context.packageManager.resolveActivity(intent) != null
    }

    fun openSystemEqualizer(context: Context) {
        val sessionId = this.equalizerManager.eqSession.id
        if (sessionId != AudioEffect.ERROR_BAD_VALUE) {
            try {
                val equalizer = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
                equalizer.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId)
                equalizer.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
                (context as? Activity)?.startActivityForResult(equalizer, 500)
            } catch (_: ActivityNotFoundException) {
                context.showToast(R.string.no_equalizer)
            }
        } else {
            context.showToast(R.string.no_audio_ID)
        }
    }
}
