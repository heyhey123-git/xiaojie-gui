package io.github.heyhey123.xiaojiegui.skript.utils

import io.github.heyhey123.xiaojiegui.XiaojieGUI
import io.github.heyhey123.xiaojiegui.gui.event.MenuInteractEvent
import org.bukkit.inventory.ItemStack
import java.util.concurrent.ConcurrentHashMap

/**
 * A button in a menu GUI.
 * Encapsulates an item and a callback function to be executed when the button is clicked.
 *
 * @param id The unique identifier for the button.
 * @property item The item representing the button.
 * @property callback The function to be called when the button is clicked.
 */
class Button(
    val id: String,
    val item: ItemStack,
    val callback: (MenuInteractEvent) -> Unit
) {
    init {
        check(XiaojieGUI.instance.isEnabled) {
            "Cannot create Button when XiaojieGUI is not enabled."
        }
        buttons[id] = this
    }

    companion object {
        val buttons: ConcurrentHashMap<String, Button> = ConcurrentHashMap()
    }
}
