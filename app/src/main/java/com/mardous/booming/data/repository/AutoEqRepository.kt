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

package com.mardous.booming.data.repository

import android.util.Log
import com.mardous.booming.core.audio.AutoEqParser
import com.mardous.booming.core.model.equalizer.autoeq.AutoEqProfile
import com.mardous.booming.core.model.equalizer.autoeq.AutoEqSyncState
import com.mardous.booming.data.local.room.AutoEqDao
import com.mardous.booming.data.local.room.AutoEqEntity
import com.mardous.booming.data.remote.github.GitHubService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

interface AutoEqRepository {
    fun syncAutoEqDatabase(): Flow<AutoEqSyncState>
    suspend fun searchHeadphones(query: String): List<AutoEqEntity>
    suspend fun loadAutoEqProfile(entity: AutoEqEntity): AutoEqProfile?
    suspend fun getRemoteProfilesCount(): Int
}

class RealAutoEqRepository(
    private val gitHubService: GitHubService,
    private val autoEqDao: AutoEqDao
) : AutoEqRepository {

    override fun syncAutoEqDatabase(): Flow<AutoEqSyncState> = flow {
        try {
            emit(AutoEqSyncState.Syncing())

            val treeResponse = gitHubService.fetchTree(AUTOEQ_REPO_AUTHOR, AUTOEQ_REPO_NAME, AUTOEQ_BRANCH)
            val nodeTree = treeResponse.tree.filter { node ->
                node.type == "blob" &&
                        node.path.startsWith("results/") &&
                        node.path.endsWith(GRAPHIC_EQ_FILE_SUFFIX)
            }

            val treeSize = nodeTree.size
            val entities = mutableListOf<AutoEqEntity>()
            nodeTree.forEachIndexed { index, node ->
                val entity = parseEntityFromPath(node.path)
                if (entity != null) entities.add(entity)

                emit(AutoEqSyncState.Syncing(index + 1, treeSize))
            }

            if (entities.isNotEmpty()) {
                autoEqDao.clearAll()
                autoEqDao.insertAll(entities)

                emit(AutoEqSyncState.Success(entities.size))
            } else {
                emit(AutoEqSyncState.Error("No AutoEq profiles found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync AutoEq database", e)
            emit(AutoEqSyncState.Error(e.message))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun searchHeadphones(query: String): List<AutoEqEntity> = withContext(Dispatchers.IO) {
        autoEqDao.search(query)
    }

    override suspend fun loadAutoEqProfile(entity: AutoEqEntity): AutoEqProfile? = withContext(Dispatchers.IO) {
        try {
            val content = gitHubService.fetchRawFile(AUTOEQ_REPO_AUTHOR, AUTOEQ_REPO_NAME, AUTOEQ_BRANCH, entity.path)
            AutoEqParser.parseContent(entity.label, content)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load AutoEq profile for ${entity.label}", e)
            null
        }
    }

    override suspend fun getRemoteProfilesCount(): Int {
        return autoEqDao.count()
    }

    private fun parseEntityFromPath(path: String): AutoEqEntity? {
        val parts = path.split("/")
        // Expected: results/{source}/{formRig}/{headphoneName}/{filename}
        if (parts.size < 5) return null

        val source = parts[1]
        val formRigDir = parts[2]
        val headphoneName = parts[3]

        val (rig, form) = parseFormAndRig(formRigDir)

        return AutoEqEntity(
            label = headphoneName,
            source = source,
            rig = rig,
            form = form,
            modelName = normalizeModelName(headphoneName),
            path = path
        )
    }

    private fun parseFormAndRig(formRig: String): Pair<String, String> {
        val formKeywords = listOf("in-ear", "over-ear", "earbud")
        val foundForm = formKeywords.firstOrNull {
            formRig.contains(it, ignoreCase = true)
        } ?: "unknown"

        val rig = formRig.replace(foundForm, "", ignoreCase = true).trim()
        return Pair(rig.ifEmpty { "unknown" }, foundForm)
    }

    private fun normalizeModelName(modelName: String): String {
        return modelName.replace(Regex("""\s*\([^)]*\)\s*"""), "").trim()
    }

    companion object {
        private const val TAG = "AutoEqRepository"

        private const val GRAPHIC_EQ_FILE_SUFFIX = " ${AutoEqParser.GRAPHIC_EQ}.txt"

        private const val AUTOEQ_REPO_AUTHOR = "mardous"
        private const val AUTOEQ_REPO_NAME = "AutoEq"
        private const val AUTOEQ_BRANCH = "master"
    }
}
