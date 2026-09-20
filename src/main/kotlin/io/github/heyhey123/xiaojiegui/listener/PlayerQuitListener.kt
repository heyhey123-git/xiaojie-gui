package io.github.heyhey123.xiaojiegui.listener

import io.github.heyhey123.xiaojiegui.gui.StaticInventory
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
        // `shut` drops a static menu's inventory too, and the call is repeated here for a player whose
        // session was taken away by something else: the entry is keyed by UUID and would outlive them.
        MenuSession.querySession(player)?.shut()
        StaticInventory.forget(player)
        MenuSession.removeSession(player)
        player.removeViewingReceptacle()
    }
}
