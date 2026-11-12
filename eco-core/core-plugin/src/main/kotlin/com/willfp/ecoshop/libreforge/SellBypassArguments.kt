package com.willfp.ecoshop.libreforge

import wiki.ecohub.config.ArgType
import wiki.ecohub.config.arguments
import wiki.ecohub.core.registry.Compilable
import wiki.ecohub.core.registry.NoCompileData

object SellBypassArguments : Compilable<NoCompileData>() {
    override val id = "sell_bypass"

    override val description = "Shop checks a sale skips. A bypassed check is ignored, and the sale doesn't count towards it."

    override val arguments = arguments {
        optional(
            "dynamic-pricing",
            description = "Whether to pay the base price and not move the dynamic price.",
            type = ArgType.BOOLEAN,
            default = "false"
        )
        optional(
            "player-limits",
            description = "Whether to ignore sell.limit and not count the sale towards it.",
            type = ArgType.BOOLEAN,
            default = "false"
        )
        optional(
            "global-limits",
            description = "Whether to ignore sell.global-limit and not count the sale towards it.",
            type = ArgType.BOOLEAN,
            default = "false"
        )
    }
}
