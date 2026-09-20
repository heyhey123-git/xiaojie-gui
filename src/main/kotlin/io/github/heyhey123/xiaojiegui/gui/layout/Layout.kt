package io.github.heyhey123.xiaojiegui.gui.layout

/**
 * Layout defines the slot arrangement for a GUI.
 *
 * `slotRange` is not a free choice: it is the set of slots the client draws for the window this layout
 * opens. Almost every window vanilla numbers is "container slots first, then the player's 27 main slots
 * and 9 hotbar slots", so the player's ranges ([mainInvSlotRange], [hotBarSlotRange]) are derived from
 * where the container ends. That derivation is only right while the window has a player half at all, and
 * [hasPlayerInventory] is what says whether it does: a client menu with no player inventory -- 26.2's
 * lectern is the one that exists -- carries no such slots, so its ranges are empty and [totalSize] stops
 * at the container.
 *
 * @property slotRange The range of slots used by the container (the GUI itself).
 * @param hasPlayerInventory Whether the client's menu for this window has the player's 27 main slots and
 * 9 hotbar slots below the container. Defaults to true because that is how vanilla numbers all but one
 * of its menus; a layout whose client menu has no player inventory overrides it to false, and its player
 * slot ranges are then empty rather than ranges pointing at slots the client does not have.
 */
abstract class Layout(val slotRange: IntRange, open val hasPlayerInventory: Boolean = true) {

    /**
     * The slots of the player's main inventory (excluding hotbar), or empty when the client menu has no
     * player inventory at all.
     */
    val mainInvSlotRange: List<Int> =
        if (hasPlayerInventory) (slotRange.last + 1..slotRange.last + 27).toList() else emptyList()

    /**
     * The slots of the player's hotbar, or empty when the client menu has no player inventory at all.
     */
    val hotBarSlotRange: List<Int> =
        if (hasPlayerInventory) (mainInvSlotRange.last() + 1..mainInvSlotRange.last() + 9).toList() else emptyList()

    /**
     * The slots of the container (the GUI itself).
     */
    val containerSlotRange: List<Int> = slotRange.toList()

    /**
     * The size of the container (the GUI itself).
     */
    val containerSize: Int = containerSlotRange.size

    /**
     * All slots including container, main inventory, and hotbar, which for a window with no player
     * inventory are the container's slots alone.
     */
    val totalSlotRange: List<Int> =
        if (hasPlayerInventory) (0..hotBarSlotRange.last()).toList() else containerSlotRange

    /**
     * The total size of all slots, including container, main inventory, and hotbar. For a window with no
     * player inventory this is [containerSize], so it stays the length of the packet the client's menu
     * accepts.
     */
    val totalSize: Int = totalSlotRange.size
}
