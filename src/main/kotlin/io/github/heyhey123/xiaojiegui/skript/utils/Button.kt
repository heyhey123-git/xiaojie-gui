package io.github.heyhey123.xiaojiegui.skript.utils

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
        val previous = buttons.putIfAbsent(id, this)
        require(previous == null) {
            "Button with id '$id' is already registered."
        }
    }

    companion object {
        val buttons: ConcurrentHashMap<String, Button> = ConcurrentHashMap()
    }
}
