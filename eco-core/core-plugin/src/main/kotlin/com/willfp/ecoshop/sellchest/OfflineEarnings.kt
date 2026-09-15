package com.willfp.ecoshop.sellchest

import com.willfp.eco.core.data.keys.PersistentDataKey
import com.willfp.eco.core.data.keys.PersistentDataKeyType
import com.willfp.eco.core.data.profile
import com.willfp.eco.util.NumberUtils
import com.willfp.ecoshop.plugin
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player

/** Offline-selling permission snapshot and the earnings summary shown on join. */
object OfflineEarnings {
    private val allowedKey = PersistentDataKey(
        plugin.createNamespacedKey("sellchest_offline_allowed"),
        PersistentDataKeyType.BOOLEAN,
        false
    )

    private val moneyKey = PersistentDataKey(
        plugin.createNamespacedKey("sellchest_offline_money"),
        PersistentDataKeyType.DOUBLE,
        0.0
    )

    private val unitsKey = PersistentDataKey(
        plugin.createNamespacedKey("sellchest_offline_units"),
        PersistentDataKeyType.INT,
        0
    )

    fun snapshotPermission(player: Player) {
        player.profile.write(allowedKey, player.hasPermission("ecoshop.sellchest.offline"))
    }

    fun isAllowed(player: OfflinePlayer): Boolean = player.profile.read(allowedKey)

    fun add(player: OfflinePlayer, money: Double, units: Int) {
        if (units <= 0) return
        player.profile.write(moneyKey, player.profile.read(moneyKey) + money)
        player.profile.write(unitsKey, player.profile.read(unitsKey) + units)
    }

    fun flush(player: Player) {
        val money = player.profile.read(moneyKey)
        val units = player.profile.read(unitsKey)
        if (units <= 0) return

        if (plugin.configYml.getBoolOrNull("sell-chests.offline-selling.summary-on-join") != false && money > 0) {
            player.sendMessage(
                plugin.langYml.getMessage("sellchest.offline-summary")
                    .replace("%price%", NumberUtils.formatWithCommas(money))
                    .replace("%amount%", units.toString())
            )
        }

        player.profile.write(moneyKey, 0.0)
        player.profile.write(unitsKey, 0)
    }
}
