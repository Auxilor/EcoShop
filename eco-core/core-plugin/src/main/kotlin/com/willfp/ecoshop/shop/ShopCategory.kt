package com.willfp.ecoshop.shop

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.gui.addPage
import com.willfp.eco.core.gui.menu
import com.willfp.eco.core.gui.menu.Menu
import com.willfp.eco.core.gui.menu.MenuLayer
import com.willfp.eco.core.gui.onLeftClick
import com.willfp.eco.core.gui.page.PageChanger
import com.willfp.eco.core.gui.slot
import com.willfp.eco.core.gui.slot.ConfigSlot
import com.willfp.eco.core.gui.slot.FillerMask
import com.willfp.eco.core.gui.slot.MaskItems
import com.willfp.eco.core.gui.slot.Slot
import com.willfp.eco.core.items.Items
import com.willfp.eco.core.items.builder.modify
import com.willfp.ecomponent.components.pageChangerWithDefault
import com.willfp.ecomponent.menuStateVar
import com.willfp.ecoshop.EcoShopPlugin
import org.bukkit.entity.Player
import java.util.Stack

// Menu state keys
val Menu.previousMenus by menuStateVar<Stack<Menu>>("previous-menu", Stack())
val Menu.parentShop by menuStateVar<Shop>("parent-shop")

fun <T> Stack<T>.popOrNull(): T? =
    if (this.empty()) null else this.pop()

class ShopCategory(
    val plugin: EcoShopPlugin,
    val id: String,
    val config: Config
) {
    val items = config.getSubsections("items").mapNotNull {
        try {
            val item = ShopItem(plugin, it)
            ShopItems.addNewItem(item)
            item
        } catch (e: InvalidShopItemException) {
            plugin.logger.warning(e.message)
            null
        }
    }

    val slot: Slot = slot(Items.lookup(config.getString("item")).modify {
        addLoreLines(config.getStrings("lore"))
    }) {
        onLeftClick { player, _, _, previousMenu ->
            // Previous for category should be shop

            val parent = previousMenu.parentShop[player]

            parent?.clickSound?.playTo(player)

            menu.openAndStack(player, previousMenu)
        }
    }

    private val menu: Menu = menu(config.getInt("gui.rows")) {
        title = config.getFormattedString("gui.title")

        allowChangingHeldItem()

        val pages = config.getSubsections("gui.pages")

        maxPages(pages.size)

        val forwardsArrow = PageChanger(
            Items.lookup(config.getString("gui.forwards-arrow.item")).item,
            PageChanger.Direction.FORWARDS
        )

        val backwardsArrow = pageChangerWithDefault(
            Items.lookup(config.getString("gui.backwards-arrow.item")).item,
            PageChanger.Direction.BACKWARDS
        ) { player, _, _, menu ->
            menu.kickBack(player)
        }

        addComponent(
            MenuLayer.TOP,
            config.getInt("gui.forwards-arrow.row"),
            config.getInt("gui.forwards-arrow.column"),
            forwardsArrow
        )

        addComponent(
            MenuLayer.TOP,
            config.getInt("gui.backwards-arrow.row"),
            config.getInt("gui.backwards-arrow.column"),
            backwardsArrow
        )

        for (pageConfig in pages) {
            val pageNumber = pageConfig.getInt("page")

            addPage(pageNumber) {
                setMask(
                    FillerMask(
                        MaskItems.fromItemNames(pageConfig.getStrings("mask.items")),
                        *pageConfig.getStrings("mask.pattern").toTypedArray()
                    )
                )

                for (item in items) {
                    if (item.page != pageNumber) {
                        continue
                    }

                    setSlot(
                        item.row,
                        item.column,
                        item.slot
                    )
                }

                for (config in pageConfig.getSubsections("custom-slots")) {
                    setSlot(
                        config.getInt("row"),
                        config.getInt("column"),
                        ConfigSlot(config)
                    )
                }
            }
        }
    }

    fun openDirect(player: Player, shop: Shop) {
        menu.open(player)
        menu.parentShop[player] = shop
    }
}
