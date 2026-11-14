package com.willfp.ecoshop.commands

import com.willfp.ecoshop.sellchest.SellChestItem
import com.willfp.ecoshop.sellchest.SellChestTypes
import com.willfp.ecoshop.sellwand.SellWands
import com.willfp.ecoshop.sellwand.WandItem
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.inventory.ItemStack
import wiki.ecohub.core.command.CommandBuilder
import wiki.ecohub.core.command.CommandContext
import wiki.ecohub.game.drop.DropQueue
import wiki.ecohub.game.entity.runOwned

private const val AMOUNT_ARGS = 3

internal fun CommandBuilder<CommandSender>.giveWandCommand() = subcommand(
    name = "givewand",
    permission = "ecoshop.command.givewand"
) {
    executes {
        val player = Bukkit.getPlayer(argument(0, "must-specify-player")) ?: fail("invalid-player")
        val wand = SellWands[args.getOrNull(1)] ?: fail("sellwand.invalid-wand")
        val amount = giveAmount()

        player.runOwned {
            DropQueue(player).forceTelekinesis()
                .addItems(List(amount) { WandItem.create(wand) })
                .push()

            reply("sellwand.given", "amount" to amount, "wand" to wand.displayName, "player" to player.name)
        }
    }

    completes {
        giveCompletions { SellWands.values().map { it.id } }
    }
}

internal fun CommandBuilder<CommandSender>.giveSellChestCommand() = subcommand(
    name = "givesellchest",
    permission = "ecoshop.command.givesellchest"
) {
    executes {
        val player = Bukkit.getPlayer(argument(0, "must-specify-player")) ?: fail("invalid-player")
        val type = SellChestTypes[args.getOrNull(1)] ?: fail("sellchest.invalid-type")
        val amount = giveAmount()

        player.runOwned {
            DropQueue(player).forceTelekinesis()
                .addItems(stacksOf(amount) { SellChestItem.create(type) })
                .push()

            reply("sellchest.given", "amount" to amount, "type" to type.displayName, "player" to player.name)
        }
    }

    completes {
        giveCompletions { SellChestTypes.values().map { it.id } }
    }
}

private fun CommandContext<CommandSender>.giveAmount(): Int =
    args.getOrNull(2)?.toIntOrNull()?.coerceAtLeast(1) ?: 1

private fun CommandContext<CommandSender>.giveCompletions(ids: () -> List<String>): List<String> =
    when (args.size) {
        1 -> playerNames()
        2 -> ids()
        AMOUNT_ARGS -> giveAmountCompletions
        else -> emptyList()
    }

private fun stacksOf(amount: Int, create: () -> ItemStack): List<ItemStack> {
    val stacks = mutableListOf<ItemStack>()
    var left = amount

    while (left > 0) {
        val stack = create()
        stack.amount = left.coerceAtMost(stack.maxStackSize)
        left -= stack.amount
        stacks += stack
    }

    return stacks
}
