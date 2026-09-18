package com.willfp.ecoshop.util

/** Formats seconds as "1h 5m 3s", omitting zero leading units. */
fun formatDuration(seconds: Long): String {
    val s = seconds.coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return buildList {
        if (h > 0) add("${h}h")
        if (h > 0 || m > 0) add("${m}m")
        add("${sec}s")
    }.joinToString(" ")
}
