package com.willfp.ecoshop.libreforge

import com.willfp.libreforge.ArgType
import com.willfp.libreforge.Compilable
import com.willfp.libreforge.NoCompileData
import com.willfp.libreforge.arguments

object SellFilterArguments : Compilable<NoCompileData>() {
    override val id = "sell_filters"

    override val description = "Which items a sale may sell, and extra multipliers for matching items."

    override val arguments = arguments {
        optional(
            "shops",
            description = "Only sell items from these shops' categories. If omitted, items from every shop can sell.",
            type = ArgType.STRING_LIST,
            default = "[]",
            example = listOf("main")
        )
        optional(
            "whitelist",
            description = "Only sell these shop item IDs or categories (category:<id>). If omitted, every item can sell.",
            type = ArgType.STRING_LIST,
            default = "[]",
            example = listOf("diamond", "category:minerals")
        )
        optional(
            "blacklist",
            description = "Never sell these shop item IDs or categories (category:<id>). Wins over the whitelist.",
            type = ArgType.STRING_LIST,
            default = "[]",
            example = listOf("cobblestone")
        )
        optional(
            "multipliers",
            description = "Extra multipliers for matching items. A shop item rule wins over a category rule.",
            type = ArgType.DYNAMIC,
            schema = SellMultiplierSpec::class
        )
    }
}
