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
import java.io.File

class XiaojieGUI : JavaPlugin() {
    companion object {
        lateinit var instance: XiaojieGUI
    }

    @Suppress("Unused")
    override fun onLoad() {
        instance = this
    }

    override fun onEnable() {
        // No `saveDefaultConfig()`: this plugin has nothing to configure, so it does not write a config
        // file either. What used to be two options is now unconditional -- the main-thread check always
        // runs, and the banner follows the console on its own (see `LogoPrinter`).
        ReceptaclePacketListener.register()
        BukkitInventoryListener.register()
        PlayerQuitListener.register()
        StaticInventoryListener.register()
        ScriptLoadListener.register()

        // The language directory has to be declared before the elements are registered, because
        // Skript reads `<dir>/default.lang` out of this plugin's own jar at that moment
        // (`LocalizerImpl.setSourceDirectories` calls `Language.loadDefault`), and an addon that
        // never declares one has its `lang/` folder ignored entirely. `types.menu` and
        // `types.menusession` live there: without them Skript names those types `types.menu` in its
        // own error messages. The second directory is where a server owner may drop a language file
        // of their own; this plugin ships no translations of its own.
        val addon = Skript.instance().registerAddon(XiaojieGUI::class.java, pluginMeta.name)
        addon.localizer().setSourceDirectories("lang", File(dataFolder, "lang").path)
        registerElements(addon)

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
