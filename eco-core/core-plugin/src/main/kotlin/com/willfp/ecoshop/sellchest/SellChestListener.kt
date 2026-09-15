package com.willfp.ecoshop.sellchest

import com.willfp.eco.util.savedDisplayName
import com.willfp.ecoshop.plugin
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.Container
import org.bukkit.block.data.type.Chest
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.Event
import org.bukkit.event.block.Action
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryMoveItemEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.ChunkUnloadEvent
import org.bukkit.entity.Player

object SellChestListener : Listener {
    private val horizontal = listOf(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST)

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onPlaceCheck(event: BlockPlaceEvent) {
        val typeId = SellChestItem.typeIdOf(event.itemInHand) ?: return
        val player = event.player
        val type = SellChestTypes[typeId]

        if (type == null) {
            event.isCancelled = true
            player.sendMessage(plugin.langYml.getMessage("sellchest.invalid-type"))
            return
        }

        if (!player.hasPermission("ecoshop.sellchest.place")) {
            event.isCancelled = true
            player.sendMessage(plugin.langYml.getMessage("no-permission"))
            return
        }

        val limit = SellChestLimits.limitFor(player)
        if (!canPlaceChest(SellChestLimits.count(player), limit)) {
            event.isCancelled = true
            player.sendMessage(
                plugin.langYml.getMessage("sellchest.limit-reached").replace("%limit%", limit.toString())
            )
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlace(event: BlockPlaceEvent) {
        val block = event.blockPlaced
        val type = SellChestItem.typeOf(event.itemInHand)

        if (type != null) {
            SellChestData.write(block, type.id, event.player.uniqueId)
            SellChestLimits.increment(event.player)
            SellChestIndex.add(block, SellChestInfo(type.id, event.player.uniqueId))
            forceSingleNextTick(block)
            return
        }

        if (block.type == Material.CHEST || block.type == Material.TRAPPED_CHEST) {
            if (horizontal.any { SellChestIndex.get(block.getRelative(it)) != null }) {
                forceSingleNextTick(block)
            }
        }
    }

    private fun forceSingleNextTick(block: Block) {
        plugin.scheduler.at(block.location).runLater(1) {
            for (target in listOf(block) + horizontal.map { block.getRelative(it) }) {
                val data = target.blockData as? Chest ?: continue
                if (data.type != Chest.Type.SINGLE) {
                    data.type = Chest.Type.SINGLE
                    target.setBlockData(data, false)
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onBreakCheck(event: BlockBreakEvent) {
        val info = SellChestData.read(event.block) ?: return
        if (!mayManage(event.player, info)) {
            event.isCancelled = true
            event.player.sendMessage(
                plugin.langYml.getMessage("sellchest.not-owner")
                    .replace("%owner%", Bukkit.getOfflinePlayer(info.owner).savedDisplayName)
            )
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBreak(event: BlockBreakEvent) {
        val info = SellChestData.read(event.block) ?: return
        event.isDropItems = false
        removeChest(event.block, info)
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onEntityExplode(event: EntityExplodeEvent) = handleExplosion(event.blockList())

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onBlockExplode(event: BlockExplodeEvent) = handleExplosion(event.blockList())

    private fun handleExplosion(blocks: MutableList<Block>) {
        val proof = plugin.configYml.getBoolOrNull("sell-chests.explosion-proof") ?: true
        val iterator = blocks.iterator()
        while (iterator.hasNext()) {
            val block = iterator.next()
            val info = SellChestData.read(block) ?: continue
            iterator.remove()
            if (!proof) {
                removeChest(block, info)
                block.type = Material.AIR
            }
        }
    }

    /** Drops contents and the sell chest item, updates count and index. Caller removes the block. */
    private fun removeChest(block: Block, info: SellChestInfo) {
        val location = block.location.add(0.5, 0.5, 0.5)
        val container = block.getState(false) as? Container
        if (container != null) {
            for (stack in container.inventory.contents.filterNotNull()) {
                if (!stack.type.isAir) block.world.dropItemNaturally(location, stack)
            }
            container.inventory.clear()
        }

        SellChestTypes[info.typeId]?.let { block.world.dropItemNaturally(location, SellChestItem.create(it)) }

        SellChestLimits.decrement(Bukkit.getOfflinePlayer(info.owner))
        SellChestIndex.remove(ChestKey.of(block))
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onOpen(event: InventoryOpenEvent) {
        val block = event.inventory.location?.block ?: return
        val info = SellChestData.read(block) ?: return
        val player = event.player as? Player ?: return
        if (!mayManage(player, info)) {
            event.isCancelled = true
            player.sendMessage(
                plugin.langYml.getMessage("sellchest.not-owner")
                    .replace("%owner%", Bukkit.getOfflinePlayer(info.owner).savedDisplayName)
            )
        }
    }

    /** Denies non-owners at NORMAL so sell wands (HIGH) and the sell_container access check see DENY. */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    fun onInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        val block = event.clickedBlock ?: return
        val info = SellChestData.read(block) ?: return
        if (!mayManage(event.player, info)) {
            event.setUseInteractedBlock(Event.Result.DENY)
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onHopperMove(event: InventoryMoveItemEvent) {
        val location = event.destination.location ?: return
        val chest = SellChestIndex.get(location.block) ?: return

        if (chest.blockedByUnsellable) {
            event.isCancelled = true
            return
        }

        plugin.scheduler.at(location).runLater(1) {
            SellChestIndex.markDirty(chest.key)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onClose(event: InventoryCloseEvent) {
        val location = event.inventory.location ?: return
        val chest = SellChestIndex.get(location.block) ?: return
        chest.blockedByUnsellable = false
        chest.dirty = true
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        SellChestIndex.markOwnerDirty(event.player.uniqueId)
    }

    @EventHandler
    fun onChunkLoad(event: ChunkLoadEvent) = SellChestIndex.scanChunk(event.chunk)

    @EventHandler
    fun onChunkUnload(event: ChunkUnloadEvent) = SellChestIndex.unloadChunk(event.chunk)

    private fun mayManage(player: Player, info: SellChestInfo) =
        player.uniqueId == info.owner || player.hasPermission("ecoshop.sellchest.bypass-owner")
}
