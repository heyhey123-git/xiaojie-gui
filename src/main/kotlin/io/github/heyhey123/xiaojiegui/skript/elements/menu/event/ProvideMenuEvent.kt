package io.github.heyhey123.xiaojiegui.skript.elements.menu.event

import io.github.heyhey123.xiaojiegui.gui.menu.Menu
import io.github.heyhey123.xiaojiegui.skript.utils.SkriptSyntax
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import org.skriptlang.skript.addon.SkriptAddon

/**
 * An event that is used to provide a menu instance in Skript.
 *
 * @property menu The menu that is being provided.
 */
class ProvideMenuEvent(val menu: Menu) : Event() {
    companion object {
        /**
         * Publishes the menu this event carries, which is how a section body that is not running in a
         * menu event still has one.
         */
        fun register(addon: SkriptAddon) {
            SkriptSyntax.eventValue(addon, ProvideMenuEvent::class.java, Menu::class.java, ProvideMenuEvent::menu)
        }
    }

    override fun getHandlers(): HandlerList =
        throw UnsupportedOperationException("Illegal Access to HandlerList of ProvideMenuEvent")
}
