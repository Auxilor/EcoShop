package com.willfp.ecoshop.shop.rotation

import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

data class CategoryRotationState(
    val categoryId: String,
    // Ordered list of item IDs per r-slot index; "" means empty slot.
    val activeItems: List<String>,
    val nextRotation: Long
) {
    fun save(dir: File) {
        dir.mkdirs()
        val file = File(dir, "$categoryId.yml")
        val yaml = YamlConfiguration()
        yaml.set("active-items", activeItems)
        yaml.set("next-rotation", nextRotation)
        yaml.save(file)
    }

    companion object {
        fun load(categoryId: String, dir: File): CategoryRotationState {
            val file = File(dir, "$categoryId.yml")
            if (!file.exists()) return CategoryRotationState(categoryId, emptyList(), 0L)
            val yaml = YamlConfiguration.loadConfiguration(file)
            return CategoryRotationState(
                categoryId = categoryId,
                activeItems = yaml.getStringList("active-items"),
                nextRotation = yaml.getLong("next-rotation", 0L)
            )
        }
    }
}
