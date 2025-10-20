package io.github.heyhey123.xiaojiegui.gui.layout

/**
 * Layout defines the slot arrangement for a GUI.
 *
 * @property slotRange The range of slots used by the container (the GUI itself).
 */
abstract class Layout(val slotRange: IntRange) {

    /**
     * The slots of the player's main inventory (excluding hotbar).
     */
    val mainInvSlotRange: List<Int> = (slotRange.last + 1..slotRange.last + 27).toList()

    /**
     * The slots of the player's hotbar.
     */
    val hotBarSlotRange: List<Int> = (mainInvSlotRange.last() + 1..mainInvSlotRange.last() + 9).toList()

    /**
     * The slots of the container (the GUI itself).
     */
    val containerSlotRange: List<Int> = slotRange.toList()

    /**
     * The size of the container (the GUI itself).
     */
    val containerSize: Int = containerSlotRange.size

    /**
     * All slots including container, main inventory, and hotbar.
     */
    val totalSlotRange: List<Int> = (0..hotBarSlotRange.last()).toList()

    /**
     * The total size of all slots, including container, main inventory, and hotbar.
     */
    val totalSize: Int = totalSlotRange.size
}
