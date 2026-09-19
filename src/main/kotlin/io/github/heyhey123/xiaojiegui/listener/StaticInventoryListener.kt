package io.github.heyhey123.xiaojiegui.listener

import io.github.heyhey123.xiaojiegui.gui.StaticInventory
import io.github.heyhey123.xiaojiegui.gui.StaticInventory.staticInventory
import io.github.heyhey123.xiaojiegui.gui.interact.BukkitClickType
import io.github.heyhey123.xiaojiegui.gui.interact.ClickType
import io.github.heyhey123.xiaojiegui.gui.receptacle.StaticReceptacle
import io.github.heyhey123.xiaojiegui.gui.receptacle.ViewReceptacle.Companion.viewingReceptacle
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.DragType
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
        if (event.inventory.getHolder(false) !is StaticInventory.Holder) return
        val player = event.whoClicked as? Player ?: return
        val receptacle = player.viewingReceptacle ?: return
        val clickType = ClickType.fromBukkit(
            event.click,
            event.action,
            if (event.click == BukkitClickType.NUMBER_KEY) event.hotbarButton else event.slot
        )
        // The raw slot, not `getSlot()`: a click in the player's own half of the window is numbered
        // relative to the inventory that was clicked there, so it would name a container slot the player
        // never touched -- and a callback for that slot would run. Both modes number window slots, so
        // this is the number a script can compare with the slots its layout declared.
        receptacle.clicked(clickType, event.rawSlot, event)
    }

    @EventHandler
    fun onDrag(event: InventoryDragEvent) {
        // For a drag it is the top inventory that says whether this window is ours: the slots a drag
        // touches can be in either half of it.
        if (event.view.topInventory.holder !is StaticInventory.Holder) return
        val player = event.whoClicked as? Player ?: return
        val receptacle = player.viewingReceptacle as? StaticReceptacle ?: return
        // A drag carries the button it was made with, and, unlike a click, it is one interaction however
        // many slots it reached: the server applies all of it or none of it, so a script cannot be asked
        // to accept half of one. Bukkit names the two buttons after what they do rather than after the
        // button: `EVEN` spreads the stack over the slots (left), `SINGLE` puts one item in each (right).
        val clickType = if (event.type == DragType.SINGLE) ClickType.RIGHT else ClickType.LEFT
        receptacle.dragged(clickType, event.rawSlots.sorted(), event.oldCursor, event)
    }

    @EventHandler
    fun onClose(event: InventoryCloseEvent) {
        val holder = event.inventory.getHolder(false)
        if (holder !is StaticInventory.Holder) return
        val player = event.player as? Player ?: return
        val receptacle = player.viewingReceptacle ?: return
        if (
            player.staticInventory!!.holder != holder &&
            event.reason == InventoryCloseEvent.Reason.OPEN_NEW
        ) {
            return // Ignore if the player is switching between static inventory menus
        }

        receptacle.closed()
    }
}
