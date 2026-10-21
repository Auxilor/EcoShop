package com.willfp.ecoshop.util

import com.willfp.ecoshop.plugin

/** The lang `infinite` symbol shown for unlimited values. */
val infiniteDisplay: String
    get() = plugin.langYml.getFormattedStringOrNull("infinite") ?: "∞"

/** This limit for display, with [Int.MAX_VALUE] (unset) shown as [infiniteDisplay]. */
fun Int.toLimitDisplay(): String = if (this == Int.MAX_VALUE) infiniteDisplay else toString()
