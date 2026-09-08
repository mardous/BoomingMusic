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

package com.mardous.booming.util

import java.io.InputStream

object FileTypeVerifier {
    private val JPEG_HEADER = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
    private val TTF_HEADER  = byteArrayOf(0x00, 0x01, 0x00, 0x00)
    private val OTF_HEADER  = byteArrayOf(0x4F, 0x54, 0x54, 0x4F) // 'O', 'T', 'T', 'O'

    fun InputStream.isJpegImage(): Boolean {
        val header = ByteArray(3)
        return read(header) == 3 && header.contentEquals(JPEG_HEADER)
    }

    fun InputStream.isFontFile(): Boolean {
        val header = ByteArray(4)
        return read(header) == 4 && (header.contentEquals(TTF_HEADER) || header.contentEquals(OTF_HEADER))
    }
}