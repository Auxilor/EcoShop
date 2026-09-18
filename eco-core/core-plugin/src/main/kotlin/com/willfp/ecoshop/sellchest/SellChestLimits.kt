package com.willfp.ecoshop.sellchest

import com.willfp.eco.core.data.keys.PersistentDataKey
import com.willfp.eco.core.data.keys.PersistentDataKeyType
import com.willfp.eco.core.data.profile
import com.willfp.ecoshop.plugin
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player

private const val LIMIT_PREFIX = "ecoshop.sellchest.limit."

internal fun resolveChestLimit(grantedPermissions: Collection<String>, default: Int): Int {
    val limits = grantedPermissions
        .filter { it.startsWith(LIMIT_PREFIX) }
        .mapNotNull { it.removePrefix(LIMIT_PREFIX).toIntOrNull() }

    if (limits.isEmpty()) return default
    if (limits.any { it < 0 }) return -1
    return limits.max()
}

internal fun canPlaceChest(count: Int, limit: Int): Boolean = limit < 0 || count < limit

object SellChestLimits {
    private val countKey = PersistentDataKey(
        plugin.createNamespacedKey("sellchest_count"),
        PersistentDataKeyType.INT,
        0
    )

    fun limitFor(player: Player): Int = resolveChestLimit(
        player.effectivePermissions.filter { it.value }.map { it.permission },
        plugin.configYml.getIntOrNull("sell-chests.default-limit") ?: 5
    )

    fun count(player: OfflinePlayer): Int = player.profile.read(countKey)

    fun setCount(player: OfflinePlayer, count: Int) = player.profile.write(countKey, count.coerceAtLeast(0))

    fun increment(player: OfflinePlayer) = setCount(player, count(player) + 1)

    fun decrement(player: OfflinePlayer) = setCount(player, count(player) - 1)
}
