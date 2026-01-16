@file:SuppressLint("LocalContextGetResourceValueCall")

package com.mardous.booming.ui.screen.equalizer

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.ShareCompat
import com.mardous.booming.R
import com.mardous.booming.core.model.LibraryMargin
import com.mardous.booming.core.model.equalizer.EqBand
import com.mardous.booming.core.model.equalizer.EqProfile
import com.mardous.booming.core.model.equalizer.ReplayGainState
import com.mardous.booming.core.model.equalizer.autoeq.AutoEqProfile
import com.mardous.booming.data.model.replaygain.ReplayGainMode
import com.mardous.booming.extensions.MIME_TYPE_APPLICATION
import com.mardous.booming.extensions.MIME_TYPE_PLAIN_TEXT
import com.mardous.booming.extensions.showToast
import com.mardous.booming.ui.component.compose.ButtonGroup
import com.mardous.booming.ui.component.compose.CollapsibleAppBarScaffold
import com.mardous.booming.ui.component.compose.ConfirmDialog
import com.mardous.booming.ui.component.compose.DialogListItemSurface
import com.mardous.booming.ui.component.compose.DialogListItemWithCheckBox
import com.mardous.booming.ui.component.compose.InputDialog
import com.mardous.booming.ui.component.compose.MaterialSwitch
import com.mardous.booming.ui.component.compose.SwitchCard
import com.mardous.booming.ui.component.compose.TitleShapedText
import com.mardous.booming.ui.component.compose.TitledCard
import com.mardous.booming.ui.screen.library.LibraryViewModel
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

private const val PRESET_NAME_MAX_LENGTH = 48

private enum class ProfilesMode(@StringRes val nameRes: Int) {
    EQ(R.string.eq_profiles_title),
    AutoEq(R.string.autoeq_profiles_title)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun EqualizerScreen(
    libraryViewModel: LibraryViewModel,
    eqViewModel: EqualizerViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current

    val importProfileLauncher = rememberLauncherForActivityResult(OpenDocument()) { data: Uri? ->
        eqViewModel.requestImport(data)
    }

    val importAutoEqProfileLauncher = rememberLauncherForActivityResult(OpenDocument()) { data: Uri? ->
        eqViewModel.requestAutoEqImport(context, data)
    }

    var exportableContent by remember { mutableStateOf<String?>(null) }
    val exportProfileLauncher =
        rememberLauncherForActivityResult(CreateDocument(MIME_TYPE_APPLICATION)) { data: Uri? ->
            eqViewModel.exportConfiguration(data, exportableContent)
        }

    fun importProfiles(autoEq: Boolean) {
        try {
            if (autoEq) {
                importAutoEqProfileLauncher.launch(arrayOf(MIME_TYPE_PLAIN_TEXT))
                context.showToast(R.string.select_a_file_containing_autoeq_params)
            } else {
                importProfileLauncher.launch(arrayOf(MIME_TYPE_APPLICATION))
                context.showToast(R.string.select_a_file_containing_booming_eq_profiles)
            }
        } catch (_: ActivityNotFoundException) {
            context.showToast("File picker not found")
        }
    }

    fun shareConfiguration(data: Uri, mimeType: String) {
        val builder = ShareCompat.IntentBuilder(context)
            .setChooserTitle(R.string.share_eq_profiles)
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
    val eqCurrentProfile by eqViewModel.currentProfile.collectAsState()
    val eqProfiles by eqViewModel.eqProfiles.collectAsState(emptyList())
    val eqBandCapabilities by eqViewModel.eqBandCapabilities.collectAsState()
    val eqBands by eqViewModel.eqBands.collectAsState(emptyList())
    val autoEqProfiles by eqViewModel.autoEqProfiles.collectAsState()

    val virtualizer by eqViewModel.virtualizerState.collectAsState()
    val bassBoost by eqViewModel.bassBoostState.collectAsState()
    val loudnessGain by eqViewModel.loudnessGainState.collectAsState()
    val replayGain by eqViewModel.replayGainState.collectAsState()

    var editProfileState by remember { mutableStateOf<Pair<EqProfile, Boolean>?>(null) }
    var deleteProfileState by remember { mutableStateOf<Pair<EqProfile, Boolean>?>(null) }

    var shareProfileState by remember { mutableStateOf<Pair<Uri, String>?>(null) }
    shareProfileState?.let { (data, mimeType) ->
        coroutineScope.launch {
            val result = snackbarHostState.showSnackbar(
                message = context.getString(R.string.profiles_exported_successfully),
                actionLabel = context.getString(R.string.action_share),
                duration = SnackbarDuration.Long
            )
            when (result) {
                SnackbarResult.Dismissed -> shareProfileState = null
                SnackbarResult.ActionPerformed -> {
                    shareConfiguration(data, mimeType)
                    shareProfileState = null
                }
            }
        }
    }

    var showBandCountSelector by remember { mutableStateOf(false) }
    var showShareProfileDialog by remember { mutableStateOf(false) }
    var showProfileSaverDialog by remember { mutableStateOf(false) }
    var showProfileSelectorDialog by remember { mutableStateOf(false) }
    var showResetEqDialog by remember { mutableStateOf(false) }

    var showImportDialog by remember { mutableStateOf(false) }
    var profilesToImport by remember { mutableStateOf<List<EqProfile>>(emptyList()) }
    val importRequestEvent by eqViewModel.importRequestEvent.collectAsState(null)
    LaunchedEffect(importRequestEvent) {
        importRequestEvent?.let {
            if (it.success) {
                profilesToImport = it.profiles
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
                context.showToast(context.getString(R.string.imported_x_profiles, it.imported))
            } else {
                context.showToast(it.messageRes)
            }
        }
    }

    if (showImportDialog && profilesToImport.isNotEmpty()) {
        ProfileCheckDialog(
            icon = painterResource(R.drawable.ic_file_save_24dp),
            title = stringResource(R.string.import_profiles),
            message = stringResource(R.string.select_profiles_to_import),
            confirmButton = stringResource(R.string.import_action),
            profiles = profilesToImport,
            onDismiss = { showImportDialog = false },
            onConfirm = { selectedProfiles ->
                eqViewModel.importProfiles(selectedProfiles)
                showImportDialog = false
            }
        )
    }

    var importAutoEqProfileState by remember { mutableStateOf<Pair<AutoEqProfile, Boolean>?>(null) }
    val importAutoEqRequestEvent by eqViewModel.autoEqImportRequestEvent.collectAsState(null)
    LaunchedEffect(importAutoEqRequestEvent) {
        importAutoEqRequestEvent?.let {
            if (it.success && it.profile != null) {
                importAutoEqProfileState = Pair(it.profile, true)
            } else {
                context.showToast(it.messageRes)
            }
        }
    }

    importAutoEqProfileState?.let { (profile, showDialog) ->
        if (showDialog) {
            InputDialog(
                icon = painterResource(R.drawable.ic_file_save_24dp),
                message = stringResource(R.string.please_enter_a_name_for_this_profile),
                inputHint = stringResource(R.string.profile_name),
                inputPrefill = profile.name,
                inputMaxLength = PRESET_NAME_MAX_LENGTH,
                checkBoxPrompt = stringResource(R.string.replace_profile_with_same_name),
                confirmButton = stringResource(R.string.action_save),
                onConfirm = { name, isChecked ->
                    eqViewModel.importAutoEqProfile(profile, name, isChecked)
                },
                onDismiss = { importAutoEqProfileState = null }
            )
        }
    }

    val autoEqImportResultEvent by eqViewModel.autoEqImportResultEvent.collectAsState(null)
    LaunchedEffect(autoEqImportResultEvent) {
        autoEqImportResultEvent?.let {
            context.showToast(it.messageRes)
            if (it.canDismiss) {
                importAutoEqProfileState = null
            }
        }
    }

    var showExportDialog by remember { mutableStateOf(false) }
    val exportRequestEvent by eqViewModel.exportRequestEvent.collectAsState(null)
    LaunchedEffect(exportRequestEvent) {
        exportRequestEvent?.let {
            if (it.success && it.profileExportData != null) {
                exportableContent = it.profileExportData.second
                try {
                    exportProfileLauncher.launch(it.profileExportData.first)
                    context.showToast(R.string.select_a_file_to_save_the_exported_profiles)
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
                    shareProfileState = Pair(it.data, it.mimeType)
                }
            } else {
                context.showToast(it.messageRes)
            }
        }
    }

    if (showExportDialog && eqProfiles.isNotEmpty()) {
        ProfileCheckDialog(
            icon = painterResource(R.drawable.ic_file_export_24dp),
            title = stringResource(R.string.export_profiles),
            message = stringResource(R.string.select_profiles_to_export),
            confirmButton = stringResource(android.R.string.ok),
            profiles = eqProfiles,
            onDismiss = { showExportDialog = false },
            onConfirm = { selectedProfiles ->
                eqViewModel.generateExportData(selectedProfiles)
                showExportDialog = false
            }
        )
    }

    if (showShareProfileDialog && eqProfiles.isNotEmpty()) {
        ProfileCheckDialog(
            icon = painterResource(R.drawable.ic_share_24dp),
            title = stringResource(R.string.share_profiles),
            message = stringResource(R.string.select_profiles_to_share),
            confirmButton = stringResource(R.string.action_share),
            profiles = eqProfiles,
            onDismiss = { showShareProfileDialog = false },
            onConfirm = { selectedProfiles ->
                eqViewModel.shareProfiles(context, selectedProfiles)
                showShareProfileDialog = false
            }
        )
    }

    val saveResultEvent by eqViewModel.saveResultEvent.collectAsState(null)
    LaunchedEffect(saveResultEvent) {
        saveResultEvent?.let {
            context.showToast(it.messageRes)
            if (it.canDismiss) {
                showProfileSaverDialog = false
            }
        }
    }

    if (showProfileSaverDialog) {
        InputDialog(
            icon = painterResource(R.drawable.ic_save_24dp),
            title = stringResource(R.string.save_profile_label),
            message = stringResource(R.string.please_enter_a_name_for_this_profile),
            inputHint = stringResource(R.string.profile_name),
            inputMaxLength = PRESET_NAME_MAX_LENGTH,
            checkBoxPrompt = stringResource(R.string.replace_profile_with_same_name),
            confirmButton = stringResource(R.string.action_save),
            onConfirm = { profileName, canReplace ->
                eqViewModel.saveProfile(profileName, canReplace)
            },
            onDismiss = { showProfileSaverDialog = false }
        )
    }

    val renameResultEvent by eqViewModel.renameResultEvent.collectAsState(null)
    LaunchedEffect(renameResultEvent) {
        renameResultEvent?.let {
            context.showToast(it.messageRes)
            if (it.canDismiss) {
                editProfileState = null
            }
        }
    }

    editProfileState?.let { (targetProfile, showDialog) ->
        if (showDialog) {
            InputDialog(
                icon = painterResource(R.drawable.ic_edit_24dp),
                title = stringResource(R.string.rename_profile_label),
                message = stringResource(R.string.please_enter_a_new_name_for_this_profile),
                inputHint = stringResource(R.string.profile_name),
                inputPrefill = targetProfile.name,
                inputMaxLength = PRESET_NAME_MAX_LENGTH,
                confirmButton = stringResource(R.string.rename_action),
                onConfirm = { input -> eqViewModel.renameProfile(targetProfile, input) },
                onDismiss = { editProfileState = null }
            )
        }
    }

    val deleteResultEvent by eqViewModel.deleteResultEvent.collectAsState(null)
    LaunchedEffect(deleteResultEvent) {
        deleteResultEvent?.let {
            if (it.success && deleteProfileState != null) {
                if (it.autoEqProfile) {
                    context.showToast(
                        context.getString(R.string.autoeq_profile_x_deleted, it.profileName)
                    )
                } else {
                    context.showToast(
                        context.getString(R.string.profile_x_deleted, it.profileName)
                    )
                }
            }
            if (it.canDismiss) {
                deleteProfileState = null
            }
        }
    }

    deleteProfileState?.let { (targetProfile, showDialog) ->
        if (showDialog) {
            ConfirmDialog(
                icon = painterResource(R.drawable.ic_delete_24dp),
                title = stringResource(R.string.delete_profile_label),
                message = stringResource(R.string.delete_profile_x, targetProfile.name),
                confirmButton = stringResource(R.string.action_delete),
                dismissButton = stringResource(R.string.no),
                onConfirm = { eqViewModel.deleteProfile(context, targetProfile) },
                onDismiss = { deleteProfileState = null }
            )
        }
    }

    var changeBandCountState by remember { mutableStateOf<Pair<Int, Boolean>?>(null) }
    val changeBandCountEvent by eqViewModel.changeBandCountEvent.collectAsState(null)
    LaunchedEffect(changeBandCountEvent) {
        changeBandCountEvent?.let { success ->
            if (success) {
                context.showToast(R.string.band_configuration_changed_successfully)
            } else {
                context.showToast(R.string.band_configuration_could_not_be_changed)
            }
        }
    }

    changeBandCountState?.let { (bandCount, showDialog) ->
        if (showDialog) {
            ConfirmDialog(
                icon = painterResource(R.drawable.ic_graphic_eq_24dp),
                title = stringResource(R.string.change_band_count_title),
                message = stringResource(R.string.change_band_count_message),
                confirmButton = stringResource(R.string.continue_action),
                dismissButton = stringResource(R.string.no),
                onConfirm = {
                    eqViewModel.setBandCount(bandCount)
                    showBandCountSelector = false
                    changeBandCountState = null
                },
                onDismiss = { changeBandCountState = null }
            )
        }
    }

    if (showProfileSelectorDialog) {
        ProfileSelectorDialog(
            profiles = eqProfiles,
            autoEqProfiles = autoEqProfiles,
            selectedProfile = eqCurrentProfile,
            onSelectEqProfile = { profile ->
                eqViewModel.setEqualizerProfile(profile)
                showProfileSelectorDialog = false
            },
            onSelectAutoEqProfile = { profile ->
                eqViewModel.setAutoEqProfile(profile)
                showProfileSelectorDialog = false
            },
            onEditEqProfile = { profile -> editProfileState = Pair(profile, true) },
            onDeleteEqProfile = { profile -> deleteProfileState = Pair(profile, true) },
            onDeleteAutoEqProfile = { eqViewModel.deleteAutoEqProfile(context, it) },
            onImportAutoEqProfile = { importProfiles(autoEq = true) },
            onDismiss = { showProfileSelectorDialog = false }
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
                    text = { Text(stringResource(R.string.share_profiles)) },
                    enabled = eqState.supported && eqProfiles.isNotEmpty(),
                    onClick = {
                        showShareProfileDialog = true
                        expandedMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.export_profiles)) },
                    enabled = eqState.supported && eqProfiles.isNotEmpty(),
                    onClick = {
                        showExportDialog = true
                        expandedMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.import_profiles)) },
                    enabled = eqState.supported,
                    onClick = {
                        importProfiles(autoEq = false)
                        expandedMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.import_autoeq_profile)) },
                    enabled = eqState.supported,
                    onClick = {
                        importProfiles(autoEq = true)
                        expandedMenu = false
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.reset_equalizer)) },
                    enabled = eqState.isUsable,
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
                subtitle = if (!eqState.supported) {
                    stringResource(R.string.not_supported)
                } else if (eqState.disabledByAudioOffload) {
                    stringResource(R.string.audio_offload_is_enabled)
                } else null,
                isChecked = eqState.isUsable,
                enabled = eqState.supported && !eqState.disabledByAudioOffload,
                onClick = {
                    eqViewModel.setEqualizerState(isEnabled = eqState.enabled.not())
                }
            )

            if (eqState.supported) {
                TitledCard(
                    title = stringResource(R.string.eq_profile_title),
                    iconRes = R.drawable.ic_equalizer_24dp
                ) { cardContentPadding ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(cardContentPadding)
                    ) {
                        val containerColor = if (eqState.isUsable) {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f)
                        }
                        val contentColor = contentColorFor(containerColor)

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .background(containerColor)
                                .clickable(
                                    enabled = eqState.isUsable,
                                    onClick = { showProfileSelectorDialog = true }
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .weight(1f)
                        ) {
                            Text(
                                text = eqCurrentProfile.getName(context),
                                style = MaterialTheme.typography.titleMedium,
                                color = contentColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_drop_down_24dp),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        FilledIconButton(
                            enabled = eqState.isUsable && eqCurrentProfile.isCustom,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            ),
                            onClick = { showProfileSaverDialog = true }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_save_24dp),
                                contentDescription = stringResource(R.string.save_profile_label)
                            )
                        }
                    }
                }

                TitledCard(
                    title = stringResource(R.string.graphic_eq_label),
                    iconRes = R.drawable.ic_graphic_eq_24dp,
                    titleEndContent = {
                        if (eqBandCapabilities.hasMultipleBandConfigurations) {
                            TitleShapedText(
                                text = stringResource(R.string.graphic_eq_band_count, eqState.preferredBandCount),
                                enabled = eqState.isUsable,
                                onClick = { showBandCountSelector = showBandCountSelector.not() }
                            )
                        }
                    }
                ) { cardContentPadding ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .padding(cardContentPadding)
                    ) {
                        AnimatedVisibility(
                            visible = eqState.enabled && showBandCountSelector
                        ) {
                            ButtonGroup(
                                onSelected = { changeBandCountState = Pair(it, true) },
                                buttonItems = eqBandCapabilities.availableBandCounts,
                                buttonStateResolver = { it == eqState.preferredBandCount },
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
                            )
                        }

                        eqBands.forEach { band ->
                            EQBandSlider(
                                enabled = eqState.isUsable,
                                band = band,
                                onValueChange = { bandGain ->
                                    eqViewModel.setCustomProfileBandGain(band.index, bandGain)
                                }
                            )
                        }
                    }
                }
            }

            if (virtualizer.supported) {
                var virtualizerStrength by remember(virtualizer.strength) {
                    mutableFloatStateOf(virtualizer.strength)
                }
                SwitchCard(
                    onCheckedChange = { eqViewModel.setVirtualizer(enabled = it) },
                    checked = virtualizer.enabled && eqState.enabled,
                    title = stringResource(R.string.virtualizer_label),
                    iconRes = R.drawable.ic_headphones_24dp,
                    enabled = eqState.isUsable
                ) { cardContentPadding ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(cardContentPadding)
                    ) {
                        Slider(
                            steps = 10,
                            value = virtualizerStrength,
                            onValueChange = { virtualizerStrength = it },
                            onValueChangeFinished = {
                                eqViewModel.setVirtualizer(strength = virtualizerStrength)
                            },
                            valueRange = virtualizer.strengthRange,
                            enabled = eqState.isUsable,
                            modifier = Modifier.weight(1f)
                        )

                        EQValueText(
                            text = "${((virtualizerStrength * 100) / virtualizer.strengthRange.endInclusive).toInt()}%",
                        )
                    }
                }
            }

            if (bassBoost.supported) {
                var bassBoostStrength by remember(bassBoost.strength) {
                    mutableFloatStateOf(bassBoost.strength)
                }
                SwitchCard(
                    onCheckedChange = { eqViewModel.setBassBoost(enabled = it) },
                    checked = bassBoost.enabled && eqState.enabled,
                    title = stringResource(R.string.bassboost_label),
                    iconRes = R.drawable.ic_edit_audio_24dp,
                    enabled = eqState.isUsable
                ) { cardContentPadding ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(cardContentPadding)
                    ) {
                        Slider(
                            steps = 10,
                            value = bassBoostStrength,
                            onValueChange = { bassBoostStrength = it },
                            onValueChangeFinished = {
                                eqViewModel.setBassBoost(strength = bassBoostStrength)
                            },
                            valueRange = bassBoost.strengthRange,
                            enabled = eqState.isUsable,
                            modifier = Modifier.weight(1f)
                        )

                        EQValueText(
                            text = "${((bassBoostStrength * 100) / bassBoost.strengthRange.endInclusive).toInt()}%"
                        )
                    }
                }
            }

            if (loudnessGain.supported) {
                var loudnessGainValue by remember(loudnessGain.gainInDb) {
                    mutableFloatStateOf(loudnessGain.gainInDb)
                }
                SwitchCard(
                    onCheckedChange = { eqViewModel.setLoudnessGain(enabled = it) },
                    checked = loudnessGain.enabled && eqState.enabled,
                    title = stringResource(R.string.loudness_enhancer),
                    iconRes = R.drawable.ic_volume_up_24dp,
                    enabled = eqState.isUsable
                ) { cardContentPadding ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(cardContentPadding)
                    ) {
                        Slider(
                            value = loudnessGainValue,
                            onValueChange = {
                                loudnessGainValue = it
                            },
                            onValueChangeFinished = {
                                eqViewModel.setLoudnessGain(gain = loudnessGainValue)
                            },
                            valueRange = loudnessGain.gainRange,
                            enabled = eqState.isUsable,
                            modifier = Modifier.weight(1f)
                        )

                        EQValueText(
                            text = "%.1f dB".format(Locale.ROOT, loudnessGainValue)
                        )
                    }
                }
            }

            var replayGainPreamp by remember(replayGain.preamp) {
                mutableFloatStateOf(replayGain.preamp)
            }
            AnimatedVisibility(visible = eqState.disabledByAudioOffload.not()) {
                TitledCard(
                    title = stringResource(R.string.replay_gain),
                    iconRes = R.drawable.ic_sound_sampler_24dp,
                    titleEndContent = {
                        AnimatedVisibility(visible = replayGain.mode.isOn) {
                            TitleShapedText("%+.1f dB".format(Locale.ROOT, replayGainPreamp))
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { cardContentPadding ->
                    Column(
                        modifier = Modifier.padding(cardContentPadding)
                    ) {
                        ReplayGainModeSelector(replayGain) {
                            eqViewModel.setReplayGain(mode = it)
                        }

                        AnimatedVisibility(
                            visible = replayGain.mode.isOn,
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        replayGainPreamp = 0f
                                        eqViewModel.setReplayGain(preamp = replayGainPreamp)
                                    }
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_restart_alt_24dp),
                                        contentDescription = null
                                    )
                                }

                                Slider(
                                    steps = 29,
                                    value = replayGainPreamp,
                                    valueRange = -15f..15f,
                                    onValueChange = {
                                        replayGainPreamp = (it / 0.2f).roundToInt() * 0.2f
                                    },
                                    onValueChangeFinished = {
                                        eqViewModel.setReplayGain(preamp = replayGainPreamp)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EQBandSlider(
    enabled: Boolean,
    band: EqBand,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var bandLevel by remember(band.value) {
        mutableFloatStateOf(band.value)
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = band.readableFrequency,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(48.dp)
        )

        Slider(
            value = bandLevel,
            onValueChange = { bandLevel = it },
            onValueChangeFinished = { onValueChange(bandLevel) },
            valueRange = band.valueRange,
            enabled = enabled,
            modifier = Modifier.weight(1f)
        )

        EQValueText(
            text = "%+.1f dB".format(Locale.ROOT, bandLevel)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileSelectorDialog(
    profiles: List<EqProfile>,
    autoEqProfiles: List<AutoEqProfile>,
    selectedProfile: EqProfile,
    onSelectEqProfile: (EqProfile) -> Unit,
    onSelectAutoEqProfile: (AutoEqProfile) -> Unit,
    onEditEqProfile: (EqProfile) -> Unit,
    onDeleteEqProfile: (EqProfile) -> Unit,
    onDeleteAutoEqProfile: (AutoEqProfile) -> Unit,
    onImportAutoEqProfile: () -> Unit,
    onDismiss: () -> Unit
) {
    var profilesMode by remember { mutableStateOf(ProfilesMode.EQ) }

    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Text(
                text = stringResource(R.string.select_profile),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(16.dp))

            ButtonGroup(
                onSelected = { profilesMode = it },
                buttonItems = ProfilesMode.entries,
                buttonTextResolver = { stringResource(it.nameRes) },
                buttonStateResolver = { it == profilesMode }
            )

            Spacer(Modifier.height(8.dp))

            when (profilesMode) {
                ProfilesMode.EQ -> {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(profiles) { profile ->
                            EqualizerProfileItem(
                                profile = profile,
                                isCurrentProfile = profile == selectedProfile,
                                onClick = { onSelectEqProfile(profile) },
                                onEditClick = { onEditEqProfile(profile) },
                                onDeleteClick = { onDeleteEqProfile(profile) }
                            )
                        }
                    }
                }

                ProfilesMode.AutoEq -> {
                    if (autoEqProfiles.isEmpty()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_equalizer_24dp),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(72.dp)
                            )

                            Text(
                                text = stringResource(R.string.no_autoeq_profiles),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.headlineSmall
                            )

                            Button(onClick = onImportAutoEqProfile) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_file_open_24dp),
                                    contentDescription = null
                                )
                                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                Text(stringResource(R.string.import_autoeq_profile))
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(autoEqProfiles) { profile ->
                                AutoEqProfileItem(
                                    profile = profile,
                                    onClick = { onSelectAutoEqProfile(profile) },
                                    onDeleteClick = { onDeleteAutoEqProfile(profile) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileCheckDialog(
    icon: Painter,
    title: String,
    message: String,
    confirmButton: String,
    profiles: List<EqProfile>,
    onDismiss: () -> Unit,
    onConfirm: (List<EqProfile>) -> Unit
) {
    val selectedProfiles = remember {
        mutableStateListOf<EqProfile>()
            .apply { addAll(profiles.filterNot { it.isCustom }) }
    }

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
                    items(profiles) { profile ->
                        if (profile.isCustom.not()) {
                            val isChecked = selectedProfiles.contains(profile)
                            DialogListItemWithCheckBox(
                                title = profile.name,
                                onClick = {
                                    if (isChecked) {
                                        selectedProfiles.remove(profile)
                                    } else {
                                        selectedProfiles.add(profile)
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
                onClick = { onConfirm(selectedProfiles) },
                enabled = selectedProfiles.isNotEmpty()
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
private fun EqualizerProfileItem(
    profile: EqProfile,
    isCurrentProfile: Boolean,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DialogListItemSurface(
        onClick = onClick,
        isSelected = isCurrentProfile,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .padding(vertical = 8.dp)
                .padding(start = 16.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_equalizer_24dp),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )

            Spacer(Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = profile.getName(LocalContext.current),
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = profile.getDescription(LocalContext.current),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!profile.isCustom) {
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onEditClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_edit_24dp),
                        contentDescription = "Edit profile ${profile.getName(LocalContext.current)}"
                    )
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete_24dp),
                        contentDescription = "Delete profile ${profile.getName(LocalContext.current)}"
                    )
                }
            }
        }
    }
}

@Composable
private fun AutoEqProfileItem(
    profile: AutoEqProfile,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DialogListItemSurface(
        onClick = onClick,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .padding(vertical = 8.dp)
                .padding(start = 16.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_equalizer_24dp),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(8.dp))

            IconButton(onClick = onDeleteClick) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete_24dp),
                    contentDescription = "Delete profile ${profile.name}"
                )
            }
        }
    }
}

@Composable
private fun ReplayGainModeSelector(
    replayGain: ReplayGainState,
    onModeSelected: (ReplayGainMode) -> Unit
) {
    ButtonGroup(
        onSelected = onModeSelected,
        buttonItems = replayGain.availableModes.toList(),
        buttonStateResolver = { mode -> mode == replayGain.mode },
        buttonIconResolver = { mode, isChecked ->
            if (isChecked) when (mode) {
                ReplayGainMode.Album -> painterResource(R.drawable.ic_album_24dp)
                ReplayGainMode.Track -> painterResource(R.drawable.ic_music_note_24dp)
                else -> null
            } else {
                null
            }
        },
        buttonTextResolver = { mode ->
            when (mode) {
                ReplayGainMode.Album -> stringResource(R.string.album)
                ReplayGainMode.Track -> stringResource(R.string.track)
                else -> stringResource(R.string.label_none)
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun EQValueText(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.secondary,
        maxLines = 1,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = modifier.width(56.dp)
    )
}