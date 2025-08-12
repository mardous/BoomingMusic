package com.mardous.booming.ui.screen.equalizer

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.mardous.booming.R
import com.mardous.booming.extensions.MIME_TYPE_APPLICATION
import com.mardous.booming.extensions.files.getContentUri
import com.mardous.booming.extensions.files.readString
import com.mardous.booming.core.model.equalizer.EQPreset
import com.mardous.booming.data.local.MediaStoreWriter
import com.mardous.booming.core.model.equalizer.EqEffectUpdate
import com.mardous.booming.core.model.equalizer.EqUpdate
import com.mardous.booming.service.equalizer.EqualizerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File

class EqualizerViewModel(
    private val contentResolver: ContentResolver,
    private val equalizerManager: EqualizerManager,
    private val mediaStoreWriter: MediaStoreWriter,
    private val audioSessionId: Int
) : ViewModel() {

    val eqStateFlow get() = equalizerManager.eqStateFlow
    val currentPresetFlow get() = equalizerManager.currentPresetFlow
    val bassBoostFlow get() = equalizerManager.bassBoostFlow
    val virtualizerFlow get() = equalizerManager.virtualizerFlow
    val loudnessGainFlow get() = equalizerManager.loudnessGainFlow
    val presetReverbFlow get() = equalizerManager.presetReverbFlow
    val presetsFlow get() = equalizerManager.presetsFlow

    val eqState get() = equalizerManager.eqState
    val bassBoostState get() = equalizerManager.bassBoostState
    val virtualizerState get() = equalizerManager.virtualizerState
    val loudnessGainState get() = equalizerManager.loudnessGainState
    val presetReverbState get() = equalizerManager.presetReverbState

    val numberOfBands: Int get() = equalizerManager.numberOfBands
    val bandLevelRange: IntArray get() = equalizerManager.bandLevelRange
    val centerFreqs: IntArray get() = equalizerManager.centerFreqs

    private var exportContent: String? = null

    fun isCustomPresetSelected() = equalizerManager.currentPreset.isCustom

    fun setEqualizerState(isEnabled: Boolean, apply: Boolean = true) {
        // update equalizer session
        equalizerManager.closeAudioEffectSession(audioSessionId, !isEnabled)
        equalizerManager.openAudioEffectSession(audioSessionId, isEnabled)

        // set parameter and state
        viewModelScope.launch(Dispatchers.Default) {
            equalizerManager.setEqualizerState(EqUpdate(eqState, isEnabled), apply)
        }
    }

    fun setLoudnessGain(
        isEnabled: Boolean = loudnessGainState.isUsable,
        value: Float = loudnessGainState.value,
        apply: Boolean = true
    ) = viewModelScope.launch(Dispatchers.Default) {
        equalizerManager.setLoudnessGain(EqEffectUpdate(loudnessGainState, isEnabled, value), apply)
    }

    fun setPresetReverb(
        isEnabled: Boolean = presetReverbState.isUsable,
        value: Int = presetReverbState.value,
        apply: Boolean = true
    ) = viewModelScope.launch(Dispatchers.Default) {
        equalizerManager.setPresetReverb(EqEffectUpdate(presetReverbState, isEnabled, value), apply)
    }

    fun setBassBoost(
        isEnabled: Boolean = bassBoostState.isUsable,
        value: Float = bassBoostState.value,
        apply: Boolean = true
    ) = viewModelScope.launch(Dispatchers.Default) {
        equalizerManager.setBassBoost(EqEffectUpdate(bassBoostState, isEnabled, value), apply)
    }

    fun setVirtualizer(
        isEnabled: Boolean = virtualizerState.isUsable,
        value: Float = virtualizerState.value,
        apply: Boolean = true
    ) = viewModelScope.launch(Dispatchers.Default) {
        equalizerManager.setVirtualizer(EqEffectUpdate(virtualizerState, isEnabled, value), apply)
    }

    fun setEqualizerPreset(eqPreset: EQPreset) = viewModelScope.launch(Dispatchers.IO) {
        equalizerManager.setCurrentPreset(eqPreset)
    }

    fun setCustomPresetBandLevel(band: Int, level: Int) = viewModelScope.launch(Dispatchers.Default) {
        equalizerManager.setCustomPresetBandLevel(band, level)
    }

    fun applyPendingStates() = viewModelScope.launch(Dispatchers.IO) {
        equalizerManager.applyPendingStates()
    }

    fun getEqualizerPresetsWithCustom(presets: List<EQPreset>) =
        equalizerManager.getEqualizerPresetsWithCustom(presets)

    fun savePreset(presetName: String?, canReplace: Boolean): LiveData<PresetOpResult> =
        liveData(Dispatchers.IO) {
            if (presetName.isNullOrBlank()) {
                emit(PresetOpResult(false, R.string.preset_name_is_empty, canDismiss = false))
            } else {
                if (!canReplace && !equalizerManager.isPresetNameAvailable(presetName)) {
                    emit(
                        PresetOpResult(
                            false,
                            R.string.that_name_is_already_in_use,
                            canDismiss = false
                        )
                    )
                } else {
                    val newPreset = equalizerManager.getNewPresetFromCustom(presetName)
                    if (equalizerManager.addPreset(newPreset, canReplace, usePreset = true)) {
                        emit(PresetOpResult(true, R.string.preset_saved_successfully))
                    } else {
                        emit(PresetOpResult(false, R.string.could_not_save_preset))
                    }
                }
            }
        }

    fun renamePreset(preset: EQPreset, newName: String?): LiveData<PresetOpResult> =
        liveData(Dispatchers.IO) {
            if (newName.isNullOrBlank()) {
                emit(PresetOpResult(false, canDismiss = false))
            } else {
                if (equalizerManager.renamePreset(preset, newName)) {
                    emit(PresetOpResult(true, R.string.preset_renamed))
                } else {
                    emit(PresetOpResult(false, R.string.preset_not_renamed))
                }
            }
        }

    fun deletePreset(preset: EQPreset): LiveData<PresetOpResult> = liveData(Dispatchers.IO) {
        emit(PresetOpResult(equalizerManager.removePreset(preset)))
    }

    fun requestExport(): LiveData<ExportRequestResult> = liveData(Dispatchers.IO) {
        val presets = equalizerManager.equalizerPresets
        val names = presets.map { it.name }
        if (names.isEmpty()) {
            emit(ExportRequestResult(false, R.string.there_is_nothing_to_export))
        } else {
            emit(ExportRequestResult(true, presets = presets, presetNames = names))
        }
    }

    fun generateExportData(presets: List<EQPreset>): LiveData<String> = liveData(Dispatchers.IO) {
        exportContent = Json.Default.encodeToString(presets)
        emit(equalizerManager.getNewExportName())
    }

    fun exportConfiguration(data: Uri?): LiveData<PresetExportResult> = liveData(Dispatchers.IO) {
        if (data == null || exportContent.isNullOrEmpty()) {
            emit(PresetExportResult(false))
        } else {
            val result = runCatching {
                mediaStoreWriter.toContentResolver(null, data) { stream ->
                    when {
                        !exportContent.isNullOrEmpty() -> {
                            stream.bufferedWriter().use { it.write(exportContent) }
                            exportContent = null
                            true
                        }

                        else -> false
                    }
                }
            }

            if (result.isFailure || result.getOrThrow().resultCode == MediaStoreWriter.Result.Code.ERROR) {
                emit(PresetExportResult(false, R.string.could_not_export_configuration))
            } else {
                emit(
                    PresetExportResult(
                        true,
                        R.string.configuration_exported_successfully,
                        data,
                        MIME_TYPE_APPLICATION
                    )
                )
            }
        }
    }

    fun requestImport(data: Uri?): LiveData<ImportRequestResult> = liveData(Dispatchers.IO) {
        if (data == null || data.path?.endsWith(".json") == false) {
            emit(ImportRequestResult(false, R.string.there_is_nothing_to_import))
        } else {
            val result = runCatching {
                contentResolver.openInputStream(data)?.use { stream ->
                    Json.Default.decodeFromString<List<EQPreset>>(stream.readString())
                }
            }
            val presets = result.getOrNull()
            if (result.isFailure || presets == null) {
                emit(ImportRequestResult(false, R.string.there_is_nothing_to_import))
            } else {
                emit(
                    ImportRequestResult(
                        true,
                        presets = presets,
                        presetNames = presets.map { it.name })
                )
            }
        }
    }

    fun importPresets(presets: List<EQPreset>): LiveData<PresetImportResult> =
        liveData(Dispatchers.IO) {
            if (presets.isNotEmpty()) {
                emit(PresetImportResult(true, imported = equalizerManager.importPresets(presets)))
            } else {
                emit(PresetImportResult(false, R.string.no_preset_imported))
            }
        }

    fun sharePresets(context: Context, presets: List<EQPreset>): LiveData<PresetExportResult> =
        liveData(Dispatchers.IO) {
            if (presets.isNotEmpty()) {
                val cacheDir = context.externalCacheDir
                if (cacheDir == null || (!cacheDir.exists() && !cacheDir.mkdirs())) {
                    emit(PresetExportResult(false, R.string.could_not_create_configurations_file))
                } else {
                    val name = equalizerManager.getNewExportName()
                    val result = runCatching {
                        File(cacheDir, name).also {
                            it.writeText(Json.Default.encodeToString(presets))
                        }.getContentUri(context)
                    }
                    if (result.isSuccess) {
                        emit(
                            PresetExportResult(
                                true,
                                data = result.getOrThrow(),
                                mimeType = MIME_TYPE_APPLICATION
                            )
                        )
                    } else {
                        emit(
                            PresetExportResult(
                                false,
                                R.string.could_not_create_configurations_file
                            )
                        )
                    }
                }
            } else {
                emit(PresetExportResult(false))
            }
        }

    fun resetEqualizer() = viewModelScope.launch(Dispatchers.IO) {
        equalizerManager.resetConfiguration()
    }
}