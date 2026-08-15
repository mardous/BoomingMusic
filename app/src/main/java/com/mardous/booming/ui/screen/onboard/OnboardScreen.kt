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

@file:OptIn(ExperimentalPermissionsApi::class)

package com.mardous.booming.ui.screen.onboard

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.booleanResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.os.LocaleListCompat
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath
import androidx.preference.PreferenceManager
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import com.mardous.booming.R
import com.mardous.booming.data.model.network.NetworkFeature
import com.mardous.booming.extensions.MIME_TYPE_APPLICATION
import com.mardous.booming.extensions.getImagesPermission
import com.mardous.booming.extensions.getNearbyDevicesPermissions
import com.mardous.booming.extensions.getStoragePermissions
import com.mardous.booming.extensions.hasS
import com.mardous.booming.extensions.hasT
import com.mardous.booming.extensions.languageEndonym
import com.mardous.booming.extensions.observeKeyAsState
import com.mardous.booming.extensions.showToast
import com.mardous.booming.ui.component.compose.ButtonGroup
import com.mardous.booming.ui.component.compose.DialogListItemWithRadio
import com.mardous.booming.util.AUTO_LANGUAGE
import com.mardous.booming.util.BackupContent
import com.mardous.booming.util.BackupHelper
import com.mardous.booming.util.GENERAL_THEME
import com.mardous.booming.util.GeneralTheme
import com.mardous.booming.util.LANGUAGE_NAME
import com.mardous.booming.util.MATERIAL_YOU
import com.mardous.booming.util.MINIMUM_SONG_DURATION
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class OnboardStep {
    WELCOME,
    PERMISSIONS,
    CONFIGURATION
}

private fun List<OnboardStep>.canGoBack(currentStep: OnboardStep) = any { it < currentStep }

@Composable
fun OnboardScreen(
    onFinish: () -> Unit,
    onBackToExit: () -> Unit,
    modifier: Modifier = Modifier,
    availableSteps: List<OnboardStep> = OnboardStep.entries
) {
    check(availableSteps.isNotEmpty())
    var currentStep by rememberSaveable { mutableStateOf(availableSteps.first()) }

    fun goToStep(step: OnboardStep, onStepNotAvailable: () -> Unit = onFinish) {
        if (step in availableSteps) {
            currentStep = step
        } else {
            onStepNotAvailable()
        }
    }

    val storagePermission = remember { getStoragePermissions().toList() }
    val storagePermissionState = rememberMultiplePermissionsState(storagePermission)
    val nearbyPermissionState =
        if (hasS()) rememberPermissionState(getNearbyDevicesPermissions().single()) else null
    val readImagesPermissionState =
        if (hasT()) rememberPermissionState(getImagesPermission().single()) else null

    BackHandler {
        when (currentStep) {
            OnboardStep.CONFIGURATION -> goToStep(OnboardStep.PERMISSIONS) { onBackToExit() }
            OnboardStep.PERMISSIONS -> goToStep(OnboardStep.WELCOME) { onBackToExit() }
            OnboardStep.WELCOME -> onBackToExit()
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            OnboardBackground()

            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState.ordinal > initialState.ordinal) {
                        (slideInHorizontally { width -> width } + fadeIn(tween(400)))
                            .togetherWith(slideOutHorizontally { width -> -width } + fadeOut(tween(400)))
                    } else {
                        (slideInHorizontally { width -> -width } + fadeIn(tween(400)))
                            .togetherWith(slideOutHorizontally { width -> width } + fadeOut(tween(400)))
                    }
                },
                label = "welcome_step_transition",
                modifier = Modifier.fillMaxSize()
            ) { step ->
                when (step) {
                    OnboardStep.WELCOME -> {
                        WelcomeStepContent(
                            onNextClick = { goToStep(OnboardStep.PERMISSIONS) }
                        )
                    }

                    OnboardStep.PERMISSIONS -> {
                        PermissionsStepContent(
                            availableSteps = availableSteps,
                            storagePermissionState = storagePermissionState,
                            nearbyPermissionState = nearbyPermissionState,
                            readImagesPermissionState = readImagesPermissionState,
                            onNextClick = { goToStep(OnboardStep.CONFIGURATION) },
                            onBackClick = { goToStep(OnboardStep.WELCOME) { onBackToExit() } }
                        )
                    }

                    OnboardStep.CONFIGURATION -> {
                        ConfigurationStepContent(
                            onFinish = onFinish,
                            onBackClick = { goToStep(OnboardStep.PERMISSIONS) { onBackToExit() } }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeStepContent(
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animState = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        animState.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    val progress = animState.value

    val configuration = LocalConfiguration.current
    if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        Row(
            modifier = modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WelcomeAppContent(
                progress = progress,
                modifier = Modifier.weight(1f)
            )

            ContinueButton(
                onClick = onNextClick,
                text = stringResource(R.string.get_started),
                modifier = Modifier.graphicsLayer {
                    alpha = (progress * 2f - 0.8f).coerceIn(0f, 1f)
                    translationX = (1f - progress) * 60f
                }
            )
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(top = 32.dp, bottom = 16.dp)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            WelcomeAppContent(
                progress = progress,
                modifier = Modifier.weight(1f)
            )

            ContinueButton(
                onClick = onNextClick,
                text = stringResource(R.string.get_started),
                modifier = Modifier.graphicsLayer {
                    alpha = (progress * 2f - 0.8f).coerceIn(0f, 1f)
                    translationY = (1f - progress) * 60f
                }
            )
        }
    }
}

@Composable
private fun PermissionsStepContent(
    availableSteps: List<OnboardStep>,
    storagePermissionState: MultiplePermissionsState,
    nearbyPermissionState: PermissionState?,
    readImagesPermissionState: PermissionState?,
    onNextClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val permissionItems = remember(
        storagePermissionState.allPermissionsGranted,
        nearbyPermissionState?.status?.isGranted,
        readImagesPermissionState?.status?.isGranted
    ) {
        buildList {
            add(
                PermissionItemData(
                    title = R.string.permission_external_storage_title,
                    description = R.string.permission_external_storage_description,
                    icon = R.drawable.ic_sd_card_24dp,
                    isGranted = storagePermissionState.allPermissionsGranted,
                    onRequest = { storagePermissionState.launchMultiplePermissionRequest() }
                )
            )
            if (nearbyPermissionState != null) {
                add(
                    PermissionItemData(
                        title = R.string.permission_bluetooth_title,
                        description = R.string.permission_bluetooth_description,
                        icon = R.drawable.ic_bluetooth_connected_24dp,
                        isGranted = nearbyPermissionState.status.isGranted,
                        onRequest = { nearbyPermissionState.launchPermissionRequest() }
                    )
                )
            }
            if (readImagesPermissionState != null) {
                add(
                    PermissionItemData(
                        title = R.string.permission_read_images_title,
                        description = R.string.permission_read_images_summary,
                        icon = R.drawable.ic_image_24dp,
                        isGranted = readImagesPermissionState.status.isGranted,
                        onRequest = { readImagesPermissionState.launchPermissionRequest() }
                    )
                )
            }
        }
    }

    OnboardSurface(
        onBackClick = onBackClick,
        onContinueClick = onNextClick,
        canGoBack = availableSteps.canGoBack(OnboardStep.PERMISSIONS),
        canGoForward = storagePermissionState.allPermissionsGranted &&
                (nearbyPermissionState?.status?.isGranted ?: true),
        title = stringResource(R.string.permissions_title),
        description = stringResource(R.string.permissions_subtitle),
        modifier = modifier
    ) {
        itemsIndexed(permissionItems) { index, item ->
            SegmentedListItem(
                onClick = {
                    if (!item.isGranted) {
                        item.onRequest()
                    }
                },
                shapes = ListItemDefaults.segmentedShapes(index, permissionItems.size),
                colors = ListItemDefaults.segmentedColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                verticalAlignment = Alignment.CenterVertically,
                leadingContent = {
                    Icon(
                        painter = painterResource(item.icon),
                        contentDescription = null,
                        tint = if (item.isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                },
                trailingContent = {
                    if (item.isGranted) {
                        FilterChip(
                            selected = true,
                            onClick = {},
                            label = {
                                Text(
                                    text = stringResource(R.string.granted),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.ic_check_24dp),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            border = null
                        )
                    } else {
                        Button(
                            onClick = item.onRequest,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.grant_access_action),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            ) {
                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                    Text(
                        text = stringResource(item.title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(item.description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfigurationStepContent(
    onFinish: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    val coroutineScope = rememberCoroutineScope()
    val preferences = remember { PreferenceManager.getDefaultSharedPreferences(context) }

    val selectedTheme by preferences.observeKeyAsState(GENERAL_THEME, GeneralTheme.AUTO)
    val materialYou by preferences.observeKeyAsState(MATERIAL_YOU, hasS())
    val minDuration by preferences.observeKeyAsState(MINIMUM_SONG_DURATION, 15)

    val networkEnabledByDefault = booleanResource(R.bool.network_features_enabled_by_default)
    val networkEnabled by preferences.observeKeyAsState(NetworkFeature.NETWORK_FEATURES_KEY, networkEnabledByDefault)
    val currentLanguageTag by preferences.observeKeyAsState(LANGUAGE_NAME, AUTO_LANGUAGE)

    val backupSelectorLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        coroutineScope.launch {
            BackupHelper.restoreBackup(context, it, BackupContent.entries) { isSuccess ->
                if (isSuccess) {
                    context.showToast(R.string.data_restored_successfully)
                } else {
                    context.showToast(R.string.could_not_restore_data)
                }
            }
        }
    }

    var languageDialogShown by rememberSaveable { mutableStateOf(false) }
    if (languageDialogShown) {
        val languageCodes = stringArrayResource(R.array.pref_language_codes)
        AlertDialog(
            onDismissRequest = { languageDialogShown = false },
            title = { Text(text = stringResource(R.string.app_language_title)) },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 500.dp)
                ) {
                    items(languageCodes) { tag ->
                        DialogListItemWithRadio(
                            title = if (tag == AUTO_LANGUAGE) {
                                stringResource(R.string.auto_theme_name)
                            } else {
                                tag.languageEndonym()
                            },
                            isSelected = (tag == currentLanguageTag),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                            onClick = {
                                preferences.edit { putString(LANGUAGE_NAME, tag) }
                                AppCompatDelegate.setApplicationLocales(
                                    if (tag == AUTO_LANGUAGE) {
                                        LocaleListCompat.getEmptyLocaleList()
                                    } else {
                                        LocaleListCompat.forLanguageTags(tag)
                                    }
                                )
                                languageDialogShown = false
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { languageDialogShown = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }

    OnboardSurface(
        onBackClick = onBackClick,
        onContinueClick = onFinish,
        title = stringResource(R.string.initial_configuration_title),
        description = stringResource(R.string.initial_configuration_subtitle),
        continueButton = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    FilledTonalIconButton(
                        onClick = { backupSelectorLauncher.launch(MIME_TYPE_APPLICATION) },
                        modifier = Modifier.size(IconButtonDefaults.mediumContainerSize())
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_settings_backup_restore_24dp),
                            contentDescription = stringResource(R.string.restore_from_backup),
                            modifier = Modifier.size(IconButtonDefaults.mediumIconSize)
                        )
                    }
                } else {
                    FilledTonalButton(
                        onClick = { backupSelectorLauncher.launch(MIME_TYPE_APPLICATION) }
                    ) {
                        Text(stringResource(R.string.restore_from_backup))
                    }
                }

                ContinueButton(
                    onClick = onFinish,
                    text = stringResource(R.string.continue_action)
                )
            }
        },
        modifier = modifier
    ) {
        item {
            Text(
                text = stringResource(R.string.appearance_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
            )
        }

        item {
            OnboardPreference(
                onClick = {},
                title = stringResource(R.string.general_theme_title),
                supportingContent = {
                    ButtonGroup(
                        onSelected = { preferences.edit { putString(GENERAL_THEME, it) } },
                        buttonItems = listOf(
                            GeneralTheme.AUTO,
                            GeneralTheme.LIGHT,
                            GeneralTheme.DARK
                        ),
                        buttonStateResolver = { selectedTheme == it },
                        buttonTextResolver = { theme ->
                            when (theme) {
                                GeneralTheme.LIGHT -> stringResource(R.string.light_theme_name)
                                GeneralTheme.DARK -> stringResource(R.string.dark_theme_name)
                                else -> stringResource(R.string.auto_theme_name)
                            }
                        },
                        buttonContentPadding = PaddingValues(
                            horizontal = 4.dp,
                            vertical = 12.dp
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                },
                shapes = ListItemDefaults.segmentedShapes(0, 2)
            )
        }

        item {
            OnboardPreference(
                onClick = { preferences.edit { putBoolean(MATERIAL_YOU, !materialYou) } },
                title = stringResource(R.string.material_you_title),
                trailingContent = {
                    Switch(
                        checked = materialYou,
                        onCheckedChange = null
                    )
                },
                shapes = ListItemDefaults.segmentedShapes(1, 2)
            )
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }

        item {
            Text(
                text = stringResource(R.string.advanced_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
            )
        }

        item {
            OnboardPreference(
                onClick = { languageDialogShown = true },
                leadingIcon = painterResource(R.drawable.ic_translate_24dp),
                title = stringResource(R.string.app_language_title),
                summary = if (currentLanguageTag == AUTO_LANGUAGE) {
                    stringResource(R.string.auto_theme_name)
                } else {
                    currentLanguageTag.languageEndonym()
                },
                trailingContent = {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_forward_24dp),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                shapes = ListItemDefaults.segmentedShapes(0, 3)
            )
        }

        item {
            OnboardPreference(
                onClick = {},
                leadingIcon = painterResource(R.drawable.ic_timer_24dp),
                title = stringResource(R.string.min_song_duration_title),
                supportingContent = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Slider(
                            value = minDuration.toFloat(),
                            onValueChange = {
                                preferences.edit {
                                    putInt(
                                        MINIMUM_SONG_DURATION,
                                        it.roundToInt()
                                    )
                                }
                            },
                            valueRange = 0f..120f,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${minDuration}s",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.widthIn(min = 32.dp)
                        )
                    }
                },
                shapes = ListItemDefaults.segmentedShapes(1, 3)
            )
        }

        item {
            OnboardPreference(
                onClick = {
                    preferences.edit {
                        putBoolean(NetworkFeature.NETWORK_FEATURES_KEY, !networkEnabled)
                    }
                },
                leadingIcon = painterResource(R.drawable.ic_language_24dp),
                title = stringResource(R.string.network_features_title),
                summary = stringResource(R.string.network_features_summary),
                trailingContent = {
                    Switch(
                        checked = networkEnabled,
                        onCheckedChange = null
                    )
                },
                shapes = ListItemDefaults.segmentedShapes(2, 3)
            )
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }

        item {
            Box(Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.initial_configuration_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
private fun OnboardSurface(
    onBackClick: () -> Unit,
    onContinueClick: () -> Unit,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    canGoBack: Boolean = true,
    canGoForward: Boolean = true,
    backButton: @Composable () -> Unit = {
        if (canGoBack) {
            IconButton(
                onClick = onBackClick,
                modifier = modifier
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_back_24dp),
                    contentDescription = stringResource(R.string.back_action),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    },
    continueButton: @Composable () -> Unit = {
        ContinueButton(
            onClick = onContinueClick,
            text = stringResource(R.string.continue_action),
            enabled = canGoForward
        )
    },
    content: LazyListScope.() -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            backButton()
            LazyColumn(
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                item { OnboardHeader(title, description) }
                item { Spacer(modifier = Modifier.height(24.dp)) }
                content()
            }
            continueButton()
        }
    } else {
        val density = LocalDensity.current
        val safeDrawingPadding = WindowInsets.safeDrawing.asPaddingValues(density)

        var buttonHeightInPx by remember { mutableIntStateOf(0) }
        val listPaddingBottom = with(density) { buttonHeightInPx.toDp() } + 32.dp

        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            LazyColumn(
                contentPadding = safeDrawingPadding + PaddingValues(top = 16.dp, bottom = listPaddingBottom),
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    Box(Modifier.align(Alignment.CenterStart)) {
                        backButton()
                    }
                }
                item { Spacer(Modifier.height(12.dp)) }
                item { OnboardHeader(title, description) }
                item { Spacer(Modifier.height(24.dp)) }
                content()
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .safeDrawingPadding()
                    .padding(bottom = 16.dp)
                    .onGloballyPositioned {
                        if (buttonHeightInPx == 0) {
                            buttonHeightInPx = it.size.height
                        }
                    }
                    .align(Alignment.BottomCenter)
            ) {
                continueButton()
            }
        }
    }
}

@Composable
private fun OnboardBackground(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "expressive_background")
    val cookieRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 48000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "cookie_rotation"
    )
    val squircleRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 54000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "squircle_rotation"
    )

    val cookiePolygon = remember {
        RoundedPolygon.star(
            numVerticesPerRadius = 12,
            innerRadius = 0.84f,
            rounding = CornerRounding(radius = 0.35f)
        )
    }
    val cookieComposePath = remember(cookiePolygon) {
        cookiePolygon.toPath().asComposePath()
    }

    val squirclePolygon = remember {
        RoundedPolygon(
            numVertices = 4,
            rounding = CornerRounding(radius = 0.45f, smoothing = 0.9f)
        )
    }
    val squircleComposePath = remember(squirclePolygon) {
        squirclePolygon.toPath().asComposePath()
    }

    val primaryTint = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    val tertiaryTint = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.10f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
    ) {
        Canvas(
            modifier = Modifier
                .size(300.dp)
                .offset(x = (-80).dp, y = (-70).dp)
                .rotate(cookieRotation)
        ) {
            val radius = size.minDimension / 2f
            withTransform({
                translate(left = center.x, top = center.y)
                scale(scaleX = radius, scaleY = radius, pivot = Offset.Zero)
            }) {
                drawPath(
                    path = cookieComposePath,
                    color = primaryTint
                )
            }
        }

        Canvas(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(260.dp)
                .offset(x = 60.dp, y = 60.dp)
                .rotate(squircleRotation)
        ) {
            val radius = size.minDimension / 2f
            withTransform({
                translate(left = center.x, top = center.y)
                scale(scaleX = radius, scaleY = radius, pivot = Offset.Zero)
            }) {
                drawPath(
                    path = squircleComposePath,
                    color = tertiaryTint
                )
            }
        }
    }
}

@Composable
private fun ContinueButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    if (isLandscape) {
        FilledIconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.size(IconButtonDefaults.largeContainerSize())
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_forward_24dp),
                contentDescription = text,
                modifier = Modifier.size(IconButtonDefaults.largeIconSize)
            )
        }
    } else {
        Button(
            onClick = onClick,
            enabled = enabled,
            shape = RoundedCornerShape(50),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            modifier = modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                painter = painterResource(R.drawable.ic_arrow_forward_24dp),
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun OnboardPreference(
    onClick: () -> Unit,
    title: String,
    shapes: ListItemShapes,
    modifier: Modifier = Modifier,
    leadingIcon: Painter? = null,
    supportingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    summary: String? = null
) {
    SegmentedListItem(
        onClick = onClick,
        shapes = shapes,
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        leadingContent = {
            if (leadingIcon != null) {
                Icon(
                    painter = leadingIcon,
                    contentDescription = null
                )
            }
        },
        trailingContent = trailingContent,
        supportingContent = supportingContent,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (!summary.isNullOrEmpty()) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun OnboardHeader(
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    Column(
        verticalArrangement = if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT)
            Arrangement.spacedBy(8.dp)
        else Arrangement.Top,
        modifier = modifier
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
private fun WelcomeAppContent(
    progress: Float,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier
                .size(100.dp)
                .graphicsLayer {
                    alpha = progress.coerceIn(0f, 1f)
                    scaleX = 0.6f + (0.4f * progress)
                    scaleY = 0.6f + (0.4f * progress)
                }
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_booming_music_24dp),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(54.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        Text(
            text = stringResource(R.string.welcome_to),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.graphicsLayer {
                alpha = (progress * 1.3f - 0.2f).coerceIn(0f, 1f)
                translationY = (1f - progress) * 30f
            }
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.graphicsLayer {
                alpha = (progress * 1.5f - 0.3f).coerceIn(0f, 1f)
                translationY = (1f - progress) * 40f
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.app_description),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .graphicsLayer {
                    alpha = (progress * 1.8f - 0.5f).coerceIn(0f, 1f)
                    translationY = (1f - progress) * 50f
                }
        )
    }
}

private data class PermissionItemData(
    val title: Int,
    val description: Int,
    val icon: Int,
    val isGranted: Boolean,
    val onRequest: () -> Unit
)
