package com.willfp.ecoshop.sellwand

import com.willfp.eco.core.data.keys.PersistentDataKey
import com.willfp.eco.core.data.keys.PersistentDataKeyType
import com.willfp.eco.core.data.profile
import com.willfp.ecoshop.plugin
import org.bukkit.OfflinePlayer
import java.util.concurrent.ConcurrentHashMap

/** Stores wand period state in the player profile, so it survives restarts and syncs across servers. */
object ProfilePeriodStore : PeriodStore {
    private val usesKeys = ConcurrentHashMap<String, PersistentDataKey<Int>>()
    private val startKeys = ConcurrentHashMap<String, PersistentDataKey<Int>>()

    private fun usesKey(id: String) = usesKeys.computeIfAbsent(id) {
        PersistentDataKey(plugin.createNamespacedKey("sellwand_${id}_period_uses"), PersistentDataKeyType.INT, 0)
    }

    private fun startKey(id: String) = startKeys.computeIfAbsent(id) {
        PersistentDataKey(plugin.createNamespacedKey("sellwand_${id}_period_start"), PersistentDataKeyType.INT, 0)
    }

    override fun read(player: OfflinePlayer, wandId: String) =
        PeriodState(player.profile.read(usesKey(wandId)), player.profile.read(startKey(wandId)))

    override fun write(player: OfflinePlayer, wandId: String, state: PeriodState) {
        player.profile.write(usesKey(wandId), state.uses)
        player.profile.write(startKey(wandId), state.startSeconds)
    }
}
