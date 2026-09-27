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

package com.mardous.booming.data.remote

import android.util.Log
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import java.util.concurrent.atomic.AtomicLong

class RateLimiter(private val tag: String, private val defaultDelayInSeconds: Long = 60) {
    private val retryAfterMs = AtomicLong(-1L)

    val isRateLimited: Boolean
        get() {
            val until = retryAfterMs.get()
            val now = System.currentTimeMillis()
            if (until > now) {
                Log.w(tag, "Rate limit exceeded, must wait ${until - now} ms")
                return true
            }
            return false
        }

    fun handleResponse(response: HttpResponse): Boolean {
        if (response.status == HttpStatusCode.TooManyRequests) {
            val retryAfterSeconds = response.headers[HttpHeaders.RetryAfter]
                ?.toLongOrNull() ?: defaultDelayInSeconds
            retryAfterMs.set(System.currentTimeMillis() + (retryAfterSeconds * 1000))
            Log.w(tag, "Rate limit exceeded, retrying in $retryAfterSeconds seconds")
            return true
        }
        return false
    }
}