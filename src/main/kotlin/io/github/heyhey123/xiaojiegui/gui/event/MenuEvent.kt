package io.github.heyhey123.xiaojiegui.gui.event

import io.github.heyhey123.xiaojiegui.gui.menu.Menu
import io.github.heyhey123.xiaojiegui.gui.menu.MenuSession
import org.bukkit.entity.Player
import org.bukkit.event.Event

/**
 * A base class for all menu-related events.
 *
 */
abstract class MenuEvent() : Event() {
    /**
     * The menu session associated with this event.
     */
    abstract val session: MenuSession

    /**
     * The menu associated with this event.
     */
    abstract val menu: Menu

    /**
     * The player associated with this event.
     */
    abstract val viewer: Player
}
