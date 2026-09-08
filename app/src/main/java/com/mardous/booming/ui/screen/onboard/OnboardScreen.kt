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

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.LocalContentColor
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.preference.PreferenceManager
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.mardous.booming.R
import com.mardous.booming.data.model.network.NetworkFeature
import com.mardous.booming.extensions.MIME_TYPE_APPLICATION
import com.mardous.booming.extensions.getImagesPermission
import com.mardous.booming.extensions.getNearbyDevicesPermissions
import com.mardous.booming.extensions.getStoragePermissions
import com.mardous.booming.extensions.hasS
import com.mardous.booming.extensions.isLandscape
import com.mardous.booming.extensions.observeKeyAsState
import com.mardous.booming.extensions.openAppDetailsSettings
import com.mardous.booming.extensions.showToast
import com.mardous.booming.ui.component.compose.ButtonGroup
import com.mardous.booming.ui.component.compose.LanguageList
import com.mardous.booming.ui.component.compose.ObserveAsEvent
import com.mardous.booming.ui.component.compose.languageTitle
import com.mardous.booming.ui.component.compose.rememberLanguageEntries
import com.mardous.booming.util.AUTO_LANGUAGE
import com.mardous.booming.util.GENERAL_THEME
import com.mardous.booming.util.GeneralTheme
import com.mardous.booming.util.LANGUAGE_NAME
import com.mardous.booming.util.MATERIAL_YOU
import com.mardous.booming.util.Preferences
import org.koin.androidx.compose.koinViewModel
import kotlin.math.roundToInt

private const val PLACEHOLDER = "%s"

enum class OnboardStep {
    WELCOME,
    PERMISSIONS,
    CONFIGURATION
}

@Composable
fun OnboardScreen(
    onFinish: () -> Unit,
    onBackToExit: () -> Unit,
    onRestartRequired: () -> Unit,
    modifier: Modifier = Modifier,
    availableSteps: List<OnboardStep> = OnboardStep.entries
) {
    check(availableSteps.isNotEmpty()) { "OnboardScreen needs at least one step" }
    var currentStep by rememberSaveable { mutableStateOf(availableSteps.first()) }

    fun goToStep(step: OnboardStep, onStepNotAvailable: () -> Unit = onFinish) {
        if (step in availableSteps) {
            currentStep = step
        } else {
            onStepNotAvailable()
        }
    }

    BackHandler {
        when (currentStep) {
            OnboardStep.CONFIGURATION -> goToStep(OnboardStep.PERMISSIONS, onBackToExit)
            OnboardStep.PERMISSIONS -> goToStep(OnboardStep.WELCOME, onBackToExit)
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
                            canGoBack = availableSteps.any { it < OnboardStep.PERMISSIONS },
                            onNextClick = { goToStep(OnboardStep.CONFIGURATION) },
                            onBackClick = { goToStep(OnboardStep.WELCOME, onBackToExit) }
                        )
                    }

                    OnboardStep.CONFIGURATION -> {
                        ConfigurationStepContent(
                            onFinish = onFinish,
                            onBackClick = { goToStep(OnboardStep.PERMISSIONS, onBackToExit) },
                            onRestartRequired = onRestartRequired
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

    val progress = { animState.value }
    val isLandscape = LocalConfiguration.current.isLandscape

    val startButton: @Composable () -> Unit = {
        ContinueButton(
            onClick = onNextClick,
            text = stringResource(R.string.get_started),
            modifier = Modifier.graphicsLayer {
                alpha = (progress() * 2f - 0.8f).coerceIn(0f, 1f)
                val offset = (1f - progress()) * 60f
                if (isLandscape) translationX = offset else translationY = offset
            }
        )
    }

    if (isLandscape) {
        Row(
            modifier = modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WelcomeAppContent(progress = progress, modifier = Modifier.weight(1f))
            startButton()
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
            WelcomeAppContent(progress = progress, modifier = Modifier.weight(1f))
            startButton()
        }
    }
}

@Composable
private fun WelcomeAppContent(
    progress: () -> Float,
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
                    alpha = progress().coerceIn(0f, 1f)
                    scaleX = 0.6f + (0.4f * progress())
                    scaleY = 0.6f + (0.4f * progress())
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

        // Split, not formatted: not every language puts the app name last (ja: "%s へようこそ").
        val appName = stringResource(R.string.app_name)
        val parts = stringResource(R.string.welcome_to_x).split(PLACEHOLDER, limit = 2)
        val beforeName = parts[0].trim()
        val afterName = parts.getOrElse(1) { "" }.trim()

        if (beforeName.isNotEmpty()) {
            Text(
                text = beforeName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer {
                    alpha = (progress() * 1.3f - 0.2f).coerceIn(0f, 1f)
                    translationY = (1f - progress()) * 30f
                }
            )

            Spacer(modifier = Modifier.height(6.dp))
        }

        Text(
            text = appName,
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.graphicsLayer {
                alpha = (progress() * 1.5f - 0.3f).coerceIn(0f, 1f)
                translationY = (1f - progress()) * 40f
            }
        )

        if (afterName.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = afterName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer {
                    alpha = (progress() * 1.5f - 0.3f).coerceIn(0f, 1f)
                    translationY = (1f - progress()) * 40f
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.app_description),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .graphicsLayer {
                    alpha = (progress() * 1.8f - 0.5f).coerceIn(0f, 1f)
                    translationY = (1f - progress()) * 50f
                }
        )
    }
}

@Composable
private fun PermissionsStepContent(
    canGoBack: Boolean,
    onNextClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val storageItem = permissionItem(
        permissions = remember { getStoragePermissions().toList() },
        title = R.string.permission_external_storage_title,
        description = R.string.permission_external_storage_description,
        icon = R.drawable.ic_sd_card_24dp
    )

    val permissionItems = listOfNotNull(
        storageItem,
        permissionItem(
            permissions = getNearbyDevicesPermissions().toList(),
            title = R.string.permission_bluetooth_title,
            description = R.string.permission_bluetooth_description,
            icon = R.drawable.ic_bluetooth_connected_24dp
        ),
        permissionItem(
            permissions = getImagesPermission().toList(),
            title = R.string.permission_read_images_title,
            description = R.string.permission_read_images_summary,
            icon = R.drawable.ic_image_24dp
        )
    )

    OnboardSurface(
        onBackClick = onBackClick,
        onContinueClick = onNextClick,
        canGoBack = canGoBack,
        canGoForward = storageItem?.isGranted == true,
        title = stringResource(R.string.permissions_needed),
        description = stringResource(R.string.permissions_subtitle),
        modifier = modifier
    ) {
        itemsIndexed(permissionItems) { index, item ->
            val onAction = {
                if (item.isDeniedForever) context.openAppDetailsSettings() else item.onRequest()
            }
            OnboardPreference(
                title = stringResource(item.title),
                summary = stringResource(item.description),
                shapes = ListItemDefaults.segmentedShapes(index, permissionItems.size),
                onClick = if (item.isGranted) null else onAction,
                leadingIcon = painterResource(item.icon),
                leadingIconTint = if (item.isGranted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                trailingContent = {
                    if (item.isGranted) {
                        GrantedBadge()
                    } else {
                        Button(
                            onClick = onAction,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Text(
                                text = if (item.isDeniedForever) {
                                    stringResource(R.string.settings_title)
                                } else {
                                    stringResource(R.string.grant_access_action)
                                },
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun GrantedBadge() {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_check_24dp),
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = stringResource(R.string.granted),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun permissionItem(
    permissions: List<String>,
    @StringRes title: Int,
    @StringRes description: Int,
    @DrawableRes icon: Int
): PermissionItemData? {
    if (permissions.isEmpty()) return null
    val state = rememberMultiplePermissionsState(permissions)
    // Persisted: onboarding relaunches on every cold start without access, and a per-activity
    // flag would make a permanently denied permission look like one never asked for.
    val key = permissions.first()
    var requested by remember(key) { mutableStateOf(key in Preferences.requestedPermissions) }
    return PermissionItemData(
        title = title,
        description = description,
        icon = icon,
        isGranted = state.allPermissionsGranted,
        // Android stops showing the dialog once denied twice; app settings is the only route.
        isDeniedForever = requested && !state.allPermissionsGranted && !state.shouldShowRationale,
        onRequest = {
            Preferences.requestedPermissions += key
            requested = true
            state.launchMultiplePermissionRequest()
        }
    )
}

private data class PermissionItemData(
    @param:StringRes val title: Int,
    @param:StringRes val description: Int,
    @param:DrawableRes val icon: Int,
    val isGranted: Boolean,
    val isDeniedForever: Boolean,
    val onRequest: () -> Unit
)

@Composable
private fun ConfigurationStepContent(
    onFinish: () -> Unit,
    onBackClick: () -> Unit,
    onRestartRequired: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val preferences = remember { PreferenceManager.getDefaultSharedPreferences(context) }

    val selectedTheme by preferences.observeKeyAsState(GENERAL_THEME, GeneralTheme.AUTO)
    val materialYou by preferences.observeKeyAsState(MATERIAL_YOU, hasS())

    val networkEnabledByDefault = booleanResource(R.bool.network_features_enabled_by_default)
    val networkEnabled by preferences.observeKeyAsState(NetworkFeature.NETWORK_FEATURES_KEY, networkEnabledByDefault)
    val currentLanguageTag by preferences.observeKeyAsState(LANGUAGE_NAME, AUTO_LANGUAGE)

    val backupSelectorLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let(viewModel::restoreBackup)
    }

    ObserveAsEvent(viewModel.restoreFailedEvent) {
        context.showToast(R.string.could_not_restore_data)
    }

    val restoreState by viewModel.restoreState.collectAsStateWithLifecycle()
    RestoreDialog(state = restoreState, onRestart = onRestartRequired)

    var languageDialogShown by rememberSaveable { mutableStateOf(false) }
    if (languageDialogShown) {
        LanguagePickerDialog(
            currentTag = currentLanguageTag,
            onDismiss = { languageDialogShown = false },
            onSelected = { tag ->
                preferences.edit { putString(LANGUAGE_NAME, tag) }
                AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.forLanguageTags(tag.takeIf { it != AUTO_LANGUAGE })
                )
                languageDialogShown = false
            }
        )
    }

    OnboardSurface(
        onBackClick = onBackClick,
        onContinueClick = onFinish,
        title = stringResource(R.string.initial_configuration_title),
        description = stringResource(R.string.initial_configuration_subtitle),
        secondaryAction = {
            RestoreBackupButton(onClick = { backupSelectorLauncher.launch(MIME_TYPE_APPLICATION) })
        },
        modifier = modifier
    ) {
        sectionHeader(R.string.appearance_title)

        item {
            OnboardPreference(
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
                        buttonContentPadding = PaddingValues(horizontal = 4.dp, vertical = 12.dp),
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
                title = stringResource(R.string.material_you_title),
                checked = materialYou,
                onCheckedChange = { preferences.edit { putBoolean(MATERIAL_YOU, it) } },
                shapes = ListItemDefaults.segmentedShapes(1, 2)
            )
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }

        sectionHeader(R.string.advanced_title)

        item {
            OnboardPreference(
                onClick = { languageDialogShown = true },
                leadingIcon = painterResource(R.drawable.ic_translate_24dp),
                title = stringResource(R.string.app_language_title),
                summary = context.languageTitle(currentLanguageTag),
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
                leadingIcon = painterResource(R.drawable.ic_timer_24dp),
                title = stringResource(R.string.min_song_duration_title),
                supportingContent = { MinDurationSlider() },
                shapes = ListItemDefaults.segmentedShapes(1, 3)
            )
        }

        item {
            OnboardPreference(
                checked = networkEnabled,
                onCheckedChange = {
                    preferences.edit { putBoolean(NetworkFeature.NETWORK_FEATURES_KEY, it) }
                },
                leadingIcon = painterResource(R.drawable.ic_language_24dp),
                title = stringResource(R.string.network_features_title),
                summary = stringResource(R.string.network_features_summary),
                shapes = ListItemDefaults.segmentedShapes(2, 3)
            )
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }

        item {
            Text(
                text = stringResource(R.string.initial_configuration_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun LazyListScope.sectionHeader(@StringRes text: Int) = item {
    Text(
        text = stringResource(text),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
    )
}

@Composable
private fun RestoreBackupButton(onClick: () -> Unit) {
    if (LocalConfiguration.current.isLandscape) {
        FilledTonalIconButton(
            onClick = onClick,
            modifier = Modifier.size(IconButtonDefaults.mediumContainerSize())
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_settings_backup_restore_24dp),
                contentDescription = stringResource(R.string.restore_from_backup),
                modifier = Modifier.size(IconButtonDefaults.mediumIconSize)
            )
        }
    } else {
        FilledTonalButton(onClick = onClick) {
            Text(stringResource(R.string.restore_from_backup))
        }
    }
}

@Composable
private fun MinDurationSlider() {
    var draggedDuration by remember {
        mutableFloatStateOf(Preferences.minimumSongDuration.toFloat())
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Slider(
            value = draggedDuration,
            onValueChange = { draggedDuration = it },
            onValueChangeFinished = {
                val seconds = draggedDuration.roundToInt()
                if (seconds != Preferences.minimumSongDuration) {
                    Preferences.minimumSongDuration = seconds
                }
            },
            valueRange = 0f..120f,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "${draggedDuration.roundToInt()}s",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.widthIn(min = 32.dp)
        )
    }
}

@Composable
private fun RestoreDialog(state: RestoreState, onRestart: () -> Unit) {
    when (state) {
        RestoreState.Restoring -> AlertDialog(
            onDismissRequest = {},
            title = { Text(text = stringResource(R.string.restore_from_backup)) },
            text = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator()
                    Text(text = stringResource(R.string.restoring_data))
                }
            },
            confirmButton = {}
        )

        RestoreState.Restored -> AlertDialog(
            onDismissRequest = {},
            title = { Text(text = stringResource(R.string.data_restored_successfully)) },
            text = { Text(text = stringResource(R.string.restart_app_message)) },
            confirmButton = {
                TextButton(onClick = onRestart) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        )

        RestoreState.Idle -> Unit
    }
}

@Composable
private fun LanguagePickerDialog(
    currentTag: String,
    onDismiss: () -> Unit,
    onSelected: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.app_language_title)) },
        text = {
            LanguageList(
                entries = rememberLanguageEntries(),
                selectedTag = currentTag,
                onSelected = { onSelected(it.tag) },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                modifier = Modifier.heightIn(max = 500.dp)
            )
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
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
    secondaryAction: @Composable () -> Unit = {},
    content: LazyListScope.() -> Unit
) {
    val isLandscape = LocalConfiguration.current.isLandscape

    val body: LazyListScope.() -> Unit = {
        if (!isLandscape) {
            if (canGoBack) {
                item { BackButton(onBackClick) }
            }
            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
        item { OnboardHeader(title, description) }
        item { Spacer(modifier = Modifier.height(24.dp)) }
        content()
    }
    val actions: @Composable () -> Unit = {
        secondaryAction()
        ContinueButton(
            onClick = onContinueClick,
            text = stringResource(R.string.continue_action),
            enabled = canGoForward
        )
    }

    if (isLandscape) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            if (canGoBack) {
                BackButton(onBackClick)
            }
            LazyColumn(
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                content = body
            )
            actions()
        }
    } else {
        val density = LocalDensity.current
        val safeDrawingPadding = WindowInsets.safeDrawing.asPaddingValues(density)

        var buttonHeightInPx by remember { mutableIntStateOf(0) }
        val listPaddingBottom = with(density) { buttonHeightInPx.toDp() } + 32.dp
        // PaddingValues.plus has no equals, so an unremembered result re-measures the list.
        val listPadding = remember(safeDrawingPadding, listPaddingBottom) {
            safeDrawingPadding + PaddingValues(top = 16.dp, bottom = listPaddingBottom)
        }

        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            LazyColumn(
                contentPadding = listPadding,
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
                modifier = Modifier.fillMaxWidth(),
                content = body
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
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
                actions()
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
                // Read inside the layer block so the rotation never recomposes this screen.
                .graphicsLayer { rotationZ = cookieRotation }
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
                .graphicsLayer { rotationZ = squircleRotation }
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
    if (LocalConfiguration.current.isLandscape) {
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
                .heightIn(min = 56.dp)
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
private fun BackButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            painter = painterResource(R.drawable.ic_back_24dp),
            contentDescription = stringResource(R.string.back_action),
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun OnboardPreference(
    title: String,
    shapes: ListItemShapes,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    checked: Boolean? = null,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    leadingIcon: Painter? = null,
    leadingIconTint: Color = LocalContentColor.current,
    supportingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = checked?.let {
        { Switch(checked = it, onCheckedChange = null) }
    },
    summary: String? = null
) {
    val colors = ListItemDefaults.segmentedColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
    val leading: @Composable () -> Unit = {
        if (leadingIcon != null) {
            Icon(painter = leadingIcon, contentDescription = null, tint = leadingIconTint)
        }
    }
    val content: @Composable () -> Unit = {
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

    when {
        checked != null && onCheckedChange != null -> SegmentedListItem(
            checked = checked,
            onCheckedChange = onCheckedChange,
            shapes = shapes,
            colors = colors,
            leadingContent = leading,
            trailingContent = trailingContent,
            supportingContent = supportingContent,
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier,
            content = content
        )

        onClick != null -> SegmentedListItem(
            onClick = onClick,
            shapes = shapes,
            colors = colors,
            leadingContent = leading,
            trailingContent = trailingContent,
            supportingContent = supportingContent,
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier,
            content = content
        )

        else -> SegmentedListItem(
            shapes = shapes,
            colors = colors,
            leadingContent = leading,
            trailingContent = trailingContent,
            supportingContent = supportingContent,
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier,
            content = content
        )
    }
}

@Composable
private fun OnboardHeader(
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = if (!LocalConfiguration.current.isLandscape)
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
