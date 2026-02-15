package com.mardous.booming.core.audio

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.mardous.booming.core.model.equalizer.autoeq.AutoEqPoint
import com.mardous.booming.core.model.equalizer.autoeq.AutoEqProfile
import java.io.BufferedReader
import java.io.InputStreamReader

object AutoEqTxtParser {

    private const val GRAPHIC_EQ_PREFIX = "GraphicEQ:"
    private const val NAME_FALLBACK = "AutoEq Preset"

    fun parse(context: Context, uri: Uri): AutoEqProfile? {
        val fileName = getFileName(context, uri) ?: NAME_FALLBACK
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null

        val fullText = BufferedReader(InputStreamReader(inputStream)).use { it.readText() }

        val graphicEqLine = fullText.lines()
            .firstOrNull { it.trim().startsWith(GRAPHIC_EQ_PREFIX, ignoreCase = true) }
            ?: return null

        val dataString = graphicEqLine.substringAfter(GRAPHIC_EQ_PREFIX).trim()
        if (dataString.isEmpty()) {
            return AutoEqProfile(name = fileName, points = emptyList())
        }

        val points = mutableListOf<AutoEqPoint>()
        val pointsString = dataString.split(';')
        for (bandStr in pointsString) {
            val parts = bandStr.trim().split(Regex("\\s+"))
            if (parts.size == 2) {
                try {
                    val frequency = parts[0].toFloat()
                    val gain = parts[1].toFloat()
                    points.add(AutoEqPoint(frequency, gain))
                } catch (_: NumberFormatException) {
                    continue
                }
            }
        }

        return if (points.isNotEmpty()) {
            AutoEqProfile(name = fileName, preamp = 0f, points = points)
        } else {
            null
        }
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        name = it.getString(nameIndex)
                    }
                }
            }
        }
        if (name == null) {
            name = uri.path
            val cut = name?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                name = name.substring(cut + 1)
            }
        }
        return name?.removeSuffix(".txt")?.removeSuffix(".TXT")?.trim()
    }
}