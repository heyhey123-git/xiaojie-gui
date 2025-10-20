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
 */
class MenuProperties(
    @Volatile var defaultTitle: Component,
    @Volatile var hidePlayerInventory: Boolean,
    val mode: Receptacle.Mode,
    @Volatile var minClickDelay: Int,
    @Volatile var defaultPage: Int,
    @Volatile var defaultLayout: List<String>
) {
    override fun toString() =
        "MenuProperties(defaultTitle=$defaultTitle, hidePlayerInventory=$hidePlayerInventory, mode=$mode, " +
                "minClickDelay=$minClickDelay, defaultPage=$defaultPage, defaultLayout=$defaultLayout)"
}
