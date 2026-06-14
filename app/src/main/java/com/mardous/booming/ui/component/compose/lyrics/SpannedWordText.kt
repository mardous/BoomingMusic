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

package com.mardous.booming.ui.component.compose.lyrics

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import com.mardous.booming.data.model.lyrics.SyncedLyrics

@Composable
fun SpannedWordText(
    selectedLine: Boolean,
    shadowEffect: Boolean,
    currentMillis: Long,
    syllables: List<SyncedLyrics.Word>,
    contentColor: Color,
    style: TextStyle,
    align: TextAlign,
    modifier: Modifier = Modifier
) {
    // Original code from Metrolist (https://github.com/mostafaalagamy/Metrolist)
    val styledText = buildAnnotatedString {
        syllables.forEach { word ->
            val wordStartMs = word.startMillis
            val wordEndMs = word.endMillis
            val wordDuration = word.durationMillis

            val isWordActive = selectedLine && currentMillis >= wordStartMs && currentMillis <= wordEndMs
            val hasWordPassed = selectedLine && currentMillis > wordEndMs

            val fadeProgress = if (isWordActive && wordDuration > 0) {
                val timeElapsed = currentMillis - wordStartMs
                val linear = (timeElapsed.toFloat() / wordDuration.toFloat()).coerceIn(0f, 1f)
                // Smooth cubic easing
                linear * linear * (3f - 2f * linear)
            } else if (hasWordPassed) 1f else 0f

            val wordAlpha = when {
                !selectedLine -> 0.5f
                hasWordPassed -> 1f
                isWordActive -> 0.4f + (0.6f * fadeProgress)
                else -> 0.4f
            }
            val wordShadow = when {
                shadowEffect && isWordActive && fadeProgress > 0.2f -> Shadow(
                    color = contentColor.copy(alpha = 0.35f * fadeProgress),
                    offset = Offset.Zero,
                    blurRadius = 10f * fadeProgress
                )
                shadowEffect && hasWordPassed -> Shadow(
                    color = contentColor.copy(alpha = 0.15f),
                    offset = Offset.Zero,
                    blurRadius = 6f
                )
                else -> null
            }
            val wordColor = contentColor.copy(alpha = wordAlpha)

            withStyle(
                style = SpanStyle(
                    color = wordColor,
                    shadow = wordShadow
                )
            ) {
                append(word.content)
            }
        }
    }
    Text(
        text = styledText,
        style = style,
        textAlign = align,
        modifier = modifier
    )
}