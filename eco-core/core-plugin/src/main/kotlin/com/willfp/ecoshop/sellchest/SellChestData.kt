package com.willfp.ecoshop.sellchest

import com.willfp.ecoshop.plugin
import com.willfp.ecoshop.sell.SellTotals
import org.bukkit.block.Block
import org.bukkit.block.BlockState
import org.bukkit.block.TileState
import org.bukkit.persistence.PersistentDataType
import java.util.UUID

data class SellChestInfo(val typeId: String, val owner: UUID)

/** Sell chest identity stored on the block entity, so it lives with the world. */
object SellChestData {
    private val typeKey = plugin.createNamespacedKey("sellchest_type")
    private val ownerKey = plugin.createNamespacedKey("sellchest_owner")
    private val soldKey = plugin.createNamespacedKey("sellchest_sold")

    fun read(block: Block): SellChestInfo? = read(block.getState(false))

    fun read(state: BlockState): SellChestInfo? {
        val pdc = (state as? TileState)?.persistentDataContainer ?: return null
        val type = pdc.get(typeKey, PersistentDataType.STRING) ?: return null
        val owner = pdc.get(ownerKey, PersistentDataType.STRING)?.let { runCatching { UUID.fromString(it) }.getOrNull() } ?: return null
        return SellChestInfo(type, owner)
    }

    fun write(block: Block, typeId: String, owner: UUID) {
        val state = block.getState(false) as? TileState ?: return
        state.persistentDataContainer.set(typeKey, PersistentDataType.STRING, typeId)
        state.persistentDataContainer.set(ownerKey, PersistentDataType.STRING, owner.toString())
    }

    fun readTotals(block: Block): SellTotals {
        val pdc = (block.getState(false) as? TileState)?.persistentDataContainer ?: return SellTotals.EMPTY
        return SellTotals.decode(pdc.get(soldKey, PersistentDataType.STRING))
    }

    fun writeTotals(block: Block, totals: SellTotals) {
        val state = block.getState(false) as? TileState ?: return
        if (totals.isEmpty) {
            state.persistentDataContainer.remove(soldKey)
        } else {
            state.persistentDataContainer.set(soldKey, PersistentDataType.STRING, totals.encode())
        }
    }
}
