package io.github.heyhey123.xiaojiegui.gui.receptacle

import io.github.heyhey123.xiaojiegui.gui.layout.Layout
import io.github.heyhey123.xiaojiegui.gui.layout.LayoutType
import org.bukkit.event.inventory.InventoryType

/**
 * ViewLayout defines the slot arrangement for a view GUI.
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
        object CRAFTER_3x3 : FixedContainer(LayoutType.CRAFTER_3x3, InventoryType.WORKBENCH, 0..8)
        object ANVIL : FixedContainer(LayoutType.ANVIL, InventoryType.ANVIL, 0..2)
        object BEACON : FixedContainer(LayoutType.BEACON, InventoryType.BEACON, 0..2)
        object BLAST_FURNACE : FixedContainer(LayoutType.BLAST_FURNACE, InventoryType.BLAST_FURNACE, 0..2)
        object BREWING_STAND : FixedContainer(LayoutType.BREWING_STAND, InventoryType.BREWING, 0..4)
        object CRAFTING : FixedContainer(LayoutType.CRAFTING, InventoryType.CRAFTING, 0..4)
        object ENCHANTMENT : FixedContainer(LayoutType.ENCHANTMENT, InventoryType.ENCHANTING, 0..2)
        object FURNACE : FixedContainer(LayoutType.FURNACE, InventoryType.FURNACE, 0..2)
        object GRINDSTONE : FixedContainer(LayoutType.GRINDSTONE, InventoryType.GRINDSTONE, 0..2)
        object HOPPER : FixedContainer(LayoutType.HOPPER, InventoryType.HOPPER, 0..4)
        object LECTERN : FixedContainer(LayoutType.LECTERN, InventoryType.LECTERN, 0..1)
        object LOOM : FixedContainer(LayoutType.LOOM, InventoryType.LOOM, 0..3)
        object MERCHANT : FixedContainer(LayoutType.MERCHANT, InventoryType.MERCHANT, 0..2)
        object SHULKER_BOX : FixedContainer(LayoutType.SHULKER_BOX, InventoryType.SHULKER_BOX, 0..26)
        object SMITHING : FixedContainer(LayoutType.SMITHING, InventoryType.SMITHING, 0..2)
        object SMOKER : FixedContainer(LayoutType.SMOKER, InventoryType.SMOKER, 0..2)
        object CARTOGRAPHY_TABLE : FixedContainer(LayoutType.CARTOGRAPHY_TABLE, InventoryType.CARTOGRAPHY, 0..3)
        object STONECUTTER : FixedContainer(LayoutType.STONECUTTER, InventoryType.STONECUTTER, 0..1)
    }

    companion object {
        fun fromInventoryType(type: InventoryType): ViewLayout {
            return when (type) {
                InventoryType.CHEST -> Chest.GENERIC_9X3
                InventoryType.DROPPER -> FixedContainer.GENERIC_3X3
                InventoryType.WORKBENCH -> FixedContainer.CRAFTER_3x3
                InventoryType.ANVIL -> FixedContainer.ANVIL
                InventoryType.BEACON -> FixedContainer.BEACON
                InventoryType.BLAST_FURNACE -> FixedContainer.BLAST_FURNACE
                InventoryType.BREWING -> FixedContainer.BREWING_STAND
                InventoryType.CRAFTING -> FixedContainer.CRAFTING
                InventoryType.ENCHANTING -> FixedContainer.ENCHANTMENT
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
}

