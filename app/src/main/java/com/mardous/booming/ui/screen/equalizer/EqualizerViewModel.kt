package com.mardous.booming.ui.screen.equalizer

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mardous.booming.R
import com.mardous.booming.core.model.equalizer.EQPreset
import com.mardous.booming.core.model.equalizer.EqEffectUpdate
import com.mardous.booming.core.model.equalizer.EqUpdate
import com.mardous.booming.data.local.MediaStoreWriter
import com.mardous.booming.extensions.MIME_TYPE_APPLICATION
import com.mardous.booming.extensions.files.getContentUri
import com.mardous.booming.extensions.files.readString
import com.mardous.booming.extensions.resolveActivity
import com.mardous.booming.extensions.showToast
import com.mardous.booming.playback.equalizer.EqualizerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File

class EqualizerViewModel(
    private val contentResolver: ContentResolver,
    private val equalizerManager: EqualizerManager,
    private val mediaStoreWriter: MediaStoreWriter
) : ViewModel() {

    val audioSessionId get() = equalizerManager.audioSessionId

    val eqState get() = equalizerManager.eqStateFlow
    val eqBandCapabilities get() = equalizerManager.bandCapabilitiesFlow
    val currentPreset get() = equalizerManager.currentPresetFlow
    val bassBoost get() = equalizerManager.bassBoostFlow
    val virtualizer get() = equalizerManager.virtualizerFlow
    val loudnessGain get() = equalizerManager.loudnessGainFlow
    val presets get() = equalizerManager.presetsFlow.map {
        equalizerManager.getEqualizerPresetsWithCustom(it.list)
    }
    val eqBands = combine(eqBandCapabilities, currentPreset) { bandCapabilities, preset ->
        bandCapabilities.getBands(preset)
    }

    private val _exportRequestEvent = Channel<ExportRequestResult>(Channel.BUFFERED)
    val exportRequestEvent: Flow<ExportRequestResult> = _exportRequestEvent.receiveAsFlow()

    private val _exportResultEvent = Channel<PresetExportResult>(Channel.BUFFERED)
    val exportResultEvent: Flow<PresetExportResult> = _exportResultEvent.receiveAsFlow()

    private val _importRequestEvent = Channel<ImportRequestResult>(Channel.BUFFERED)
    val importRequestEvent: Flow<ImportRequestResult> = _importRequestEvent.receiveAsFlow()

    private val _importResultEvent = Channel<PresetImportResult>(Channel.BUFFERED)
    val importResultEvent: Flow<PresetImportResult> = _importResultEvent.receiveAsFlow()

    private val _saveResultEvent = Channel<PresetOpResult>(Channel.BUFFERED)
    val saveResultEvent: Flow<PresetOpResult> = _saveResultEvent.receiveAsFlow()

    private val _renameResultEvent = Channel<PresetOpResult>(Channel.BUFFERED)
    val renameResultEvent: Flow<PresetOpResult> = _renameResultEvent.receiveAsFlow()

    private val _deleteResultEvent = Channel<PresetOpResult>(Channel.BUFFERED)
    val deleteResultEvent: Flow<PresetOpResult> = _deleteResultEvent.receiveAsFlow()

    fun setEqualizerState(isEnabled: Boolean, apply: Boolean = true) {
        // set parameter and state
        viewModelScope.launch(Dispatchers.Default) {
            equalizerManager.setEqualizerState(EqUpdate(eqState.value, isEnabled), apply)
        }
    }

    fun setLoudnessGain(
        isEnabled: Boolean = loudnessGain.value.isUsable,
        value: Float = loudnessGain.value.value,
        apply: Boolean = true
    ) = viewModelScope.launch(Dispatchers.Default) {
        equalizerManager.setLoudnessGain(EqEffectUpdate(loudnessGain.value, isEnabled, value), apply)
    }

    fun setBassBoost(
        isEnabled: Boolean = bassBoost.value.isUsable,
        value: Float = bassBoost.value.value,
        apply: Boolean = true
    ) = viewModelScope.launch(Dispatchers.Default) {
        equalizerManager.setBassBoost(EqEffectUpdate(bassBoost.value, isEnabled, value), apply)
    }

    fun setVirtualizer(
        isEnabled: Boolean = virtualizer.value.isUsable,
        value: Float = virtualizer.value.value,
        apply: Boolean = true
    ) = viewModelScope.launch(Dispatchers.Default) {
        equalizerManager.setVirtualizer(EqEffectUpdate(virtualizer.value, isEnabled, value), apply)
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

    fun savePreset(
        presetName: String?,
        canReplace: Boolean
    ) = viewModelScope.launch(Dispatchers.IO) {
        val result = if (presetName.isNullOrBlank()) {
            PresetOpResult(false, R.string.preset_name_is_empty, canDismiss = false)
        } else {
            if (!canReplace && !equalizerManager.isPresetNameAvailable(presetName)) {
                PresetOpResult(false, R.string.that_name_is_already_in_use, canDismiss = false)
            } else {
                val newPreset = equalizerManager.getNewPresetFromCustom(presetName)
                if (equalizerManager.addPreset(newPreset, canReplace, usePreset = true)) {
                    PresetOpResult(true, R.string.preset_saved_successfully)
                } else {
                    PresetOpResult(false, R.string.could_not_save_preset)
                }
            }
        }
        _saveResultEvent.send(result)
    }

    fun renamePreset(preset: EQPreset, newName: String?) = viewModelScope.launch(Dispatchers.IO) {
        val result = if (newName.isNullOrBlank()) {
            PresetOpResult(false, canDismiss = false)
        } else {
            if (equalizerManager.renamePreset(preset, newName)) {
                PresetOpResult(true, R.string.preset_renamed)
            } else {
                PresetOpResult(false, R.string.preset_not_renamed)
            }
        }
        _renameResultEvent.send(result)
    }

    fun deletePreset(preset: EQPreset) = viewModelScope.launch(Dispatchers.IO) {
        _deleteResultEvent.send(PresetOpResult(equalizerManager.removePreset(preset)))
    }

    fun generateExportData(presets: List<EQPreset>) = viewModelScope.launch(Dispatchers.IO) {
        val exportName = equalizerManager.getNewExportName()
        val exportContent = runCatching { Json.encodeToString(presets) }.getOrNull()
        val result = if (exportName.isNotEmpty() && !exportContent.isNullOrEmpty()) {
            ExportRequestResult(success = true, presetExportData = Pair(exportName, exportContent))
        } else {
            ExportRequestResult(false)
        }
        _exportRequestEvent.send(result)
    }

    fun exportConfiguration(data: Uri?, content: String?) = viewModelScope.launch(Dispatchers.IO) {
        val result = if (data == null || content.isNullOrEmpty()) {
            PresetExportResult(false)
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
                PresetExportResult(false, R.string.could_not_export_configuration)
            } else {
                PresetExportResult(
                    success = true,
                    messageRes = R.string.configuration_exported_successfully,
                    data = data,
                    mimeType = MIME_TYPE_APPLICATION
                )
            }
        }
        _exportResultEvent.send(result)
    }

    fun requestImport(data: Uri?) = viewModelScope.launch(Dispatchers.IO) {
        val result = if (data == null || data.path?.endsWith(".json") == false) {
            ImportRequestResult(false, R.string.there_is_nothing_to_import)
        } else {
            val parseResult = runCatching {
                contentResolver.openInputStream(data)?.use { stream ->
                    Json.decodeFromString<List<EQPreset>>(stream.readString())
                }
            }
            val presets = parseResult.getOrNull()
            if (parseResult.isFailure || presets == null) {
                ImportRequestResult(false, R.string.there_is_nothing_to_import)
            } else {
                ImportRequestResult(
                    true,
                    presets = presets
                )
            }
        }
        _importRequestEvent.send(result)
    }

    fun importPresets(presets: List<EQPreset>) = viewModelScope.launch(Dispatchers.IO) {
        val result = if (presets.isNotEmpty()) {
            PresetImportResult(true, imported = equalizerManager.importPresets(presets))
        } else {
            PresetImportResult(false, R.string.no_preset_imported)
        }
        _importResultEvent.send(result)
    }

    fun sharePresets(
        context: Context,
        presets: List<EQPreset>
    ) = viewModelScope.launch(Dispatchers.IO) {
        val result = if (presets.isNotEmpty()) {
            val cacheDir = context.externalCacheDir
            if (cacheDir == null || (!cacheDir.exists() && !cacheDir.mkdirs())) {
                PresetExportResult(false, R.string.could_not_create_configurations_file)
            } else {
                val name = equalizerManager.getNewExportName()
                val result = runCatching {
                    File(cacheDir, name)
                        .also { it.writeText(Json.encodeToString(presets)) }
                        .getContentUri(context)
                }
                if (result.isSuccess) {
                    PresetExportResult(
                        success = true,
                        isShareRequest = true,
                        data = result.getOrThrow(),
                        mimeType = MIME_TYPE_APPLICATION
                    )
                } else {
                    PresetExportResult(
                        success = false,
                        isShareRequest = true,
                        messageRes = R.string.could_not_create_configurations_file
                    )
                }
            }
        } else {
            PresetExportResult(false)
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
        val sessionId = this.audioSessionId
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