package com.willfp.ecoshop.sellwand

import org.bukkit.Material
import org.bukkit.Tag

/** Container materials a wand or effect may target. "shulker_box" matches every colour. */
class ContainerTypes(names: List<String>) {
    private val includeShulkers = names.any { it.equals("shulker_box", ignoreCase = true) }

    private val materials = names
        .filterNot { it.equals("shulker_box", ignoreCase = true) }
        .mapNotNull { Material.matchMaterial(it) }
        .toSet()

    fun matches(material: Material): Boolean =
        material in materials || (includeShulkers && Tag.SHULKER_BOXES.isTagged(material))
}
