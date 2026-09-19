package io.github.heyhey123.xiaojiegui

import ch.njol.skript.Skript
import io.github.heyhey123.xiaojiegui.gui.menu.Menu
import io.github.heyhey123.xiaojiegui.listener.BukkitInventoryListener
import io.github.heyhey123.xiaojiegui.listener.PlayerQuitListener
import io.github.heyhey123.xiaojiegui.listener.ReceptaclePacketListener
import io.github.heyhey123.xiaojiegui.listener.ScriptLoadListener
import io.github.heyhey123.xiaojiegui.listener.StaticInventoryListener
import io.github.heyhey123.xiaojiegui.logging.LogoPrinter
import io.github.heyhey123.xiaojiegui.skript.registerElements
import io.github.heyhey123.xiaojiegui.skript.utils.Button
import org.bukkit.plugin.java.JavaPlugin

class XiaojieGUI : JavaPlugin() {
    companion object {
        lateinit var instance: XiaojieGUI

        val enableAsyncCheck: Boolean
            by lazy { instance.config.getBoolean("enable-async-check", true) }
        val forceTrueColor: Boolean
            by lazy { instance.config.getBoolean("force-truecolor", true) }
    }

    @Suppress("Unused")
    override fun onLoad() {
        instance = this
    }

    override fun onEnable() {
        saveDefaultConfig()

        ReceptaclePacketListener.register()
        BukkitInventoryListener.register()
        PlayerQuitListener.register()
        StaticInventoryListener.register()
        ScriptLoadListener.register()

        Skript.instance().registerAddon(XiaojieGUI::class.java, pluginMeta.name)
            .let(::registerElements)

        LogoPrinter.print(pluginMeta.version)
        logger.info("XiaojieGUI has been enabled!")
    }

    override fun onDisable() {
        Menu.destroyAll()

        ReceptaclePacketListener.unregister()
        BukkitInventoryListener.unregister()
        PlayerQuitListener.unregister()
        StaticInventoryListener.unregister()
        ScriptLoadListener.unregister()
        Button.buttons.clear()
        logger.info("XiaojieGUI has been disabled!")
    }
}
