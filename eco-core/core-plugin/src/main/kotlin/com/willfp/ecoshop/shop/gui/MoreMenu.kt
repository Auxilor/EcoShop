package com.willfp.ecoshop.shop.gui

import com.willfp.eco.core.gui.addCustomSlots
import com.willfp.eco.core.gui.getSlotLocations
import com.willfp.eco.core.gui.menu
import com.willfp.eco.core.gui.menu.Menu
import com.willfp.eco.core.gui.onLeftClick
import com.willfp.eco.core.gui.setMask
import com.willfp.eco.core.gui.slot
import com.willfp.eco.core.gui.slot.Slot
import com.willfp.eco.core.items.Items
import com.willfp.ecoshop.plugin
import com.willfp.ecoshop.shop.ShopItem
import com.willfp.ecoshop.shop.kickBack
import com.willfp.ecoshop.shop.openAndStack
import org.bukkit.entity.Player

/** Key is buy or sell, slot is the actual slot given the amount of stacks. */
class MoreMenu(
    val item: ShopItem,
    private val key: String,
    val slotBuilder: (amount: Int, stacks: Int) -> Slot,
) {
    private val menu = menu(plugin.configYml.getInt("$key-more.rows")) {
        title = plugin.configYml.getFormattedString("$key-more.title")
            .replace("%item%", item.displayName)

        allowChangingHeldItem()

        setMask(plugin.configYml, "$key-more")

        // Command/effect-only items have no item, so fall back to the display item's stack size.
        val stackSize = (item.item?.item ?: item.displayItem).maxStackSize.takeIf { it > 0 } ?: 64

        for (config in plugin.configYml.getSubsections("$key-more.amounts")) {
            val stacks = config.getInt("stacks")
            val amount = stacks * stackSize

            for (location in config.getSlotLocations()) {
                setSlot(location.row, location.column, slotBuilder(amount, stacks))
            }
        }

        for (location in plugin.configYml.getSlotLocations("$key-more.back")) {
            setSlot(
                location.row,
                location.column,
                slot(Items.lookup(plugin.configYml.getString("$key-more.back.item"))) {
                    onLeftClick { player, _, _, menu ->
                        menu.kickBack(player)
                    }
                }
            )
        }

        addCustomSlots(plugin.configYml, "$key-more")
    }

    fun open(player: Player, previousMenu: Menu) {
        menu.openAndStack(player, previousMenu)
    }
}
