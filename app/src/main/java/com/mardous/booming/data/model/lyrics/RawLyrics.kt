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

package com.mardous.booming.data.model.lyrics

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
sealed interface RawLyrics : Parcelable {

    val lyrics: String?

    fun accept(other: RawLyrics?): RawLyrics

    @Parcelize
    @Serializable
    data class Embedded(
        override val lyrics: String?
    ) : RawLyrics {

        val hasLyrics get() = !lyrics.isNullOrEmpty()

        override fun accept(other: RawLyrics?): Embedded {
            if (other is Embedded && !hasLyrics && other.hasLyrics) {
                return this.copy(lyrics = other.lyrics)
            }
            return this
        }

    }

    @Parcelize
    @Serializable
    data class Remote(
        val plain: Content? = null,
        val synced: Content? = null,
        private val instrumental: Boolean = false
    ) : RawLyrics {

        val hasPlain get() = plain?.isEmpty == false
        val hasSynced get() = synced?.isEmpty == false
        val hasBoth get() = hasPlain && hasSynced

        override val lyrics: String?
            get() = synced?.lyrics ?: plain?.lyrics

        override fun accept(other: RawLyrics?): Remote {
            if (other is Remote) {
                return accept(other.plain, other.synced)
            }
            return this
        }

        fun accept(plain: Content?, synced: Content?): Remote {
            var result = this
            if (!hasPlain && plain?.isEmpty == false) {
                result = result.copy(plain = plain)
            }
            if (!hasSynced && synced?.isEmpty == false) {
                result = result.copy(synced = synced)
            }
            return result
        }

        fun prepareToStore(): Stored? {
            val contentToStore = synced ?: plain
            if (contentToStore != null) {
                return Stored(
                    lyrics = contentToStore.lyrics,
                    provider = contentToStore.source,
                    instrumental = instrumental
                )
            }
            return null
        }

        @Parcelize
        @Serializable
        class Content(
            val source: String? = null,
            val lyrics: String? = null
        ) : Parcelable {
            val isEmpty get() = lyrics.isNullOrEmpty()
        }
    }

    @Parcelize
    @Serializable
    data class File(
        val file: LyricsFile,
        override val lyrics: String
    ) : RawLyrics {
        override fun accept(other: RawLyrics?): File = this
    }

    @Parcelize
    @Serializable
    data class Stored(
        override val lyrics: String? = null,
        val provider: String? = null,
        val instrumental: Boolean = false
    ) : RawLyrics {
        override fun accept(other: RawLyrics?): RawLyrics = this

    }

    @Parcelize
    @Serializable
    data class Edited(
        val originalLyrics: RawLyrics,
        val newContent: String,
        val newContentProvider: String? = null,
        val instrumental: Boolean = false
    ) : RawLyrics {
        override val lyrics: String get() = newContent
        override fun accept(other: RawLyrics?): RawLyrics = this
    }
}