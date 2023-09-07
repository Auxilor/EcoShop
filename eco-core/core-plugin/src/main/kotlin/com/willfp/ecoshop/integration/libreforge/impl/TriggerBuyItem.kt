package com.willfp.ecoshop.integration.libreforge.impl

import com.willfp.ecoshop.event.EcoShopBuyEvent
import com.willfp.libreforge.triggers.Trigger
import com.willfp.libreforge.triggers.TriggerData
import com.willfp.libreforge.triggers.TriggerParameter
import org.bukkit.event.EventHandler

object TriggerBuyItem : Trigger("buy_item") {
    override val parameters = setOf(
        TriggerParameter.PLAYER,
        TriggerParameter.LOCATION,
        TriggerParameter.EVENT
    )

    @EventHandler(ignoreCancelled = true)
    fun handle(event: EcoShopBuyEvent) {
        val player = event.player

        this.dispatch(
            player,
            TriggerData(
                player = player,
                location = player.location,
                event = event,
                value = event.price.getValue(player)
            )
        )
    }
}
