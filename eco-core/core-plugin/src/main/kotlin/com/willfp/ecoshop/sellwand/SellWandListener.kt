package com.willfp.ecoshop.sellwand

import com.willfp.ecoshop.plugin
import org.bukkit.block.Container
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot

object SellWandListener : Listener {
    @EventHandler(priority = EventPriority.HIGH)
    fun onInteract(event: PlayerInteractEvent) {
        if (ContainerAccess.isFiringSynthetic) return
        if (event.action != Action.RIGHT_CLICK_BLOCK || event.hand != EquipmentSlot.HAND) return

        val player = event.player
        val stack = player.inventory.itemInMainHand
        if (!WandItem.isWand(stack)) return

        val block = event.clickedBlock ?: return
        val container = block.getState(false) as? Container ?: return

        val wand = WandItem.wandOf(stack)
        if (wand == null) {
            event.setUseInteractedBlock(Event.Result.DENY)
            player.sendMessage(plugin.langYml.getMessage("sellwand.invalid"))
            return
        }

        if (!wand.containers.matches(block.type)) return
        if (wand.requireSneak && !player.isSneaking) return

        if (event.useInteractedBlock() == Event.Result.DENY) {
            player.sendMessage(plugin.langYml.getMessage("sellwand.no-access"))
            return
        }

        event.setUseInteractedBlock(Event.Result.DENY)
        event.setUseItemInHand(Event.Result.DENY)

        WandSeller.use(player, block, container, WandProfile.of(wand), consumeFromHand = true)
    }
}
