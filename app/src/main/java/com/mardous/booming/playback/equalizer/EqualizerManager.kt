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

package com.mardous.booming.playback.equalizer

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.media3.common.util.UnstableApi
import com.mardous.booming.core.model.audiodevice.AudioDevice
import com.mardous.booming.core.model.equalizer.BalanceState
import com.mardous.booming.core.model.equalizer.BassBoostState
import com.mardous.booming.core.model.equalizer.EqBandCapabilities
import com.mardous.booming.core.model.equalizer.EqProfile
import com.mardous.booming.core.model.equalizer.EqProfileAssociation
import com.mardous.booming.core.model.equalizer.EqSession
import com.mardous.booming.core.model.equalizer.EqSession.SessionType
import com.mardous.booming.core.model.equalizer.EqState
import com.mardous.booming.core.model.equalizer.LoudnessGainState
import com.mardous.booming.core.model.equalizer.ReplayGainState
import com.mardous.booming.core.model.equalizer.TempoState
import com.mardous.booming.core.model.equalizer.VirtualizerState
import com.mardous.booming.core.model.equalizer.VolumeState
import com.mardous.booming.core.model.equalizer.autoeq.AutoEqProfile
import com.mardous.booming.data.model.replaygain.ReplayGainMode
import com.mardous.booming.extensions.files.getFormattedFileName
import com.mardous.booming.extensions.utilities.toEnum
import com.mardous.booming.playback.equalizer.engine.DynamicsProcessingEngine
import com.mardous.booming.playback.equalizer.engine.EQEngine
import com.mardous.booming.playback.processor.BalanceAudioProcessor
import com.mardous.booming.playback.processor.ReplayGainAudioProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

val Context.eqDataStore by preferencesDataStore("equalizer")

@OptIn(FlowPreview::class)
@androidx.annotation.OptIn(UnstableApi::class)
class EqualizerManager(
    private val context: Context,
    private val balanceProcessor: BalanceAudioProcessor,
    private val replayGainProcessor: ReplayGainAudioProcessor
) {

    private val eqScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var eqEngine: EQEngine? = null

    private val _eqState: Flow<EqState> =
        context.eqDataStore.data.map {
            EqState(
                supported = it[Keys.EQ_SUPPORTED] ?: false,
                enabled = it[Keys.EQ_ENABLED] ?: false,
                disabledByAudioOffload = it[Keys.AUDIO_OFFLOAD] ?: false,
                preferredBandCount = it[Keys.EQ_BAND_COUNT] ?: DEFAULT_BAND_COUNT
            )
        }

    val eqState = _eqState
        .stateIn(eqScope, SharingStarted.Eagerly, EqState.Unspecified)

    private val _eqCustomProfile: Flow<EqProfile> =
        combine(_eqState, context.eqDataStore.data) { eqState, prefs ->
            val json = prefs[Keys.CUSTOM_PRESET].orEmpty().trim()
            runCatching {
                Json.decodeFromString<EqProfile>(json)
            }.getOrElse { null }
                ?.takeIf { it.isValid }
                ?: getEmptyCustomProfile(eqState.preferredBandCount)
        }

    val eqCustomProfile = _eqCustomProfile
        .stateIn(eqScope, SharingStarted.Eagerly, getEmptyCustomProfile(DEFAULT_BAND_COUNT))

    private val _eqProfiles: Flow<List<EqProfile>> =
        context.eqDataStore.data.map { prefs ->
            val json = prefs[Keys.PRESETS].orEmpty().trim()
            runCatching {
                Json.decodeFromString<List<EqProfile>>(json)
            }.getOrElse {
                emptyList()
            }
        }

    val eqProfiles = _eqProfiles
        .stateIn(eqScope, SharingStarted.Eagerly, emptyList())

    private val _eqCurrentProfile: Flow<EqProfile> =
        combine(_eqState, _eqProfiles, context.eqDataStore.data) { state, profiles, prefs ->
            val json = prefs[Keys.PRESET].orEmpty().trim()
            runCatching {
                Json.decodeFromString<EqProfile>(json)
            }.getOrElse {
                profiles.firstOrNull()
                    ?: getEmptyCustomProfile(state.preferredBandCount)
            }
        }

    val eqCurrentProfile = _eqCurrentProfile
        .stateIn(eqScope, SharingStarted.Eagerly, getEmptyCustomProfile(0))

    private val _autoEqProfiles: Flow<List<AutoEqProfile>> =
        context.eqDataStore.data.map { prefs ->
            val json = prefs[Keys.AUTO_EQ_PROFILES].orEmpty().trim()
            runCatching {
                Json.decodeFromString<List<AutoEqProfile>>(json)
            }.getOrElse {
                emptyList()
            }
        }

    val autoEqProfiles = _autoEqProfiles
        .stateIn(eqScope, SharingStarted.Eagerly, emptyList())

    private val _loudnessGainState: Flow<LoudnessGainState> =
        context.eqDataStore.data.map { prefs ->
            LoudnessGainState(
                supported = prefs[Keys.LOUDNESS_SUPPORTED] ?: false,
                enabled = prefs[Keys.LOUDNESS_ENABLED] ?: false,
                gainInDb = prefs[Keys.LOUDNESS_GAIN] ?: MINIMUM_LOUDNESS_GAIN,
                gainRange = MINIMUM_LOUDNESS_GAIN..MAXIMUM_LOUDNESS_GAIN,
            )
        }

    val loudnessGainState = _loudnessGainState
        .stateIn(eqScope, SharingStarted.Eagerly, LoudnessGainState.Unspecified)

    private val _bassBoostState: Flow<BassBoostState> =
        context.eqDataStore.data.map { prefs ->
            BassBoostState(
                supported = prefs[Keys.BASS_BOOST_SUPPORTED] ?: false,
                enabled = prefs[Keys.BASS_BOOST_ENABLED] ?: false,
                strength = prefs[Keys.BASS_BOOST_STRENGTH] ?: 0f,
                strengthRange = BASSBOOST_MIN_STRENGTH..BASSBOOST_MAX_STRENGTH
            )
        }

    val bassBoostState = _bassBoostState
        .stateIn(eqScope, SharingStarted.Eagerly, BassBoostState.Unspecified)

    private val _virtualizerState: Flow<VirtualizerState> =
        context.eqDataStore.data.map { prefs ->
            VirtualizerState(
                supported = prefs[Keys.VIRTUALIZER_SUPPORTED] ?: false,
                enabled = prefs[Keys.VIRTUALIZER_ENABLED] ?: false,
                strength = prefs[Keys.VIRTUALIZER_STRENGTH] ?: 0f,
                strengthRange = VIRTUALIZER_MIN_STRENGTH..VIRTUALIZER_MAX_STRENGTH
            )
        }

    val virtualizerState = _virtualizerState
        .stateIn(eqScope, SharingStarted.Eagerly, VirtualizerState.Unspecified)

    private val _tempoState: Flow<TempoState> =
        context.eqDataStore.data.map { prefs ->
            TempoState(
                speed = prefs[Keys.SPEED] ?: 1f,
                speedRange = MIN_SPEED..MAX_SPEED,
                pitch = prefs[Keys.PITCH] ?: 1f,
                pitchRange = MIN_PITCH..MAX_PITCH,
                isFixedPitch = prefs[Keys.IS_FIXED_PITCH] ?: true
            )
        }

    val tempoState = _tempoState
        .stateIn(eqScope, SharingStarted.Eagerly, TempoState.Unspecified)

    private val _volumeState: Flow<VolumeState> =
        context.eqDataStore.data.map { prefs ->
            VolumeState(
                currentVolume = prefs[Keys.VOLUME] ?: 1f,
                volumeRange = MIN_VOLUME..MAX_VOLUME
            )
        }

    val volumeState = _volumeState
        .stateIn(eqScope, SharingStarted.Eagerly, VolumeState.Unspecified)

    private val _balanceState: Flow<BalanceState> =
        context.eqDataStore.data.map { prefs ->
            BalanceState(
                center = prefs[Keys.CENTER_BALANCE] ?: 0f,
                range = -MAX_VOLUME..MAX_VOLUME
            )
        }

    val balanceState = _balanceState
        .stateIn(eqScope, SharingStarted.Eagerly, BalanceState.Unspecified)

    private val _replayGainState: Flow<ReplayGainState> =
        context.eqDataStore.data.map { prefs ->
            ReplayGainState(
                mode = prefs[Keys.REPLAYGAIN_MODE]?.toEnum<ReplayGainMode>() ?: ReplayGainMode.Off,
                preamp = prefs[Keys.REPLAYGAIN_PREAMP] ?: 0f,
                preampWithoutGain = prefs[Keys.REPLAYGAIN_PREAMP_WITHOUT_GAIN] ?: 0f
            )
        }

    val replayGainState = _replayGainState
        .stateIn(eqScope, SharingStarted.Eagerly, ReplayGainState.Unspecified)

    private val _audioOffload: Flow<Boolean> =
        context.eqDataStore.data.map { prefs ->
            prefs[Keys.AUDIO_OFFLOAD] ?: false
        }

    val audioOffload = _audioOffload
        .stateIn(eqScope, SharingStarted.Eagerly, false)

    private val _audioFloatOutput: Flow<Boolean> =
        context.eqDataStore.data.map { prefs ->
            prefs[Keys.AUDIO_FLOAT_OUTPUT] ?: false
        }

    val audioFloatOutput = _audioFloatOutput
        .stateIn(eqScope, SharingStarted.Eagerly, false)

    private val _skipSilence: Flow<Boolean> =
        context.eqDataStore.data.map { prefs ->
            prefs[Keys.SKIP_SILENCE] ?: false
        }

    val skipSilence = _skipSilence
        .stateIn(eqScope, SharingStarted.Eagerly, false)

    private val _bandCapabilities = MutableStateFlow(EqBandCapabilities.Empty)
    val bandCapabilities: StateFlow<EqBandCapabilities> get() = _bandCapabilities

    var eqSession = EqSession(SessionType.Internal, NO_SESSION_ID, false)
        private set

    init {
        _eqState.debounce(100)
            .onEach { newState ->
                val isOffload = newState.disabledByAudioOffload
                if (eqEngine == null && eqSession.id != NO_SESSION_ID && !isOffload) {
                    eqEngine = createEngine(eqSession.id, newState.preferredBandCount)
                }
                if (!isOffload) {
                    if (newState.isUsable) {
                        setSession(eqSession.copy(type = SessionType.Internal), newState)
                    } else {
                        setSession(eqSession.copy(type = SessionType.External), newState)
                    }
                } else {
                    setSessionIsActive(false, newState)
                }
            }
            .launchIn(eqScope)

        _balanceState.debounce(50)
            .onEach { balanceState ->
                balanceProcessor.setBalance(balanceState.left, balanceState.right)
            }
            .flowOn(Dispatchers.Main)
            .launchIn(eqScope)

        _replayGainState.debounce(50)
            .onEach { state ->
                if (state.mode.isOn) {
                    replayGainProcessor.mode = state.mode
                    replayGainProcessor.preAmpGain = state.preamp
                    replayGainProcessor.preAmpGainWithoutTag = state.preampWithoutGain
                } else {
                    replayGainProcessor.mode = ReplayGainMode.Off
                    replayGainProcessor.preAmpGain = 0f
                    replayGainProcessor.preAmpGainWithoutTag = 0f
                }
            }
            .flowOn(Dispatchers.Main)
            .launchIn(eqScope)
    }

    @SuppressLint("NewApi")
    suspend fun initializeEqualizer() = withContext(IO) {
        try {
            val effects = AudioEffect.queryEffects()
            context.eqDataStore.edit { prefs ->
                val eqInitialized = prefs[Keys.EQ_INITIALIZED]
                if (eqInitialized != true) {
                    prefs[Keys.PRESETS] = Json.encodeToString(
                        getPresetsByBandCount(DEFAULT_BAND_COUNT)
                    )
                    prefs[Keys.EQ_INITIALIZED] = true
                }
                prefs[Keys.EQ_SUPPORTED] = effects.any {
                    it.type == AudioEffect.EFFECT_TYPE_DYNAMICS_PROCESSING
                }
                prefs[Keys.VIRTUALIZER_SUPPORTED] = effects.any {
                    it.type == AudioEffect.EFFECT_TYPE_VIRTUALIZER
                }
                prefs[Keys.BASS_BOOST_SUPPORTED] = effects.any {
                    it.type == AudioEffect.EFFECT_TYPE_BASS_BOOST
                }
                prefs[Keys.LOUDNESS_SUPPORTED] = effects.any {
                    it.type == AudioEffect.EFFECT_TYPE_LOUDNESS_ENHANCER
                }
            }
        } catch (e: NoClassDefFoundError) {
            Log.e(TAG, "Audio effects are not available on this device", e)
        }
    }

    fun release() {
        setSession(EqSession(SessionType.Internal, NO_SESSION_ID, false))
        eqEngine?.release()
        eqEngine = null
    }

    fun isProfileNameAvailable(profileName: String): Boolean {
        return eqProfiles.value.none { it.name.equals(profileName, ignoreCase = true) }
    }

    fun isAutoEqProfileNameAvailable(profileName: String): Boolean {
        return autoEqProfiles.value.none { it.name.equals(profileName, ignoreCase = true) }
    }

    fun getNewExportName(): String = getFormattedFileName("BoomingEQ", "json")

    fun getEmptyCustomProfile(bandCount: Int): EqProfile {
        return EqProfile(EqProfile.CUSTOM_PRESET_NAME, FloatArray(bandCount), isCustom = true)
    }

    fun getNewProfileFromCustom(profileName: String): EqProfile {
        return eqCustomProfile.value.copy(name = profileName, isCustom = false)
    }

    suspend fun renameProfile(profile: EqProfile, newName: String): Boolean {
        val trimmedName = newName.trim()
        if (trimmedName.isEmpty()) return false

        val currentProfiles = eqProfiles.value.toMutableList()
        if (currentProfiles.any { it.name.equals(trimmedName, ignoreCase = true) }) {
            return false
        }

        val index = currentProfiles.indexOfFirst { it.name == profile.name }
        if (index == -1) return false

        currentProfiles[index] = profile.copy(name = trimmedName)

        setEqualizerProfiles(currentProfiles)
        if (profile == eqCurrentProfile.value) {
            setCurrentProfile(currentProfiles[index])
        }
        return true
    }

    suspend fun addProfile(profile: EqProfile, allowReplace: Boolean, useProfile: Boolean): Boolean {
        if (!profile.isValid) return false

        val currentProfiles = eqProfiles.value.toMutableList()
        val index = currentProfiles.indexOfFirst { it.name.equals(profile.name, ignoreCase = true) }
        if (index != -1) {
            if (allowReplace) {
                currentProfiles[index] = profile
                setEqualizerProfiles(currentProfiles)
                if (useProfile) {
                    setCurrentProfile(profile)
                }
                return true
            }
            return false
        }

        currentProfiles.add(profile)
        setEqualizerProfiles(currentProfiles)
        if (useProfile) {
            setCurrentProfile(profile)
        }
        return true
    }

    suspend fun removeProfile(profile: EqProfile): Boolean {
        val currentProfiles = eqProfiles.value.toMutableList()
        val removed = currentProfiles.removeIf { it.name == profile.name }
        if (!removed) return false

        setEqualizerProfiles(currentProfiles)
        if (profile == eqCurrentProfile.value) {
            setCurrentProfile(eqCustomProfile.value)
        }
        return true
    }

    suspend fun deleteAutoEqProfile(profile: AutoEqProfile): Boolean {
        val currentAutoEqProfiles = autoEqProfiles.value.toMutableList()
        val removed = currentAutoEqProfiles.removeIf { it.name == profile.name }
        if (!removed) return false

        setAutoEqProfiles(currentAutoEqProfiles)
        return true
    }

    suspend fun importProfiles(toImport: List<EqProfile>): Int {
        if (toImport.isEmpty()) return 0

        val currentProfiles = eqProfiles.value.toMutableList()
        val bandCapabilities = bandCapabilities.value

        var imported = 0
        for (profile in toImport) {
            if (!profile.isValid ||
                profile.isCustom ||
                !bandCapabilities.isBandCountSupported(profile.numberOfBands)) {
                continue
            }
            val existingIndex = currentProfiles.indexOfFirst { it.name.equals(profile.name, ignoreCase = true) }
            if (existingIndex >= 0) {
                currentProfiles[existingIndex] = profile
                imported++
            } else {
                currentProfiles.add(profile)
                imported++
            }
        }
        if (imported > 0) {
            setEqualizerProfiles(currentProfiles)
        }
        return imported
    }

    suspend fun importAutoEqProfile(
        profile: AutoEqProfile,
        suggestedName: String,
        allowReplace: Boolean
    ): Boolean {
        val actualProfile = if (profile.name == suggestedName) {
            profile
        } else {
            profile.copy(name = suggestedName)
        }
        if (actualProfile.name.isNotEmpty() && actualProfile.points.isNotEmpty()) {
            val autoEqProfiles = this.autoEqProfiles.value.toMutableList()

            val existingIndex = autoEqProfiles.indexOfFirst { it.name == actualProfile.name }
            if (existingIndex != -1) {
                if (allowReplace) {
                    if (autoEqProfiles.add(actualProfile)) {
                        setAutoEqProfiles(autoEqProfiles)
                        return true
                    }
                }
                return false
            }

            if (autoEqProfiles.add(actualProfile)) {
                setAutoEqProfiles(autoEqProfiles)
                return true
            }
        }
        return false
    }

    private suspend fun setEqualizerProfiles(profiles: List<EqProfile>) {
        context.eqDataStore.edit {
            it[Keys.PRESETS] = Json.encodeToString(profiles)
        }
    }

    private suspend fun setAutoEqProfiles(profiles: List<AutoEqProfile>) {
        context.eqDataStore.edit {
            it[Keys.AUTO_EQ_PROFILES] = Json.encodeToString(profiles)
        }
    }

    suspend fun addDeviceAssociation(
        profile: EqProfile,
        device: AudioDevice
    ): Boolean {
        if (device == AudioDevice.UnknownDevice) return false

        val profiles = eqProfiles.value
        val profileIndex = profiles.indexOfFirst { it == profile }
        if (profileIndex == -1) return false

        if (profile.associations.any { it.id == device.id }) {
            return false
        }

        val deviceId = device.id
        val deviceName = device.getDeviceName(context).toString()

        val newAssociation = EqProfileAssociation(deviceId, deviceName)
        val updatedProfiles = profiles.map { p ->
            when {
                p == profile -> p.copy(
                    associations = p.associations + newAssociation
                )

                p.associations.any { it.id == deviceId } -> p.copy(
                    associations = p.associations.filterNot { it.id == deviceId }.toSet()
                )

                else -> p
            }
        }

        setEqualizerProfiles(updatedProfiles)
        return true
    }

    suspend fun removeDeviceAssociation(
        profile: EqProfile,
        deviceId: String
    ): Boolean {
        val profiles = eqProfiles.value
        val profileIndex = profiles.indexOfFirst { it == profile }
        if (profileIndex == -1) return false

        if (profile.associations.none { it.id == deviceId }) {
            return false
        }

        val updatedProfile = profile.copy(
            associations = profile.associations
                .filterNot { it.id == deviceId }
                .toSet()
        )

        val updatedProfiles = profiles.mapIndexed { index, p ->
            if (index == profileIndex) updatedProfile else p
        }

        setEqualizerProfiles(updatedProfiles)
        return true
    }

    suspend fun setCurrentProfile(eqProfile: EqProfile) {
        if (bandCapabilities.value.isBandCountSupported(eqProfile.numberOfBands)) {
            if (eqProfile.numberOfBands != eqState.value.preferredBandCount) {
                setBandCount(
                    bandCount = eqProfile.numberOfBands,
                    profileAfterChange = eqProfile
                )
            } else {
                context.eqDataStore.edit {
                    it[Keys.PRESET] = Json.encodeToString(eqProfile)
                }
                applyChangesToEngine(profile = eqProfile)
            }
        }
    }

    suspend fun setCustomProfileBandGain(band: Int, gainInDb: Float) {
        val currentProfile = eqCurrentProfile.value
        val newBandLevels = currentProfile.levels.copyOf()
        if (band in newBandLevels.indices) {
            newBandLevels[band] = gainInDb
        }
        val customProfile = currentProfile.copy(
            name = EqProfile.CUSTOM_PRESET_NAME,
            levels = newBandLevels,
            isCustom = true
        )
        setCustomProfile(customProfile, fromUser = true)
    }

    private suspend fun setCustomProfile(profile: EqProfile, fromUser: Boolean) {
        if (profile.isCustom) {
            val serializedProfile = Json.encodeToString(profile)
            context.eqDataStore.edit {
                it[Keys.CUSTOM_PRESET] = serializedProfile
                if (fromUser) {
                    it[Keys.PRESET] = serializedProfile
                }
            }
            if (fromUser) {
                applyChangesToEngine(profile = profile)
            }
        }
    }

    fun setSessionId(audioSessionId: Int, eqState: EqState = this.eqState.value) {
        setSession(
            eqSession.copy(
                id = audioSessionId,
                type = if (eqState.enabled) {
                    SessionType.Internal
                } else {
                    SessionType.External
                }
            )
        )
    }

    fun setSessionIsActive(isActive: Boolean, eqState: EqState = this.eqState.value) {
        setSession(
            newSession = eqSession.copy(
                active = isActive,
                type = if (eqState.enabled) {
                    SessionType.Internal
                } else {
                    SessionType.External
                }
            ),
            eqState = eqState
        )
    }

    private fun setSession(newSession: EqSession, eqState: EqState = this.eqState.value) {
        val oldSession = this.eqSession
        if (newSession == oldSession)
            return

        this.eqSession = newSession
        when (oldSession.type) {
            SessionType.Internal -> {
                eqEngine?.setEnabled(false)
                if (eqState.disabledByAudioOffload) {
                    eqEngine?.release()
                    eqEngine = null
                }
            }

            SessionType.External -> {
                if (oldSession.id != NO_SESSION_ID) {
                    val intent = Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION)
                        .putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                        .putExtra(AudioEffect.EXTRA_AUDIO_SESSION, oldSession.id)

                    context.sendBroadcast(intent)
                }
            }
        }

        if (!eqState.disabledByAudioOffload && newSession.active && newSession.id != NO_SESSION_ID) {
            when (newSession.type) {
                SessionType.Internal -> {
                    if (newSession.id != this.eqEngine?.sessionId) {
                        eqEngine?.release()
                        eqEngine = createEngine(newSession.id, eqState.preferredBandCount)
                    }
                    eqEngine?.setEnabled(true)
                }

                SessionType.External -> {
                    val intent = Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION)
                        .putExtra(AudioEffect.EXTRA_AUDIO_SESSION, newSession.id)
                        .putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                        .putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)

                    context.sendBroadcast(intent)
                }
            }
        }
    }

    suspend fun setEqualizerState(state: EqState, newProfile: EqProfile? = null) {
        context.eqDataStore.edit { prefs ->
            prefs[Keys.EQ_ENABLED] = state.enabled
            prefs[Keys.EQ_BAND_COUNT] = state.preferredBandCount
            if (newProfile != null) {
                val serializedProfile = Json.encodeToString(newProfile)
                prefs[Keys.PRESET] = serializedProfile
                if (newProfile.isCustom) {
                    prefs[Keys.CUSTOM_PRESET] = serializedProfile
                }
            }
        }
        if (newProfile != null) {
            applyChangesToEngine(state = state, profile = newProfile)
        } else {
            applyChangesToEngine(state = state)
        }
    }

    suspend fun setLoudnessGain(state: LoudnessGainState) {
        context.eqDataStore.edit { prefs ->
            prefs[Keys.LOUDNESS_ENABLED] = state.enabled
            prefs[Keys.LOUDNESS_GAIN] = state.gainInDb
        }
        applyChangesToEngine(loudnessGainState = state)
    }

    suspend fun setBassBoost(state: BassBoostState) {
        context.eqDataStore.edit { prefs ->
            prefs[Keys.BASS_BOOST_ENABLED] = state.enabled
            prefs[Keys.BASS_BOOST_STRENGTH] = state.strength
        }
        applyChangesToEngine(bassBoostState = state)
    }

    suspend fun setVirtualizer(state: VirtualizerState) {
        context.eqDataStore.edit { prefs ->
            prefs[Keys.VIRTUALIZER_ENABLED] = state.enabled
            prefs[Keys.VIRTUALIZER_STRENGTH] = state.strength
        }
        applyChangesToEngine(virtualizerState = state)
    }

    suspend fun setBandCount(
        bandCount: Int,
        profileAfterChange: EqProfile = getEmptyCustomProfile(bandCount = bandCount)
    ): Boolean {
        if (eqState.value.preferredBandCount == bandCount)
            return false

        val bandCapabilities = this.bandCapabilities.value
        if (bandCapabilities.hasMultipleBandConfigurations &&
            bandCapabilities.isBandCountSupported(bandCount)) {
            eqEngine?.let { engine ->
                if (engine.setBandCount(bandCount)) {
                    setEqualizerState(
                        state = eqState.value.copy(preferredBandCount = bandCount),
                        newProfile = profileAfterChange
                    )
                    return true
                }
            }
        }
        return false
    }

    suspend fun setEnableAudioOffload(audioOffload: Boolean) {
        context.eqDataStore.edit { prefs ->
            prefs[Keys.AUDIO_OFFLOAD] = audioOffload
        }
    }

    suspend fun setEnableAudioFloatOutput(audioFloatOutput: Boolean) {
        context.eqDataStore.edit { prefs ->
            prefs[Keys.AUDIO_FLOAT_OUTPUT] = audioFloatOutput
        }
    }

    suspend fun setEnableSkipSilence(skipSilence: Boolean) {
        context.eqDataStore.edit { prefs ->
            prefs[Keys.SKIP_SILENCE] = skipSilence
        }
    }

    suspend fun setVolume(volume: Float) {
        context.eqDataStore.edit { prefs ->
            prefs[Keys.VOLUME] = volume
        }
    }

    suspend fun setBalance(balance: BalanceState) {
        context.eqDataStore.edit { prefs ->
            prefs[Keys.CENTER_BALANCE] = balance.center
        }
    }

    suspend fun setTempo(tempo: TempoState) {
        context.eqDataStore.edit { prefs ->
            prefs[Keys.SPEED] = tempo.speed
            prefs[Keys.PITCH] = tempo.pitch
            prefs[Keys.IS_FIXED_PITCH] = tempo.isFixedPitch
        }
    }

    suspend fun setReplayGain(replayGain: ReplayGainState) {
        context.eqDataStore.edit { prefs ->
            prefs[Keys.REPLAYGAIN_PREAMP] = replayGain.preamp
            prefs[Keys.REPLAYGAIN_PREAMP_WITHOUT_GAIN] = replayGain.preampWithoutGain
            prefs[Keys.REPLAYGAIN_MODE] = replayGain.mode.name
        }
    }

    private fun setBandCapabilities(bandCapabilities: EqBandCapabilities) {
        _bandCapabilities.value = bandCapabilities
    }

    suspend fun setCurrentDevice(currentDevice: AudioDevice) {
        if (currentDevice == AudioDevice.UnknownDevice)
            return

        val profileByDevice = eqProfiles.value.firstOrNull { profile ->
            profile.associations.any { association ->
                association.id == currentDevice.id
            }
        }
        if (profileByDevice != null) {
            setCurrentProfile(profileByDevice)
        }
    }

    suspend fun setAutoEqProfile(profile: AutoEqProfile) {
        val currentBandCount = eqState.value.preferredBandCount
        val bandCapabilities = bandCapabilities.value
        if (bandCapabilities.isBandCountSupported(currentBandCount)) {
            val frequencies = bandCapabilities.getFrequencies(currentBandCount)
            val profile = EqProfile(
                name = profile.name,
                levels = profile.getBandGains(frequencies, bandCapabilities.bandRange),
                isCustom = true,
                isAutoEq = true
            )
            setCustomProfile(profile, fromUser = true)
        }
    }

    private fun getPresetsByBandCount(bandCount: Int) = when (bandCount) {
        5 -> listOf(
            EqProfile("Bass Boost", floatArrayOf(11f, 6f, -1f, -2.5f, -0.5f)),
            EqProfile("Classical", floatArrayOf(4f, 0.5f, 0.5f, 2.5f, 4.5f)),
            EqProfile("Flat", floatArrayOf(0f, 0f, 0f, 0f, 0f)),
            EqProfile("Jazz", floatArrayOf(4f, 2f, 1f, 3.5f, 1.5f)),
            EqProfile("Pop", floatArrayOf(0.5f, 6f, 2f, 0.5f, 4.5f)),
            EqProfile("Rock", floatArrayOf(7f, 2.5f, -2f, 3.5f, 7.5f)),
            EqProfile("Treble Boost", floatArrayOf(-2f, -0.5f, 2f, 7f, 11f)),
            EqProfile("Vocal", floatArrayOf(-2.5f, 2f, 9f, 4f, -0.5f))
        )

        15 -> listOf(
            EqProfile("Bass Boost", floatArrayOf(13f, 12f, 10f, 8f, 6f, 3f, 0f, -2f, -3f, -3f, -2f, -1f, 0f, 0f, 0f)),
            EqProfile("Classical", floatArrayOf(6f, 5f, 3f, 2f, 1f, 0f, 0f, 1f, 2f, 3f, 4f, 5f, 6f, 7f, 7f)),
            EqProfile("Flat", floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)),
            EqProfile("Jazz", floatArrayOf(4f, 5f, 4f, 3f, 2f, 1f, 0f, 2f, 4f, 5f, 4f, 3f, 2f, 1f, 0f)),
            EqProfile("Pop", floatArrayOf(-1f, 0f, 3f, 5f, 7f, 5f, 2f, 0f, -1f, 0f, 2f, 4f, 5f, 6f, 6f)),
            EqProfile("Rock", floatArrayOf(9f, 8f, 6f, 4f, 2f, 0f, -2f, -3f, -2f, 0f, 3f, 6f, 8f, 9f, 10f)),
            EqProfile("Treble Boost", floatArrayOf(-3f, -2f, -1f, 0f, 1f, 2f, 3f, 4f, 6f, 8f, 10f, 12f, 13f, 14f, 14f)),
            EqProfile("Vocal", floatArrayOf(-4f, -3f, -2f, 0f, 4f, 8f, 11f, 12f, 9f, 6f, 3f, 1f, 0f, -1f, -1f))
        )

        else -> listOf(
            EqProfile("Bass Boost", floatArrayOf(12f, 10f, 8f, 4f, 0f, -2f, -3f, -2f, -1f, 0f)),
            EqProfile("Classical", floatArrayOf(5f, 3f, 1f, 0f, 0f, 1f, 2f, 3f, 4f, 5f)),
            EqProfile("Flat", floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)),
            EqProfile("Jazz", floatArrayOf(3f, 5f, 3f, 1f, 0f, 2f, 4f, 3f, 2f, 1f)),
            EqProfile("Pop", floatArrayOf(-1f, 2f, 5f, 7f, 4f, 0f, -1f, 2f, 4f, 5f)),
            EqProfile("Rock", floatArrayOf(8f, 6f, 4f, 1f, -2f, -2f, 2f, 5f, 7f, 8f)),
            EqProfile("Treble Boost", floatArrayOf(-2f, -2f, -1f, 0f, 1f, 3f, 6f, 8f, 10f, 12f)),
            EqProfile("Vocal", floatArrayOf(-3f, -2f, 0f, 4f, 8f, 10f, 6f, 2f, 0f, -1f))
        )
    }

    private fun createEngine(sessionId: Int, bandCount: Int): EQEngine? {
        return runCatching {
            DynamicsProcessingEngine(sessionId, bandCount)
        }.onSuccess { newEngine ->
            applyChangesToEngine(engine = newEngine)
            setBandCapabilities(newEngine.bandCapabilities)
        }.onFailure {
            Log.e(TAG, "Failed to open EQ session", it)
        }.getOrNull()
    }

    private fun applyChangesToEngine(
        engine: EQEngine? = this.eqEngine,
        state: EqState = this.eqState.value,
        profile: EqProfile = this.eqCurrentProfile.value,
        bassBoostState: BassBoostState = this.bassBoostState.value,
        virtualizerState: VirtualizerState = this.virtualizerState.value,
        loudnessGainState: LoudnessGainState = this.loudnessGainState.value
    ) {
        engine?.let {
            applyEngine(
                engine = it,
                state = state,
                profile = profile,
                bassBoostState = bassBoostState,
                virtualizerState = virtualizerState,
                loudnessGainState = loudnessGainState
            )
        }
    }

    private fun applyEngine(
        engine: EQEngine,
        state: EqState,
        profile: EqProfile,
        bassBoostState: BassBoostState,
        virtualizerState: VirtualizerState,
        loudnessGainState: LoudnessGainState
    ) {
        runCatching {
            if (state.isUsable) {
                // Apply EQ
                engine.setEnabled(true)
                engine.setProfile(profile)

                // Apply Bass Boost
                runCatching {
                    if (bassBoostState.isUsable) {
                        engine.setBassBoostState(bassBoostState)
                    } else {
                        engine.setBassBoostState(BassBoostState.Unspecified)
                    }
                }.onFailure { Log.e(TAG, "Error setting up bass boost!", it) }

                // Apply Virtualizer
                runCatching {
                    if (virtualizerState.isUsable) {
                        engine.setVirtualizerState(virtualizerState)
                    } else {
                        engine.setVirtualizerState(VirtualizerState.Unspecified)
                    }
                }.onFailure { Log.e(TAG, "Error setting up virtualizer!", it) }

                // Apply Loudness Enhancer
                runCatching {
                    if (loudnessGainState.isUsable) {
                        engine.setLoudnessGainState(loudnessGainState)
                    } else {
                        engine.setLoudnessGainState(LoudnessGainState.Unspecified)
                    }
                }.onFailure { Log.e(TAG, "Error setting up loudness enhancer!", it) }
            } else {
                engine.setEnabled(false)
                engine.setVirtualizerState(VirtualizerState.Unspecified)
                engine.setBassBoostState(BassBoostState.Unspecified)
                engine.setLoudnessGainState(LoudnessGainState.Unspecified)
            }
        }.onFailure {
            Log.e(TAG, "Error setting up EQ engine", it)
        }
    }

    suspend fun resetConfiguration() {
        context.eqDataStore.edit {
            it.clear()
        }
        eqEngine?.release()
        eqEngine = null
        initializeEqualizer()
    }

    interface Keys {
        companion object {
            val EQ_INITIALIZED = booleanPreferencesKey("eq.initialized")
            val EQ_ENABLED = booleanPreferencesKey("eq.enabled")
            val EQ_SUPPORTED = booleanPreferencesKey("eq.supported")
            val EQ_BAND_COUNT = intPreferencesKey("eq.band.count")
            val VIRTUALIZER_SUPPORTED = booleanPreferencesKey("eq.virtualizer.supported")
            val VIRTUALIZER_ENABLED = booleanPreferencesKey("eq.virtualizer.enabled")
            val VIRTUALIZER_STRENGTH = floatPreferencesKey("eq.virtualizer.strength")
            val BASS_BOOST_SUPPORTED = booleanPreferencesKey("eq.bassboost.supported")
            val BASS_BOOST_ENABLED = booleanPreferencesKey("eq.bassboost.enabled")
            val BASS_BOOST_STRENGTH = floatPreferencesKey("eq.bassboost.strength")
            val LOUDNESS_SUPPORTED = booleanPreferencesKey("eq.loudness.supported")
            val LOUDNESS_ENABLED = booleanPreferencesKey("eq.loudness.enabled")
            val LOUDNESS_GAIN = floatPreferencesKey("eq.loudness.gain")
            val AUTO_EQ_PROFILES = stringPreferencesKey("eq.profiles.autoeq")
            val PRESETS = stringPreferencesKey("eq.profiles")
            val PRESET = stringPreferencesKey("eq.profile")
            val CUSTOM_PRESET = stringPreferencesKey("eq.profile.custom")
            val AUDIO_OFFLOAD = booleanPreferencesKey("audio.offload")
            val AUDIO_FLOAT_OUTPUT = booleanPreferencesKey("audio.float_output")
            val SKIP_SILENCE = booleanPreferencesKey("audio.skip_silence")
            val REPLAYGAIN_MODE = stringPreferencesKey("replaygain.mode")
            val REPLAYGAIN_PREAMP = floatPreferencesKey("replaygain.preamp")
            val REPLAYGAIN_PREAMP_WITHOUT_GAIN = floatPreferencesKey("replaygain.preamp.without_gain")
            val VOLUME = floatPreferencesKey("player.volume")
            val CENTER_BALANCE = floatPreferencesKey("eq.balance")
            val SPEED = floatPreferencesKey("eq.speed")
            val PITCH = floatPreferencesKey("eq.pitch")
            val IS_FIXED_PITCH = booleanPreferencesKey("eq.pitch.fixed")
        }
    }

    companion object {
        private const val TAG = "EqualizerManager"

        private const val DEFAULT_BAND_COUNT = 10
        private const val NO_SESSION_ID = 0

        const val MINIMUM_LOUDNESS_GAIN = 0f
        const val MAXIMUM_LOUDNESS_GAIN = 40f

        const val BASSBOOST_MIN_STRENGTH = 0f
        const val BASSBOOST_MAX_STRENGTH = 1000f

        const val VIRTUALIZER_MIN_STRENGTH = 0f
        const val VIRTUALIZER_MAX_STRENGTH = 1000f

        const val MIN_SPEED = .5f
        const val MAX_SPEED = 2f

        const val MIN_PITCH = .5f
        const val MAX_PITCH = 2f

        const val MIN_VOLUME = 0f
        const val MAX_VOLUME = 1f
    }
}