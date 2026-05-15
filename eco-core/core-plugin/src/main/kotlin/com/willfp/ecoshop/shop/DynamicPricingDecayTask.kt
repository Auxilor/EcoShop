package com.willfp.ecoshop.shop

import org.bukkit.scheduler.BukkitRunnable

class DynamicPricingDecayTask : BukkitRunnable() {
    override fun run() {
        for (item in ShopItems.values()) {
            if (item.hasDynamicActivity()) item.applyDecay()
        }
    }
}
