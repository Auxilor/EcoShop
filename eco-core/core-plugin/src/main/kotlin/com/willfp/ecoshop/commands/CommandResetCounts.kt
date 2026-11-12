package com.willfp.ecoshop.commands

import com.willfp.ecoshop.shop.ShopItem
import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import wiki.ecohub.core.command.CommandBuilder
import wiki.ecohub.core.command.CommandContext
import wiki.ecohub.data.playerbase.forEachSavedProfile
import wiki.ecohub.data.profile.PlayerProfile
import wiki.ecohub.game.entity.player.PlayerUtils
import java.util.logging.Level

/**
 * `<resetbuys|resetsells> <player|all> <item|all>`.
 */
internal fun CommandBuilder<CommandSender>.resetCountsCommand(kind: ResetKind) = subcommand(
    name = kind.command,
    permission = "ecoshop.command.${kind.command}"
) {
    executes {
        val target = argument(0, "must-specify-player")
        argument(1, "must-specify-item")
        val player = if (target.equals("all", true)) null else knownPlayer(0)
        val items = shopItems(1)

        if (player == null) {
            resetAllPlayers(kind, items)
        } else {
            resetPlayer(kind, player, items)
        }
    }

    completes {
        when (args.size) {
            1 -> playerNamesAndAll()
            2 -> shopItemIdsAndAll()
            else -> emptyList()
        }
    }
}

private fun CommandContext<CommandSender>.resetPlayer(kind: ResetKind, player: OfflinePlayer, items: List<ShopItem>) {
    items.forEach { kind.reset(it, player) }

    if (items.size == 1) {
        replyWithoutPlaceholders(
            kind.key,
            "player" to PlayerUtils.getSavedDisplayName(player),
            "item" to items.first().displayName
        )
    } else {
        replyBatch(kind, players = 1, items = items.size)
    }
}

private fun CommandContext<CommandSender>.resetAllPlayers(kind: ResetKind, items: List<ShopItem>) {
    val resets = items.associateWith { kind.start(it) }
    var playerCount = 0

    forEachSavedProfile(plugin, onProgress = { _, total -> playerCount = total }) { uuid ->
        val profile = PlayerProfile.load(uuid)
        resets.forEach { (item, reset) -> kind.resetProfile(item, uuid, profile, reset) }
    }.whenComplete { _, _ ->
        resets.forEach { (item, reset) -> kind.finish(item, reset) }
    }.thenRun {
        plugin.scheduler.global().run {
            if (playerCount == 0) {
                reply("invalid-player")
            } else {
                replyBatch(kind, players = playerCount, items = items.size)
            }
        }
    }.exceptionally { error ->
        plugin.logger.log(Level.SEVERE, "Failed to ${kind.command} for every player", error)
        null
    }
}

private fun CommandContext<CommandSender>.replyBatch(kind: ResetKind, players: Int, items: Int) {
    replyWithoutPlaceholders(
        "${kind.key}-batch",
        "players" to players.toString(),
        "items" to items.toString(),
        "resets" to (players * items).toString()
    )
}
