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

package com.mardous.booming.ui.screen.backup

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree
import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.ShareCompat
import androidx.core.content.pm.PackageInfoCompat
import com.mardous.booming.App
import com.mardous.booming.R
import com.mardous.booming.data.local.backup.BackupComponent
import com.mardous.booming.data.local.backup.BackupContent
import com.mardous.booming.data.local.backup.BackupFile
import com.mardous.booming.data.local.backup.BackupFileWithMetadata
import com.mardous.booming.data.local.backup.BackupMetadata
import com.mardous.booming.extensions.files.asReadableFileSize
import com.mardous.booming.extensions.showToast
import com.mardous.booming.extensions.utilities.buildInfoString
import com.mardous.booming.extensions.utilities.dateStr
import com.mardous.booming.ui.component.base.AbsThemeActivity
import com.mardous.booming.ui.component.compose.CollapsibleAppBarScaffold
import com.mardous.booming.ui.component.compose.DialogListItemWithCheckBox
import com.mardous.booming.ui.component.compose.EmptyView
import com.mardous.booming.ui.component.compose.EmptyViewDefaults
import com.mardous.booming.ui.component.compose.ObserveAsEvent
import com.mardous.booming.ui.component.compose.menu.MenuItem
import com.mardous.booming.ui.component.compose.menu.TopAppBarMenu
import com.mardous.booming.ui.theme.BoomingMusicTheme
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.compose.viewmodel.koinActivityViewModel

@StringRes
private fun BackupContent.titleRes(): Int = when (this) {
    BackupContent.Settings -> R.string.backup_content_settings
    BackupContent.Lyrics -> R.string.backup_content_lyrics
    BackupContent.PlayInfo -> R.string.backup_content_play_info
    BackupContent.ArtistImages -> R.string.backup_content_artist_images
    BackupContent.Playlists -> R.string.backup_content_playlists
}

class BackupActivity : AbsThemeActivity() {

    private val viewModel: BackupViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BoomingMusicTheme {
                BackupScreen(onBack = { finish() })
            }
        }
        handleIntent()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent = this.intent) {
        if (intent.action == Intent.ACTION_VIEW) {
            viewModel.loadBackupInfo(intent.data)
            this.intent = Intent()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun BackupScreen(
        onBack: () -> Unit,
        viewModel: BackupViewModel = koinActivityViewModel()
    ) {
        val context = LocalContext.current
        val activity = LocalActivity.current
        val state by viewModel.state.collectAsState()

        var showCreateBackupDialog by rememberSaveable { mutableStateOf(false) }
        var showRestartDialog by rememberSaveable { mutableStateOf(false) }
        var backupToDelete by remember { mutableStateOf<BackupFile?>(null) }
        var backupToRestore by remember { mutableStateOf<BackupFileWithMetadata?>(null) }

        val folderPickerLauncher = rememberLauncherForActivityResult(OpenDocumentTree()) { uri ->
            viewModel.setBackupDirectory(uri)
        }

        val manualPickerLauncher = rememberLauncherForActivityResult(OpenDocument()) { uri ->
            viewModel.loadBackupInfo(uri)
        }

        fun openBackupFile() {
            manualPickerLauncher.launch(arrayOf(BackupComponent.BACKUP_MIME_TYPE))
        }

        @SuppressLint("LocalContextGetResourceValueCall")
        fun shareBackupFile(backupFileUri: Uri) {
            ShareCompat.IntentBuilder(context)
                .setChooserTitle(context.getString(R.string.share_profiles))
                .setType(BackupComponent.BACKUP_MIME_TYPE)
                .setStream(backupFileUri)
                .startChooser()
        }

        ObserveAsEvent(viewModel.backupInfoEvent) { backupFileWithMetadata ->
            backupToRestore = backupFileWithMetadata
        }

        ObserveAsEvent(viewModel.createBackupEvent) { success ->
            if (success) context.showToast(R.string.backup_successful)
            else context.showToast(R.string.backup_failed)
        }

        ObserveAsEvent(viewModel.restoreBackupEvent) { success ->
            if (success) showRestartDialog = true
            else context.showToast(R.string.could_not_restore_data)
        }

        ObserveAsEvent(viewModel.shareBackupEvent) { shareUri ->
            if (shareUri != null) shareBackupFile(shareUri)
            else context.showToast(R.string.cannot_share_this_backup)
        }

        ObserveAsEvent(viewModel.deleteBackupEvent) { success ->
            if (success) context.showToast(R.string.backup_deleted_successfully)
            else context.showToast(R.string.could_not_delete_backup)
        }

        if (showRestartDialog) {
            AlertDialog(
                onDismissRequest = { showRestartDialog = false },
                title = { Text(text = stringResource(R.string.data_restored_successfully)) },
                text = { Text(text = stringResource(R.string.restart_app_message)) },
                confirmButton = {
                    Button(
                        onClick = {
                            showRestartDialog = false
                            activity?.let { App.restart(it) }
                        }
                    ) {
                        Text(text = stringResource(android.R.string.ok))
                    }
                },
                properties = DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false
                )
            )
        }

        if (showCreateBackupDialog) {
            CreateBackupDialog(
                onDismiss = { showCreateBackupDialog = false },
                onConfirm = { name, contents ->
                    showCreateBackupDialog = false
                    viewModel.createBackup(name, contents)
                }
            )
        }

        backupToDelete?.let {
            AlertDialog(
                onDismissRequest = { backupToDelete = null },
                text = { Text(text = stringResource(R.string.confirm_delete_backup)) },
                confirmButton = {
                    Button(onClick = {
                        viewModel.deleteBackup(it)
                        backupToDelete = null
                    }) {
                        Text(text = stringResource(R.string.yes))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { backupToDelete = null }) {
                        Text(text = stringResource(R.string.no))
                    }
                }
            )
        }

        backupToRestore?.let { backupFileWithMetadata ->
            RestoreContentDialog(
                onDismiss = { backupToRestore = null },
                onConfirm = { selectedContent ->
                    viewModel.restoreBackup(backupFileWithMetadata.uri, selectedContent)
                    backupToRestore = null
                },
                backupFileWithMetadata = backupFileWithMetadata
            )
        }

        CollapsibleAppBarScaffold(
            onBackClick = onBack,
            title = stringResource(R.string.backup_restore_title),
            actions = {
                TopAppBarMenu(
                    showItemIcons = true,
                    items = listOf(
                        MenuItem.Button.DropDown(
                            icon = painterResource(R.drawable.ic_folder_24dp),
                            text = stringResource(R.string.select_backup_folder),
                            onClick = { folderPickerLauncher.launch(null) }
                        )
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                HeaderButtonGroup(
                    onCreateBackup = { showCreateBackupDialog = true },
                    onImportBackup = { openBackupFile() },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                when (val currentState = state) {
                    is BackupsState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            CircularWavyProgressIndicator(modifier = Modifier.align(Alignment.Center))
                        }
                    }

                    is BackupsState.NoFolderSelected -> {
                        EmptyView(
                            icon = painterResource(R.drawable.ic_folder_off_24dp),
                            title = stringResource(R.string.no_backup_folder_selected),
                            button = {
                                Button(onClick = { folderPickerLauncher.launch(null) }) {
                                    Text(text = stringResource(R.string.select_backup_folder))
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    is BackupsState.Success -> {
                        if (currentState.backups.isEmpty()) {
                            EmptyView(
                                icon = painterResource(R.drawable.ic_folder_off_24dp),
                                title = stringResource(R.string.no_backups_found),
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                item {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp)
                                            .padding(bottom = 8.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.backup_recent_list),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (currentState.isInBackupOperation) {
                                            CircularProgressIndicator(
                                                strokeWidth = 2.dp,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                                itemsIndexed(
                                    items = currentState.backups,
                                    key = { _, item -> item.uri.toString() }
                                ) { index, backup ->
                                    BackupItem(
                                        onClick = { viewModel.loadBackupInfo(backup.uri) },
                                        onShareClick = { viewModel.shareBackup(backup) },
                                        onDeleteClick = { backupToDelete = backup },
                                        backup = backup,
                                        shapes = ListItemDefaults.segmentedShapes(index, currentState.backups.size)
                                    )
                                }
                            }
                        }
                    }

                    is BackupsState.Error -> {
                        EmptyView(
                            icon = painterResource(R.drawable.ic_error_24dp),
                            title = currentState.message
                                ?: stringResource(R.string.an_unexpected_error_occurred),
                            colors = EmptyViewDefaults.defaultColors(
                                iconColor = MaterialTheme.colorScheme.error,
                                iconContainerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun BackupItem(
        onClick: () -> Unit,
        onShareClick: () -> Unit,
        onDeleteClick: () -> Unit,
        shapes: ListItemShapes,
        backup: BackupFile,
        modifier: Modifier = Modifier
    ) {
        SegmentedListItem(
            shapes = shapes,
            onClick = onClick,
            supportingContent = {
                Text(
                    text = buildInfoString(
                        LocalContext.current.dateStr(backup.lastModified),
                        backup.size.asReadableFileSize()
                    )
                )
            },
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onShareClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_share_24dp),
                            contentDescription = stringResource(R.string.share_backup)
                        )
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_delete_24dp),
                            contentDescription = stringResource(R.string.delete_action)
                        )
                    }
                }
            },
            colors = ListItemDefaults.segmentedColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            contentPadding = PaddingValues(start = 16.dp, end = 8.dp) +
                    PaddingValues(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
        ) {
            Text(
                text = backup.name,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )
        }
    }

    @Composable
    private fun HeaderButtonGroup(
        onCreateBackup: () -> Unit,
        onImportBackup: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        val createInteractionSource = remember { MutableInteractionSource() }
        val importInteractionSource = remember { MutableInteractionSource() }

        val isCreatePressed by createInteractionSource.collectIsPressedAsState()
        val isImportPressed by importInteractionSource.collectIsPressedAsState()

        val createWeight by animateFloatAsState(
            targetValue = when {
                isCreatePressed -> 0.55f
                isImportPressed -> 0.45f
                else -> 0.5f
            },
            label = "create_button_weight"
        )

        val importWeight by animateFloatAsState(
            targetValue = when {
                isImportPressed -> 0.55f
                isCreatePressed -> 0.45f
                else -> 0.5f
            },
            label = "import_button_weight"
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = modifier.padding(vertical = 16.dp)
        ) {
            Button(
                onClick = onCreateBackup,
                interactionSource = createInteractionSource,
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding +
                        PaddingValues(vertical = 8.dp),
                modifier = Modifier.weight(createWeight)
            ) {
                Icon(painterResource(R.drawable.ic_add_24dp), null)
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text(
                    text = stringResource(R.string.create_new_backup),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )
            }

            FilledTonalButton(
                onClick = onImportBackup,
                interactionSource = importInteractionSource,
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding +
                        PaddingValues(vertical = 8.dp),
                modifier = Modifier.weight(importWeight)
            ) {
                Icon(painterResource(R.drawable.ic_file_open_24dp), null)
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text(
                    text = stringResource(R.string.open_backup),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )
            }
        }
    }

    @Composable
    private fun CreateBackupDialog(
        onDismiss: () -> Unit,
        onConfirm: (String, List<BackupContent>) -> Unit
    ) {
        val maxLength = 30
        var name by remember { mutableStateOf("") }
        SelectContentDialog(
            onDismiss = onDismiss,
            onConfirm = { onConfirm(name, it) },
            title = stringResource(R.string.select_content_to_backup),
            message = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.backup_file_name)) },
                    isError = name.length > maxLength,
                    suffix = { Text("${name.length}/$maxLength") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
            }
        )
    }

    @Composable
    private fun RestoreContentDialog(
        onDismiss: () -> Unit,
        onConfirm: (List<BackupContent>) -> Unit,
        backupFileWithMetadata: BackupFileWithMetadata,
    ) {
        val availableContents = backupFileWithMetadata.metadata.contents
            .ifEmpty { BackupContent.entries.toList() }

        SelectContentDialog(
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            title = stringResource(R.string.select_content_to_restore),
            contents = availableContents,
            message = {
                BackupMetadataInfo(backupFileWithMetadata.metadata)
            }
        )
    }

    @Composable
    private fun SelectContentDialog(
        onConfirm: (List<BackupContent>) -> Unit,
        onDismiss: () -> Unit,
        title: String,
        contents: List<BackupContent> = BackupContent.entries.toList(),
        message: @Composable (() -> Unit)? = null
    ) {
        val selectedContent = remember {
            mutableStateListOf<BackupContent>()
                .apply { addAll(contents) }
        }

        AlertDialog(
            title = { Text(title) },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (message != null) {
                        item { message() }
                    }

                    items(contents) { content ->
                        val isChecked = selectedContent.contains(content)
                        DialogListItemWithCheckBox(
                            title = stringResource(content.titleRes()),
                            onClick = {
                                if (isChecked) {
                                    selectedContent.remove(content)
                                } else {
                                    selectedContent.add(content)
                                }
                            },
                            isSelected = isChecked,
                            contentPadding = PaddingValues(
                                horizontal = 8.dp,
                                vertical = 12.dp
                            ),
                            modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { onConfirm(selectedContent) },
                    enabled = selectedContent.isNotEmpty()
                ) {
                    Text(stringResource(android.R.string.ok))
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
    private fun BackupMetadataInfo(
        metadata: BackupMetadata,
        modifier: Modifier = Modifier
    ) {
        val context = LocalContext.current
        val packageInfo = remember {
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        val currentAppVersionCode = remember {
            PackageInfoCompat.getLongVersionCode(packageInfo)
        }

        val isLegacy = metadata.backupVersion == BackupComponent.FIRST_BACKUP_VERSION
        val isNewerApp = metadata.appVersionCode > currentAppVersionCode

        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!metadata.appVersionName.isNullOrEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        MetadataRow(
                            label = stringResource(R.string.backup_app_version),
                            value = metadata.appVersionName
                        )
                    }
                }
            }

            if (isLegacy) {
                MetadataWarning(
                    text = stringResource(R.string.backup_warning_legacy_format),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            if (metadata.isNewerFormat) {
                MetadataWarning(
                    text = stringResource(R.string.backup_warning_ver_newer),
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            if (isNewerApp) {
                MetadataWarning(
                    text = stringResource(R.string.backup_warning_app_ver_newer),
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }

    @Composable
    private fun MetadataRow(
        label: String,
        value: String
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }

    @Composable
    private fun MetadataWarning(
        text: String,
        containerColor: Color,
        contentColor: Color
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = containerColor,
                contentColor = contentColor,
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_error_24dp),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
