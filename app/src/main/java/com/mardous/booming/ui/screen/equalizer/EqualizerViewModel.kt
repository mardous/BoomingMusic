package com.mardous.booming.ui.screen.equalizer

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mardous.booming.R
import com.mardous.booming.core.audio.AudioOutputObserver
import com.mardous.booming.core.audio.AutoEqTxtParser
import com.mardous.booming.core.model.equalizer.EqProfile
import com.mardous.booming.core.model.equalizer.autoeq.AutoEqProfile
import com.mardous.booming.data.local.MediaStoreWriter
import com.mardous.booming.data.model.replaygain.ReplayGainMode
import com.mardous.booming.extensions.MIME_TYPE_APPLICATION
import com.mardous.booming.extensions.MIME_TYPE_PLAIN_TEXT
import com.mardous.booming.extensions.files.getContentUri
import com.mardous.booming.extensions.files.readString
import com.mardous.booming.extensions.resolveActivity
import com.mardous.booming.extensions.showToast
import com.mardous.booming.playback.equalizer.EqualizerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File

class EqualizerViewModel(
    private val contentResolver: ContentResolver,
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
    val balanceState = equalizerManager.balanceState
    val tempoState = equalizerManager.tempoState
    val replayGainState = equalizerManager.replayGainState
    val audioOffload = equalizerManager.audioOffload
    val audioFloatOutput = equalizerManager.audioFloatOutput
    val skipSilence = equalizerManager.skipSilence
    val volumeState = equalizerManager.volumeState
    val audioDevice = audioOutputObserver.audioDevice

    val autoEqProfiles = equalizerManager.autoEqProfiles

    val eqProfiles = combine(
        equalizerManager.eqProfiles,
        equalizerManager.eqCustomProfile
    ) { profiles, custom -> profiles + custom }

    val eqBands = combine(eqState, eqBandCapabilities, currentProfile) { state, bandCapabilities, profile ->
        bandCapabilities.getBands(profile, state.preferredBandCount)
    }

    private val _exportRequestEvent = Channel<ProfileExportRequest>(Channel.BUFFERED)
    val exportRequestEvent: Flow<ProfileExportRequest> = _exportRequestEvent.receiveAsFlow()

    private val _exportResultEvent = Channel<ProfileExportResult>(Channel.BUFFERED)
    val exportResultEvent: Flow<ProfileExportResult> = _exportResultEvent.receiveAsFlow()

    private val _importRequestEvent = Channel<ProfileImportRequest>(Channel.BUFFERED)
    val importRequestEvent: Flow<ProfileImportRequest> = _importRequestEvent.receiveAsFlow()

    private val _importResultEvent = Channel<ProfileImportResult>(Channel.BUFFERED)
    val importResultEvent: Flow<ProfileImportResult> = _importResultEvent.receiveAsFlow()

    private val _autoEqImportRequestEvent = Channel<AutoEqImportRequest>(Channel.BUFFERED)
    val autoEqImportRequestEvent: Flow<AutoEqImportRequest> = _autoEqImportRequestEvent.receiveAsFlow()

    private val _autoEqImportResultEvent = Channel<ProfileOpResult>(Channel.BUFFERED)
    val autoEqImportResultEvent: Flow<ProfileOpResult> = _autoEqImportResultEvent.receiveAsFlow()

    private val _saveResultEvent = Channel<ProfileOpResult>(Channel.BUFFERED)
    val saveResultEvent: Flow<ProfileOpResult> = _saveResultEvent.receiveAsFlow()

    private val _renameResultEvent = Channel<ProfileOpResult>(Channel.BUFFERED)
    val renameResultEvent: Flow<ProfileOpResult> = _renameResultEvent.receiveAsFlow()

    private val _deleteResultEvent = Channel<ProfileDeletionResult>(Channel.BUFFERED)
    val deleteResultEvent: Flow<ProfileDeletionResult> = _deleteResultEvent.receiveAsFlow()

    private val _changeBandCountEvent = Channel<Boolean>(Channel.BUFFERED)
    val changeBandCountEvent: Flow<Boolean> = _changeBandCountEvent.receiveAsFlow()

    init {
        audioOutputObserver.startObserver()
    }

    override fun onCleared() {
        super.onCleared()
        audioOutputObserver.stopObserver()
    }

    fun setEqualizerState(isEnabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            equalizerManager.setEqualizerState(
                eqState.value.copy(enabled = isEnabled)
            )
        }
    }

    fun setLoudnessGain(
        enabled: Boolean = loudnessGainState.value.isUsable,
        gain: Float = loudnessGainState.value.gainInDb
    ) = viewModelScope.launch(Dispatchers.IO) {
        equalizerManager.setLoudnessGain(
            loudnessGainState.value.copy(
                enabled = enabled,
                gainInDb = gain
            )
        )
    }

    fun setBassBoost(
        enabled: Boolean = bassBoostState.value.isUsable,
        strength: Float = bassBoostState.value.strength
    ) = viewModelScope.launch(Dispatchers.IO) {
        equalizerManager.setBassBoost(
            bassBoostState.value.copy(
                enabled = enabled,
                strength = strength
            )
        )
    }

    fun setVirtualizer(
        enabled: Boolean = virtualizerState.value.isUsable,
        strength: Float = virtualizerState.value.strength
    ) = viewModelScope.launch(Dispatchers.IO) {
        equalizerManager.setVirtualizer(
            virtualizerState.value.copy(
                enabled = enabled,
                strength = strength
            )
        )
    }

    fun setBandCount(bandCount: Int) = viewModelScope.launch(Dispatchers.IO) {
        _changeBandCountEvent.send(equalizerManager.setBandCount(bandCount))
    }

    fun setEqualizerProfile(eqProfile: EqProfile) = viewModelScope.launch(Dispatchers.IO) {
        equalizerManager.setCurrentProfile(eqProfile)
    }

    fun setAutoEqProfile(profile: AutoEqProfile) = viewModelScope.launch(Dispatchers.IO) {
        equalizerManager.setAutoEqProfile(profile)
    }

    fun setCustomProfileBandGain(band: Int, gainInDb: Float) = viewModelScope.launch(Dispatchers.IO) {
        equalizerManager.setCustomProfileBandGain(band, gainInDb)
    }

    fun setEnableAudioOffload(enable: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        equalizerManager.setEnableAudioOffload(enable)
    }

    fun setEnableAudioFloatOutput(enable: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        equalizerManager.setEnableAudioFloatOutput(enable)
    }

    fun setEnableSkipSilences(enable: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        equalizerManager.setEnableSkipSilence(enable)
    }

    fun setVolume(volume: Float) = viewModelScope.launch(Dispatchers.IO) {
        equalizerManager.setVolume(volume)
    }

    fun setBalance(
        center: Float
    ) = viewModelScope.launch(Dispatchers.Default) {
        equalizerManager.setBalance(
            balanceState.value.copy(center = center)
        )
    }

    fun setTempo(
        speed: Float = tempoState.value.speed,
        pitch: Float = tempoState.value.pitch,
        isFixedPitch: Boolean = tempoState.value.isFixedPitch
    ) = viewModelScope.launch(Dispatchers.Default) {
        equalizerManager.setTempo(
            tempoState.value.copy(
                speed = speed,
                pitch = pitch,
                isFixedPitch = isFixedPitch
            )
        )
    }

    fun setReplayGain(
        mode: ReplayGainMode = replayGainState.value.mode,
        preamp: Float = replayGainState.value.preamp,
        preampWithoutGain: Float = replayGainState.value.preampWithoutGain
    ) = viewModelScope.launch(Dispatchers.IO) {
        equalizerManager.setReplayGain(
            replayGainState.value.copy(
                mode = mode,
                preamp = preamp,
                preampWithoutGain = preampWithoutGain
            )
        )
    }

    fun showOutputDeviceSelector(context: Context) {
        audioOutputObserver.showOutputDeviceSelector(context)
    }

    fun saveProfile(profileName: String, canReplace: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        val result = if (!canReplace && !equalizerManager.isProfileNameAvailable(profileName)) {
            ProfileOpResult(false, R.string.that_name_is_already_in_use, canDismiss = false)
        } else {
            val newProfile = equalizerManager.getNewProfileFromCustom(profileName)
            if (equalizerManager.addProfile(newProfile, canReplace, useProfile = true)) {
                ProfileOpResult(true, R.string.profile_saved_successfully)
            } else {
                ProfileOpResult(false, R.string.the_profile_could_not_be_saved)
            }
        }
        _saveResultEvent.send(result)
    }

    fun renameProfile(profile: EqProfile, newName: String?) = viewModelScope.launch(Dispatchers.IO) {
        val result = if (newName.isNullOrBlank()) {
            ProfileOpResult(false, canDismiss = false)
        } else {
            if (equalizerManager.renameProfile(profile, newName)) {
                ProfileOpResult(true, R.string.profile_renamed)
            } else {
                ProfileOpResult(false, R.string.the_profile_could_not_be_renamed)
            }
        }
        _renameResultEvent.send(result)
    }

    fun deleteProfile(
        context: Context,
        profile: EqProfile
    ) = viewModelScope.launch(Dispatchers.IO) {
        _deleteResultEvent.send(
            ProfileDeletionResult(
                success = equalizerManager.removeProfile(profile),
                profileName = profile.getName(context),
                autoEqProfile = false
            )
        )
    }

    fun deleteAutoEqProfile(
        context: Context,
        profile: AutoEqProfile
    ) = viewModelScope.launch(Dispatchers.IO) {
        _deleteResultEvent.send(
            ProfileDeletionResult(
                success = equalizerManager.deleteAutoEqProfile(profile),
                profileName = profile.name,
                autoEqProfile = true
            )
        )
    }

    fun generateExportData(profiles: List<EqProfile>) = viewModelScope.launch(Dispatchers.IO) {
        val exportName = equalizerManager.getNewExportName()
        val exportContent = runCatching { Json.encodeToString(profiles) }.getOrNull()
        val result = if (exportName.isNotEmpty() && !exportContent.isNullOrEmpty()) {
            ProfileExportRequest(success = true, profileExportData = Pair(exportName, exportContent))
        } else {
            ProfileExportRequest(false)
        }
        _exportRequestEvent.send(result)
    }

    fun exportConfiguration(data: Uri?, content: String?) = viewModelScope.launch(Dispatchers.IO) {
        val result = if (data == null || content.isNullOrEmpty()) {
            ProfileExportResult(false)
        } else {
            val result = runCatching {
                mediaStoreWriter.toContentResolver(null, data) { stream ->
                    when {
                        content.isNotEmpty() -> {
                            stream.bufferedWriter().use { it.write(content) }
                            true
                        }

                        else -> false
                    }
                }
            }

            if (result.isFailure || result.getOrThrow().resultCode == MediaStoreWriter.Result.Code.ERROR) {
                ProfileExportResult(false, R.string.an_unexpected_error_occurred)
            } else {
                ProfileExportResult(
                    success = true,
                    messageRes = R.string.profiles_exported_successfully,
                    data = data,
                    mimeType = MIME_TYPE_APPLICATION
                )
            }
        }
        _exportResultEvent.send(result)
    }

    fun requestImport(data: Uri?) = viewModelScope.launch(Dispatchers.IO) {
        val result = if (data == null || data.path?.endsWith(".json") == false) {
            ProfileImportRequest(false, R.string.there_is_nothing_to_import)
        } else {
            val parseResult = runCatching {
                contentResolver.openInputStream(data)?.use { stream ->
                    Json.decodeFromString<List<EqProfile>>(stream.readString())
                }
            }
            val profiles = parseResult.getOrNull()
            if (parseResult.isFailure || profiles == null) {
                ProfileImportRequest(false, R.string.there_is_nothing_to_import)
            } else {
                ProfileImportRequest(true, profiles = profiles)
            }
        }
        _importRequestEvent.send(result)
    }

    fun requestAutoEqImport(
        context: Context,
        uri: Uri?
    ) = viewModelScope.launch(Dispatchers.IO) {
        val result = if (uri == null) {
            AutoEqImportRequest(false, R.string.there_is_nothing_to_import)
        } else {
            val mimeType = context.contentResolver.getType(uri)
            if (mimeType == null || mimeType != MIME_TYPE_PLAIN_TEXT) {
                AutoEqImportRequest(false, R.string.there_is_nothing_to_import)
            } else {
                val parseResult = runCatching {
                    AutoEqTxtParser.parse(context, uri)
                }
                val profile = parseResult.getOrNull()
                if (parseResult.isFailure || profile == null) {
                    parseResult.exceptionOrNull()?.let {
                        Log.e("EqualizerViewModel", "AutoEq profile parsing failed!", it)
                    }
                    AutoEqImportRequest(false, R.string.there_is_nothing_to_import)
                } else {
                    AutoEqImportRequest(true, profile = profile)
                }
            }
        }
        _autoEqImportRequestEvent.send(result)
    }

    fun importProfiles(profiles: List<EqProfile>) = viewModelScope.launch(Dispatchers.IO) {
        val result = if (profiles.isNotEmpty()) {
            ProfileImportResult(true, imported = equalizerManager.importProfiles(profiles))
        } else {
            ProfileImportResult(false, R.string.no_profile_imported)
        }
        _importResultEvent.send(result)
    }

    fun importAutoEqProfile(
        profile: AutoEqProfile,
        profileName: String,
        canReplace: Boolean
    ) = viewModelScope.launch(Dispatchers.IO) {
        val result = if (!canReplace && !equalizerManager.isAutoEqProfileNameAvailable(profileName)) {
            ProfileOpResult(false, R.string.that_name_is_already_in_use, canDismiss = false)
        } else {
            if (equalizerManager.importAutoEqProfile(profile, profileName, canReplace)) {
                ProfileOpResult(true, R.string.autoeq_profile_imported_successfully)
            } else {
                ProfileOpResult(false, R.string.no_profile_imported)
            }
        }
        _autoEqImportResultEvent.send(result)
    }

    fun shareProfiles(
        context: Context,
        profiles: List<EqProfile>
    ) = viewModelScope.launch(Dispatchers.IO) {
        val result = if (profiles.isNotEmpty()) {
            val cacheDir = context.externalCacheDir
            if (cacheDir == null || (!cacheDir.exists() && !cacheDir.mkdirs())) {
                ProfileExportResult(false, R.string.an_unexpected_error_occurred)
            } else {
                val name = equalizerManager.getNewExportName()
                val result = runCatching {
                    File(cacheDir, name)
                        .also { it.writeText(Json.encodeToString(profiles)) }
                        .getContentUri(context)
                }
                if (result.isSuccess) {
                    ProfileExportResult(
                        success = true,
                        isShareRequest = true,
                        data = result.getOrThrow(),
                        mimeType = MIME_TYPE_APPLICATION
                    )
                } else {
                    ProfileExportResult(
                        success = false,
                        isShareRequest = true,
                        messageRes = R.string.an_unexpected_error_occurred
                    )
                }
            }
        } else {
            ProfileExportResult(false)
        }
        _exportResultEvent.send(result)
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