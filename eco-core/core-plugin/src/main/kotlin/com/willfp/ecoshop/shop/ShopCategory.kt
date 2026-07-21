package com.willfp.ecoshop.shop

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.gui.addPage
import com.willfp.eco.core.gui.addPageChanger
import com.willfp.eco.core.gui.menu
import com.willfp.eco.core.gui.menu.Menu
import com.willfp.eco.core.gui.onLeftClick
import com.willfp.eco.core.gui.page.PageChanger
import com.willfp.eco.core.gui.slot
import com.willfp.eco.core.gui.slot.ConfigSlot
import com.willfp.eco.core.gui.slot.FillerMask
import com.willfp.eco.core.gui.slot.MaskItems
import com.willfp.eco.core.gui.slot.Slot
import com.willfp.eco.core.items.Items
import com.willfp.eco.core.items.builder.modify
import com.willfp.eco.core.registry.KRegistrable
import com.willfp.eco.core.sound.PlayableSound
import com.willfp.eco.util.StringUtils
import com.willfp.eco.util.formatEco
import com.willfp.eco.util.openMenu
import com.willfp.ecomponent.menuStateVar
import com.willfp.ecoshop.plugin
import com.willfp.ecoshop.shop.gui.RotationSlot
import com.willfp.ecoshop.shop.rotation.RotationScheduler
import org.bukkit.Bukkit
import org.bukkit.entity.Player
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

        title = StringUtils.format(config.getString("gui.title"))

        allowChangingHeldItem()

        maxPages(pages.size)

        val pageChangeSound = PlayableSound.create(config.getSubsection("gui.page-change-sound"))

        addPageChanger(config, "gui.forwards-arrow", PageChanger.Direction.FORWARDS, pageChangeSound)

        val backwardsActive = Items.lookup(config.getString("gui.backwards-arrow.item")).item
        val backwardsRow = config.getIntOrNull("gui.backwards-arrow.location.row")
            ?: config.getInt("gui.backwards-arrow.row")
        val backwardsColumn = config.getIntOrNull("gui.backwards-arrow.location.column")
            ?: config.getInt("gui.backwards-arrow.column")

        addPageChanger(
            PageChanger.Direction.BACKWARDS,
            backwardsActive,
            null,
            pageChangeSound,
            backwardsRow,
            backwardsColumn
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
                    Bukkit.getScheduler().runTask(plugin, Runnable {
                        val message = if (broadcastOnRotate) {
                            plugin.langYml.getMessage("rotated").replace("%category%", id)
                        } else null

                        for (player in Bukkit.getOnlinePlayers()) {
                            if (message != null) {
                                player.sendMessage(message)
                            }
                            if (player.openMenu === menu) {
                                menu.refresh(player)
                            }
                        }
                    })
                }
            ).also { it.start() }
        }

        for (pageConfig in pages) {
            val pageNumber = pageConfig.getInt("page")

            addPage(pageNumber) {
                setMask(
                    FillerMask(
                        MaskItems.fromItemNames(pageConfig.getStrings("mask.items")),
                        *pageConfig.getStrings("mask.pattern").toTypedArray()
                    )
                )

                setSlot(
                    backwardsRow,
                    backwardsColumn,
                    slot(backwardsActive) {
                        onLeftClick { player, _, _, menu ->
                            menu.kickBack(player)
                        }
                    }
                )

                for (item in staticItems) {
                    if (item.page != pageNumber) {
                        continue
                    }

                    setSlot(
                        item.row,
                        item.column,
                        item.slot
                    )
                }

                if (rotationEnabled) {
                    for ((pos, slotIdx) in rSlotIndexMap) {
                        val (pg, row, col) = pos
                        if (pg != pageNumber) continue
                        val sched = scheduler!!
                        setSlot(row, col, RotationSlot(slotIdx) { sched.state })
                    }
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
