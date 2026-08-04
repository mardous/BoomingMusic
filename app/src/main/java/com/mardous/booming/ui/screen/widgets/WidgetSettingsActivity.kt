package com.mardous.booming.ui.screen.widgets

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.lifecycleScope
import com.mardous.booming.core.appwidgets.BoomingWidget
import com.mardous.booming.core.appwidgets.WidgetUpdater
import com.mardous.booming.core.appwidgets.config.WidgetConfig
import com.mardous.booming.core.appwidgets.config.WidgetConfigStore
import com.mardous.booming.core.appwidgets.config.WidgetSetting
import com.mardous.booming.core.appwidgets.config.WidgetValue
import com.mardous.booming.core.appwidgets.widgetsByReceiver
import com.mardous.booming.ui.component.base.AbsThemeActivity
import com.mardous.booming.ui.component.compose.CollapsibleAppBarScaffold
import com.mardous.booming.ui.component.compose.DialogListItemWithRadio
import com.mardous.booming.ui.theme.BoomingMusicTheme
import kotlinx.coroutines.launch

class WidgetSettingsActivity : AbsThemeActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        val info = AppWidgetManager.getInstance(this).getAppWidgetInfo(appWidgetId)
        val widget = widgetsByReceiver[info?.provider?.className]
        if (widget == null) {
            finish()
            return
        }

        // Settings apply as they are changed
        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))

        val stored = WidgetConfigStore.read(this, appWidgetId, widget.settings)

        setContent {
            BoomingMusicTheme {
                CollapsibleAppBarScaffold(
                    title = info.loadLabel(packageManager),
                    onBackClick = { finish() }
                ) { padding ->
                    WidgetSettingsList(
                        settings = widget.settings,
                        stored = stored,
                        onChanged = { config -> apply(widget, appWidgetId, config) },
                        modifier = Modifier.padding(padding)
                    )
                }
            }
        }
    }

    private fun apply(widget: BoomingWidget, appWidgetId: Int, config: WidgetConfig) {
        WidgetConfigStore.write(this, appWidgetId, widget.settings, config)
        lifecycleScope.launch {
            // What a widget collects depends on these settings, so the state is rebuilt first
            WidgetUpdater.refresh(this@WidgetSettingsActivity)
            val glanceId = GlanceAppWidgetManager(this@WidgetSettingsActivity).getGlanceIdBy(appWidgetId)
            WidgetUpdater.render(this@WidgetSettingsActivity, widget, glanceId)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WidgetSettingsList(
    settings: List<WidgetSetting>,
    stored: WidgetConfig,
    onChanged: (WidgetConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    var config by remember { mutableStateOf(stored) }
    var choosing by remember { mutableStateOf<WidgetSetting.Choice?>(null) }

    fun change(next: WidgetConfig) {
        if (next == config) return
        config = next
        onChanged(next)
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
    ) {
        settings.forEachIndexed { index, setting ->
            val shapes = ListItemDefaults.segmentedShapes(index, settings.size)
            when (setting) {
                is WidgetSetting.Switch -> {
                    val checked = setting.read(config)
                    SegmentedListItem(
                        checked = checked,
                        onCheckedChange = { change(setting.write(config, it)) },
                        shapes = shapes,
                        colors = segmentedColors(),
                        verticalAlignment = Alignment.CenterVertically,
                        leadingContent = { SettingIcon(setting.icon) },
                        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
                        supportingContent = { Text(stringResource(setting.summary)) }
                    ) {
                        Text(stringResource(setting.title))
                    }
                }

                is WidgetSetting.Choice -> {
                    val available = setting.isAvailable(config)
                    SegmentedListItem(
                        onClick = { choosing = setting },
                        shapes = shapes,
                        enabled = available,
                        colors = segmentedColors(),
                        verticalAlignment = Alignment.CenterVertically,
                        leadingContent = { SettingIcon(setting.icon) },
                        supportingContent = { Text(stringResource(setting.read(config).label)) }
                    ) {
                        Text(stringResource(setting.title))
                    }
                }
            }
        }
    }

    choosing?.let { setting ->
        ChoiceDialog(
            setting = setting,
            selected = setting.read(config),
            onSelected = { value ->
                change(setting.write(config, value))
                choosing = null
            },
            onDismiss = { choosing = null }
        )
    }
}

@Composable
private fun ChoiceDialog(
    setting: WidgetSetting.Choice,
    selected: WidgetValue,
    onSelected: (WidgetValue) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { SettingIcon(setting.icon) },
        title = { Text(stringResource(setting.title)) },
        text = {
            Column {
                for (value in setting.values) {
                    DialogListItemWithRadio(
                        title = stringResource(value.label),
                        isSelected = value == selected,
                        onClick = { onSelected(value) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun segmentedColors(): ListItemColors = ListItemDefaults.segmentedColors(
    containerColor = MaterialTheme.colorScheme.surfaceContainer
)

@Composable
private fun SettingIcon(icon: Int) {
    Icon(painter = painterResource(icon), contentDescription = null)
}
