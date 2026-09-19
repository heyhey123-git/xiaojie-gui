package io.github.heyhey123.xiaojiegui.gui.layout

import net.minecraft.world.inventory.MenuType

/**
 * LayoutType defines various standard inventory layouts.
 *
 * Each layout type corresponds to a specific arrangement of slots in a GUI **and** to the menu the client
 * draws for it: [toNMSType] is what the open-screen packet carries, so a layout has to name the menu the
 * client will actually show rather than one that merely looks similar. A crafting table and the crafter
 * block are both 3x3 and are still different menus here.
 */
enum class LayoutType {

    // chest
    GENERIC_9X1,
    GENERIC_9X2,
    GENERIC_9X3,
    GENERIC_9X4,
    GENERIC_9X5,
    GENERIC_9X6,

    // dropper and dispenser
    GENERIC_3X3,

    // crafting table
    WORKBENCH,

    ANVIL,
    BEACON,
    BLAST_FURNACE,
    BREWING_STAND,

    // the player's own crafting grid, not a block
    CRAFTING,

    // the crafter block, which is a 3x3 menu of its own
    CRAFTER,

    ENCHANTMENT,
    FURNACE,
    GRINDSTONE,
    HOPPER,
    LECTERN,
    LOOM,
    MERCHANT,
    SHULKER_BOX,
    SMITHING,
    SMOKER,
    CARTOGRAPHY_TABLE,
    STONECUTTER;

    fun toNMSType(): MenuType<*> =
        when (this) {
            GENERIC_9X1 -> MenuType.GENERIC_9x1
            GENERIC_9X2 -> MenuType.GENERIC_9x2
            GENERIC_9X3 -> MenuType.GENERIC_9x3
            GENERIC_9X4 -> MenuType.GENERIC_9x4
            GENERIC_9X5 -> MenuType.GENERIC_9x5
            GENERIC_9X6 -> MenuType.GENERIC_9x6
            GENERIC_3X3 -> MenuType.GENERIC_3x3
            WORKBENCH -> MenuType.CRAFTING
            ANVIL -> MenuType.ANVIL
            BEACON -> MenuType.BEACON
            BLAST_FURNACE -> MenuType.BLAST_FURNACE
            BREWING_STAND -> MenuType.BREWING_STAND
            CRAFTING -> MenuType.CRAFTING
            CRAFTER -> MenuType.CRAFTER_3x3
            ENCHANTMENT -> MenuType.ENCHANTMENT
            FURNACE -> MenuType.FURNACE
            GRINDSTONE -> MenuType.GRINDSTONE
            HOPPER -> MenuType.HOPPER
            LECTERN -> MenuType.LECTERN
            LOOM -> MenuType.LOOM
            MERCHANT -> MenuType.MERCHANT
            SHULKER_BOX -> MenuType.SHULKER_BOX
            SMITHING -> MenuType.SMITHING
            SMOKER -> MenuType.SMOKER
            CARTOGRAPHY_TABLE -> MenuType.CARTOGRAPHY_TABLE
            STONECUTTER -> MenuType.STONECUTTER
        }
}
