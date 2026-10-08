package com.willfp.ecoshop

import com.willfp.eco.core.Eco
import com.willfp.eco.core.Prerequisite
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Entity

internal inline fun Entity.runOwned(crossinline block: () -> Unit) {
    if (Eco.get().isOwnedByCurrentRegion(this)) {
        block()
    } else {
        plugin.scheduler.on(this).run { block() }
    }
}

internal inline fun Location.runOwned(crossinline block: () -> Unit) {
    if (Eco.get().isOwnedByCurrentRegion(this)) {
        block()
    } else {
        plugin.scheduler.at(this).run { block() }
    }
}

internal inline fun runOnGlobalRegion(crossinline block: () -> Unit) {
    if (Prerequisite.HAS_FOLIA.isMet && !Bukkit.isGlobalTickThread()) {
        plugin.scheduler.global().run { block() }
    } else {
        block()
    }
}
