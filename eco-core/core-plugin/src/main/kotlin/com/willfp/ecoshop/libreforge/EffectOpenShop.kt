package com.willfp.ecoshop.libreforge

import com.willfp.ecoshop.shop.ShopCategories
import com.willfp.ecoshop.shop.Shops
import wiki.ecohub.config.ArgType
import wiki.ecohub.config.Config
import wiki.ecohub.config.arguments
import wiki.ecohub.core.registry.NoCompileData
import wiki.ecohub.rules.effect.Effect
import wiki.ecohub.rules.trigger.TriggerData
import wiki.ecohub.rules.trigger.TriggerParameter

object EffectOpenShop : Effect<NoCompileData>("open_shop") {
    override val description = "Opens a shop for the player."

    override val categories = setOf("meta")

    override val parameters = setOf(
        TriggerParameter.PLAYER
    )

    override val arguments = arguments {
        require(
            "shop",
            "You must specify the shop ID!",
            description = "The ID of the shop to open for the player.",
            type = ArgType.STRING
        )

        optional(
            "category",
            description = "The ID of a category to open directly, instead of the shop's main menu.",
            type = ArgType.STRING
        )
    }

    override fun onTrigger(config: Config, data: TriggerData, compileData: NoCompileData): Boolean {
        val player = data.player ?: return false

        val shop = Shops[config.getString("shop")] ?: return false

        val categoryId = config.getStringOrNull("category")
        if (categoryId != null) {
            val category = ShopCategories.getByID(categoryId) ?: return false
            category.openDirect(player, shop)
            return true
        }

        val menu = shop.menu
        if (menu != null) {
            menu.open(player)
        } else {
            shop.directCategory?.openDirect(player, shop) ?: return false
        }

        return true
    }
}
