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
import com.mardous.booming.R
import com.mardous.booming.data.remote.github.model.GitHubRelease
import com.mardous.booming.data.remote.github.model.GitHubTreeResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlin.time.ExperimentalTime

class GitHubService(private val context: Context, private val client: HttpClient, private val authToken: String? = null) {

    private suspend fun get(url: String): HttpResponse {
        return client.get(url) {
            authToken?.let {
                headers { append("Authorization", "token $it") }
            }
        }
    }

    private suspend fun fetchStableRelease(user: String, repo: String): GitHubRelease =
        get("${GITHUB_API_URL}repos/$user/$repo/releases/latest").body()

    private suspend fun fetchAllReleases(user: String, repo: String, page: Int = 1, limit: Int = 20): List<GitHubRelease> =
        get("${GITHUB_API_URL}repos/$user/$repo/releases?page=$page&per_page=$limit").body()

    @OptIn(ExperimentalTime::class)
    suspend fun latestRelease(user: String = DEFAULT_USER, repo: String = DEFAULT_REPO, allowExperimental: Boolean = true): GitHubRelease {
        val updaterEnabled = context.resources.getBoolean(R.bool.enable_builtin_updater)
        if (!updaterEnabled) return GitHubRelease("", "", "", "", "", false, emptyList())

        val stableRelease = fetchStableRelease(user, repo)
        if (stableRelease.hasApk && stableRelease.isNewer(context)) {
            return stableRelease
        }
        if (allowExperimental) {
            val allReleases = fetchAllReleases(user, repo)
                .filter { it.isPrerelease }
                .sortedByDescending { it.publishedAt }
            return allReleases.firstOrNull()
                ?: stableRelease
        }
        return stableRelease
    }

    suspend fun fetchTree(
        owner: String,
        repo: String,
        branch: String
    ): GitHubTreeResponse {
        val url = "${GITHUB_API_URL}repos/$owner/$repo/git/trees/$branch?recursive=1"
        return client.get(url).body()
    }

    suspend fun fetchRawFile(
        owner: String,
        repo: String,
        branch: String,
        path: String
    ): String {
        val url = "${GITHUB_CONTENT_URL}$owner/$repo/$branch/$path"
        return client.get(url).bodyAsText()
    }

    companion object {
        private const val GITHUB_API_URL = "https://api.github.com/"
        private const val GITHUB_CONTENT_URL = "https://raw.githubusercontent.com/"

        private const val DEFAULT_USER = "mardous"
        private const val DEFAULT_REPO = "BoomingMusic"
    }
}