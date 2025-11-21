package com.mardous.booming.core.appwidgets.state

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import androidx.glance.state.GlanceStateDefinition
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

object PlaybackStateDefinition : GlanceStateDefinition<PlaybackState> {

    private const val DATASTORE_FILE_NAME = "playback_state_v1.json"

    private val json = Json {
        coerceInputValues = true
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val Context.playbackStateDataStore: DataStore<PlaybackState> by dataStore(
        fileName = DATASTORE_FILE_NAME,
        serializer = PlaybackStateSerializer(json)
    )

    override suspend fun getDataStore(context: Context, fileKey: String): DataStore<PlaybackState> {
        return context.playbackStateDataStore
    }

    override fun getLocation(context: Context, fileKey: String): File {
        return File(context.filesDir, "datastore/$DATASTORE_FILE_NAME")
    }

    private class PlaybackStateSerializer(private val json: Json) : Serializer<PlaybackState> {
        override val defaultValue: PlaybackState
            get() = PlaybackState()

        override suspend fun readFrom(input: InputStream): PlaybackState {
            try {
                val string = input.bufferedReader().use { it.readText() }
                if (string.isBlank()) return defaultValue
                return json.decodeFromString<PlaybackState>(string)
            } catch (e: SerializationException) {
                throw CorruptionException("Cannot read from json", e)
            } catch (e: IOException) {
                throw CorruptionException("Cannot read from json", e)
            }
        }

        override suspend fun writeTo(t: PlaybackState, output: OutputStream) {
            output.bufferedWriter().use {
                it.write(json.encodeToString(t))
            }
        }
    }
}