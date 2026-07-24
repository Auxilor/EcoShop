package com.willfp.ecoshop.shop.rotation

import com.willfp.eco.core.placeholder.DynamicPlaceholder
import com.willfp.ecoshop.plugin
import com.willfp.ecoshop.shop.ShopCategories
import java.util.regex.Pattern

object RotationPlaceholders {
    private val placeholder = DynamicPlaceholder(
        plugin,
        Pattern.compile("ecoshop_rotation_(.+)")
    ) { args ->
        // args is the full matched string, e.g. "ecoshop_rotation_minerals"
        val categoryId = args.removePrefix("ecoshop_rotation_")
        val scheduler = ShopCategories.getByID(categoryId)?.scheduler
            ?: return@DynamicPlaceholder ""
        formatCountdown(scheduler.state.nextRotation)
    }

    fun register() {
        placeholder.register()
    }

    private fun formatCountdown(nextRotationMs: Long): String {
        val remaining = (nextRotationMs - System.currentTimeMillis()).coerceAtLeast(0L)
        if (remaining == 0L) return plugin.langYml.getString("rotation-ready")

        val totalSeconds = remaining / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return plugin.langYml.getString("rotation-countdown")
            .replace("%hours%", hours.toString())
            .replace("%minutes%", minutes.toString().padStart(2, '0'))
            .replace("%seconds%", seconds.toString().padStart(2, '0'))
    }
}
