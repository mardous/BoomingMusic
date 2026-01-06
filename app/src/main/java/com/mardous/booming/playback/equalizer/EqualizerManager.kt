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
import android.media.audiofx.AudioEffect
import android.media.audiofx.Equalizer
import androidx.core.content.edit
import com.mardous.booming.core.model.equalizer.*
import com.mardous.booming.core.model.equalizer.EQPreset.Companion.getEmptyPreset
import com.mardous.booming.extensions.files.getFormattedFileName
import com.mardous.booming.util.Preferences.requireString
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * @author Christians M. A. (mardous)
 */
@OptIn(FlowPreview::class)
class EqualizerManager internal constructor(context: Context) {

    private val mPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val eqScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val eqSession = EqualizerSession(context, this)

    private var isEqualizerSupported = false
    private var isVirtualizerSupported = false
    private var isBassBoostSupported = false
    private var isLoudnessEnhancerSupported = false

    private val _eqStateFlow: MutableStateFlow<EqState>
    private val _bandCapabilitiesFlow: MutableStateFlow<EqBandCapabilities>
    private val _bassBoostFlow: MutableStateFlow<EqEffectState<Float>>
    private val _virtualizerFlow: MutableStateFlow<EqEffectState<Float>>
    private val _loudnessGainFlow: MutableStateFlow<EqEffectState<Float>>
    private val _currentPresetFlow: MutableStateFlow<EQPreset>
    private val _presetsFlow: MutableStateFlow<EqPresetList>

    var audioSessionId = AudioEffect.ERROR_BAD_VALUE
        private set

    var isSessionActive = false
        private set

    val eqStateFlow: StateFlow<EqState> get() = _eqStateFlow
    val bandCapabilitiesFlow: StateFlow<EqBandCapabilities> get() = _bandCapabilitiesFlow
    val bassBoostFlow: StateFlow<EqEffectState<Float>> get() = _bassBoostFlow
    val virtualizerFlow: StateFlow<EqEffectState<Float>> get() = _virtualizerFlow
    val loudnessGainFlow: StateFlow<EqEffectState<Float>> get() = _loudnessGainFlow
    val currentPresetFlow: StateFlow<EQPreset> get() = _currentPresetFlow
    val presetsFlow: StateFlow<EqPresetList> get() = _presetsFlow

    val eqState get() = eqStateFlow.value
    val bandCapabilities get() = bandCapabilitiesFlow.value
    val bassBoostState get() = bassBoostFlow.value
    val virtualizerState get() = virtualizerFlow.value
    val loudnessGainState get() = loudnessGainFlow.value

    val equalizerPresets get() = presetsFlow.value.list
    val currentPreset get() = currentPresetFlow.value

    var isInitialized: Boolean
        get() = mPreferences.getBoolean(Keys.IS_INITIALIZED, false)
        set(value) = mPreferences.edit {
            putBoolean(Keys.IS_INITIALIZED, value)
        }

    init {
        try {
            //Query available effects
            val effects = AudioEffect.queryEffects()
            //Determine available/supported effects
            if (!effects.isNullOrEmpty()) {
                for (effect in effects) {
                    when (effect.type) {
                        UUID.fromString(EFFECT_TYPE_EQUALIZER) -> isEqualizerSupported = true
                        UUID.fromString(EFFECT_TYPE_BASS_BOOST) -> isBassBoostSupported = true
                        UUID.fromString(EFFECT_TYPE_VIRTUALIZER) -> isVirtualizerSupported = true
                        UUID.fromString(EFFECT_TYPE_LOUDNESS_ENHANCER) -> isLoudnessEnhancerSupported = true
                    }
                }
            }
        } catch (_: NoClassDefFoundError) {
            //The user doesn't have the AudioEffect/AudioEffect.Descriptor class. How sad.
        }

        _eqStateFlow = MutableStateFlow(initializeEqState()).also {
            it.debounce(100)
                .onEach { newState ->
                    if (newState.isUsable) {
                        eqSession.openInternalSession(audioSessionId, closeExternal = true)
                    } else {
                        eqSession.openExternalSession(audioSessionId, closeInternal = true)
                    }
                }
                .launchIn(eqScope)
        }
        _bandCapabilitiesFlow = MutableStateFlow(initializeEqBandCapabilities())
        _presetsFlow = MutableStateFlow(initializePresets())
        _currentPresetFlow = MutableStateFlow(initializeCurrentPreset())
        _bassBoostFlow = MutableStateFlow(initializeBassBoostState())
        _virtualizerFlow = MutableStateFlow(initializeVirtualizerState())
        _loudnessGainFlow = MutableStateFlow(initializeLoudnessGain())
    }

    suspend fun initializeEqualizer() = withContext(IO) {
        if (!isInitialized) {
            val result = runCatching { EffectSet(0) }
            if (result.isSuccess) {
                val temp = result.getOrThrow()
                if (temp.equalizer == null)
                    return@withContext

                val numberOfBands = temp.getNumEqualizerBands().toInt().coerceAtMost(MAX_BANDS)
                val levelRange = temp.equalizer.bandLevelRange
                    .joinToString(DEFAULT_DELIMITER)

                val centerFreqs = (0 until numberOfBands)
                    .map { temp.equalizer.getCenterFreq(it.toShort()) }
                    .joinToString(DEFAULT_DELIMITER)

                mPreferences.edit(commit = true) {
                    putInt(Keys.NUM_BANDS, numberOfBands)
                    putString(Keys.CENTER_FREQUENCIES, centerFreqs)
                    putString(Keys.BAND_LEVEL_RANGE, levelRange)
                }

                setDefaultPresets(temp, temp.equalizer)

                temp.release()
            }

            isInitialized = true
            initializeFlow()

            eqSession.update()
        }
    }

    suspend fun initializeFlow() {
        _eqStateFlow.emit(initializeEqState())
        _presetsFlow.emit(initializePresets())
        _currentPresetFlow.emit(initializeCurrentPreset())
        _bassBoostFlow.emit(initializeBassBoostState())
        _virtualizerFlow.emit(initializeVirtualizerState())
        _loudnessGainFlow.emit(initializeLoudnessGain())
    }

    fun release() {
        eqSession.closeExternalSession(audioSessionId)
        eqSession.release()
    }

    fun isPresetNameAvailable(presetName: String): Boolean {
        for ((name) in equalizerPresets) {
            if (name.equals(presetName, ignoreCase = true)) return false
        }
        return true
    }

    fun getNewExportName(): String = getFormattedFileName("BoomingEQ", "json")

    fun getNewPresetFromCustom(presetName: String): EQPreset {
        return EQPreset(getCustomPreset(), presetName, isCustom = false)
    }

    fun getEqualizerPresetsWithCustom(presets: List<EQPreset> = equalizerPresets) =
        presets.toMutableList().apply { add(getCustomPreset()) }

    fun renamePreset(preset: EQPreset, newName: String): Boolean {
        val trimmedName = newName.trim()
        if (trimmedName.isEmpty()) return false

        val currentPresets = equalizerPresets.toMutableList()
        if (currentPresets.any { it.name.equals(trimmedName, ignoreCase = true) }) {
            return false
        }

        val index = currentPresets.indexOfFirst { it.name == preset.name }
        if (index == -1) return false

        currentPresets[index] = preset.copy(name = trimmedName)

        setEqualizerPresets(currentPresets, updateFlow = true)
        if (preset == currentPreset) {
            setCurrentPreset(currentPresets[index])
        }
        return true
    }

    fun addPreset(preset: EQPreset, allowReplace: Boolean, usePreset: Boolean): Boolean {
        if (!preset.isValid) return false

        val currentPresets = equalizerPresets.toMutableList()
        val index = currentPresets.indexOfFirst { it.name.equals(preset.name, ignoreCase = true) }
        if (index != -1) {
            if (allowReplace) {
                currentPresets[index] = preset
                setEqualizerPresets(currentPresets, updateFlow = true)
                if (usePreset) {
                    setCurrentPreset(preset)
                }
                return true
            }
            return false
        }

        currentPresets.add(preset)
        setEqualizerPresets(currentPresets, updateFlow = true)
        if (usePreset) {
            setCurrentPreset(preset)
        }
        return true
    }

    fun removePreset(preset: EQPreset): Boolean {
        val currentPresets = equalizerPresets.toMutableList()
        val removed = currentPresets.removeIf { it.name == preset.name }
        if (!removed) return false

        setEqualizerPresets(currentPresets, updateFlow = true)
        if (preset == currentPreset) {
            setCurrentPreset(getCustomPreset())
        }
        return true
    }

    fun importPresets(toImport: List<EQPreset>): Int {
        if (toImport.isEmpty()) return 0

        val currentPresets = equalizerPresets.toMutableList()
        val numBands = bandCapabilities.bandCount

        var imported = 0
        for (preset in toImport) {
            if (!preset.isValid || preset.isCustom || preset.numberOfBands != numBands) {
                continue
            }
            val existingIndex = currentPresets.indexOfFirst { it.name.equals(preset.name, ignoreCase = true) }
            if (existingIndex >= 0) {
                currentPresets[existingIndex] = preset
                imported++
            } else {
                currentPresets.add(preset)
                imported++
            }
        }
        if (imported > 0) {
            setEqualizerPresets(currentPresets, updateFlow = true)
        }
        return imported
    }

    private fun setEqualizerPresets(presets: List<EQPreset>, updateFlow: Boolean) {
        mPreferences.edit { putString(Keys.PRESETS, Json.encodeToString(presets)) }
        if (updateFlow) {
            _presetsFlow.tryEmit(EqPresetList(presets))
        }
    }

    @SuppressLint("KotlinPropertyAccess")
    fun setDefaultPresets(effectSet: EffectSet, equalizer: Equalizer) {
        val presets = arrayListOf<EQPreset>()

        val numPresets = effectSet.getNumEqualizerPresets().toInt()
        val numBands = effectSet.getNumEqualizerBands().toInt()

        for (i in 0 until numPresets) {
            val name = equalizer.getPresetName(i.toShort())

            val levels = IntArray(numBands)
            try {
                equalizer.usePreset(i.toShort())
            } catch (e: RuntimeException) {
                e.printStackTrace()
            }

            for (j in 0 until numBands) {
                levels[j] = equalizer.getBandLevel(j.toShort()).toInt()
            }

            presets.add(EQPreset(name, levels, isCustom = false))
        }

        setEqualizerPresets(presets, false)
    }

    @Synchronized
    private fun getCustomPreset(): EQPreset {
        val json = mPreferences.getString(Keys.CUSTOM_PRESET, null).orEmpty().trim()
        return if (json.isEmpty()) {
            getAndSaveEmptyCustomPreset()
        } else runCatching {
            Json.decodeFromString<EQPreset>(json)
        }.getOrElse { null }?.takeIf { it.isValid } ?: getAndSaveEmptyCustomPreset()
    }

    @Synchronized
    private fun setCustomPreset(preset: EQPreset, fromUser: Boolean) {
        if (preset.isCustom) {
            if (fromUser) {
                setCurrentPreset(preset, fromUser = true)
            }
            mPreferences.edit {
                putString(Keys.CUSTOM_PRESET, Json.encodeToString(preset))
            }
        }
    }

    private fun getAndSaveEmptyCustomPreset(): EQPreset {
        val emptyPreset = getEmptyPreset(CUSTOM_PRESET_NAME, true, bandCapabilities.bandCount)
        setCustomPreset(emptyPreset, fromUser = false)
        return emptyPreset
    }

    private fun getAndSaveDefaultOrEmptyPreset(): EQPreset {
        return equalizerPresets.firstOrNull()
            ?: getAndSaveEmptyCustomPreset()
    }

    @Synchronized
    private fun getCustomPresetFromCurrent(): EQPreset {
        return EQPreset(currentPreset, CUSTOM_PRESET_NAME, true)
    }

    /**
     * Copies the current preset to a "Custom" configuration
     * and sets the band level on it
     */
    fun setCustomPresetBandLevel(band: Int, level: Int) {
        val currentPreset = getCustomPresetFromCurrent()
        currentPreset.setBandLevel(band, level)
        setCustomPreset(currentPreset, fromUser = true)
    }

    /**
     * Copies the current preset to a "Custom" configuration
     * and sets the effect value on it
     */
    private fun setCustomPresetEffect(effect: String, value: Float) {
        val currentPreset = getCustomPresetFromCurrent()
        if (value == 0f) { // zero means "disabled", we must remove disabled effects
            currentPreset.removeEffect(effect)
        } else {
            currentPreset.setEffect(effect, value)
        }
        setCustomPreset(currentPreset, fromUser = true)
    }

    fun setCurrentPreset(eqPreset: EQPreset, fromUser: Boolean = false) {
        mPreferences.edit {
            putString(Keys.PRESET, Json.encodeToString(eqPreset))
        }
        _currentPresetFlow.tryEmit(eqPreset)
        if (fromUser) {
            // We must force the preset list in the adapter to be updated so
            // that the "Custom" entry reflects the new parameters.
            _presetsFlow.tryEmit(EqPresetList(equalizerPresets))
        } else {
            // In this case, the changes were not made by the user so these
            // flows are not aware of the new state, we need to refresh them.
            _virtualizerFlow.tryEmit(initializeVirtualizerState(eqPreset))
            _bassBoostFlow.tryEmit(initializeBassBoostState(eqPreset))
        }
        eqSession.update()
    }

    fun setSessionId(audioSessionId: Int) {
        if (this.audioSessionId == audioSessionId)
            return

        val oldIsActive = this.isSessionActive
        setSessionIsActiveImpl(
            isActive = false,
            isCloseSessions = this.audioSessionId != AudioEffect.ERROR_BAD_VALUE
        )
        this.audioSessionId = audioSessionId
        if (oldIsActive && audioSessionId != AudioEffect.ERROR_BAD_VALUE) {
            setSessionIsActiveImpl(isActive = true, isCloseSessions = false)
        }
    }

    fun setSessionIsActive(isActive: Boolean) {
        setSessionIsActiveImpl(isActive, false)
    }

    suspend fun setTransientEqualizerState(
        isEnabled: Boolean,
        isDisabledByAudioOffload: Boolean,
        isSupported: Boolean = isEnabled
    ) {
        setEqualizerState(
            update = EqUpdate(
                state = eqState,
                isEnabled = mPreferences.getBoolean(Keys.GLOBAL_ENABLED, false) && isEnabled,
                isSupported = isEqualizerSupported && isSupported,
                isDisabledByAudioOffload = isDisabledByAudioOffload,
                isTransient = true
            ),
            apply = false
        )
    }

    suspend fun setEqualizerState(update: EqUpdate<EqState>, apply: Boolean) {
        val newState = update.toState()
        _eqStateFlow.emit(newState)
        if (apply) newState.apply()
    }

    suspend fun setLoudnessGain(update: EqEffectUpdate<Float>, apply: Boolean) {
        val newState = update.toState()
        _loudnessGainFlow.emit(newState)
        if (apply) newState.apply()
    }

    suspend fun setBassBoost(update: EqEffectUpdate<Float>, apply: Boolean) {
        val newState = update.toState()
        _bassBoostFlow.emit(newState)
        if (apply) newState.apply()
    }

    suspend fun setVirtualizer(update: EqEffectUpdate<Float>, apply: Boolean) {
        val newState = update.toState()
        _virtualizerFlow.emit(newState)
        if (apply) newState.apply()
    }

    suspend fun applyPendingStates() {
        eqState.apply()
        loudnessGainState.apply()
        bassBoostState.apply()
        virtualizerState.apply()
    }

    private fun initializePresets(): EqPresetList {
        val json = mPreferences.getString(Keys.PRESETS, null).orEmpty()
        val presets = runCatching {
            Json.decodeFromString<List<EQPreset>>(json).toMutableList()
        }.getOrElse {
            arrayListOf()
        }
        return EqPresetList(presets)
    }

    private fun initializeCurrentPreset(): EQPreset {
        val json = mPreferences.getString(Keys.PRESET, null).orEmpty().trim()
        if (json.isEmpty()) {
            return getAndSaveDefaultOrEmptyPreset()
        }
        return runCatching {
            Json.decodeFromString<EQPreset>(json)
        }.getOrElse { getAndSaveDefaultOrEmptyPreset() }
    }

    private fun initializeEqState(): EqState {
        return EqState(
            isSupported = isEqualizerSupported,
            isEnabled = mPreferences.getBoolean(Keys.GLOBAL_ENABLED, false),
            isDisabledByAudioOffload = false,
            onCommit = { state ->
                mPreferences.edit(commit = true) {
                    putBoolean(Keys.GLOBAL_ENABLED, state.isEnabled)
                }
                eqSession.update()
            }
        )
    }

    fun initializeEqBandCapabilities(): EqBandCapabilities {
        val bandCount = mPreferences.getInt(Keys.NUM_BANDS, DEFAULT_BAND_COUNT)
        val zeroedBands = EqualizerSession.getZeroedBandsString(bandCount)
        val bandFrequencies = mPreferences.requireString(Keys.CENTER_FREQUENCIES, zeroedBands)
            .split(DEFAULT_DELIMITER)
            .map { it.toInt() / 1000 }
            .toIntArray()

        val ranges = mPreferences.requireString(Keys.BAND_LEVEL_RANGE, DEFAULT_BAND_RANGE)
            .split(DEFAULT_DELIMITER)
            .map { it.toInt() }
            .toIntArray()

        if (ranges.size == 2 && bandFrequencies.size == bandCount) {
            return EqBandCapabilities(bandCount, ranges[0]..ranges[1], bandFrequencies)
        }

        return EqBandCapabilities(bandCount, -1500..1500, bandFrequencies)
    }

    private fun initializeLoudnessGain(): EqEffectState<Float> {
        return EqEffectState(
            isSupported = isLoudnessEnhancerSupported,
            isEnabled = mPreferences.getBoolean(Keys.LOUDNESS_ENABLED, false),
            value = mPreferences.getFloat(Keys.LOUDNESS_GAIN, MINIMUM_LOUDNESS_GAIN.toFloat()),
            valueMin = MINIMUM_LOUDNESS_GAIN.toFloat(),
            valueMax = MAXIMUM_LOUDNESS_GAIN.toFloat(),
            onCommitEffect = { state ->
                mPreferences.edit(commit = true) {
                    putBoolean(Keys.LOUDNESS_ENABLED, state.isEnabled)
                    putFloat(Keys.LOUDNESS_GAIN, state.value)
                }
                eqSession.update()
            }
        )
    }

    private fun initializeBassBoostState(preset: EQPreset = currentPreset): EqEffectState<Float> {
        return EqEffectState(
            isSupported = isBassBoostSupported,
            isEnabled = preset.hasEffect(EFFECT_TYPE_BASS_BOOST),
            value = preset.getEffect(EFFECT_TYPE_BASS_BOOST),
            valueMin = BASSBOOST_MIN_STRENGTH,
            valueMax = BASSBOOST_MAX_STRENGTH,
            onCommitEffect = { state ->
                setCustomPresetEffect(EFFECT_TYPE_BASS_BOOST, state.value)
            }
        )
    }

    private fun initializeVirtualizerState(preset: EQPreset = currentPreset): EqEffectState<Float> {
        return EqEffectState(
            isSupported = isVirtualizerSupported,
            isEnabled = preset.hasEffect(EFFECT_TYPE_VIRTUALIZER),
            value = preset.getEffect(EFFECT_TYPE_VIRTUALIZER),
            valueMin = VIRTUALIZER_MIN_STRENGTH,
            valueMax = VIRTUALIZER_MAX_STRENGTH,
            onCommitEffect = { state ->
                setCustomPresetEffect(EFFECT_TYPE_VIRTUALIZER, state.value)
            }
        )
    }

    private fun setSessionIsActiveImpl(isActive: Boolean, isCloseSessions: Boolean) {
        this.isSessionActive = isActive
        if (isActive) {
            if (eqState.isEnabled) {
                eqSession.openInternalSession(audioSessionId, closeExternal = true)
            } else {
                eqSession.openExternalSession(audioSessionId, closeInternal = true)
            }
        } else {
            if (isCloseSessions) {
                eqSession.closeExternalSession(audioSessionId)
                eqSession.closeInternalSession(audioSessionId)
            }
        }
    }

    suspend fun resetConfiguration() {
        mPreferences.edit {
            putBoolean(Keys.IS_INITIALIZED, false)
            putBoolean(Keys.GLOBAL_ENABLED, false)
            putBoolean(Keys.LOUDNESS_ENABLED, false)
            remove(Keys.PRESETS)
            remove(Keys.PRESET)
            remove(Keys.CUSTOM_PRESET)
            remove(Keys.BAND_LEVEL_RANGE)
            remove(Keys.LOUDNESS_GAIN)
        }
        initializeEqualizer()
    }

    interface Keys {
        companion object {
            const val GLOBAL_ENABLED = "audiofx.global.enable"
            const val NUM_BANDS = "equalizer.number_of_bands"
            const val IS_INITIALIZED = "equalizer.initialized"
            const val LOUDNESS_ENABLED = "audiofx.eq.loudness.enable"
            const val LOUDNESS_GAIN = "audiofx.eq.loudness.gain"
            const val PRESETS = "audiofx.eq.presets"
            const val PRESET = "audiofx.eq.preset"
            const val CUSTOM_PRESET = "audiofx.eq.preset.custom"
            const val BAND_LEVEL_RANGE = "equalizer.band_level_range"
            const val CENTER_FREQUENCIES = "equalizer.center_frequencies"
        }
    }

    companion object {

        const val PREFERENCES_NAME = "BoomingAudioFX"
        private const val CUSTOM_PRESET_NAME = "Custom"
        private const val DEFAULT_DELIMITER = ";"

        const val EFFECT_TYPE_EQUALIZER = "0bed4300-ddd6-11db-8f34-0002a5d5c51b"
        const val EFFECT_TYPE_BASS_BOOST = "0634f220-ddd4-11db-a0fc-0002a5d5c51b"
        const val EFFECT_TYPE_VIRTUALIZER = "37cc2c00-dddd-11db-8577-0002a5d5c51b"
        const val EFFECT_TYPE_LOUDNESS_ENHANCER = "fe3199be-aed0-413f-87bb-11260eb63cf1"

        const val MINIMUM_LOUDNESS_GAIN: Int = 0
        const val MAXIMUM_LOUDNESS_GAIN: Int = 4000

        const val BASSBOOST_MIN_STRENGTH = 0f
        const val BASSBOOST_MAX_STRENGTH = 1000f

        const val VIRTUALIZER_MIN_STRENGTH = 0f
        const val VIRTUALIZER_MAX_STRENGTH = 1000f

        const val MAX_BANDS = 10
        const val DEFAULT_BAND_COUNT = 5
        const val DEFAULT_BAND_RANGE = "-1500${DEFAULT_DELIMITER}1500"
    }
}