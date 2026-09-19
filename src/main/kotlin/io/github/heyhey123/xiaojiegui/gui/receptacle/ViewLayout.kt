// The object names below deliberately mirror the inventory constants they stand for
// (GENERIC_9X3, BLAST_FURNACE, ...): reading a `when` block against Bukkit's InventoryType and the
// client's MenuType is the whole point of these layouts. ktlint's class-naming rule asks for camel
// case, which would break that correspondence, so it is suppressed for this file only.
@file:Suppress("ktlint:standard:class-naming")

package io.github.heyhey123.xiaojiegui.gui.receptacle

import io.github.heyhey123.xiaojiegui.gui.layout.Layout
import io.github.heyhey123.xiaojiegui.gui.layout.LayoutType
import org.bukkit.event.inventory.InventoryType

/**
 * ViewLayout defines the slot arrangement for a view GUI.
 *
 * `slotRange` is not a free choice: it is the set of slots the client draws for the window this layout
 * opens, so it has to match the size of [inventoryType]. The player's 27 main slots and 9 hotbar slots
 * follow immediately after it (see [Layout]), which is how vanilla numbers a window; a range that is one
 * slot too short does not fail, it moves every player slot in the window by one and clicks land on the
 * wrong item. `ViewLayoutTest` pins each range to Bukkit's own size for the type.
 *
 * @param type the layout type
 * @param inventoryType the Bukkit inventory type
 * @param slotRange the range of valid slot indices for this layout
 */
sealed class ViewLayout(
    val type: LayoutType,
    val inventoryType: InventoryType,
    slotRange: IntRange
) : Layout(slotRange) {

    /**
     * Chest layouts (9x1 to 9x6)
     */
    sealed class Chest(type: LayoutType, slotRange: IntRange) : ViewLayout(type, InventoryType.CHEST, slotRange) {
        object GENERIC_9X1 : Chest(LayoutType.GENERIC_9X1, 0..8)
        object GENERIC_9X2 : Chest(LayoutType.GENERIC_9X2, 0..17)
        object GENERIC_9X3 : Chest(LayoutType.GENERIC_9X3, 0..26)
        object GENERIC_9X4 : Chest(LayoutType.GENERIC_9X4, 0..35)
        object GENERIC_9X5 : Chest(LayoutType.GENERIC_9X5, 0..44)
        object GENERIC_9X6 : Chest(LayoutType.GENERIC_9X6, 0..53)
    }

    /**
     * Fixed-size container layouts
     */
    sealed class FixedContainer(type: LayoutType, inventoryType: InventoryType, slotRange: IntRange) :
        ViewLayout(type, inventoryType, slotRange) {
        object GENERIC_3X3 : FixedContainer(LayoutType.GENERIC_3X3, InventoryType.DROPPER, 0..8)
        object WORKBENCH : FixedContainer(LayoutType.WORKBENCH, InventoryType.WORKBENCH, 0..9)
        object ANVIL : FixedContainer(LayoutType.ANVIL, InventoryType.ANVIL, 0..2)
        object BARREL : FixedContainer(LayoutType.GENERIC_9X3, InventoryType.BARREL, 0..26)
        object BEACON : FixedContainer(LayoutType.BEACON, InventoryType.BEACON, 0..0)
        object BLAST_FURNACE : FixedContainer(LayoutType.BLAST_FURNACE, InventoryType.BLAST_FURNACE, 0..2)
        object BREWING_STAND : FixedContainer(LayoutType.BREWING_STAND, InventoryType.BREWING, 0..4)
        object CRAFTER : FixedContainer(LayoutType.CRAFTER, InventoryType.CRAFTER, 0..8)
        object DISPENSER : FixedContainer(LayoutType.GENERIC_3X3, InventoryType.DISPENSER, 0..8)
        object ENCHANTMENT : FixedContainer(LayoutType.ENCHANTMENT, InventoryType.ENCHANTING, 0..1)
        object ENDER_CHEST : FixedContainer(LayoutType.GENERIC_9X3, InventoryType.ENDER_CHEST, 0..26)
        object FURNACE : FixedContainer(LayoutType.FURNACE, InventoryType.FURNACE, 0..2)
        object GRINDSTONE : FixedContainer(LayoutType.GRINDSTONE, InventoryType.GRINDSTONE, 0..2)
        object HOPPER : FixedContainer(LayoutType.HOPPER, InventoryType.HOPPER, 0..4)
        object LECTERN : FixedContainer(LayoutType.LECTERN, InventoryType.LECTERN, 0..0)
        object LOOM : FixedContainer(LayoutType.LOOM, InventoryType.LOOM, 0..3)
        object MERCHANT : FixedContainer(LayoutType.MERCHANT, InventoryType.MERCHANT, 0..2)
        object SHULKER_BOX : FixedContainer(LayoutType.SHULKER_BOX, InventoryType.SHULKER_BOX, 0..26)
        object SMITHING : FixedContainer(LayoutType.SMITHING, InventoryType.SMITHING, 0..3)
        object SMOKER : FixedContainer(LayoutType.SMOKER, InventoryType.SMOKER, 0..2)
        object CARTOGRAPHY_TABLE : FixedContainer(LayoutType.CARTOGRAPHY_TABLE, InventoryType.CARTOGRAPHY, 0..2)
        object STONECUTTER : FixedContainer(LayoutType.STONECUTTER, InventoryType.STONECUTTER, 0..1)
    }

    companion object {
        fun fromInventoryType(type: InventoryType): ViewLayout = when (type) {
            InventoryType.CHEST -> Chest.GENERIC_9X3
            InventoryType.DROPPER -> FixedContainer.GENERIC_3X3
            InventoryType.WORKBENCH -> FixedContainer.WORKBENCH
            InventoryType.CRAFTER -> FixedContainer.CRAFTER
            InventoryType.ANVIL -> FixedContainer.ANVIL
            InventoryType.BARREL -> FixedContainer.BARREL
            InventoryType.BEACON -> FixedContainer.BEACON
            InventoryType.BLAST_FURNACE -> FixedContainer.BLAST_FURNACE
            InventoryType.BREWING -> FixedContainer.BREWING_STAND
            InventoryType.DISPENSER -> FixedContainer.DISPENSER
            InventoryType.ENCHANTING -> FixedContainer.ENCHANTMENT
            InventoryType.ENDER_CHEST -> FixedContainer.ENDER_CHEST
            InventoryType.FURNACE -> FixedContainer.FURNACE
            InventoryType.GRINDSTONE -> FixedContainer.GRINDSTONE
            InventoryType.HOPPER -> FixedContainer.HOPPER
            InventoryType.LECTERN -> FixedContainer.LECTERN
            InventoryType.LOOM -> FixedContainer.LOOM
            InventoryType.MERCHANT -> FixedContainer.MERCHANT
            InventoryType.SHULKER_BOX -> FixedContainer.SHULKER_BOX
            InventoryType.SMITHING -> FixedContainer.SMITHING
            InventoryType.SMOKER -> FixedContainer.SMOKER
            InventoryType.CARTOGRAPHY -> FixedContainer.CARTOGRAPHY_TABLE
            InventoryType.STONECUTTER -> FixedContainer.STONECUTTER
            else -> throw IllegalArgumentException("Unsupported inventory type: $type")
        }
    }
}
