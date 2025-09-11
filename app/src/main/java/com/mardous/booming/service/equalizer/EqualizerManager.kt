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

package com.mardous.booming.service.equalizer

import android.annotation.SuppressLint
import android.content.Context
import android.media.audiofx.AudioEffect
import android.media.audiofx.Equalizer
import androidx.core.content.edit
import com.mardous.booming.core.model.equalizer.*
import com.mardous.booming.core.model.equalizer.EQPreset.Companion.getEmptyPreset
import com.mardous.booming.extensions.files.getFormattedFileName
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.Locale
import java.util.UUID

/**
 * @author Christians M. A. (mardous)
 */
class EqualizerManager internal constructor(context: Context) {

    private val mPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val eqSession = EqualizerSession(context, this)

    private var isEqualizerSupported = false
    private var isVirtualizerSupported = false
    private var isBassBoostSupported = false
    private var isLoudnessEnhancerSupported = false
    private var isPresetReverbSupported = false

    private val _eqStateFlow: MutableStateFlow<EqState>
    private val _bassBoostFlow: MutableStateFlow<EqEffectState<Float>>
    private val _virtualizerFlow: MutableStateFlow<EqEffectState<Float>>
    private val _loudnessGainFlow: MutableStateFlow<EqEffectState<Float>>
    private val _presetReverbFlow: MutableStateFlow<EqEffectState<Int>>
    private val _currentPresetFlow: MutableStateFlow<EQPreset>
    private val _presetsFlow: MutableStateFlow<EqPresetList>

    val eqStateFlow get() = _eqStateFlow
    val bassBoostFlow get() = _bassBoostFlow
    val virtualizerFlow get() = _virtualizerFlow
    val loudnessGainFlow get() = _loudnessGainFlow
    val presetReverbFlow get() = _presetReverbFlow
    val currentPresetFlow get() = _currentPresetFlow
    val presetsFlow get() = _presetsFlow

    val eqState get() = eqStateFlow.value
    val bassBoostState get() = bassBoostFlow.value
    val virtualizerState get() = virtualizerFlow.value
    val loudnessGainState get() = loudnessGainFlow.value
    val presetReverbState get() = presetReverbFlow.value

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
                        //UUID.fromString(EFFECT_TYPE_PRESET_REVERB) -> isPresetReverbSupported = true
                        //We've temporarily disabled PresetReverb because it was not working as expected.
                    }
                }
            }
        } catch (_: NoClassDefFoundError) {
            //The user doesn't have the AudioEffect/AudioEffect.Descriptor class. How sad.
        }

        _eqStateFlow = MutableStateFlow(initializeEqState())
        _presetsFlow = MutableStateFlow(initializePresets())
        _currentPresetFlow = MutableStateFlow(initializeCurrentPreset())
        _bassBoostFlow = MutableStateFlow(initializeBassBoostState())
        _virtualizerFlow = MutableStateFlow(initializeVirtualizerState())
        _loudnessGainFlow = MutableStateFlow(initializeLoudnessGain())
        _presetReverbFlow = MutableStateFlow(initializePresetReverb())
    }

    suspend fun initializeEqualizer() = withContext(IO) {
        if (!isInitialized) {
            val result = runCatching { EffectSet(0) }
            if (result.isSuccess) {
                val temp = result.getOrThrow()
                if (temp.equalizer == null)
                    return@withContext

                setDefaultPresets(temp, temp.equalizer)
                numberOfBands = temp.getNumEqualizerBands().toInt()
                setBandLevelRange(temp.equalizer.getBandLevelRange())
                setCenterFreqs(temp.equalizer)

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
        _presetReverbFlow.emit(initializePresetReverb())
    }

    fun openAudioEffectSession(audioSessionId: Int, internal: Boolean) {
        eqSession.openEqualizerSession(internal, audioSessionId)
    }

    fun closeAudioEffectSession(audioSessionId: Int, internal: Boolean) {
        eqSession.closeEqualizerSessions(internal, audioSessionId)
    }

    fun release() {
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
        val numBands = numberOfBands

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
        val emptyPreset = getEmptyPreset(CUSTOM_PRESET_NAME, true, numberOfBands)
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
        var currentPreset = getCustomPresetFromCurrent()
        currentPreset.setBandLevel(band, level)
        setCustomPreset(currentPreset, fromUser = true)
    }

    /**
     * Copies the current preset to a "Custom" configuration
     * and sets the effect value on it
     */
    private fun setCustomPresetEffect(effect: String, value: Float) {
        var currentPreset = getCustomPresetFromCurrent()
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

    suspend fun setPresetReverb(update: EqEffectUpdate<Int>, apply: Boolean) {
        val newState = update.toState()
        _presetReverbFlow.tryEmit(newState)
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
        presetReverbState.apply()
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
            onCommit = { state ->
                mPreferences.edit(commit = true) {
                    putBoolean(Keys.GLOBAL_ENABLED, state.isEnabled)
                }
                eqSession.update()
            }
        )
    }

    private fun initializeLoudnessGain(): EqEffectState<Float> {
        return EqEffectState(
            isSupported = isLoudnessEnhancerSupported,
            isEnabled = mPreferences.getBoolean(Keys.LOUDNESS_ENABLED, false),
            value = mPreferences.getFloat(
                Keys.LOUDNESS_GAIN,
                OpenSLESConstants.MINIMUM_LOUDNESS_GAIN.toFloat()
            ),
            onCommitEffect = { state ->
                mPreferences.edit(commit = true) {
                    putBoolean(Keys.LOUDNESS_ENABLED, state.isEnabled)
                    putFloat(Keys.LOUDNESS_GAIN, state.value)
                }
                eqSession.update()
            }
        )
    }

    private fun initializePresetReverb(): EqEffectState<Int> {
        return EqEffectState(
            isSupported = isPresetReverbSupported,
            isEnabled = mPreferences.getBoolean(Keys.PRESET_REVERB_ENABLED, false),
            value = mPreferences.getInt(Keys.PRESET_REVERB_PRESET, 0),
            onCommitEffect = { state ->
                mPreferences.edit(commit = true) {
                    putBoolean(Keys.PRESET_REVERB_ENABLED, state.isEnabled)
                    putInt(Keys.PRESET_REVERB_PRESET, state.value)
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
            onCommitEffect = { state ->
                setCustomPresetEffect(EFFECT_TYPE_VIRTUALIZER, state.value)
            }
        )
    }

    var numberOfBands: Int
        get() = mPreferences.getInt(Keys.NUM_BANDS, 5)
        set(numberOfBands) = mPreferences.edit {
            putInt(Keys.NUM_BANDS, numberOfBands)
        }

    val bandLevelRange: IntArray
        get() {
            val bandLevelRange = mPreferences.getString(Keys.BAND_LEVEL_RANGE, null)
            if (bandLevelRange == null || bandLevelRange.trim().isEmpty()) {
                return intArrayOf(-1500, 1500)
            }
            val ranges = bandLevelRange.split(";").toTypedArray()
            val values = IntArray(ranges.size)
            for (i in values.indices) {
                values[i] = ranges[i].toInt()
            }
            return values
        }

    fun setBandLevelRange(bandLevelRange: ShortArray) {
        if (bandLevelRange.size == 2) {
            mPreferences.edit {
                putString(
                    Keys.BAND_LEVEL_RANGE,
                    String.format(Locale.ROOT, "%d;%d", bandLevelRange[0], bandLevelRange[1])
                )
            }
        }
    }

    val centerFreqs: IntArray
        get() = mPreferences.getString(Keys.CENTER_FREQUENCIES, EqualizerSession.getZeroedBandsString(numberOfBands))!!
            .split(DEFAULT_DELIMITER).toTypedArray().let { savedValue ->
                val frequencies = IntArray(savedValue.size)
                for (i in frequencies.indices) {
                    frequencies[i] = savedValue[i].toInt()
                }
                frequencies
            }

    fun setCenterFreqs(equalizer: Equalizer) {
        val numBands = numberOfBands
        val centerFreqs = StringBuilder()
        for (i in 0 until numBands) {
            centerFreqs.append(equalizer.getCenterFreq(i.toShort()))
            if (i < numBands - 1) {
                centerFreqs.append(DEFAULT_DELIMITER)
            }
        }
        mPreferences.edit {
            putString(Keys.CENTER_FREQUENCIES, centerFreqs.toString())
        }
    }

    suspend fun resetConfiguration() {
        mPreferences.edit {
            putBoolean(Keys.IS_INITIALIZED, false)
            putBoolean(Keys.GLOBAL_ENABLED, false)
            putBoolean(Keys.LOUDNESS_ENABLED, false)
            putBoolean(Keys.PRESET_REVERB_ENABLED, false)
            remove(Keys.PRESETS)
            remove(Keys.PRESET)
            remove(Keys.CUSTOM_PRESET)
            remove(Keys.BAND_LEVEL_RANGE)
            remove(Keys.LOUDNESS_GAIN)
            remove(Keys.PRESET_REVERB_PRESET)
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
            const val PRESET_REVERB_ENABLED = "audiofx.eq.presetreverb.enable"
            const val PRESET_REVERB_PRESET = "audiofx.eq.presetreverb.preset"
            const val PRESETS = "audiofx.eq.presets"
            const val PRESET = "audiofx.eq.preset"
            const val CUSTOM_PRESET = "audiofx.eq.preset.custom"
            const val BAND_LEVEL_RANGE = "equalizer.band_level_range"
            const val CENTER_FREQUENCIES = "equalizer.center_frequencies"
        }
    }

    companion object {

        const val EFFECT_TYPE_EQUALIZER = "0bed4300-ddd6-11db-8f34-0002a5d5c51b"
        const val EFFECT_TYPE_BASS_BOOST = "0634f220-ddd4-11db-a0fc-0002a5d5c51b"
        const val EFFECT_TYPE_VIRTUALIZER = "37cc2c00-dddd-11db-8577-0002a5d5c51b"
        const val EFFECT_TYPE_LOUDNESS_ENHANCER = "fe3199be-aed0-413f-87bb-11260eb63cf1"
        const val EFFECT_TYPE_PRESET_REVERB = "47382d60-ddd8-11db-bf3a-0002a5d5c51b"

        /**
         * Max number of EQ bands supported
         */
        const val EQUALIZER_MAX_BANDS = 6

        const val PREFERENCES_NAME = "BoomingAudioFX"
        private const val CUSTOM_PRESET_NAME = "Custom"
        private const val DEFAULT_DELIMITER = ";"
    }
}