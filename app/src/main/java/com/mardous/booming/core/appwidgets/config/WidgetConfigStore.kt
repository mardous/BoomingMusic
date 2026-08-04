package com.mardous.booming.core.appwidgets.config

import android.content.Context
import androidx.core.content.edit

object WidgetConfigStore {

    private const val FILE_NAME = "widget_config"

    fun read(context: Context, appWidgetId: Int, settings: List<WidgetSetting>): WidgetConfig {
        val preferences = preferences(context)
        var config = WidgetConfig()
        for (setting in settings) {
            val key = key(appWidgetId, setting)
            config = when (setting) {
                is WidgetSetting.Switch ->
                    setting.write(config, preferences.getBoolean(key, setting.default))

                is WidgetSetting.Choice -> {
                    val stored = preferences.getString(key, null)
                    setting.write(config, setting.values.firstOrNull { it.name == stored } ?: setting.default)
                }
            }
        }
        return config.copy(page = preferences.getInt(pageKey(appWidgetId), 0))
    }

    fun write(context: Context, appWidgetId: Int, settings: List<WidgetSetting>, config: WidgetConfig) {
        preferences(context).edit {
            for (setting in settings) when (setting) {
                is WidgetSetting.Switch -> putBoolean(key(appWidgetId, setting), setting.read(config))
                is WidgetSetting.Choice -> putString(key(appWidgetId, setting), setting.read(config).name)
            }
        }
    }

    fun stepPage(context: Context, appWidgetId: Int, step: Int) {
        val preferences = preferences(context)
        val key = pageKey(appWidgetId)
        preferences.edit { putInt(key, preferences.getInt(key, 0) + step) }
    }

    fun clear(context: Context, appWidgetIds: IntArray) {
        // Remove obsolete setting keys for the deleted widget too
        val preferences = preferences(context)
        val prefixes = appWidgetIds.map { "${it}_" }
        preferences.edit {
            for (key in preferences.all.keys) {
                if (prefixes.any { key.startsWith(it) }) remove(key)
            }
        }
    }

    private fun preferences(context: Context) =
        context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    private fun key(appWidgetId: Int, setting: WidgetSetting) = "${appWidgetId}_${setting.key}"

    private fun pageKey(appWidgetId: Int) = "${appWidgetId}_Page"
}
