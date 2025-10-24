package io.github.heyhey123.xiaojiegui.gui.event

import io.github.heyhey123.xiaojiegui.gui.interact.ClickType
import io.github.heyhey123.xiaojiegui.gui.receptacle.Receptacle
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class ReceptacleInteractEvent(
    val player: Player,
    val receptacle: Receptacle,
    val clickType: ClickType,
    val slot: Int
) : Event(), Cancellable {

    private var cancelled = false

    override fun getHandlers() = HANDLERS

    override fun isCancelled(): Boolean = cancelled

    override fun setCancelled(cancel: Boolean) {
        this.cancelled = cancel
    }

    companion object {
        private val HANDLERS = HandlerList()
        fun getHandlerList() = HANDLERS
    }
}
