package com.willfp.ecoshop.libreforge

import com.willfp.ecoshop.event.EcoShopSellEvent
import org.bukkit.event.EventHandler
import wiki.ecohub.game.entity.toDispatcher
import wiki.ecohub.rules.trigger.Trigger
import wiki.ecohub.rules.trigger.TriggerData
import wiki.ecohub.rules.trigger.TriggerParameter

object TriggerSellItem : Trigger("sell_item") {
    override val description = "Fires when the player sells an item to the shop."

    override val categories = setOf("economy")

    override val parameters = setOf(
        TriggerParameter.PLAYER,
        TriggerParameter.LOCATION,
        TriggerParameter.EVENT,
        TriggerParameter.VALUE
    )

    override val parameterDescriptions = mapOf(
        TriggerParameter.VALUE to "The price the player received for selling the item"
    )

    @EventHandler(ignoreCancelled = true)
    fun handle(event: EcoShopSellEvent) {
        val player = event.player

        this.dispatch(
            player.toDispatcher(),
            TriggerData(
                player = player,
                location = player.location,
                event = event,
                value = event.price.getValue(player)
            )
        )
    }
}
