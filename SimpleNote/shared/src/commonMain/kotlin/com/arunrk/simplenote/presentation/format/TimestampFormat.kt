package com.arunrk.simplenote.presentation.format

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * Formats a stored timestamp for display.
 *
 * Timestamps travel and are stored in UTC; conversion to the viewer's zone happens here, at
 * the very edge, so nothing below the presentation layer has to care about time zones.
 *
 * Formatted by hand rather than with a locale-aware formatter because there is no single
 * cross-platform one in Kotlin Multiplatform, and a stable unambiguous format beats a
 * different one on each target.
 */
fun Instant.formatForDisplay(timeZone: TimeZone = TimeZone.currentSystemDefault()): String {
    val local = toLocalDateTime(timeZone)
    val day = local.day.toString().padStart(2, '0')
    // Month is an enum here, so its ordinal indexes the name table directly. Using the enum
    // rather than a numeric accessor keeps this compiling across kotlinx-datetime versions,
    // which have moved that property around.
    val month = MONTH_NAMES[local.month.ordinal]
    val hour = local.hour.toString().padStart(2, '0')
    val minute = local.minute.toString().padStart(2, '0')
    return "$day $month ${local.year} · $hour:$minute"
}

private val MONTH_NAMES = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)
