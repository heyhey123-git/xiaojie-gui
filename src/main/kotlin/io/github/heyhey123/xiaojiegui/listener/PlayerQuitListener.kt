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
        // `close` first, so the window is still there while the script's `on menu close` runs: that is the
        // only chance a backpack has to save what the player was carrying when he left, and `shut` alone
        // dropped the inventory before anyone could look at it. `shut` runs inside `close`; the two calls
        // below stay as the belt and braces for a player whose session was taken away by something else.
        MenuSession.querySession(player)?.close()
        // `shut` drops a static menu's inventory too, and the call is repeated here for a player whose
        // session was taken away by something else: the entry is keyed by UUID and would outlive them.
        StaticInventory.forget(player)
        MenuSession.removeSession(player)
        player.removeViewingReceptacle()
    }
}
