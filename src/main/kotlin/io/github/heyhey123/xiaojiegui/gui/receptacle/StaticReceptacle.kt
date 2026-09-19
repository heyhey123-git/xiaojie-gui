package io.github.heyhey123.xiaojiegui.gui.receptacle

import io.github.heyhey123.xiaojiegui.gui.StaticInventory
import io.github.heyhey123.xiaojiegui.gui.event.ReceptacleInteractEvent
import io.github.heyhey123.xiaojiegui.gui.interact.ClickType
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.inventory.ItemStack

class StaticReceptacle(
    title: Component,
    layout: ViewLayout
) : ViewReceptacle(title, layout, Mode.STATIC) {

    /**
     * The current static inventory holder for the receptacle.
     */
    private var currentInventoryHolder: StaticInventory.Holder = StaticInventory.create(layout, title)

    override fun getElement(slot: Int): ItemStack? =
        // Slots past the container belong to the player's own inventory, which this mode does not own: an
        // item placed there is the player's, and there is nothing here that could be read or written.
        if (slot in layout.containerSlotRange) currentInventoryHolder.inventory.getItem(slot) else null

    override fun setElement(slot: Int, item: ItemStack?) {
        require(slot in layout.containerSlotRange) {
            "Slot $slot is not part of this menu: a ${mode.id} menu owns its ${layout.containerSize} container slots, and the rest of the window is the player's own inventory."
        }
        currentInventoryHolder.inventory.setItem(slot, item)
    }

    override fun doOpen(player: Player) {
        StaticInventory.open(player, currentInventoryHolder)
    }

    override fun doClose() {
        StaticInventory.close(viewer!!)
    }

    override fun interruptItemDrag(event: ReceptacleInteractEvent) {
        // has been handled in clicked method
    }

    override fun title(title: Component, render: Boolean) {
        this.title = title
        // `render` decides whether the client is told now. Menu.turnPage loads the page with render
        // false and then sends one title update itself, so pushing here unconditionally re-sent the
        // whole window twice on every page turn.
        if (!render || viewer == null) return
        currentInventoryHolder.setTitle(title)
    }

    override fun clear(render: Boolean) {
        currentInventoryHolder.inventory.clear()
    }

    override fun refresh(slot: Int) {
        // nop
    }

    override fun clicked(clickType: ClickType, slot: Int, staticInventoryEvent: InventoryClickEvent?) {
        val event = ReceptacleInteractEvent(viewer!!, this, clickType, slot)
        onClick(event)

        if (!event.callEvent()) {
            staticInventoryEvent!!.isCancelled = true
        }
    }

    override fun dragged(
        clickType: ClickType,
        slots: List<Int>,
        cursor: ItemStack?,
        dragEvent: InventoryDragEvent?
    ) {
        val event = ReceptacleInteractEvent(viewer!!, this, clickType, slots.first(), slots, cursor)
        onClick(event)

        // The inventory behind this mode is real, so a cancelled drag has to be stopped here and now:
        // the items are already in the slots the drag reached, and nothing else would put them back.
        if (!event.callEvent()) {
            dragEvent!!.isCancelled = true
        }
    }
}
