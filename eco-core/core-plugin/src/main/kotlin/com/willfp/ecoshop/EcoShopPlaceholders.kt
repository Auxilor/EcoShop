package com.willfp.ecoshop

import com.willfp.eco.core.placeholder.PlayerPlaceholder
import com.willfp.eco.core.placeholder.PlayerlessPlaceholder
import com.willfp.ecoshop.shop.BuyType
import com.willfp.ecoshop.shop.ShopCategories
import com.willfp.ecoshop.shop.ShopItem
import com.willfp.ecoshop.shop.rotation.RotationPlaceholders

fun registerPlaceholders(plugin: EcoShopPlugin) {
    RotationPlaceholders.register()

    for (category in ShopCategories.values()) {
        for (item in category.items) {
            registerItemPlaceholders(plugin, category.id, item)
        }
    }
}

private fun registerItemPlaceholders(plugin: EcoShopPlugin, categoryId: String, item: ShopItem) {
    val prefix = "${categoryId}_${item.id}"

    // buy pricing
    PlayerPlaceholder(plugin, "${prefix}_buy_base") { player ->
        item.buyPrice?.getValue(player)?.toString() ?: "0"
    }.register()

    PlayerPlaceholder(plugin, "${prefix}_buy_base_display") { player ->
        item.buyPrice?.getDisplay(player) ?: "0"
    }.register()

    PlayerPlaceholder(plugin, "${prefix}_buy_price") { player ->
        val base = item.buyPrice?.getValue(player) ?: return@PlayerPlaceholder "0"
        (base * item.getEffectiveBuyMultiplier(BuyType.NORMAL, player)).toString()
    }.register()

    PlayerPlaceholder(plugin, "${prefix}_buy_price_display") { player ->
        item.buyPrice?.getDisplay(player, item.getEffectiveBuyMultiplier(BuyType.NORMAL, player)) ?: "0"
    }.register()

    // alt-buy pricing
    PlayerPlaceholder(plugin, "${prefix}_alt_buy_base") { player ->
        item.altBuyPrice?.getValue(player)?.toString() ?: "0"
    }.register()

    PlayerPlaceholder(plugin, "${prefix}_alt_buy_base_display") { player ->
        item.altBuyPrice?.getDisplay(player) ?: "0"
    }.register()

    PlayerPlaceholder(plugin, "${prefix}_alt_buy_price") { player ->
        val base = item.altBuyPrice?.getValue(player) ?: return@PlayerPlaceholder "0"
        (base * item.getEffectiveBuyMultiplier(BuyType.ALT, player)).toString()
    }.register()

    PlayerPlaceholder(plugin, "${prefix}_alt_buy_price_display") { player ->
        item.altBuyPrice?.getDisplay(player, item.getEffectiveBuyMultiplier(BuyType.ALT, player)) ?: "0"
    }.register()

    // buy limits / amounts
    PlayerlessPlaceholder(plugin, "${prefix}_buy_limit") {
        if (item.limit == Int.MAX_VALUE) "-1" else item.limit.toString()
    }.register()

    PlayerlessPlaceholder(plugin, "${prefix}_buy_global_limit") {
        if (item.globalLimit == Int.MAX_VALUE) "-1" else item.globalLimit.toString()
    }.register()

    PlayerPlaceholder(plugin, "${prefix}_buy_amount") { player ->
        item.getTotalBuys(player).toString()
    }.register()

    PlayerlessPlaceholder(plugin, "${prefix}_buy_global_amount") {
        item.getTotalGlobalBuys().toString()
    }.register()

    PlayerPlaceholder(plugin, "${prefix}_buy_remaining") { player ->
        item.getBuysLeft(player).toString()
    }.register()

    PlayerlessPlaceholder(plugin, "${prefix}_buy_global_remaining") {
        if (item.globalLimit == Int.MAX_VALUE) "-1"
        else (item.globalLimit - item.getTotalGlobalBuys()).toString()
    }.register()

    // sell pricing
    PlayerPlaceholder(plugin, "${prefix}_sell_base") { player ->
        item.sellPrice?.getValue(player)?.toString() ?: "0"
    }.register()

    PlayerPlaceholder(plugin, "${prefix}_sell_base_display") { player ->
        item.sellPrice?.getDisplay(player) ?: "0"
    }.register()

    PlayerPlaceholder(plugin, "${prefix}_sell_price") { player ->
        val base = item.sellPrice?.getValue(player) ?: return@PlayerPlaceholder "0"
        (base * item.getEffectiveSellMultiplier(player)).toString()
    }.register()

    PlayerPlaceholder(plugin, "${prefix}_sell_price_display") { player ->
        item.sellPrice?.getDisplay(player, item.getEffectiveSellMultiplier(player)) ?: "0"
    }.register()

    // sell limits / amounts
    PlayerlessPlaceholder(plugin, "${prefix}_sell_limit") {
        if (item.sellLimit == Int.MAX_VALUE) "-1" else item.sellLimit.toString()
    }.register()

    PlayerlessPlaceholder(plugin, "${prefix}_sell_global_limit") {
        if (item.globalSellLimit == Int.MAX_VALUE) "-1" else item.globalSellLimit.toString()
    }.register()

    PlayerPlaceholder(plugin, "${prefix}_sell_amount") { player ->
        item.getTotalSells(player).toString()
    }.register()

    PlayerlessPlaceholder(plugin, "${prefix}_sell_global_amount") {
        item.getTotalGlobalSells().toString()
    }.register()

    PlayerPlaceholder(plugin, "${prefix}_sell_remaining") { player ->
        item.getSellsLeft(player).toString()
    }.register()

    PlayerlessPlaceholder(plugin, "${prefix}_sell_global_remaining") {
        if (item.globalSellLimit == Int.MAX_VALUE) "-1"
        else (item.globalSellLimit - item.getTotalGlobalSells()).toString()
    }.register()
}