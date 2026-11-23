package com.willfp.ecoshop.commands

import com.willfp.ecoshop.shop.ShopItem
import com.willfp.ecoshop.shop.ShopItems
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import wiki.ecohub.core.command.CommandContext
import wiki.ecohub.ui.text.StringUtils

internal val giveAmountCompletions = listOf("1", "8", "16")

/**
 * The player named by argument [index], if they are online or have joined before.
 */
internal fun CommandContext<*>.knownPlayer(index: Int): OfflinePlayer {
    @Suppress("DEPRECATION")
    val player = Bukkit.getOfflinePlayer(argument(index, "must-specify-player"))

    if (!player.hasPlayedBefore() && !player.isOnline) {
        fail("invalid-player")
    }

    return player
}

/**
 * The shop items named by argument [index]: one, or every item for `all`.
 */
internal fun CommandContext<*>.shopItems(index: Int): List<ShopItem> {
    val id = argument(index, "must-specify-item")
    val items = if (id.equals("all", true)) ShopItems.values().toList() else listOfNotNull(ShopItems[id])
    return items.ifEmpty { fail("invalid-item") }
}

internal fun shopItemIdsAndAll(): List<String> = ShopItems.values().map { it.id } + "all"

internal fun playerNamesAndAll(): List<String> = Bukkit.getOnlinePlayers().map { it.name } + "all"

/**
 * Send the lang message [key] with [replacements], without translating placeholders in it first.
 */
internal fun CommandContext<*>.replyWithoutPlaceholders(key: String, vararg replacements: Pair<String, String>) {
    val message = plugin.langYml.getMessage(key, StringUtils.FormatOption.WITHOUT_PLACEHOLDERS)

    sender.sendMessage(
        replacements.fold(message) { replaced, (placeholder, value) ->
            replaced.replace(oldValue = "%$placeholder%", newValue = value)
        }
    )
}
