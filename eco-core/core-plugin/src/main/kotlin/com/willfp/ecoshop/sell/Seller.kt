package com.willfp.ecoshop.sell

import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player

/** Who receives the payout for a sale. */
sealed interface Seller {
    val owner: OfflinePlayer

    /** Full pipeline: any price type, permissions, conditions, events, effects. */
    data class Online(val player: Player) : Seller {
        override val owner: OfflinePlayer get() = player
    }

    /** Money-only pipeline. [isEligible] decides which candidates may be sold. */
    data class Offline(
        override val owner: OfflinePlayer,
        val isEligible: (SellCandidate) -> Boolean
    ) : Seller
}
