/*
 * Copyright (c) 2025 Christians Martínez Alvarado
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

package com.mardous.booming.core.model.about

import android.content.Context
import com.mardous.booming.extensions.readStringFromAsset
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
class Contribution(
    @SerialName("contrib_name")
    val name: String = "",
    @SerialName("contrib_description")
    val description: String? = null,
    @SerialName("contrib_image")
    val image: String? = null,
    @SerialName("contrib_url")
    val url: String? = null
) {

    val imageUrl: String?
        get() = image?.let {
            if (!it.startsWith("https://")) {
                "file:///android_asset/images/$image"
            } else it
        }

    companion object {
        fun loadContributions(context: Context, assetName: String): List<Contribution> {
            return try {
                val json = Json {
                    ignoreUnknownKeys = true
                }
                val contributionsContent = context.readStringFromAsset(assetName)
                if (contributionsContent.isNullOrEmpty()) {
                    return emptyList()
                }
                return json.decodeFromString<List<Contribution>>(contributionsContent)
            } catch (_: Exception) {
                emptyList()
            }
        }
    }
}