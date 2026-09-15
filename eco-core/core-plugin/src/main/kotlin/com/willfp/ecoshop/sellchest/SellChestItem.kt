package com.willfp.ecoshop.sellchest

import com.willfp.eco.core.fast.fast
import com.willfp.ecoshop.plugin
import com.willfp.ecoshop.sell.SellTotals
import com.willfp.ecoshop.sell.SellValueFormat
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.util.Locale

object SellChestItem {
    private val typeKey = plugin.createNamespacedKey("sellchest_type")
    private val soldKey = plugin.createNamespacedKey("sellchest_sold")

    fun create(type: SellChestType, totals: SellTotals = SellTotals.EMPTY): ItemStack {
        val stack = type.baseItem.clone()
        stack.amount = 1
        val meta = stack.itemMeta
        meta.persistentDataContainer.set(typeKey, PersistentDataType.STRING, type.id)
        if (!totals.isEmpty) {
            meta.persistentDataContainer.set(soldKey, PersistentDataType.STRING, totals.encode())
        }
        stack.itemMeta = meta
        if (type.loreTemplate.isNotEmpty()) {
            stack.fast().lore = type.loreTemplate.map {
                it.replace("%sold_items%", "%,d".format(Locale.ROOT, totals.units))
                    .replace("%sold_value%", SellValueFormat.format(totals, null))
            }
        }
        return stack
    }

    fun totalsOf(stack: ItemStack?): SellTotals =
        SellTotals.decode(stack?.itemMeta?.persistentDataContainer?.get(soldKey, PersistentDataType.STRING))

    fun typeIdOf(stack: ItemStack?): String? =
        stack?.itemMeta?.persistentDataContainer?.get(typeKey, PersistentDataType.STRING)

    fun typeOf(stack: ItemStack?): SellChestType? = SellChestTypes[typeIdOf(stack)]
}
