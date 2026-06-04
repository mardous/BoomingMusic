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
package com.mardous.booming.data.remote.github

import android.content.Context
import com.mardous.booming.data.remote.github.model.GitHubRelease
import io.ktor.client.HttpClient

class GitHubService(
    private val context: Context,
    private val client: HttpClient,
    private val authToken: String? = null
) {
    suspend fun latestRelease(
        user: String = "",
        repo: String = "",
        allowExperimental: Boolean = true
    ): GitHubRelease {
        throw UnsupportedOperationException()
    }
}