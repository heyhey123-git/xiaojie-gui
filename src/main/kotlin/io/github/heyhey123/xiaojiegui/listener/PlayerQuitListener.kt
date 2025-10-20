package io.github.heyhey123.xiaojiegui.listener

import io.github.heyhey123.xiaojiegui.gui.menu.MenuSession
import io.github.heyhey123.xiaojiegui.gui.receptacle.ViewReceptacle.Companion.removeViewingReceptacle
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

object PlayerQuitListener : Listener, BaseListener {

    override fun unregister() {
        PlayerQuitEvent.getHandlerList().unregister(this)
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        val player = event.player
        MenuSession.querySession(player)?.shut()
        MenuSession.removeSession(player)
        player.removeViewingReceptacle()
    }
}
