package com.willfp.ecoshop.shop

import com.willfp.eco.core.data.Profile
import com.willfp.eco.core.data.keys.PersistentDataKey
import com.willfp.eco.core.data.profile
import org.bukkit.Bukkit
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

/**
 * Resets a counter for every player while saved profiles are scanned, so each player is reset exactly once.
 */
internal class GlobalCounterReset(
    private val lock: Any,
    private val key: PersistentDataKey<Int>
) {
    private val pending = AtomicReference<MutableSet<UUID>?>()

    fun start(): MutableSet<UUID> = synchronized(lock) {
        Bukkit.getServer().profile.write(key, 0)
        mutableSetOf<UUID>().also { pending.set(it) }
    }

    fun reset(uuid: UUID, profile: Profile, reset: MutableSet<UUID>) {
        if (pending.get() === reset) {
            claim(reset, uuid, profile)
        }
    }

    fun claimPending(uuid: UUID, profile: Profile) {
        pending.get()?.let { claim(it, uuid, profile) }
    }

    fun finish(reset: MutableSet<UUID>) {
        synchronized(lock) {
            pending.compareAndSet(reset, null)
        }
    }

    private fun claim(reset: MutableSet<UUID>, uuid: UUID, profile: Profile) {
        synchronized(lock) {
            if (reset.add(uuid)) {
                profile.write(key, 0)
            }
        }
    }
}
