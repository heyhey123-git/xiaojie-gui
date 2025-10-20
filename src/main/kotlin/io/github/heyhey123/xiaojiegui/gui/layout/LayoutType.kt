package io.github.heyhey123.xiaojiegui.gui.layout

/**
 * LayoutType defines various standard inventory layouts.
 *
 * Each layout type corresponds to a specific arrangement of slots in a GUI.
 *
 * @property id The unique identifier for the layout type.
 */
enum class
LayoutType(val id: Int) {

    // chest
    GENERIC_9X1(0),
    GENERIC_9X2(1),
    GENERIC_9X3(2),
    GENERIC_9X4(3),
    GENERIC_9X5(4),
    GENERIC_9X6(5),

    // dropper
    GENERIC_3X3(6),

    // crafter
    CRAFTER_3x3(7),

    ANVIL(8),
    BEACON(9),
    BLAST_FURNACE(10),
    BREWING_STAND(11),
    CRAFTING(12),
    ENCHANTMENT(13),
    FURNACE(14),
    GRINDSTONE(15),
    HOPPER(16),
    LECTERN(17),
    LOOM(18),
    MERCHANT(19),
    SHULKER_BOX(20),
    SMITHING(21),
    SMOKER(22),
    CARTOGRAPHY_TABLE(23),
    STONECUTTER(24);
}
