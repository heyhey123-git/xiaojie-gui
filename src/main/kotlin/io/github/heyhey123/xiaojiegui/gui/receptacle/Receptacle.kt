package io.github.heyhey123.xiaojiegui.gui.receptacle

import io.github.heyhey123.xiaojiegui.gui.event.ReceptacleInteractEvent
import io.github.heyhey123.xiaojiegui.gui.layout.Layout
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

abstract class Receptacle(title: Component, val layout: Layout, val mode: Mode) {

    /**
     * Mode of the receptacle, either STATIC or PHANTOM.
     *
     * STATIC: Elements can be moved and interacted with normally.
     * PHANTOM: Elements are fixed and cannot be moved, but can still be interacted.
     */
    enum class Mode {
        STATIC, PHANTOM
    }

    var title: Component = title
        protected set

    protected abstract var onOpen: (player: Player, receptacle: Receptacle) -> Unit
    protected abstract var onClose: (player: Player, receptacle: Receptacle) -> Unit
    protected abstract var onClick: (event: ReceptacleInteractEvent) -> Unit

    /**
     * Get the element in the receptacle at the specified slot.
     *
     * @param slot the slot index to get the element
     * @return the ItemStack at the specified slot, or null if the slot is empty
     */
    abstract fun getElement(slot: Int): ItemStack?

    /**
     * Set an element in the receptacle at the specified slot.
     *
     * @param slot the slot index to set the element
     * @param item the ItemStack to set, or null to clear the slot
     */
    abstract fun setElement(slot: Int, item: ItemStack?)

    /**
     * Set the title of the receptacle.
     *
     * @param title the title to set
     * @param render whether to send a packet to the client to update the inventory title
     */
    abstract fun title(title: Component, render: Boolean)

    /**
     * Clear all elements in the receptacle and refresh.
     */
    abstract fun clear()

    /**
     * Refresh a specific slot in the receptacle for the player.
     * If slot is -1, refresh the entire receptacle.
     *
     * @param slot the slot index to refresh
     */
    abstract fun refresh(slot: Int = -1)

    /**
     * Open the receptacle for the player.
     *
     * @param player the player to open the receptacle for
     */
    abstract fun open(player: Player)

    /**
     * Close the receptacle for the player.
     *
     * @param render whether to send a packet to the client to close the inventory view
     */
    abstract fun close(render: Boolean)

    /**
     * Interrupts item dragging after click event using refresh mechanism.
     * This method prevents item dragging by refreshing inventory contents,
     * creating a button-like click effect.
     *
     * @param event the ReceptacleInteractEvent that was triggered by the player's click
     */
    abstract fun interruptItemDrag(event: ReceptacleInteractEvent)

    /**
     * Set the handler to be executed when the receptacle is opened by a player.
     *
     * @param handler the handler function to be executed
     */
    fun onOpen(handler: (player: Player, receptacle: Receptacle) -> Unit) {
        this.onOpen = handler
    }

    /**
     * Set the handler to be executed when the receptacle is closed by a player.
     *
     * @param handler the handler function to be executed
     */
    fun onClose(handler: (player: Player, receptacle: Receptacle) -> Unit) {
        this.onClose = handler
    }

    /**
     * Set the handler to be executed when a player interacts with the receptacle.
     *
     * @param clickEvent the handler function to be executed
     */
    fun onClick(clickEvent: (event: ReceptacleInteractEvent) -> Unit) {
        this.onClick = clickEvent
    }
}
