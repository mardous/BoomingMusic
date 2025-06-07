/*
 * Copyright (c) 2024 Christians Martínez Alvarado
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

package com.mardous.booming.extensions.utilities

import android.content.Context
import com.mardous.booming.R
import com.mardous.booming.util.PlaylistCutoff
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar

private const val MS_PER_MINUTE = (60 * 1000).toLong()
private const val MS_PER_DAY = 24 * 60 * MS_PER_MINUTE

val calendarSingleton: Calendar by lazy {
    Calendar.getInstance()
}

/**
 * Returns the time elapsed so far today in milliseconds.
 *
 * @return Time elapsed today in milliseconds.
 */
fun Calendar.getElapsedToday(): Long {
    // Time elapsed so far today
    return (get(Calendar.HOUR_OF_DAY) * 60 + get(Calendar.MINUTE)) * MS_PER_MINUTE + get(Calendar.SECOND) * 1000 + get(
        Calendar.MILLISECOND
    )
}

/**
 * Returns the time elapsed so far last N days in milliseconds.
 *
 * @return Time elapsed since N days in milliseconds.
 */
fun Calendar.getElapsedDays(numDays: Int): Long {
    var elapsed = getElapsedToday()
    elapsed += numDays * MS_PER_DAY
    return elapsed
}

/**
 * Returns the time elapsed so far this week in milliseconds.
 *
 * @return Time elapsed this week in milliseconds.
 */
fun Calendar.getElapsedWeek(): Long {
    // Today + days passed this week
    var elapsed = getElapsedToday()
    val passedWeekdays = get(Calendar.DAY_OF_WEEK) - 1 - firstDayOfWeek
    if (passedWeekdays > 0) {
        elapsed += passedWeekdays * MS_PER_DAY
    }
    return elapsed
}

/**
 * Returns the time elapsed so far this month in milliseconds.
 *
 * @return Time elapsed this month in milliseconds.
 */
fun Calendar.getElapsedMonth(): Long {
    // Today + rest of this month
    return getElapsedToday() + (get(Calendar.DAY_OF_MONTH) - 1) * MS_PER_DAY
}

/**
 * Returns the time elapsed so far this month and the last numMonths months in milliseconds.
 *
 * @param numMonths Additional number of months prior to the current month to calculate.
 * @return Time elapsed this month and the last numMonths months in milliseconds.
 */
fun Calendar.getElapsedMonths(numMonths: Int): Long {
    // Today + rest of this month
    var elapsed = getElapsedMonth()

    // Previous numMonths months
    var month = get(Calendar.MONTH)
    var year = get(Calendar.YEAR)
    for (i in 0 until numMonths) {
        month--
        if (month < Calendar.JANUARY) {
            month = Calendar.DECEMBER
            year--
        }
        elapsed += getDaysInMonth(year, month) * MS_PER_DAY
    }
    return elapsed
}

/**
 * Returns the time elapsed so far this year in milliseconds.
 *
 * @return Time elapsed this year in milliseconds.
 */
fun Calendar.getElapsedYear(): Long {
    // Today + rest of this month + previous months until January
    var elapsed = getElapsedMonth()
    var month = get(Calendar.MONTH) - 1
    val year = get(Calendar.YEAR)
    while (month > Calendar.JANUARY) {
        elapsed += getDaysInMonth(year, month) * MS_PER_DAY
        month--
    }
    return elapsed
}

/**
 * Gets the number of days for the given month in the given year.
 *
 * @param year  The year.
 * @param month The month (1 - 12).
 * @return The days in that month/year.
 */
private fun getDaysInMonth(year: Int, month: Int): Int {
    val monthCal = GregorianCalendar(year, month, 1)
    return monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
}

fun Calendar.getCutoffTimeMillis(cutoff: String): Long {
    val interval: Long = when (cutoff) {
        PlaylistCutoff.TODAY -> getElapsedToday()
        PlaylistCutoff.YESTERDAY -> getElapsedDays(1)
        PlaylistCutoff.THIS_WEEK -> getElapsedWeek()
        PlaylistCutoff.PAST_THREE_MONTHS -> getElapsedMonths(3)
        PlaylistCutoff.THIS_YEAR -> getElapsedYear()
        PlaylistCutoff.THIS_MONTH -> getElapsedMonth()
        else -> getElapsedMonth()
    }
    return System.currentTimeMillis() - interval
}

/**
 * Formats the given milliseconds into an human readable date-and-time
 * format. The result will depend on the current system's locale.
 *
 * @param timeMillis The time that you want to format (in milliseconds).
 * @return The formatted date/time string.
 */
fun Context.dateStr(timeMillis: Long): String {
    if (timeMillis <= -1) {
        return getString(R.string.label_never)
    }
    return Date(timeMillis).format(this)
}

fun Date.format(context: Context): String {
    val resLocale = context.resources.configuration.locales[0]
    return DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, resLocale).format(this)
}