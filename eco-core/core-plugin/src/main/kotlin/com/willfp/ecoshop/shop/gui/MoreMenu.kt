package com.willfp.ecoshop.shop.gui

import com.willfp.eco.core.gui.menu
import com.willfp.eco.core.gui.menu.Menu
import com.willfp.eco.core.gui.onLeftClick
import com.willfp.eco.core.gui.slot
import com.willfp.eco.core.gui.slot.ConfigSlot
import com.willfp.eco.core.gui.slot.FillerMask
import com.willfp.eco.core.gui.slot.MaskItems
import com.willfp.eco.core.gui.slot.Slot
import com.willfp.eco.core.items.Items
import com.willfp.ecoshop.EcoShopPlugin
import com.willfp.ecoshop.shop.ShopItem
import com.willfp.ecoshop.shop.kickBack
import com.willfp.ecoshop.shop.openAndStack
import org.bukkit.entity.Player

/** Key is buy or sell, slot is the actual slot given the amount of stacks. */
class MoreMenu(
    val item: ShopItem,
    private val plugin: EcoShopPlugin,
    private val key: String,
    val slotBuilder: (amount: Int, stacks: Int) -> Slot,
) {
    private val menu = menu(plugin.configYml.getInt("$key-more.rows")) {
        title = plugin.configYml.getFormattedString("$key-more.title")
            .replace("%item%", item.displayName)

        allowChangingHeldItem()

        setMask(
            FillerMask(
                MaskItems.fromItemNames(
                    plugin.configYml.getStrings("$key-more.mask.items")
                ),
                *plugin.configYml.getStrings("$key-more.mask.pattern").toTypedArray()
            )
        )

        for (config in plugin.configYml.getSubsections("$key-more.amounts")) {
            val stacks = config.getInt("stacks")
            val amount = stacks * (item.item?.item?.maxStackSize ?: 1)

            setSlot(
                config.getInt("row"),
                config.getInt("column"),
                slotBuilder(amount, stacks)
            )
        }

        setSlot(
            plugin.configYml.getInt("$key-more.back.row"),
            plugin.configYml.getInt("$key-more.back.column"),
            slot(Items.lookup(plugin.configYml.getString("$key-more.back.item"))) {
                onLeftClick { player, _, _, menu ->
                    menu.kickBack(player)
                }
            }
        )

        for (config in plugin.configYml.getSubsections("$key-more.custom-slots")) {
            setSlot(
                config.getInt("row"),
                config.getInt("column"),
                ConfigSlot(config)
            )
        }
    }

    fun open(player: Player, previousMenu: Menu) {
        menu.openAndStack(player, previousMenu)
    }
}
