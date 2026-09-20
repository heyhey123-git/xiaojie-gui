package io.github.heyhey123.xiaojiegui.gui.menu

import io.github.heyhey123.xiaojiegui.gui.receptacle.Receptacle
import net.kyori.adventure.text.Component

/**
 * Menu properties that define the behavior and appearance of a menu.
 *
 * @property defaultTitle The default title of the menu.
 * @property hidePlayerInventory Whether to hide the player's inventory when the menu is open.
 * @property mode The mode of the receptacle (STATIC or PHANTOM).
 * @property minClickDelay The minimum delay (in milliseconds) between clicks to prevent spamming.
 * @property defaultPage The default page number to open when the menu is first opened.
 * @property lockedIcons Whether the slots a page puts an icon in belong to the menu rather than to the
 * player: nothing can be taken out of them, put into them, swapped with them, dropped from them or
 * collected from them, and a drag that touches one of them is refused as a whole. The slots a layout
 * leaves empty stay free, which is what makes a shop (icons) and a backpack (empty slots) the same
 * mechanism. A menu without icons has nothing to lock.
 */
class MenuProperties(
    var defaultTitle: Component,
    var hidePlayerInventory: Boolean,
    val mode: Receptacle.Mode,
    var minClickDelay: Int,
    var defaultPage: Int,
    var defaultLayout: List<String>,
    var lockedIcons: Boolean = false
) {
    override fun toString() =
        "MenuProperties(defaultTitle=$defaultTitle, hidePlayerInventory=$hidePlayerInventory, mode=$mode, " +
            "minClickDelay=$minClickDelay, defaultPage=$defaultPage, defaultLayout=$defaultLayout, " +
            "lockedIcons=$lockedIcons)"
}
