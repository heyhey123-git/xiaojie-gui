package io.github.heyhey123.xiaojiegui.it

import io.github.heyhey123.xiaojiegui.it.command.CommandsRegistry
import org.bukkit.plugin.java.JavaPlugin

class ItPlugin: JavaPlugin() {
    companion object {
        lateinit var instance: ItPlugin
    }

    override fun onEnable() {
        instance = this
        CommandsRegistry.register()
        logger.info("ItPlugin has been enabled!")
    }
}
