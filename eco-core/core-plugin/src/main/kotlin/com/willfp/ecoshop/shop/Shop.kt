package com.willfp.ecoshop.shop

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.gui.addPage
import com.willfp.eco.core.gui.menu
import com.willfp.eco.core.gui.menu.Menu
import com.willfp.eco.core.gui.menu.MenuLayer
import com.willfp.eco.core.gui.page.PageChanger
import com.willfp.eco.core.gui.slot.ConfigSlot
import com.willfp.eco.core.gui.slot.FillerMask
import com.willfp.eco.core.gui.slot.MaskItems
import com.willfp.eco.core.items.Items
import com.willfp.eco.core.registry.KRegistrable
import com.willfp.eco.core.sound.PlayableSound
import com.willfp.eco.util.formatEco
import com.willfp.eco.util.savedDisplayName
import com.willfp.ecoshop.EcoShopPlugin
import com.willfp.ecoshop.commands.CommandShop
import org.bukkit.Bukkit
import org.bukkit.entity.Player

fun <T> Config.ifHasOrNull(key: String, block: (String) -> T): T? =
    if (this.has(key)) block(key) else null

fun Menu.openWithParentShop(player: Player, from: Menu) {
    val parent = from.parentShop[player]
    this.open(player)
    this.parentShop[player] = parent
}

fun Menu.openAndStack(player: Player, from: Menu) {
    val parent = from.parentShop[player]
    this.open(player)
    this.previousMenus[player] += from
    this.parentShop[player] = parent
}

fun Menu.kickBack(player: Player) =
    this.previousMenus[player].popOrNull()?.openWithParentShop(player, this) ?: player.closeInventory()

class Shop(
    val plugin: EcoShopPlugin,
    override val id: String,
    val config: Config
): KRegistrable {
    val clickSound = config.ifHasOrNull("click-sound") {
        PlayableSound.create(config.getSubsection(it))
    }

    val buySound = config.ifHasOrNull("buy-sound") {
        PlayableSound.create(config.getSubsection(it))
    }

    val sellSound = config.ifHasOrNull("sell-sound") {
        PlayableSound.create(config.getSubsection(it))
    }

    private val broadcastSound = config.ifHasOrNull("buy-broadcasts.sound") {
        PlayableSound.create(config.getSubsection(it))
    }

    val isBroadcasting = config.getBool("buy-broadcasts.enabled")

    val commandName = config.getString("command")

    val directCategory: ShopCategory?
        get() = ShopCategories.getByID(config.getString("direct-category"))

    val menu = if (config.has("pages")) menu(config.getInt("rows")) {
        title = config.getFormattedString("title")

        allowChangingHeldItem()

        val pages = config.getSubsections("pages")

        maxPages(pages.size)

        val forwardsArrow = PageChanger(
            Items.lookup(config.getString("forwards-arrow.item")).item,
            PageChanger.Direction.FORWARDS
        )

        val backwardsArrow = PageChanger(
            Items.lookup(config.getString("backwards-arrow.item")).item,
            PageChanger.Direction.BACKWARDS
        )

        addComponent(
            MenuLayer.TOP,
            config.getInt("forwards-arrow.row"),
            config.getInt("forwards-arrow.column"),
            forwardsArrow
        )

        addComponent(
            MenuLayer.TOP,
            config.getInt("backwards-arrow.row"),
            config.getInt("backwards-arrow.column"),
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

                for (config in pageConfig.getSubsections("categories")) {
                    val category = ShopCategories.getByID(config.getString("id")) ?: continue

                    setSlot(
                        config.getInt("row"),
                        config.getInt("column"),
                        category.slot
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

        onOpen { player, menu -> menu.parentShop[player] = this@Shop }
    } else null

    val command = CommandShop(this, plugin)

    fun broadcastPurchase(player: Player, item: ShopItem, amount: Int) {
        Bukkit.broadcastMessage(
            config.getString("buy-broadcasts.message")
                .replace("%player%", player.savedDisplayName)
                .replace("%item%", item.displayName)
                .replace("%amount%", amount.toString())
                .formatEco(player)
        )

        for (p in Bukkit.getOnlinePlayers()) {
            broadcastSound?.playTo(p)
        }
    }

    init {
        command.register()
    }

    fun remove() {
        command.unregister()
    }
}
