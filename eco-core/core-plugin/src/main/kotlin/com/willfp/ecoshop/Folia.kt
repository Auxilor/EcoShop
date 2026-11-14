package com.willfp.ecoshop

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Entity
import wiki.ecohub.core.Eco
import wiki.ecohub.core.platform.ownsGlobal
import wiki.ecohub.core.plugin.startup.Prerequisite

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
    if (Prerequisite.HAS_FOLIA.isMet && !Bukkit.getServer().ownsGlobal()) {
        plugin.scheduler.global().run { block() }
    } else {
        block()
    }
}
