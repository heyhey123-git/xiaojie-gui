package io.github.heyhey123.xiaojiegui.gui.interact

/**
 * Different inventory operation of clicking in an inventory GUI.
 *
 * @property id
 */
enum class ClickMode(val id: Int) {
    PICK_UP(0),

    QUICK_MOVE(1),

    SWAP(2),

    CLONE(3),

    THROW(4),

    QUICK_CRAFT(5),

    PICK_UP_ALL(6),

    UNKNOWN(-1);

    override fun toString(): String =
        name.lowercase().replace("_", " ")

    companion object {
        fun fromId(id: Int): ClickMode {
            if (id < 0 || id >= ClickMode.entries.size) {
                return UNKNOWN
            }
            return ClickMode.entries[id]
        }
    }
}
