package io.github.heyhey123.xiaojiegui.gui.receptacle

import io.github.heyhey123.xiaojiegui.gui.StaticInventory
import io.github.heyhey123.xiaojiegui.gui.event.ReceptacleInteractEvent
import io.github.heyhey123.xiaojiegui.gui.interact.ClickType
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
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
        currentInventoryHolder.inventory.getItem(slot)

    override fun setElement(slot: Int, item: ItemStack?) {
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
        if (viewer == null) return
        currentInventoryHolder.setTitle(title)
    }

    override fun clear(render: Boolean) {
        currentInventoryHolder.inventory.clear()
    }

    override fun refresh(slot: Int) {
        //nop
    }

    override fun clicked(clickType: ClickType, slot: Int, staticInventoryEvent: InventoryClickEvent?) {
        val event = ReceptacleInteractEvent(viewer!!, this, clickType, slot)
        onClick(event)

        if (!event.callEvent()) {
            staticInventoryEvent!!.isCancelled = true
        }
    }
}
