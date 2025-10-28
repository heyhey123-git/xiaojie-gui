package io.github.heyhey123.xiaojiegui

import ch.njol.skript.Skript
import ch.njol.skript.SkriptAddon
import io.github.heyhey123.xiaojiegui.gui.menu.MenuSession
import io.github.heyhey123.xiaojiegui.listener.BukkitInventoryListener
import io.github.heyhey123.xiaojiegui.listener.PlayerQuitListener
import io.github.heyhey123.xiaojiegui.listener.ReceptaclePacketListener
import io.github.heyhey123.xiaojiegui.listener.StaticInventoryListener
import org.bukkit.plugin.java.JavaPlugin
import org.spigotmc.SpigotConfig.config

class XiaojieGUI : JavaPlugin() {
    companion object {
        lateinit var instance: XiaojieGUI
        lateinit var skriptAddon: SkriptAddon

        val enableAsyncCheck: Boolean = config.getBoolean("enable-async-check", true)
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

        skriptAddon = Skript.registerAddon(this).apply {
            loadClasses("io.github.heyhey123.xiaojiegui.skript", "elements")
        }
        logger.info("XiaojieGUI has been enabled!")
    }

    override fun onDisable() {
        MenuSession.apply {
            forEachSession { it.close() }
            clearSessions()
        }

        ReceptaclePacketListener.unregister()
        BukkitInventoryListener.unregister()
        PlayerQuitListener.unregister()
        StaticInventoryListener.unregister()
        logger.info("XiaojieGUI has been disabled!")
    }
}
