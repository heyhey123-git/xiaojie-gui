package io.github.heyhey123.xiaojiegui.gui.interact

import org.bukkit.event.inventory.InventoryAction

typealias BukkitClickType = org.bukkit.event.inventory.ClickType

/**
 * Enums for different types of clicks in a GUI.
 *
 * @property mode Inventory operation mode.
 * @property button The button used in the click.
 * @property bukkitClickType The corresponding Bukkit click type.
 */
enum class ClickType(
    val mode: ClickMode,
    val button: Int,
    val bukkitClickType: BukkitClickType?
) {
    LEFT(ClickMode.PICK_UP, 0, BukkitClickType.LEFT),

    RIGHT(ClickMode.PICK_UP, 1, BukkitClickType.RIGHT),

    OUTSIDE_LEFT(ClickMode.PICK_UP, 0, BukkitClickType.LEFT),

    OUTSIDE_RIGHT(ClickMode.PICK_UP, 1, BukkitClickType.RIGHT),

    SHIFT_LEFT(ClickMode.QUICK_MOVE, 0, BukkitClickType.SHIFT_LEFT),

    SHIFT_RIGHT(ClickMode.QUICK_MOVE, 1, BukkitClickType.SHIFT_RIGHT),

    NUMBER_KEY_1(ClickMode.SWAP, 0, BukkitClickType.NUMBER_KEY),

    NUMBER_KEY_2(ClickMode.SWAP, 1, BukkitClickType.NUMBER_KEY),

    NUMBER_KEY_3(ClickMode.SWAP, 2, BukkitClickType.NUMBER_KEY),

    NUMBER_KEY_4(ClickMode.SWAP, 3, BukkitClickType.NUMBER_KEY),

    NUMBER_KEY_5(ClickMode.SWAP, 4, BukkitClickType.NUMBER_KEY),

    NUMBER_KEY_6(ClickMode.SWAP, 5, BukkitClickType.NUMBER_KEY),

    NUMBER_KEY_7(ClickMode.SWAP, 6, BukkitClickType.NUMBER_KEY),

    NUMBER_KEY_8(ClickMode.SWAP, 7, BukkitClickType.NUMBER_KEY),

    NUMBER_KEY_9(ClickMode.SWAP, 8, BukkitClickType.NUMBER_KEY),

    NUMBER_KEY_INVALID(ClickMode.SWAP, -1, BukkitClickType.NUMBER_KEY),

    SWAP_OFFHAND(ClickMode.SWAP, 40, BukkitClickType.SWAP_OFFHAND),

    MIDDLE(ClickMode.CLONE, 2, BukkitClickType.MIDDLE),

    DROP(ClickMode.THROW, 0, BukkitClickType.DROP),

    CONTROL_DROP(ClickMode.THROW, 1, BukkitClickType.CONTROL_DROP),

    LEFT_DROP(ClickMode.THROW, 0, BukkitClickType.DROP),

    RIGHT_DROP(ClickMode.THROW, 1, BukkitClickType.CONTROL_DROP),

    LEFT_MOUSE_DRAG_ADD(ClickMode.QUICK_CRAFT, 1, null),

    RIGHT_MOUSE_DRAG_ADD(ClickMode.QUICK_CRAFT, 5, null),

    MIDDLE_MOUSE_DRAG_ADD(ClickMode.QUICK_CRAFT, 9, null),

    DOUBLE_CLICK(ClickMode.CLONE, 0, BukkitClickType.DOUBLE_CLICK),

    UNKNOWN(ClickMode.UNKNOWN, -1, BukkitClickType.UNKNOWN);

    /**
     * Gets whether this ClickType represents a right click.
     *
     * @return `true` if this ClickType represents a right click
     */
    fun isRightClick(): Boolean = bukkitClickType?.isRightClick ?: false

    /**
     * Gets whether this ClickType represents a left click.
     *
     * @return `true` if this ClickType represents a left click
     */
    fun isLeftClick(): Boolean = bukkitClickType?.isLeftClick ?: false

    /**
     * Gets whether this ClickType indicates that the shift key was pressed
     * down when the click was made.
     *
     * @return `true` if the action uses Shift.
     */
    fun isShiftClick(): Boolean = bukkitClickType?.isShiftClick ?: false

    /**
     * Gets whether this ClickType represents the pressing of a key on a
     * keyboard.
     *
     * @return {@code true} if this ClickType represents the pressing of a key
     */
    fun isKeyboardClick(): Boolean = bukkitClickType?.isKeyboardClick ?: false

    /**
     * Gets whether this ClickType represents the pressing of a mouse button
     *
     * @return {@code true} if this ClickType represents the pressing of a mouse button
     */
    fun isMouseClick(): Boolean = bukkitClickType?.isMouseClick ?: false

    /**
     * Gets whether this ClickType represents the pressing of a number key
     *
     * @return {@code true} if this ClickType represents the pressing of a number key
     */
    fun isNumberKeyClick(): Boolean = bukkitClickType == BukkitClickType.NUMBER_KEY

    /**
     * Determines if the item can be moved based on the click type.
     *
     * @return `true` if the item can be moved, `false` otherwise.
     */
    fun isItemMoveable(): Boolean {
        return isKeyboardClick() || isShiftClick() || mustBeCreativeAction() || this == DOUBLE_CLICK
    }

    /**
     * Gets whether this ClickType requires creative action.
     *
     * @return `true` if this ClickType requires creative action, `false` otherwise.
     */
    private fun mustBeCreativeAction(): Boolean {
        return this == MIDDLE || this == MIDDLE_MOUSE_DRAG_ADD
    }

    override fun toString(): String =
        name.lowercase().replace('_', ' ')

    /**
     * Checks if this ClickType matches the given mode and button.
     *
     * @param mode The click mode to check against.
     * @param button The button to check against.
     * @return `true` if this ClickType matches the given mode and button, {@code false} otherwise.
     */
    fun matches(mode: Int, button: Int): Boolean {
        return this.mode.id == mode && this.button == button
    }

    companion object {
        fun from(mode: Int, button: Int, slot: Int = -1): ClickType? {
            if (slot == -999) {
                return when {
                    LEFT.matches(mode, button) -> OUTSIDE_LEFT
                    RIGHT.matches(mode, button) -> OUTSIDE_RIGHT
                    LEFT_DROP.matches(mode, button) -> LEFT_DROP
                    RIGHT_DROP.matches(mode, button) -> RIGHT_DROP
                    else -> UNKNOWN
                }
            }
            return entries.find { it.matches(mode, button) }
        }

        fun find(mode: Int, button: Int, bukkitClickType: BukkitClickType): ClickType {
            return entries.find { it.mode.id == mode && it.button == button && it.bukkitClickType == bukkitClickType }
                ?: when (bukkitClickType) {
                    BukkitClickType.NUMBER_KEY -> NUMBER_KEY_INVALID
                    else -> UNKNOWN
                }
        }

        fun fromBukkit(clickType: BukkitClickType, action: InventoryAction, slot: Int): ClickType {
            if (clickType == BukkitClickType.NUMBER_KEY) {
                val button = if (slot in 0..8) slot else -1
                return find(ClickMode.SWAP.id, button, clickType)
            }
            return when (action) {
                InventoryAction.DROP_ONE_SLOT -> OUTSIDE_LEFT
                InventoryAction.DROP_ALL_SLOT -> OUTSIDE_RIGHT
                InventoryAction.DROP_ONE_CURSOR -> LEFT_DROP
                InventoryAction.DROP_ALL_CURSOR -> RIGHT_DROP
                else -> entries.find { it.bukkitClickType == clickType }
            } ?: UNKNOWN
        }
    }
}
