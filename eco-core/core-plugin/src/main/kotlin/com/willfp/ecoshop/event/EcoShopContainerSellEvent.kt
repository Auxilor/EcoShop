package com.willfp.ecoshop.event

import com.willfp.ecoshop.sell.QuoteLine
import com.willfp.ecoshop.sell.SellSource
import org.bukkit.Location
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/** Fired once before a wand or sell chest commits a sale. [actor] is null when the owner is offline. */
class EcoShopContainerSellEvent(
    val owner: OfflinePlayer,
    val actor: Player?,
    val source: SellSource,
    val location: Location,
    val lines: List<QuoteLine>
) : Event(), Cancellable {
    private var cancelled = false

    override fun isCancelled() = cancelled

    override fun setCancelled(cancel: Boolean) {
        cancelled = cancel
    }

    // Below here is bukkit boilerplate
    override fun getHandlers(): HandlerList {
        return HANDLERS
    }

    companion object {
        @JvmStatic
        private val HANDLERS: HandlerList = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList {
            return HANDLERS
        }
    }
}
