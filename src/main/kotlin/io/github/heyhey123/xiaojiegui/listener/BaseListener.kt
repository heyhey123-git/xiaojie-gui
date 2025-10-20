package io.github.heyhey123.xiaojiegui.listener

import io.github.heyhey123.xiaojiegui.XiaojieGUI
import org.bukkit.Bukkit
import org.bukkit.event.Listener

/**
 * Base listener.
 *
 */
interface BaseListener {

    /**
     * Register this listener.
     *
     */
    fun register() {
        Bukkit.getPluginManager().registerEvents(
            this as Listener,
            XiaojieGUI.instance
        )
    }

    /**
     * Unregister this listener from the eventbus.
     *
     */
    fun unregister()
}
