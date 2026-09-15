package com.willfp.ecoshop.sell

/** Where a sale originated. Written to logs and events. */
enum class SellSource {
    COMMAND,
    GUI,
    MENU,
    ADAPTER,
    WAND,
    CHEST
}
