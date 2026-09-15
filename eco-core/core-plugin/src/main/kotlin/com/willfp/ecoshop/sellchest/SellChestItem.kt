package com.willfp.ecoshop.sellchest

import com.willfp.eco.core.fast.fast
import com.willfp.ecoshop.plugin
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

object SellChestItem {
    private val typeKey = plugin.createNamespacedKey("sellchest_type")

    fun create(type: SellChestType): ItemStack {
        val stack = type.baseItem.clone()
        stack.amount = 1
        val meta = stack.itemMeta
        meta.persistentDataContainer.set(typeKey, PersistentDataType.STRING, type.id)
        stack.itemMeta = meta
        if (type.loreTemplate.isNotEmpty()) {
            stack.fast().lore = type.loreTemplate
        }
        return stack
    }

    fun typeIdOf(stack: ItemStack?): String? =
        stack?.itemMeta?.persistentDataContainer?.get(typeKey, PersistentDataType.STRING)

    fun typeOf(stack: ItemStack?): SellChestType? = SellChestTypes[typeIdOf(stack)]
}
