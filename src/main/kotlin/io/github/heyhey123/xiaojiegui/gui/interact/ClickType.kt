package io.github.heyhey123.xiaojiegui.gui.interact

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

    DOUBLE_CLICK(ClickMode.PICK_UP_ALL, 0, BukkitClickType.DOUBLE_CLICK),

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
     * The number key the player pressed, 1 to 9, or `null` when this click was not one.
     *
     * The nine `NUMBER_KEY_*` types exist because the protocol carries the key as the button of a swap:
     * key 1 is button 0, key 9 is button 8. Skript's own click type has a single `number key` value and
     * drops the digit, which is why the digit is read from here. An offhand swap and a number key that
     * arrived with a button outside 0..8 (`NUMBER_KEY_INVALID`) are not number keys.
     */
    val numberKey: Int?
        get() = if (mode == ClickMode.SWAP && button in 0..8) button + 1 else null

    /**
     * Whether this click can move an item to a slot other than the one it names, which is what a
     * `phantom` menu has to know: it puts back what the client changed locally, one slot at a time when
     * the click only touched its own slot, and the whole window when it did not.
     *
     * The answer is not `isKeyboardClick()`, which is true for the `Q` drop as well: a drop takes the
     * item out of the window, so the slot it names is the only one that changed.
     *
     * @return `true` if the click can move items between slots
     */
    fun isItemMoveable(): Boolean =
        isShiftClick() || isNumberKeyClick() || this == SWAP_OFFHAND || mustBeCreativeAction() || this == DOUBLE_CLICK

    /**
     * Gets whether this ClickType requires creative action.
     *
     * @return `true` if this ClickType requires creative action, `false` otherwise.
     */
    private fun mustBeCreativeAction(): Boolean = this == MIDDLE

    override fun toString(): String =
        name.lowercase().replace('_', ' ')

    /**
     * Checks if this ClickType matches the given mode and button.
     *
     * @param mode The click mode to check against.
     * @param button The button to check against.
     * @return `true` if this ClickType matches the given mode and button, {@code false} otherwise.
     */
    fun matches(mode: Int, button: Int): Boolean = this.mode.id == mode && this.button == button

    companion object {

        /**
         * The slot number the game sends for a click that did not land in the window at all, which is
         * how a click on the background and a drop into the world arrive.
         */
        const val OUTSIDE_SLOT = -999

        /**
         * The click type of one container click packet, or null when the packet is one this addon has no
         * name for. A client can send anything, so a caller has to decide what to do with an unknown one;
         * `ReceptaclePacketListener` ignores it, because the window it belongs to is the server's own.
         *
         * @param mode the packet's own click type, by its protocol number
         * @param button the packet's button
         * @param slot the slot the packet names, [OUTSIDE_SLOT] for a click outside the window
         */
        fun from(mode: Int, button: Int, slot: Int = -1): ClickType? {
            if (slot == OUTSIDE_SLOT) {
                return when {
                    LEFT.matches(mode, button) -> OUTSIDE_LEFT
                    RIGHT.matches(mode, button) -> OUTSIDE_RIGHT
                    LEFT_DROP.matches(mode, button) -> LEFT_DROP
                    RIGHT_DROP.matches(mode, button) -> RIGHT_DROP
                    else -> UNKNOWN
                }
            }
            return entries.find { it.matches(mode, button) }
                // A swap carries its hotbar slot as the button, so a swap with a button that is not a
                // hotbar slot is still a swap, and reports itself as one that named no key.
                ?: if (mode == ClickMode.SWAP.id) NUMBER_KEY_INVALID else null
        }

        fun find(mode: Int, button: Int, bukkitClickType: BukkitClickType): ClickType =
            entries.find { it.mode.id == mode && it.button == button && it.bukkitClickType == bukkitClickType }
                ?: when (bukkitClickType) {
                    BukkitClickType.NUMBER_KEY -> NUMBER_KEY_INVALID
                    else -> UNKNOWN
                }

        /**
         * The click type of one bukkit click event.
         *
         * Bukkit's own click type is what a script reads -- `the click type` is Skript's, and this addon
         * answers with the matching one -- so it decides the answer here. The action is deliberately not
         * consulted: it says *what* happened to the items, not which gesture it was, and reading it as
         * well would report a `Q` drop as the left button and a left click on the background as a drop.
         *
         * @param clickType the event's click type
         * @param slot the event's raw slot, [OUTSIDE_SLOT] when the click was outside the window
         */
        fun fromBukkit(clickType: BukkitClickType, slot: Int): ClickType {
            if (clickType == BukkitClickType.NUMBER_KEY) {
                val button = if (slot in 0..8) slot else -1
                return find(ClickMode.SWAP.id, button, clickType)
            }
            if (slot == OUTSIDE_SLOT) {
                return when (clickType) {
                    BukkitClickType.LEFT -> OUTSIDE_LEFT
                    BukkitClickType.RIGHT -> OUTSIDE_RIGHT
                    BukkitClickType.DROP -> LEFT_DROP
                    BukkitClickType.CONTROL_DROP -> RIGHT_DROP
                    else -> entries.find { it.bukkitClickType == clickType } ?: UNKNOWN
                }
            }
            return entries.find { it.bukkitClickType == clickType } ?: UNKNOWN
        }
    }
}
