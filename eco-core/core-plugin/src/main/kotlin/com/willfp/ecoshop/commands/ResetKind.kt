package com.willfp.ecoshop.commands

import com.willfp.ecoshop.shop.ShopItem
import org.bukkit.OfflinePlayer
import wiki.ecohub.data.profile.Profile
import java.util.UUID

/**
 * What `resetbuys` and `resetsells` reset, and the lang keys they reply with.
 */
internal enum class ResetKind(
    val command: String,
    val key: String,
    val reset: (ShopItem, OfflinePlayer) -> Unit,
    val start: (ShopItem) -> MutableSet<UUID>,
    val resetProfile: (ShopItem, UUID, Profile, MutableSet<UUID>) -> Unit,
    val finish: (ShopItem, MutableSet<UUID>) -> Unit
) {
    BUYS(
        command = "resetbuys",
        key = "reset-buys",
        reset = { item, player -> item.resetTimesBought(player) },
        start = { it.startResetAllTimesBought() },
        resetProfile = { item, uuid, profile, reset -> item.resetTimesBought(uuid, profile, reset) },
        finish = { item, reset -> item.finishResetAllTimesBought(reset) }
    ),

    SELLS(
        command = "resetsells",
        key = "reset-sells",
        reset = { item, player -> item.resetTimesSold(player) },
        start = { it.startResetAllTimesSold() },
        resetProfile = { item, uuid, profile, reset -> item.resetTimesSold(uuid, profile, reset) },
        finish = { item, reset -> item.finishResetAllTimesSold(reset) }
    )
}
