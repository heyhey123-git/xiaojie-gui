package io.github.heyhey123.xiaojiegui.gui.event

import io.github.heyhey123.xiaojiegui.gui.menu.Menu
import io.github.heyhey123.xiaojiegui.gui.menu.MenuSession
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class MenuCloseEvent(
    val session: MenuSession,
    val viewer: Player,
    val menu: Menu
) : Event() {

    companion object {
        private val HANDLERS = HandlerList()

        fun getHandlers(): HandlerList = HANDLERS
    }

    override fun getHandlers(): HandlerList = HANDLERS
}
