package com.willfp.ecoshop.integration.libreforge

import com.willfp.eco.core.integrations.Integration
import com.willfp.ecoshop.integration.libreforge.impl.FilterShopItem
import com.willfp.ecoshop.integration.libreforge.impl.TriggerBuyItem
import com.willfp.ecoshop.integration.libreforge.impl.TriggerSellItem
import com.willfp.libreforge.filters.Filters
import com.willfp.libreforge.triggers.Triggers

object LibreforgeIntegration: Integration {
    fun load() {
        Filters.register(FilterShopItem)
        Triggers.register(TriggerBuyItem)
        Triggers.register(TriggerSellItem)
    }

    override fun getPluginName(): String {
        return "libreforge"
    }
}
