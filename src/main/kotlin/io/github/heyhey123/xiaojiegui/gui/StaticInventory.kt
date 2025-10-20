package io.github.heyhey123.xiaojiegui.gui

import io.github.heyhey123.xiaojiegui.gui.receptacle.ViewLayout
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.InventoryView
import java.util.*

object StaticInventory {

    /**
     * A map of player UUIDs to their static inventory holders.
     */
    private val inventories = HashMap<UUID, Holder>()

    /**
     * Get the static inventory for the player,
     * null if the player has no static inventory.
     */
    val Player.staticInventory get() = inventories[this.uniqueId]?.inventory

    /**
     * Get the inventory view for the player's static inventory,
     * null if the player has no static inventory.
     */
    val Player.inventoryView get() = inventories[this.uniqueId]?.view

    /**
     * Open the static inventory for the player with the given holder.
     *
     * @param player the player to open the inventory for
     * @param holder the static inventory holder to open
     */
    fun open(player: Player, holder: Holder) {
        holder.open(player)
        inventories[player.uniqueId] = holder
    }

    /**
     * Create a static inventory holder with the given layout and title.
     *
     * @param layout the layout of the inventory
     * @param title the title of the inventory
     * @return the created static inventory holder
     */
    fun create(layout: ViewLayout, title: Component): Holder {
        val holder = Holder(layout, title)
        return holder
    }

    /**
     * Close the static inventory for the player
     * and clear its contents.
     *
     * @param player the player whose inventory to close
     */
    fun close(player: Player) {
        player.closeInventory()
        inventories.remove(player.uniqueId)?.clear()
    }

    /**
     * A holder for a static inventory.
     *
     * @param layout the layout of the inventory
     * @param title the title of the inventory
     */
    class Holder(layout: ViewLayout, title: Component) : InventoryHolder {

        private val inventory: Inventory = when (val type = layout.inventoryType) {
            InventoryType.CHEST -> Bukkit.createInventory(this, layout.slotRange.last + 1, title)
            else -> Bukkit.createInventory(this, type, title)
        }

        /**
         * The current inventory view, null if not opened.
         */
        var view: InventoryView? = null
            private set

        override fun getInventory(): Inventory {
            return inventory
        }

        /**
         * Open the inventory for the given player.
         *
         * @param player the player to open the inventory for
         */
        fun open(player: Player) {
            view = player.openInventory(inventory)
        }

        /**
         * Set the title of the inventory.
         *
         * @param title the new title of the inventory
         */
        @Suppress("DEPRECATION")
        fun setTitle(title: Component) {
            view?.title = LegacyComponentSerializer.legacySection().serialize(title)
        }

        /**
         * Clears out the whole Inventory.
         */
        fun clear() {
            inventory.clear()
            view = null
        }
    }
}
