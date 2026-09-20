package io.github.heyhey123.xiaojiegui.listener

import ch.njol.skript.ScriptLoader
import ch.njol.skript.config.Config
import io.github.heyhey123.xiaojiegui.gui.menu.Menu

/**
 * Closes every menu before Skript loads scripts.
 *
 * ScriptPreInitEvent runs once for the batch, before parsing the new scripts, on initial load and reload.
 * Menus retain callbacks from their scripts, so the old menus must go before newly loaded scripts rebuild
 * them. The first load finds no menus. Keep the same listener instance for registration and removal.
 */
object ScriptLoadListener : ScriptLoader.ScriptPreInitEvent, BaseListener {

    override fun register() {
        ScriptLoader.eventRegistry().register(this)
    }

    override fun unregister() {
        ScriptLoader.eventRegistry().unregister(this)
    }

    override fun onPreInit(configs: Collection<Config>) {
        Menu.destroyAll()
    }
}
