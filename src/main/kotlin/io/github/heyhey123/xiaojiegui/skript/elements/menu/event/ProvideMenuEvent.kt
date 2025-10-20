package io.github.heyhey123.xiaojiegui.skript.elements.menu.event

import ch.njol.skript.registrations.EventValues
import io.github.heyhey123.xiaojiegui.gui.menu.Menu
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * An event that is fired when a menu is being provided.
 *
 * @property menu The menu that is being provided.
 */
class ProvideMenuEvent(private val menu: Menu) : Event() {
    companion object {
        init {
            EventValues.registerEventValue(
                ProvideMenuEvent::class.java,
                Menu::class.java,
                ProvideMenuEvent::getMenu
            )
        }
    }

    fun getMenu(): Menu = menu

    override fun getHandlers(): HandlerList =
        throw UnsupportedOperationException("Illegal Access to HandlerList of ProvideMenuEvent")

}
