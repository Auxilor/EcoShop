package com.willfp.ecoshop.shop

/** Represents the ability of a player to buy an item. */
enum class BuyStatus {
    CANNOT_BUY,
    BOUGHT_TOO_MANY,
    NO_PERMISSION,
    MISSING_REQUIREMENTS,
    CANNOT_AFFORD,
    ALLOW
}

/** Represents the ability of a player to sell an item. */
enum class SellStatus {
    CANNOT_SELL,
    NO_PERMISSION,
    MISSING_REQUIREMENTS,
    DONT_HAVE_ITEM,
    DONT_HAVE_ENOUGH,
    ALLOW
}

val <T : Enum<T>> T.configKey: String
    get() = this.name.lowercase().replace("_", "-")
