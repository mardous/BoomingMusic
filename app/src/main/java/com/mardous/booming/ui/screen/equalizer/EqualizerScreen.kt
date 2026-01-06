@file:SuppressLint("LocalContextGetResourceValueCall")

package com.mardous.booming.ui.screen.equalizer

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ShareCompat
import com.mardous.booming.R
import com.mardous.booming.core.model.LibraryMargin
import com.mardous.booming.core.model.equalizer.EQPreset
import com.mardous.booming.core.model.equalizer.EqBand
import com.mardous.booming.extensions.MIME_TYPE_APPLICATION
import com.mardous.booming.extensions.showToast
import com.mardous.booming.ui.component.compose.*
import com.mardous.booming.ui.screen.library.LibraryViewModel
import kotlinx.coroutines.launch
import java.util.Locale

private const val PRESET_NAME_MAX_LENGTH = 32

@Composable
fun EqualizerScreen(
    libraryViewModel: LibraryViewModel,
    eqViewModel: EqualizerViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current

    fun shareConfiguration(data: Uri, mimeType: String) {
        val builder = ShareCompat.IntentBuilder(context)
            .setChooserTitle(R.string.share_eq_configuration)
            .setStream(data)
            .setType(mimeType)
        try {
            builder.startChooser()
        } catch (_: ActivityNotFoundException) {
        }
    }

    val miniPlayerMargin by libraryViewModel.getMiniPlayerMargin().observeAsState(LibraryMargin(0))

    var expandedMenu by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val eqState by eqViewModel.eqState.collectAsState()
    val eqCurrentPreset by eqViewModel.currentPreset.collectAsState()
    val eqPresets by eqViewModel.presets.collectAsState(emptyList())
    val eqBands by eqViewModel.eqBands.collectAsState(emptyList())

    val virtualizer by eqViewModel.virtualizer.collectAsState()
    val bassBoost by eqViewModel.bassBoost.collectAsState()
    val loudnessGain by eqViewModel.loudnessGain.collectAsState()

    var editPresetState by remember { mutableStateOf<Pair<EQPreset, Boolean>?>(null) }
    var deletePresetState by remember { mutableStateOf<Pair<EQPreset, Boolean>?>(null) }

    var sharePresetState by remember { mutableStateOf<Pair<Uri, String>?>(null) }
    sharePresetState?.let { (data, mimeType) ->
        coroutineScope.launch {
            val result = snackbarHostState.showSnackbar(
                message = context.getString(R.string.configuration_exported_successfully),
                actionLabel = context.getString(R.string.action_share),
                duration = SnackbarDuration.Long
            )
            when (result) {
                SnackbarResult.Dismissed -> sharePresetState = null
                SnackbarResult.ActionPerformed -> {
                    shareConfiguration(data, mimeType)
                    sharePresetState = null
                }
            }
        }
    }

    var showSharePresetDialog by remember { mutableStateOf(false) }
    var showPresetSaverDialog by remember { mutableStateOf(false) }
    var showPresetSelectorDialog by remember { mutableStateOf(false) }
    var showResetEqDialog by remember { mutableStateOf(false) }

    var showImportDialog by remember { mutableStateOf(false) }
    var presetsToImport by remember { mutableStateOf<List<EQPreset>>(emptyList()) }
    val importPresetLauncher = rememberLauncherForActivityResult(OpenDocument()) { data: Uri? ->
        eqViewModel.requestImport(data)
    }

    val importRequestEvent by eqViewModel.importRequestEvent.collectAsState(null)
    LaunchedEffect(importRequestEvent) {
        importRequestEvent?.let {
            if (it.success) {
                presetsToImport = it.presets
                showImportDialog = true
            } else {
                context.showToast(it.messageRes)
            }
        }
    }

    val importResultEvent by eqViewModel.importResultEvent.collectAsState(null)
    LaunchedEffect(importResultEvent) {
        importResultEvent?.let {
            if (it.success && it.imported > 0) {
                context.showToast(context.getString(R.string.imported_x_presets, it.imported))
            } else {
                context.showToast(it.messageRes)
            }
        }
    }

    if (showImportDialog && presetsToImport.isNotEmpty()) {
        PresetCheckDialog(
            icon = painterResource(R.drawable.ic_file_save_24dp),
            title = stringResource(R.string.import_configuration),
            message = stringResource(R.string.select_configurations_to_import),
            confirmButton = stringResource(R.string.import_action),
            presets = presetsToImport,
            onDismiss = { showImportDialog = false },
            onConfirm = { selectedPresets ->
                eqViewModel.importPresets(selectedPresets)
                showImportDialog = false
            }
        )
    }

    var showExportDialog by remember { mutableStateOf(false) }
    var exportableContent by remember { mutableStateOf<String?>(null) }
    val exportPresetLauncher =
        rememberLauncherForActivityResult(CreateDocument(MIME_TYPE_APPLICATION)) { data: Uri? ->
            eqViewModel.exportConfiguration(data, exportableContent)
        }

    val exportRequestEvent by eqViewModel.exportRequestEvent.collectAsState(null)
    LaunchedEffect(exportRequestEvent) {
        exportRequestEvent?.let {
            if (it.success && it.presetExportData != null) {
                exportableContent = it.presetExportData.second
                try {
                    exportPresetLauncher.launch(it.presetExportData.first)
                    context.showToast(R.string.select_a_file_to_save_exported_configurations)
                } catch (_: ActivityNotFoundException) {
                    exportableContent = null
                    context.showToast("File picker not found")
                }
            } else {
                context.showToast(it.messageRes)
            }
        }
    }

    val exportResultEvent by eqViewModel.exportResultEvent.collectAsState(null)
    LaunchedEffect(exportResultEvent) {
        exportResultEvent?.let {
            if (it.success && it.data != null && it.mimeType != null) {
                if (it.isShareRequest) {
                    shareConfiguration(it.data, it.mimeType)
                } else {
                    sharePresetState = Pair(it.data, it.mimeType)
                }
            } else {
                context.showToast(it.messageRes)
            }
        }
    }

    if (showExportDialog && eqPresets.isNotEmpty()) {
        PresetCheckDialog(
            icon = painterResource(R.drawable.ic_file_export_24dp),
            title = stringResource(R.string.export_configuration),
            message = stringResource(R.string.select_configurations_to_export),
            confirmButton = stringResource(android.R.string.ok),
            presets = eqPresets,
            onDismiss = { showExportDialog = false },
            onConfirm = { selectedPresets ->
                eqViewModel.generateExportData(selectedPresets)
                showExportDialog = false
            }
        )
    }

    if (showSharePresetDialog && eqPresets.isNotEmpty()) {
        PresetCheckDialog(
            icon = painterResource(R.drawable.ic_share_24dp),
            title = stringResource(R.string.share_configuration),
            message = stringResource(R.string.select_configurations_to_share),
            confirmButton = stringResource(R.string.action_share),
            presets = eqPresets,
            onDismiss = { showSharePresetDialog = false },
            onConfirm = { selectedPresets ->
                eqViewModel.sharePresets(context, selectedPresets)
                showSharePresetDialog = false
            }
        )
    }

    val saveResultEvent by eqViewModel.saveResultEvent.collectAsState(null)
    LaunchedEffect(saveResultEvent) {
        saveResultEvent?.let {
            context.showToast(it.messageRes)
            if (it.canDismiss) {
                showPresetSaverDialog = false
            }
        }
    }

    if (showPresetSaverDialog) {
        InputDialog(
            icon = painterResource(R.drawable.ic_save_24dp),
            title = stringResource(R.string.save_preset),
            message = stringResource(R.string.please_enter_a_name_for_this_preset),
            inputHint = stringResource(R.string.preset_name),
            inputMaxLength = PRESET_NAME_MAX_LENGTH,
            checkBoxPrompt = stringResource(R.string.replace_preset_with_same_name),
            confirmButton = stringResource(R.string.action_save),
            onConfirm = { presetName, canReplace ->
                eqViewModel.savePreset(
                    presetName,
                    canReplace
                )
            },
            onDismiss = { showPresetSaverDialog = false }
        )
    }

    val renameResultEvent by eqViewModel.renameResultEvent.collectAsState(null)
    LaunchedEffect(renameResultEvent) {
        renameResultEvent?.let {
            context.showToast(it.messageRes)
            if (it.canDismiss) {
                editPresetState = null
            }
        }
    }

    editPresetState?.let { (targetPreset, showDialog) ->
        if (showDialog) {
            InputDialog(
                icon = painterResource(R.drawable.ic_edit_24dp),
                title = stringResource(R.string.rename_preset),
                message = stringResource(R.string.please_enter_a_new_name_for_this_preset),
                inputHint = stringResource(R.string.preset_name),
                inputPrefill = targetPreset.name,
                inputMaxLength = PRESET_NAME_MAX_LENGTH,
                confirmButton = stringResource(R.string.rename_action),
                onConfirm = { input, _ -> eqViewModel.renamePreset(targetPreset, input) },
                onDismiss = { editPresetState = null }
            )
        }
    }

    val deleteResultEvent by eqViewModel.deleteResultEvent.collectAsState(null)
    LaunchedEffect(deleteResultEvent) {
        deleteResultEvent?.let {
            if (it.success && deletePresetState != null) {
                context.showToast(
                    context.getString(R.string.preset_x_deleted, deletePresetState!!.first.name)
                )
            }
            if (it.canDismiss) {
                deletePresetState = null
            }
        }
    }

    deletePresetState?.let { (targetPreset, showDialog) ->
        if (showDialog) {
            ConfirmDialog(
                icon = painterResource(R.drawable.ic_delete_24dp),
                title = stringResource(R.string.delete_preset),
                message = stringResource(
                    R.string.are_you_sure_you_want_to_delete_preset_x,
                    targetPreset.name
                ),
                confirmButton = stringResource(R.string.action_delete),
                dismissButton = stringResource(R.string.no),
                onConfirm = { eqViewModel.deletePreset(targetPreset) },
                onDismiss = { deletePresetState = null }
            )
        }
    }

    if (showPresetSelectorDialog) {
        PresetSelectorDialog(
            presets = eqPresets,
            selectedPreset = eqCurrentPreset,
            onSelect = { preset ->
                eqViewModel.setEqualizerPreset(preset)
                showPresetSelectorDialog = false
            },
            onEdit = { preset -> editPresetState = Pair(preset, true) },
            onDelete = { preset -> deletePresetState = Pair(preset, true) },
            onDismiss = { showPresetSelectorDialog = false }
        )
    }

    if (showResetEqDialog) {
        ConfirmDialog(
            icon = painterResource(R.drawable.ic_restart_alt_24dp),
            title = stringResource(R.string.reset_equalizer),
            message = stringResource(R.string.are_you_sure_you_want_to_reset_the_equalizer),
            confirmButton = stringResource(R.string.reset_action),
            onConfirm = {
                eqViewModel.resetEqualizer()
                showResetEqDialog = false
            },
            onDismiss = { showResetEqDialog = false }
        )
    }

    CollapsibleAppBarScaffold(
        title = stringResource(R.string.equalizer_label),
        actions = {
            if (eqViewModel.hasSystemEqualizer(context)) {
                IconButton(onClick = { eqViewModel.openSystemEqualizer(context) }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_equalizer_24dp),
                        contentDescription = stringResource(R.string.action_external_eq)
                    )
                }
            }

            IconButton(onClick = { expandedMenu = !expandedMenu }) {
                Icon(
                    painter = painterResource(R.drawable.ic_more_vert_24dp),
                    contentDescription = stringResource(R.string.action_more)
                )
            }

            DropdownMenu(
                expanded = expandedMenu,
                onDismissRequest = { expandedMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.share_configuration)) },
                    enabled = eqState.isSupported && eqPresets.isNotEmpty(),
                    onClick = {
                        showSharePresetDialog = true
                        expandedMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.export_configuration)) },
                    enabled = eqState.isSupported && eqPresets.isNotEmpty(),
                    onClick = {
                        showExportDialog = true
                        expandedMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.import_configuration)) },
                    enabled = eqState.isSupported,
                    onClick = {
                        try {
                            importPresetLauncher.launch(arrayOf(MIME_TYPE_APPLICATION))
                            context.showToast(R.string.select_a_file_containing_booming_eq_presets)
                        } catch (_: ActivityNotFoundException) {
                            context.showToast("File picker not found")
                        }
                        expandedMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.reset_equalizer)) },
                    enabled = eqState.isSupported,
                    onClick = {
                        showResetEqDialog = true
                        expandedMenu = false
                    }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        miniPlayerMargin = miniPlayerMargin.totalMargin,
        onBackClick = onBackClick
    ) { contentPadding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding)
                .padding(16.dp)
        ) {
            MaterialSwitch(
                title = stringResource(R.string.enable_equalizer),
                subtitle = if (eqState.isDisabledByAudioOffload) {
                    stringResource(R.string.audio_offload_is_enabled)
                } else null,
                isChecked = eqState.isUsable,
                enabled = eqState.isSupported,
                onClick = {
                    eqViewModel.setEqualizerState(isEnabled = eqState.isEnabled.not())
                }
            )

            TitledSurface(
                title = stringResource(R.string.preset_title),
                iconRes = R.drawable.ic_equalizer_24dp
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .weight(1f)
                ) {
                    val containerColor = if (eqState.isUsable) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    }
                    val contentColor = contentColorFor(containerColor)

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(50))
                            .background(containerColor)
                            .clickable(onClick = { showPresetSelectorDialog = true })
                            .padding(start = 16.dp, end = 8.dp)
                            .padding(vertical = 8.dp)
                            .weight(1f)
                    ) {
                        Text(
                            text = eqCurrentPreset.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = contentColor,
                            modifier = Modifier.weight(1f)
                        )

                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = CircleShape,
                            color = contentColor.copy(alpha = .1f),
                            contentColor = contentColor
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_arrow_drop_down_24dp),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    FilledIconButton(
                        enabled = eqState.isUsable && eqCurrentPreset.isCustom,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        ),
                        onClick = { showPresetSaverDialog = true },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_save_24dp),
                            contentDescription = stringResource(R.string.save_preset),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            TitledSurface(
                title = stringResource(R.string.manual_adjustment_title),
                iconRes = R.drawable.ic_tune_24dp
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) {
                    eqBands.forEach { band ->
                        EQBandSlider(
                            enabled = eqState.isUsable,
                            band = band,
                            onValueChange = { newBandLevel ->
                                eqViewModel.setCustomPresetBandLevel(band.index, newBandLevel)
                            }
                        )
                    }
                }
            }

            TitledSurface(
                title = stringResource(R.string.virtualizer_label),
                iconRes = R.drawable.ic_headphones_24dp,
                collapsible = false,
                titleEndContent = {
                    TitleShapedText(
                        text = "${((virtualizer.value * 100) / virtualizer.valueMax).toInt()}%"
                    )
                }
            ) {
                Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Slider(
                        steps = 10,
                        value = virtualizer.value,
                        onValueChange = {
                            eqViewModel.setVirtualizer(
                                isEnabled = it > 0f,
                                value = it,
                                apply = false
                            )
                        },
                        onValueChangeFinished = {
                            eqViewModel.applyPendingStates()
                        },
                        valueRange = virtualizer.valueMin..virtualizer.valueMax,
                        enabled = eqState.isUsable && virtualizer.isSupported,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // --- BassBoost Effect ---
            TitledSurface(
                title = stringResource(R.string.bassboost_label),
                iconRes = R.drawable.ic_edit_audio_24dp,
                collapsible = false,
                titleEndContent = {
                    TitleShapedText(
                        text = "${((bassBoost.value * 100) / bassBoost.valueMax).toInt()}%"
                    )
                }
            ) {
                Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Slider(
                        steps = 10,
                        value = bassBoost.value,
                        onValueChange = {
                            eqViewModel.setBassBoost(isEnabled = it > 0f, value = it, apply = false)
                        },
                        onValueChangeFinished = {
                            eqViewModel.applyPendingStates()
                        },
                        valueRange = bassBoost.valueMin..bassBoost.valueMax,
                        enabled = eqState.isUsable && bassBoost.isSupported,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // --- Loudness Enhancer (Amplifier) ---
            TitledSurface(
                title = stringResource(R.string.loudness_enhancer),
                iconRes = R.drawable.ic_volume_up_24dp,
                collapsible = false,
                titleEndContent = {
                    TitleShapedText(
                        text = "%.0f mDb".format(Locale.ROOT, loudnessGain.value)
                    )
                }
            ) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Slider(
                        value = loudnessGain.value,
                        onValueChange = {
                            eqViewModel.setLoudnessGain(value = it, apply = false)
                        },
                        onValueChangeFinished = {
                            eqViewModel.applyPendingStates()
                        },
                        valueRange = loudnessGain.valueMin..loudnessGain.valueMax,
                        enabled = eqState.isUsable && loudnessGain.isSupported,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun EQBandSlider(
    enabled: Boolean,
    band: EqBand,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(12.dp)
    ) {
        Slider(
            value = band.value,
            onValueChange = { onValueChange(band.getActualLevel(it)) },
            valueRange = band.valueRange,
            enabled = enabled,
            modifier = Modifier.weight(1f)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.tertiaryContainer)
                .clickable(
                    enabled = enabled,
                    onClick = { onValueChange(band.getActualLevel(0f)) }
                )
                .padding(2.dp)
        ) {
            Text(
                text = band.readableFrequency,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                fontSize = 10.sp,
                maxLines = 1
            )

            Text(
                text = band.readableLevel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PresetSelectorDialog(
    presets: List<EQPreset>,
    selectedPreset: EQPreset,
    onSelect: (EQPreset) -> Unit,
    onEdit: (EQPreset) -> Unit,
    onDelete: (EQPreset) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Text(
                text = stringResource(R.string.select_preset),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(24.dp))

            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(presets) { preset ->
                    EqualizerPresetItem(
                        presetName = preset.name,
                        isCurrentPreset = preset == selectedPreset,
                        isCustomPreset = preset.isCustom,
                        onClick = { onSelect(preset) },
                        onEditClick = { onEdit(preset) },
                        onDeleteClick = { onDelete(preset) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PresetCheckDialog(
    icon: Painter,
    title: String,
    message: String,
    confirmButton: String,
    presets: List<EQPreset>,
    onDismiss: () -> Unit,
    onConfirm: (List<EQPreset>) -> Unit
) {
    val selectedPresets = remember { mutableStateListOf<EQPreset>().apply { addAll(presets) } }

    AlertDialog(
        icon = {
            Icon(
                painter = icon,
                contentDescription = null
            )
        },
        title = { Text(title) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(presets) { preset ->
                        if (preset.isCustom.not()) {
                            val isChecked = selectedPresets.contains(preset)
                            DialogListItemWithCheckBox(
                                title = preset.name,
                                onClick = {
                                    if (isChecked) {
                                        selectedPresets.remove(preset)
                                    } else {
                                        selectedPresets.add(preset)
                                    }
                                },
                                isSelected = isChecked
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedPresets) },
                enabled = selectedPresets.isNotEmpty()
            ) {
                Text(confirmButton)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
        onDismissRequest = onDismiss
    )
}

@Composable
private fun EqualizerPresetItem(
    presetName: String,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
    isCurrentPreset: Boolean = false,
    isCustomPreset: Boolean = false,
) {
    DialogListItemSurface(
        onClick = onClick,
        isSelected = isCurrentPreset,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .padding(vertical = 8.dp)
                .padding(start = 24.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = presetName,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            if (isCustomPreset.not()) {
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onEditClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_edit_24dp),
                        contentDescription = "Edit preset $presetName"
                    )
                }
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete_24dp),
                        contentDescription = "Delete preset $presetName"
                    )
                }
            }
        }
    }
}