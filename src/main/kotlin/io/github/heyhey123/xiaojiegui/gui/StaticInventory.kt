package io.github.heyhey123.xiaojiegui.gui

import io.github.heyhey123.xiaojiegui.gui.receptacle.ViewLayout
import io.github.heyhey123.xiaojiegui.gui.utils.Reflection
import io.papermc.paper.adventure.PaperAdventure
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket
import org.bukkit.Bukkit
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.craftbukkit.inventory.CraftAbstractInventoryView
import org.bukkit.craftbukkit.inventory.CraftContainer
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.InventoryView
import java.util.UUID

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
        inventories[player.uniqueId] = holder
        holder.open(player)
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
     * Drop the static inventory of a player without touching the player.
     *
     * A player who quits, dies or is teleported to another world never closes their window through this
     * addon, and the entry would otherwise stay in [inventories] with its items for as long as the server
     * runs. This is what the paths that end a session out from under it call.
     *
     * @param player the player whose entry to drop
     */
    fun forget(player: Player) {
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
            else -> {
                // The merchant is the one layout Bukkit will not build: its trades come from the merchant
                // API, so there is no inventory behind it. Phantom mode can still draw the window, which
                // is why the layout exists at all; say so instead of letting Bukkit's own error surface.
                require(type.isCreatable) {
                    "A ${layout.type} layout cannot be opened as a static menu: Bukkit cannot create a $type inventory."
                }
                Bukkit.createInventory(this, type, title)
            }
        }

        /**
         * The current inventory view, null if not opened.
         */
        var view: InventoryView? = null
            private set

        override fun getInventory(): Inventory = inventory

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
        fun setTitle(title: Component) {
            val craftContainerView = view as? CraftAbstractInventoryView
                ?: throw ClassCastException("InventoryView is not a CraftAbstractInventoryView")

            Reflection.CraftContainerViewProxy.setTitle(
                craftContainerView,
                LegacyComponentSerializer.legacySection().serialize(title)
            )

            val serverPlayer = (view?.player as CraftPlayer).handle
            val containerMenu = serverPlayer.containerMenu
            val menuType = CraftContainer.getNotchInventoryType(craftContainerView.topInventory)
            serverPlayer.connection.send(
                ClientboundOpenScreenPacket(
                    containerMenu.containerId,
                    menuType,
                    PaperAdventure.asVanilla(title)
                )
            )
            containerMenu.sendAllDataToRemote()
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
