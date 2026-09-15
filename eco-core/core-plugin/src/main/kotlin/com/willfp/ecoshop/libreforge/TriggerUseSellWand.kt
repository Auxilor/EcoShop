package com.willfp.ecoshop.libreforge

import com.willfp.libreforge.toDispatcher
import com.willfp.libreforge.triggers.Trigger
import com.willfp.libreforge.triggers.TriggerData
import com.willfp.libreforge.triggers.TriggerParameter
import org.bukkit.Location
import org.bukkit.entity.Player

object TriggerUseSellWand : Trigger("use_sell_wand") {
    override val description = "Fires when the player sells a container's contents with a sell wand or the sell_container effect."

    override val categories = setOf("economy")

    override val parameters = setOf(
        TriggerParameter.PLAYER,
        TriggerParameter.LOCATION,
        TriggerParameter.VALUE,
        TriggerParameter.ALT_VALUE
    )

    override val parameterDescriptions = mapOf(
        TriggerParameter.LOCATION to "The location of the container that was sold from.",
        TriggerParameter.VALUE to "The money earned, counting eco:economy prices only.",
        TriggerParameter.ALT_VALUE to "The number of items sold."
    )

    fun fire(player: Player, location: Location, economyTotal: Double, units: Int) {
        this.dispatch(
            player.toDispatcher(),
            TriggerData(
                player = player,
                location = location,
                value = economyTotal,
                altValue = units.toDouble()
            )
        )
    }
}
