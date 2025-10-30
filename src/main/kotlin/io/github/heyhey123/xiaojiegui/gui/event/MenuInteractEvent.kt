package io.github.heyhey123.xiaojiegui.gui.event

import io.github.heyhey123.xiaojiegui.gui.interact.ClickType
import io.github.heyhey123.xiaojiegui.gui.menu.Menu
import io.github.heyhey123.xiaojiegui.gui.menu.MenuSession
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.HandlerList
import org.bukkit.inventory.ItemStack

/**
 * Menu interact event.
 * This event is called when a player interacts with a menu.
 *
 * @property session The menu session of the player who interacted with the menu.
 * @property menu The menu that was interacted with.
 * @property page The page number of the menu that was interacted with.
 * @property slot The slot number that was interacted with.
 * @property icon The icon that was interacted with, or null if the slot is empty.
 */
class MenuInteractEvent(
    override val session: MenuSession,
    override val viewer: Player,
    override val menu: Menu,
    val page: Int,
    val slot: Int,
    val icon: ItemStack?,
    val clickType: ClickType
) : MenuEvent(), Cancellable {

    companion object {

        private val HANDLERS = HandlerList()

        @JvmStatic
        fun getHandlerList() = HANDLERS
    }

    private var cancelled = false

    override fun getHandlers() = HANDLERS

    override fun isCancelled(): Boolean = cancelled

    override fun setCancelled(cancel: Boolean) {
        this.cancelled = cancel
    }
}
