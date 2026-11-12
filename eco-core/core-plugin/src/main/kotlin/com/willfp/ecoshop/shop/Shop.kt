package com.willfp.ecoshop.shop

import com.willfp.ecoshop.commands.shopCommand
import com.willfp.ecoshop.runOwned
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import wiki.ecohub.config.Config
import wiki.ecohub.core.registry.KRegistrable
import wiki.ecohub.game.entity.player.PlayerUtils
import wiki.ecohub.game.sound.PlayableSound
import wiki.ecohub.modules.menus.addCustomSlots
import wiki.ecohub.modules.menus.addPage
import wiki.ecohub.modules.menus.addPageChanger
import wiki.ecohub.modules.menus.getSlotLocations
import wiki.ecohub.modules.menus.menu
import wiki.ecohub.modules.menus.menu.Menu
import wiki.ecohub.modules.menus.page.PageChanger
import wiki.ecohub.modules.menus.setMask
import wiki.ecohub.ui.text.StringUtils
import wiki.ecohub.ui.text.formatEco

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
    override val id: String,
    val config: Config
) : KRegistrable {
    val clickSound = PlayableSound.create(config.getSubsection("click-sound"))

    val buySound = PlayableSound.create(config.getSubsection("buy-sound"))

    val sellSound = PlayableSound.create(config.getSubsection("sell-sound"))

    private val broadcastSound = PlayableSound.create(config.getSubsection("buy-broadcasts.sound"))

    val isBroadcasting = config.getBool("buy-broadcasts.enabled")

    val commandName = config.getString("command")

    val directCategory: ShopCategory?
        get() = ShopCategories.getByID(config.getString("direct-category"))

    /** Ids of every category placed in this shop, used by sell filter `shops` scoping. */
    val categoryIds: Set<String> = buildSet {
        config.getStringOrNull("direct-category")?.let { add(it) }
        for (page in config.getSubsections("pages")) {
            for (category in page.getSubsections("categories")) {
                add(category.getString("id"))
            }
        }
    }

    val menu = if (config.has("pages")) menu(config.getInt("rows")) {
        val pages = config.getSubsections("pages")

        title = StringUtils.format(config.getString("title"))

        allowChangingHeldItem()

        maxPages(pages.size)

        val pageChangeSound = PlayableSound.create(config.getSubsection("page-change-sound"))

        addPageChanger(config, "forwards-arrow", PageChanger.Direction.FORWARDS, pageChangeSound)
        addPageChanger(config, "backwards-arrow", PageChanger.Direction.BACKWARDS, pageChangeSound)

        for (pageConfig in pages) {
            val pageNumber = pageConfig.getInt("page")

            addPage(pageNumber) {
                setMask(pageConfig)

                for (config in pageConfig.getSubsections("categories")) {
                    val category = ShopCategories.getByID(config.getString("id")) ?: continue

                    for (location in config.getSlotLocations()) {
                        setSlot(location.row, location.column, category.slot)
                    }
                }

                addCustomSlots(pageConfig, "")
            }
        }

        onOpen { player, menu -> menu.parentShop[player] = this@Shop }
    } else null

    val command = shopCommand(this)

    fun broadcastPurchase(player: Player, item: ShopItem, amount: Int) {
        @Suppress("DEPRECATION")
        Bukkit.broadcastMessage(
            config.getString("buy-broadcasts.message")
                .replace("%player%", PlayerUtils.getSavedDisplayName(player))
                .replace("%item%", item.displayName)
                .replace("%amount%", amount.toString())
                .formatEco(player)
        )

        val sound = broadcastSound ?: return
        for (p in Bukkit.getOnlinePlayers()) {
            p.runOwned { sound.playTo(p) }
        }
    }

    init {
        command.register()
    }

    fun remove() {
        command.unregister()
    }
}
