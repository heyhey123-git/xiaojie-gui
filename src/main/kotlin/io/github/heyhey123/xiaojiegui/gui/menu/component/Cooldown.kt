package io.github.heyhey123.xiaojiegui.gui.menu.component

import io.github.heyhey123.xiaojiegui.gui.menu.MenuProperties
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * A utility class to manage cooldowns for players.
 * This class allows you to set, check, and reset cooldowns for individual players.
 *
 * @param property the menu properties containing the default cooldown settings
 */
class Cooldown(
    val property: MenuProperties
) {

    private val defaultCooldown: Int
        get() = property.minClickDelay

    private val lastClickedData: MutableMap<UUID, Long> = ConcurrentHashMap()

    /**
     * Try to consume the cooldown for the player.
     * If the cooldown has not expired, return false.
     * If the cooldown has expired, update the last clicked time and return true.
     *
     * @param player the player to check the cooldown for
     * @param cooldownMillis the cooldown duration in milliseconds
     * @return true if the cooldown has expired and was consumed, false otherwise
     */
    fun tryConsumeCooldown(player: Player, cooldownMillis: Int = defaultCooldown): Boolean {
        val now = System.currentTimeMillis()
        val last = lastClickedData[player.uniqueId] ?: 0L
        if (now - last < cooldownMillis) return false
        lastClickedData[player.uniqueId] = now
        return true
    }

    /**
     * Get the remaining cooldown time for the player in milliseconds.
     *
     * @param player the player to check the cooldown for
     * @param cooldownMillis the cooldown duration in milliseconds
     * @return the remaining cooldown time in milliseconds, or 0 if the cooldown has expired
     */
    fun remainingCooldown(player: Player, cooldownMillis: Int = defaultCooldown): Long {
        val now = System.currentTimeMillis()
        val last = lastClickedData[player.uniqueId] ?: 0L
        val remaining = cooldownMillis - (now - last)
        return if (remaining > 0) remaining else 0
    }

    /**
     * Reset the cooldown for the player, allowing immediate action.
     *
     * @param player the player whose cooldown should be reset
     */
    fun resetCooldown(player: Player) {
        lastClickedData.remove(player.uniqueId)
    }

    /**
     * Clear all cooldown data.
     * This will remove cooldowns for all players.
     */
    fun clear() {
        lastClickedData.clear()
    }
}
