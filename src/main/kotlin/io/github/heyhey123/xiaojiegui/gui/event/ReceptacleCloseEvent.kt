package io.github.heyhey123.xiaojiegui.gui.event

import io.github.heyhey123.xiaojiegui.gui.receptacle.Receptacle
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class ReceptacleCloseEvent(
    val player: Player,
    val receptacle: Receptacle
) : Event() {

    companion object {
        private val HANDLERS = HandlerList()

        @JvmStatic
        fun getHandlerList() = HANDLERS
    }

    override fun getHandlers() = HANDLERS
}
