package com.willfp.ecoshop.sellwand

import org.bukkit.Bukkit
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot

/** Asks protection plugins whether a player may open a container, via a synthetic interact event. */
object ContainerAccess {
    private val firingSynthetic = ThreadLocal.withInitial { false }

    val isFiringSynthetic: Boolean
        get() = firingSynthetic.get()

    fun canAccess(player: Player, block: Block): Boolean {
        val event = PlayerInteractEvent(
            player,
            Action.RIGHT_CLICK_BLOCK,
            player.inventory.itemInMainHand,
            block,
            BlockFace.UP,
            EquipmentSlot.HAND
        )

        firingSynthetic.set(true)
        try {
            Bukkit.getPluginManager().callEvent(event)
        } finally {
            firingSynthetic.set(false)
        }

        return event.useInteractedBlock() != Event.Result.DENY
    }
}
