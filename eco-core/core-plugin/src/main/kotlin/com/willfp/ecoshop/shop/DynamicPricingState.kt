package com.willfp.ecoshop.shop

import com.willfp.eco.core.data.keys.PersistentDataKey
import com.willfp.eco.core.data.keys.PersistentDataKeyType
import com.willfp.eco.core.data.profile
import com.willfp.ecoshop.plugin
import com.willfp.ecoshop.sell.DynamicPricer
import org.bukkit.Bukkit

/**
 * Server-wide dynamic pricing counters and decay time for one shop item.
 */
internal class DynamicPricingState(id: String) {
    private val buysKey = PersistentDataKey(
        plugin.createNamespacedKey("${id}_dp_buys"),
        PersistentDataKeyType.INT,
        0
    )

    private val sellsKey = PersistentDataKey(
        plugin.createNamespacedKey("${id}_dp_sells"),
        PersistentDataKeyType.INT,
        0
    )

    private val legacyLastDecayTimeKey = PersistentDataKey(
        plugin.createNamespacedKey("${id}_dp_last_decay"),
        PersistentDataKeyType.INT,
        0
    )

    private val lastDecayTimeKey = PersistentDataKey(
        plugin.createNamespacedKey("${id}_dp_last_decay_time"),
        PersistentDataKeyType.DOUBLE,
        0.0
    )

    fun globalBuys(): Int = Bukkit.getServer().profile.read(buysKey)

    fun globalSells(): Int = Bukkit.getServer().profile.read(sellsKey)

    fun hasActivity(): Boolean = globalBuys() > 0 || globalSells() > 0

    fun addBuys(amount: Int) {
        Bukkit.getServer().profile.write(buysKey, globalBuys() + amount)
    }

    fun addSells(amount: Int) {
        Bukkit.getServer().profile.write(sellsKey, globalSells() + amount)
    }

    fun reset() {
        Bukkit.getServer().profile.write(buysKey, 0)
        Bukkit.getServer().profile.write(sellsKey, 0)
        Bukkit.getServer().profile.write(lastDecayTimeKey, 0.0)
        Bukkit.getServer().profile.write(legacyLastDecayTimeKey, 0)
    }

    private fun getLastDecayTime(): Long =
        Bukkit.getServer().profile.read(lastDecayTimeKey).toLong().takeIf { it > 0 }
            ?: Bukkit.getServer().profile.read(legacyLastDecayTimeKey).toLong()

    fun applyDecay(dp: DynamicPricingConfig) {
        if (!dp.decayEnabled || dp.decayRate <= 0.0) return

        val nowSeconds = System.currentTimeMillis() / 1000L
        val lastDecay = getLastDecayTime()

        if (lastDecay == 0L) {
            Bukkit.getServer().profile.write(lastDecayTimeKey, nowSeconds.toDouble())
            return
        }

        val periodSeconds = dp.decayPeriodMinutes * 60L
        val periods = DynamicPricer.elapsedDecayPeriods(nowSeconds - lastDecay, periodSeconds)
        if (periods >= 1) {
            Bukkit.getServer().profile.write(buysKey, DynamicPricer.decay(globalBuys(), dp.decayRate, periods))
            Bukkit.getServer().profile.write(sellsKey, DynamicPricer.decay(globalSells(), dp.decayRate, periods))
            Bukkit.getServer().profile.write(lastDecayTimeKey, (lastDecay + periods * periodSeconds).toDouble())
        }
    }
}
