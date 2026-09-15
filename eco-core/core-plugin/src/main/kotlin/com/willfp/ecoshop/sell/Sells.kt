package com.willfp.ecoshop.sell

import com.willfp.eco.core.integrations.economy.EconomyManager
import com.willfp.ecoshop.logging.ShopLogger
import com.willfp.ecoshop.plugin
import com.willfp.ecoshop.shop.shopItem
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.inventory.ItemStack

/** Shared production instances of the sell core. */
object Sells {
    val quoter by lazy {
        SellQuoter(
            resolve = { it.shopItem },
            chunkSize = { plugin.configYml.getIntOrNull("dynamic-pricing.bulk-chunk-size") ?: 64 }
        )
    }

    val engine by lazy {
        SellEngine(
            callEvent = { Bukkit.getPluginManager().callEvent(it) },
            payOffline = { player, value -> EconomyManager.giveMoney(player, value) },
            log = { entry ->
                if (plugin.configYml.getBool("logging.enabled")) {
                    ShopLogger.log(entry.playerName, "SELL", entry.itemId, entry.units, entry.value, entry.source.name)
                }
            }
        )
    }

    fun sell(
        request: SellRequest,
        location: Location? = null,
        resolveWith: ((ItemStack) -> SellCandidate?)? = null
    ): SellResult = engine.commit(quoter.quote(request, resolveWith), location)
}
