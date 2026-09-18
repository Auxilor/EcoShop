package com.willfp.ecoshop.sellwand

import com.willfp.ecoshop.sell.ItemFilter
import com.willfp.ecoshop.sell.SellBypass
import com.willfp.libreforge.conditions.ConditionList

/** The settings one wand use runs with: from a wand type, or inline effect args. */
data class WandProfile(
    val wand: SellWand?,
    val limits: WandLimits?,
    val multiplier: Double,
    val maxItems: Int,
    val maxValue: Double,
    val containers: ContainerTypes,
    val filter: ItemFilter,
    val bypass: SellBypass,
    val conditions: ConditionList?
) {
    companion object {
        fun of(wand: SellWand) = WandProfile(
            wand = wand,
            limits = wand.limits,
            multiplier = wand.multiplier,
            maxItems = wand.maxItemsPerUse,
            maxValue = wand.maxValuePerUse,
            containers = wand.containers,
            filter = wand.filter,
            bypass = wand.bypass,
            conditions = wand.conditions
        )
    }
}
