package com.willfp.ecoshop.config

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.config.ConfigType
import com.willfp.eco.core.config.ExtendableConfig

class UsermadeConfig(name: String, directory: String, plugin: EcoPlugin) : ExtendableConfig(
    name,
    true,
    plugin,
    plugin::class.java,
    "$directory/",
    ConfigType.YAML
)
