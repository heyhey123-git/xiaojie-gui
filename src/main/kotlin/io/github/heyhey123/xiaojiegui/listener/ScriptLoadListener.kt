package io.github.heyhey123.xiaojiegui.listener

import ch.njol.skript.events.bukkit.PreScriptLoadEvent
import io.github.heyhey123.xiaojiegui.gui.menu.Menu
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

/**
 * Closes every menu before Skript loads scripts.
 *
 * This fires on the first load and on every reload. A menu holds the callbacks of the scripts that built
 * it, so a reload leaves them pointing at triggers that are about to be unloaded: the next click would
 * run code that is no longer in any script, or nothing at all. Menus are script state, so they are
 * closed with the scripts, and the scripts that load build their own.
 *
 * The first load finds no menus, which is why this needs no "is this a reload" test.
 */
object ScriptLoadListener : Listener, BaseListener {

    override fun unregister() {
        PreScriptLoadEvent.getHandlerList().unregister(this)
    }

    @EventHandler
    fun onPreScriptLoad(event: PreScriptLoadEvent) {
        Menu.destroyAll()
    }
}
