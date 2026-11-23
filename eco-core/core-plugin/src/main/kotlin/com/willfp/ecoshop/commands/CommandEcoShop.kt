package com.willfp.ecoshop.commands

import com.willfp.ecoshop.EcoShopPlugin
import wiki.ecohub.core.command.command

internal fun EcoShopPlugin.ecoShopCommand() = command(name = "ecoshop", permission = "ecoshop.command.ecoshop") {
    reloadCommand()
    resetCountsCommand(ResetKind.BUYS)
    resetCountsCommand(ResetKind.SELLS)
    resetDynamicPricingCommand()
    rotateCommand()
    historyCommand()
    giveWandCommand()
    giveSellChestCommand()
    sellChestCountCommand()
}
