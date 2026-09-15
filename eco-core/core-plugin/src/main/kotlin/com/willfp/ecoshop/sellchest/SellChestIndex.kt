package com.willfp.ecoshop.sellchest

import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.block.Block
import java.util.UUID

data class ChestKey(val world: UUID, val x: Int, val y: Int, val z: Int) {
    fun block(): Block? {
        val world = Bukkit.getWorld(world) ?: return null
        if (!world.isChunkLoaded(x shr 4, z shr 4)) return null
        return world.getBlockAt(x, y, z)
    }

    companion object {
        fun of(block: Block) = ChestKey(block.world.uid, block.x, block.y, block.z)
    }
}

class IndexedChest(
    val key: ChestKey,
    val typeId: String,
    val owner: UUID
) {
    var dirty = true
    var lastSoldTick = Long.MIN_VALUE / 2
    var sellsSinceNotify = 0
    var blockedByUnsellable = false
}

/** Sell chests in loaded chunks. Rebuilt from block data on chunk load, so nothing is persisted. */
object SellChestIndex {
    private val chests = LinkedHashMap<ChestKey, IndexedChest>()

    fun add(block: Block, info: SellChestInfo): IndexedChest {
        val key = ChestKey.of(block)
        return chests.getOrPut(key) { IndexedChest(key, info.typeId, info.owner) }
    }

    fun remove(key: ChestKey) {
        chests.remove(key)
    }

    fun get(key: ChestKey): IndexedChest? = chests[key]

    fun get(block: Block): IndexedChest? = chests[ChestKey.of(block)]

    fun markDirty(key: ChestKey) {
        chests[key]?.dirty = true
    }

    fun markOwnerDirty(owner: UUID) {
        chests.values.filter { it.owner == owner }.forEach { it.dirty = true }
    }

    fun markAllDirty() {
        chests.values.forEach { it.dirty = true }
    }

    fun dirty(): List<IndexedChest> = chests.values.filter { it.dirty }

    fun scanChunk(chunk: Chunk) {
        for (state in chunk.getTileEntities(false)) {
            val info = SellChestData.read(state) ?: continue
            add(state.block, info)
        }
    }

    fun unloadChunk(chunk: Chunk) {
        val uid = chunk.world.uid
        chests.keys.removeIf { it.world == uid && it.x shr 4 == chunk.x && it.z shr 4 == chunk.z }
    }

    fun rescanLoaded() {
        chests.clear()
        for (world in Bukkit.getWorlds()) {
            for (chunk in world.loadedChunks) {
                scanChunk(chunk)
            }
        }
    }
}
