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
        // The raw slot, not `getSlot()`: a click in the player's own half of the window is numbered
        // relative to the inventory that was clicked there, so it would name a container slot the player
        // never touched -- and a callback for that slot would run. Both modes number window slots, so
        // this is the number a script can compare with the slots its layout declared.
        //
        // A number key is the one click whose meaning is carried by the button instead: it names the
        // hotbar slot the stack is swapped with, so that is what `the pressed number key` reads.
        val numberKeySlot = if (event.click == BukkitClickType.NUMBER_KEY) event.hotbarButton else event.rawSlot
        receptacle.clicked(ClickType.fromBukkit(event.click, numberKeySlot), event.rawSlot, event)
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
        // Looked up rather than asserted: the server closes a container of its own accord when
        // `stillValid` turns false (`ServerPlayer.tick` -> `closeContainer`), and that can arrive after
        // this addon has already dropped the player's entry -- on a quit, or after the menu was
        // destroyed. There is no "switching between static menus" to tell apart then, so the close is
        // passed on as the close it is.
        val viewing = player.staticInventory?.holder
        if (
            viewing != null &&
            viewing != holder &&
            event.reason == InventoryCloseEvent.Reason.OPEN_NEW
        ) {
            return // Ignore if the player is switching between static inventory menus
        }

        receptacle.closed()
    }
}
