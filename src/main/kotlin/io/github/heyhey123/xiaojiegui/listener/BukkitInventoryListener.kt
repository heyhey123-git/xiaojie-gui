package io.github.heyhey123.xiaojiegui.listener

import io.github.heyhey123.xiaojiegui.gui.menu.MenuSession
import io.github.heyhey123.xiaojiegui.gui.receptacle.Receptacle
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.player.PlayerChangedWorldEvent

object BukkitInventoryListener : Listener, BaseListener {

    override fun unregister() {
        InventoryOpenEvent.getHandlerList().unregister(this)
        PlayerChangedWorldEvent.getHandlerList().unregister(this)
        PlayerDeathEvent.getHandlerList().unregister(this)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onOpen(event: InventoryOpenEvent) {
        val player = event.player as Player
        val session = MenuSession.querySession(player) ?: return
        if (session.receptacle?.mode != Receptacle.Mode.PHANTOM) return
        // close the phantom inventory session

        session.close()
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerChangeWorld(event: PlayerChangedWorldEvent) {
        // `close` rather than `shut`: the window is still there while the script's `on menu close` runs, so
        // a backpack that saves on close keeps what the player was carrying across the world change.
        MenuSession.querySession(event.player)?.close()
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerDeath(event: PlayerDeathEvent) {
        // The same, for the same reason: a death is one of the ways a window ends without the player
        // closing it, and the script has to get its one chance to read the contents first.
        MenuSession.querySession(event.entity)?.close()
    }
}
