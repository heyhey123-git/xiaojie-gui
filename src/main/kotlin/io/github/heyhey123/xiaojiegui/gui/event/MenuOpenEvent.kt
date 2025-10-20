package io.github.heyhey123.xiaojiegui.gui.event

import io.github.heyhey123.xiaojiegui.gui.menu.Menu
import io.github.heyhey123.xiaojiegui.gui.menu.MenuSession
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class MenuOpenEvent(
    val session: MenuSession,
    val viewer: Player,
    val menu: Menu,
    val page: Int
) : Event(), Cancellable {

    companion object {
        private val HANDLERS = HandlerList()

        fun getHandlers(): HandlerList = HANDLERS
    }

    private var cancelled = false

    override fun getHandlers(): HandlerList = HANDLERS

    override fun isCancelled(): Boolean = cancelled

    override fun setCancelled(cancel: Boolean) {
        cancelled = true
    }
}
