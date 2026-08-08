package com.mardous.booming.core.model.about

import android.content.Context
import com.mardous.booming.extensions.readStringFromAsset
import kotlinx.serialization.json.Json

fun loadTranslators(context: Context): Map<String, String> = try {
    Json.decodeFromString(context.readStringFromAsset("translators.json").orEmpty())
} catch (_: Exception) {
    emptyMap()
}
