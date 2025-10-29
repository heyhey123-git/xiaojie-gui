package io.github.heyhey123.xiaojiegui.listener

import io.github.heyhey123.xiaojiegui.gui.StaticInventory
import io.github.heyhey123.xiaojiegui.gui.interact.BukkitClickType
import io.github.heyhey123.xiaojiegui.gui.interact.ClickType
import io.github.heyhey123.xiaojiegui.gui.receptacle.ViewReceptacle.Companion.viewingReceptacle
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent

object StaticInventoryListener : Listener, BaseListener {

    override fun unregister() {
        InventoryClickEvent.getHandlerList().unregister(this)
        InventoryCloseEvent.getHandlerList().unregister(this)
        InventoryDragEvent.getHandlerList().unregister(this)
    }

    @EventHandler
    fun onClick(event: InventoryClickEvent) {
        if (event.inventory.holder !is StaticInventory.Holder) return
//        event.isCancelled = true
        val player = event.whoClicked as? Player ?: return
        val receptacle = player.viewingReceptacle ?: return
        val clickType = ClickType.fromBukkit(
            event.click,
            event.action,
            if (event.click == BukkitClickType.NUMBER_KEY) event.hotbarButton else event.slot
        )
        receptacle.clicked(clickType, event.slot, event)
    }

    @EventHandler
    fun onClose(event: InventoryCloseEvent) {
        if (
            event.inventory.holder !is StaticInventory.Holder ||
            event.reason == InventoryCloseEvent.Reason.OPEN_NEW
        ) return
        val player = event.player as? Player ?: return
        val receptacle = player.viewingReceptacle ?: return
        receptacle.closed()
    }

//    @EventHandler
//    fun onDrag(event: InventoryDragEvent) {
//        if (event.inventory.holder !is StaticInventory.Holder) return
//        event.isCancelled = true
//    }
}
