package io.github.heyhey123.xiaojiegui.gui.menu

import io.github.heyhey123.xiaojiegui.gui.receptacle.PhantomReceptacle
import io.github.heyhey123.xiaojiegui.gui.receptacle.ViewReceptacle
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.function.Consumer

class MenuSession(
    val viewer: Player,
    var menu: Menu?,
    var page: Int = -1,
    var receptacle: ViewReceptacle?
) {

    operator fun component1() = viewer
    operator fun component2() = menu
    operator fun component3() = receptacle
    // In order to allow destructuring declarations

    /**
     * Get the icon at the specified slot in the receptacle.
     *
     * @param slot the slot index
     * @return the ItemStack at the specified slot, or null if the slot is empty or the receptacle is null
     */
    fun getIcon(slot: Int): ItemStack? =
        receptacle?.let {
            return it.getElement(slot)
        }

    /**
     * Set the icon at the specified slot in the receptacle.
     *
     * @param slot the slot index
     * @param item the ItemStack to set, or null to clear the slot
     * @param refresh whether to refresh the slot immediately
     */
    fun setIcon(slot: Int, item: ItemStack?, refresh: Boolean) =
        receptacle?.apply {
            setElement(slot, item)
            if (refresh) refresh(slot)
        }

    /**
     * Set multiple icons in the receptacle.
     *
     * @param icons a map of slot indices to ItemStacks (or null to clear the slot)
     * @param refresh whether to refresh the receptacle immediately after setting the icons
     */
    fun setIcons(icons: Map<Int, ItemStack?>, refresh: Boolean) =
        receptacle?.apply {
            for ((slot, item) in icons) {
                setElement(slot, item)
            }
            if (refresh) refresh()
        }


    /**
     * Set the title of the receptacle.
     *
     * @param title the new title as a Component
     * @param refresh whether to refresh the title immediately
     */
    fun title(title: Component, refresh: Boolean) =
        receptacle?.apply {
            title(title, refresh)
        }

    /**
     * Refresh the receptacle, optionally refreshing a specific slot.
     *
     * @param slot
     */
    fun refresh(slot: Int = -1) =
        receptacle?.apply {
            refresh(slot)
        }

    /**
     * Update the player inventory slots in the receptacle.
     *
     */
    fun updatePlayerSlots() {
        menu ?: return
        receptacle ?: return
        if (!receptacle!!.hidePlayerInventory) return
        if (receptacle !is PhantomReceptacle) return

        (receptacle as PhantomReceptacle).setupPlayerInventory()
    }

    /**
     * Close the menu session, triggering a MenuCloseEvent and closing the receptacle.
     *
     */
    fun close() {
        receptacle?.close(true)
    }

    /**
     * Shut down the menu session, clearing references to the menu and receptacle.
     *
     */
    fun shut() {
        menu?.viewers?.remove(viewer.uniqueId)
        menu = null
        page = -1
        receptacle = null
        removeSession(viewer)
    }

    override fun toString() =
        "MenuSession(viewer=$viewer, menu=$menu, page=$page, receptacle=$receptacle)"

    companion object {

        internal val SESSIONS: ConcurrentMap<UUID, MenuSession> = ConcurrentHashMap()

        /**
         * Get the MenuSession associated with the given player.
         * If no session exists, a new one is created.
         *
         * @param player the player to get the session for
         * @return the MenuSession associated with the player
         */
        fun getSession(player: Player): MenuSession =
            SESSIONS.computeIfAbsent(player.uniqueId) { MenuSession(player, null, -1, null) }

        /**
         * Query the MenuSession associated with the given player.
         *
         * @param player the player to query the session for
         * @return the MenuSession associated with the player, or null if none exists
         */
        fun querySession(player: Player): MenuSession? =
            SESSIONS[player.uniqueId]

        /**
         * Query the MenuSession associated with the given UUID.
         * @param uuid the UUID to query the session for
         * @return the MenuSession associated with the UUID, or null if none exists
         */
        fun querySession(uuid: UUID): MenuSession? =
            SESSIONS[uuid]

        /**
         * Remove the MenuSession associated with the given player.
         *
         * @param player the player whose session should be removed
         */
        fun removeSession(player: Player) {
            SESSIONS.remove(player.uniqueId)
        }

        /**
         * Perform the given action for each active MenuSession.
         *
         * @param action the action to perform for each session
         */
        fun forEachSession(action: (MenuSession) -> Unit) {
            SESSIONS.values.forEach(action)
        }

        /**
         * Perform the given action for each active MenuSession.
         * This version accepts a Consumer as the action.
         *
         * @param action the action to perform for each session
         */
        fun forEachSession(action: Consumer<MenuSession>) =
            forEachSession { action.accept(it) }

        /**
         * Clear all MenuSessions.
         *
         */
        fun clearSessions() {
            SESSIONS.clear()
        }
    }
}
