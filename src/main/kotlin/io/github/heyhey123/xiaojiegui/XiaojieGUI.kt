package io.github.heyhey123.xiaojiegui

import ch.njol.skript.Skript
import io.github.heyhey123.xiaojiegui.gui.menu.Menu
import io.github.heyhey123.xiaojiegui.listener.BukkitInventoryListener
import io.github.heyhey123.xiaojiegui.listener.PlayerQuitListener
import io.github.heyhey123.xiaojiegui.listener.ReceptaclePacketListener
import io.github.heyhey123.xiaojiegui.listener.StaticInventoryListener
import io.github.heyhey123.xiaojiegui.logging.LogoPrinter
import org.bukkit.plugin.java.JavaPlugin
import org.spigotmc.SpigotConfig.config

class XiaojieGUI : JavaPlugin() {
    companion object {
        lateinit var instance: XiaojieGUI

        val enableAsyncCheck: Boolean
                by lazy { config.getBoolean("enable-async-check", true) }
        val forceTrueColor: Boolean
                by lazy { config.getBoolean("force-truecolor", true) }
    }

    @Suppress("Unused")
    override fun onLoad() {
        instance = this
    }

    override fun onEnable() {
        ReceptaclePacketListener.register()
        BukkitInventoryListener.register()
        PlayerQuitListener.register()
        StaticInventoryListener.register()

        Skript.registerAddon(this)
            .loadClasses("io.github.heyhey123.xiaojiegui.skript", "elements")

        LogoPrinter.print(pluginMeta.version)
        logger.info("XiaojieGUI has been enabled!")
    }

    override fun onDisable() {
        Menu.menus.forEach { it.destroy() }

        ReceptaclePacketListener.unregister()
        BukkitInventoryListener.unregister()
        PlayerQuitListener.unregister()
        StaticInventoryListener.unregister()
        logger.info("XiaojieGUI has been disabled!")
    }
}
