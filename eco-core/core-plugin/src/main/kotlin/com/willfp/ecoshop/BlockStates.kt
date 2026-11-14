package com.willfp.ecoshop

import org.bukkit.Chunk
import org.bukkit.block.Block
import org.bukkit.block.BlockState
import org.bukkit.block.TileState
import wiki.ecohub.core.plugin.startup.Prerequisite

/**
 * This block's state, read without copying it where the server allows. Paper hands out the live
 * state; Spigot only has snapshots, which read the same but need [editTileState] to write back.
 */
internal val Block.liveState: BlockState
    get() = if (Prerequisite.HAS_PAPER.isMet) getState(false) else state

/** The block entities in this chunk, without copying them where the server allows. */
internal val Chunk.liveTileEntities: Array<BlockState>
    get() = if (Prerequisite.HAS_PAPER.isMet) getTileEntities(false) else tileEntities

/** Edit this block's block entity and write the change back to the world. */
internal inline fun Block.editTileState(edit: (TileState) -> Unit) {
    val state = liveState as? TileState ?: return
    edit(state)
    if (!Prerequisite.HAS_PAPER.isMet) {
        state.update(true, false)
    }
}
