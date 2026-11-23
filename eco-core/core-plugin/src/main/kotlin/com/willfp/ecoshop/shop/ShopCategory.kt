package com.willfp.ecoshop.shop

import com.willfp.ecoshop.plugin
import com.willfp.ecoshop.runOwned
import com.willfp.ecoshop.shop.gui.RotationSlot
import com.willfp.ecoshop.shop.rotation.RotationScheduler
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import wiki.ecohub.config.Config
import wiki.ecohub.core.registry.KRegistrable
import wiki.ecohub.game.item.Items
import wiki.ecohub.game.item.builder.modify
import wiki.ecohub.game.sound.PlayableSound
import wiki.ecohub.modules.menus.addCustomSlots
import wiki.ecohub.modules.menus.addPage
import wiki.ecohub.modules.menus.addPageChanger
import wiki.ecohub.modules.menus.component.ConfigButton
import wiki.ecohub.modules.menus.component.addComponent
import wiki.ecohub.modules.menus.menu
import wiki.ecohub.modules.menus.menu.Menu
import wiki.ecohub.modules.menus.menu.menuStateVar
import wiki.ecohub.modules.menus.onLeftClick
import wiki.ecohub.modules.menus.openMenu
import wiki.ecohub.modules.menus.page.PageChanger
import wiki.ecohub.modules.menus.setMask
import wiki.ecohub.modules.menus.slot
import wiki.ecohub.modules.menus.slot.Slot
import wiki.ecohub.ui.text.StringUtils
import wiki.ecohub.ui.text.formatEco
import java.util.Stack

// Menu state keys
val Menu.previousMenus by menuStateVar<Stack<Menu>>("previous-menu", Stack())
val Menu.parentShop by menuStateVar<Shop>("parent-shop")

fun <T> Stack<T>.popOrNull(): T? =
    if (this.empty()) null else this.pop()

class ShopCategory(
    override val id: String,
    val config: Config
) : KRegistrable {
    private val permission = config.getStringOrNull("permission")

    private val dynamicPricing: DynamicPricingConfig? =
        if (config.has("dynamic-pricing"))
            DynamicPricingConfig.from(config.getSubsection("dynamic-pricing"))
        else null

    val items = config.getSubsections("items").mapNotNull {
        try {
            val item = ShopItem(it, dynamicPricing)
            item.addCategory(id)
            ShopItems.register(item)
            item
        } catch (e: InvalidShopItemException) {
            plugin.logger.warning(e.message)
            null
        }
    }

    private val rotationEnabled: Boolean =
        config.getBoolOrNull("rotation.enabled") ?: false

    private val broadcastOnRotate: Boolean =
        config.getBoolOrNull("rotation.broadcast-message") ?: true

    // Non-null only if rotationEnabled = true.
    var scheduler: RotationScheduler? = null
        private set

    val slot: Slot = slot({ player, _ ->
        Items.lookup(config.getString("item")).modify {
            addLoreLines(config.getStrings("lore").formatEco(player, true))
        }
    }) {
        onLeftClick { player, _, _, previousMenu ->
            // Previous for category should be shop

            if (permission != null && !player.hasPermission(permission)) {
                player.sendMessage(plugin.langYml.getMessage("no-permission"))
                return@onLeftClick
            }

            val parent = previousMenu.parentShop[player]

            parent?.clickSound?.playTo(player)

            menu.openAndStack(player, previousMenu)
        }
    }

    private val menu: Menu = menu(config.getInt("gui.rows")) {
        val pages = config.getSubsections("gui.pages")

        setTitle(StringUtils.format(config.getString("gui.title")))

        allowChangingHeldItem()

        maxPages(pages.size)

        val pageChangeSound = PlayableSound.create(config.getSubsection("gui.page-change-sound"))

        addPageChanger(config, "gui.forwards-arrow", PageChanger.Direction.FORWARDS, pageChangeSound)

        addPageChanger(
            config,
            "gui.backwards-arrow",
            PageChanger.Direction.BACKWARDS,
            pageChangeSound,
            hideInactive = true
        )

        // (page, row, column) -> r-slot index, scanned from every page's mask pattern.
        val rSlotPositions = mutableMapOf<Triple<Int, Int, Int>, Int>()
        if (rotationEnabled) {
            var rSlotCounter = 0
            for (pageConfig in pages) {
                val pageNumber = pageConfig.getInt("page")
                val pattern = pageConfig.getStrings("mask.pattern")
                for ((rowIdx, row) in pattern.withIndex()) {
                    for ((colIdx, char) in row.withIndex()) {
                        if (char == 'r') {
                            rSlotPositions[Triple(pageNumber, rowIdx + 1, colIdx + 1)] = rSlotCounter++
                        }
                    }
                }
            }
        }

        val staticItems = if (rotationEnabled) items.filter { !it.rotationEnabled } else items
        val rotationItems = if (rotationEnabled) items.filter { it.rotationEnabled } else emptyList()

        val blockedRSlotIndices = mutableSetOf<Int>()
        for (item in staticItems) {
            val key = Triple(item.page, item.row, item.column)
            rSlotPositions[key]?.let { blockedRSlotIndices.add(it) }
        }

        // Reserved before always-show/weighted, so they can't be stolen.
        val fixedSlotItems = mutableMapOf<Int, ShopItem>()
        for (item in rotationItems) {
            if (!item.hasFixedPosition) continue
            val key = Triple(item.page, item.row, item.column)
            val idx = rSlotPositions[key] ?: continue
            if (idx in blockedRSlotIndices) {
                plugin.logger.warning(
                    "Rotation: fixed-slot rotation item '${item.id}' in category '$id' " +
                        "conflicts with a static item at the same position — skipped."
                )
                continue
            }
            if (fixedSlotItems.containsKey(idx)) {
                plugin.logger.warning(
                    "Rotation: two fixed-slot rotation items claim r-slot $idx in category '$id' — " +
                        "keeping '${fixedSlotItems[idx]!!.id}', skipping '${item.id}'."
                )
                continue
            }
            fixedSlotItems[idx] = item
        }

        val capacity = rSlotPositions.size - blockedRSlotIndices.size

        // Non-blocked r-slot indices, renumbered 0..capacity-1 in position order.
        val nonBlockedRSlots = rSlotPositions.entries
            .filter { it.value !in blockedRSlotIndices }
            .sortedBy { it.value }
        val rSlotIndexMap = nonBlockedRSlots.mapIndexed { slotIdx, entry -> entry.key to slotIdx }.toMap()

        if (rotationEnabled) {
            val rotationConfig = config.getSubsection("rotation")
            val weightedRotationItems = rotationItems.filter { it.id !in fixedSlotItems.values.map { i -> i.id } }

            scheduler = RotationScheduler(
                categoryId = id,
                rotationConfig = rotationConfig,
                rotatingItems = weightedRotationItems + fixedSlotItems.values,
                fixedSlotItems = fixedSlotItems,
                capacity = capacity,
                // Slots read scheduler.state live, but eco does not guarantee a live
                // re-render of an already-open menu. On rotation, re-render for any
                // player currently viewing THIS menu. Deferred to the next tick so the
                // `menu` property is fully assigned (start() may rotate() synchronously
                // during construction, before `menu` is set).
                onRotate = {
                    plugin.scheduler.global().run {
                        val message = if (broadcastOnRotate) {
                            plugin.langYml.getMessage("rotated").replace("%category%", id)
                        } else null

                        for (player in Bukkit.getOnlinePlayers()) {
                            player.runOwned {
                                if (message != null) {
                                    player.sendMessage(message)
                                }
                                if (player.isOnline && player.openMenu === menu) {
                                    menu.refresh(player)
                                }
                            }
                        }
                    }
                }
            ).also { it.start() }
        }

        for (pageConfig in pages) {
            val pageNumber = pageConfig.getInt("page")

            addPage(pageNumber) {
                setMask(pageConfig)

                addComponent(
                    ConfigButton(config.getSubsection("gui.backwards-arrow")) { player, _, _, menu ->
                        menu.kickBack(player)
                    }
                )

                for (item in staticItems) {
                    if (item.page != pageNumber) {
                        continue
                    }

                    setSlot(
                        row = item.row,
                        column = item.column,
                        slot = item.slot
                    )
                }

                if (rotationEnabled) {
                    for ((pos, slotIdx) in rSlotIndexMap) {
                        val (pg, row, col) = pos
                        if (pg != pageNumber) continue
                        val sched = scheduler!!
                        setSlot(row = row, column = col, slot = RotationSlot(slotIdx) { sched.state })
                    }
                }

                addCustomSlots(pageConfig, "")
            }
        }
    }

    fun stopRotation() {
        scheduler?.persist()
        scheduler?.stop()
    }

    fun openDirect(player: Player, shop: Shop) {
        if (permission != null && !player.hasPermission(permission)) {
            player.sendMessage(plugin.langYml.getMessage("no-permission"))
            return
        }
        menu.open(player)
        menu.parentShop[player] = shop
    }
}
