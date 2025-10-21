package io.github.heyhey123.xiaojiegui.gui.event

import io.github.heyhey123.xiaojiegui.gui.menu.Menu
import io.github.heyhey123.xiaojiegui.gui.menu.MenuSession
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * An event that is called when a player turns a page in a menu.
 *
 * @param session The menu session of the player turning the page.
 * @param from The page number the player is turning from.
 * @param to The page number the player is turning to.
 * @param title The title of the menu for the new page.
 */
class PageTurnEvent(
    val session: MenuSession,
    val viewer: Player,
    val menu: Menu,
    val from: Int,
    val to: Int,
    var title: Component
) : Event(), Cancellable {

    companion object {

        private val HANDLERS = HandlerList()

        fun getHandlers(): HandlerList = HANDLERS

    }

    private var cancelled = false

    override fun getHandlers(): HandlerList = HANDLERS

    override fun isCancelled(): Boolean = cancelled

    override fun setCancelled(cancel: Boolean) {
        cancelled = false
    }
}
