package com.willfp.ecoshop.shop.gui

import com.willfp.ecoshop.shop.ShopItem
import com.willfp.ecoshop.shop.parentShop
import org.bukkit.entity.Player
import wiki.ecohub.config.Config
import wiki.ecohub.game.item.Items
import wiki.ecohub.modules.menus.component.GUIComponent
import wiki.ecohub.modules.menus.menu.Menu
import wiki.ecohub.modules.menus.menu.menuStateVar
import wiki.ecohub.modules.menus.onLeftClick
import wiki.ecohub.modules.menus.slot
import wiki.ecohub.modules.menus.slot.Slot
import kotlin.math.min

val Menu.amountOfItem by menuStateVar(1)

class AmountAdjustSelector(
    private val item: ShopItem,
    private val adjust: Int,
    config: Config
) : GUIComponent {
    private val slot = slot(Items.lookup(config.getString("item"))) {
        onLeftClick { player, _, _, menu ->
            menu.parentShop[player]?.clickSound?.playTo(player)
            menu.amountOfItem[player] += adjust
        }
    }

    override fun getRows() = 1
    override fun getColumns() = 1

    override fun getSlotAt(row: Int, column: Int, player: Player, menu: Menu): Slot? {
        val currentAmount = menu.amountOfItem[player]

        val adjusted = currentAmount + adjust

        val max = min(item.getMaxBuysAtOnce(player), item.item?.item?.type?.maxStackSize ?: Int.MAX_VALUE)

        return if (adjusted !in 1..max) {
            null
        } else {
            slot
        }
    }
}

class AmountSetSelector(
    config: Config,
    private val amount: (Player) -> Int
) : GUIComponent {
    private val slot = slot(Items.lookup(config.getString("item"))) {
        onLeftClick { player, _, _, menu ->
            menu.parentShop[player]?.clickSound?.playTo(player)
            menu.amountOfItem[player] = amount(player)
        }
    }

    override fun getRows() = 1
    override fun getColumns() = 1

    override fun getSlotAt(row: Int, column: Int, player: Player, menu: Menu): Slot? {
        val currentAmount = menu.amountOfItem[player]
        val setAmount = amount(player)

        return if (setAmount < 1 || currentAmount == setAmount) {
            null
        } else {
            slot
        }
    }
}
