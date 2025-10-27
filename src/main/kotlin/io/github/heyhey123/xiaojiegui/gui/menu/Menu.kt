package io.github.heyhey123.xiaojiegui.gui.menu

import io.github.heyhey123.xiaojiegui.gui.event.MenuCloseEvent
import io.github.heyhey123.xiaojiegui.gui.event.MenuInteractEvent
import io.github.heyhey123.xiaojiegui.gui.event.MenuOpenEvent
import io.github.heyhey123.xiaojiegui.gui.event.PageTurnEvent
import io.github.heyhey123.xiaojiegui.gui.menu.component.Cooldown
import io.github.heyhey123.xiaojiegui.gui.menu.component.Page
import io.github.heyhey123.xiaojiegui.gui.receptacle.ViewReceptacle
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.ItemStack
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList


/**
 * A menu that can be opened by players.
 *
 * @property id The unique identifier of the menu.
 * @property properties The properties of the menu.
 * @property inventoryType The type of inventory for the menu.
 */
class Menu(
    val id: String?,
    val properties: MenuProperties,
    val inventoryType: InventoryType
) {

    init {
        id?.let {
            if (menus.containsKey(it)) {
                throw IllegalArgumentException("Menu with id $it already exists.")
            }
            menus[it] = this
        }
    }

    /**
     * The viewers of the menu.
     */
    val viewers: MutableSet<UUID> = mutableSetOf()

    /**
     * The cooldown manager for the menu, which handles the minimum delay between clicks.
     */
    val cooldownManager: Cooldown = Cooldown(properties)

    /**
     * The pages of the menu.
     */
    val pages: MutableList<Page> = CopyOnWriteArrayList()

    /**
     * The number of pages in the menu.
     */
    val size: Int
        get() = pages.size

    /**
     * A mapping of keys to ItemStacks for easy icon management.
     */
    val iconMapper: ConcurrentHashMap<String, ItemStack> = ConcurrentHashMap()
    // key or value of a ConcurrentHashMap can be null,
    // but we can use ItemStack.empty() to represent null value

    /**
     * Open the menu for a player at a specific page.
     *
     * @param viewer The player who is viewing the menu.
     * @param page The page number to open.
     */
    fun open(
        viewer: Player,
        page: Int = properties.defaultPage
    ) {
        val session = MenuSession.getSession(viewer)

        check(session.menu != this) {
            "Player ${viewer.name} is already viewing this menu."
        }

        check(page in 0..<pages.size) {
            "Page $page does not exist in this menu."
        }

        val pageInstance = pages[page]
        val title = pageInstance.title
        val layout = pageInstance.layout
        val mode = properties.mode

        val event = MenuOpenEvent(session, viewer, this, page)
        if (!event.callEvent()) return

        viewers.add(viewer.uniqueId)

        if (session.menu != null) {
            MenuCloseEvent(session, viewer, session.menu!!).callEvent()
            session.shut()
        }

        session.menu = this

        val receptacle = ViewReceptacle.create(title, layout, mode)
        session.page = page
        session.receptacle = receptacle
        session.updatePlayerSlots()

        pageInstance.loadInPage(session)

        receptacle.open(viewer)
    }

    /**
     * Turn to a specific page in the menu.
     *
     * @param viewer The player who is viewing the menu.
     * @param page The page number to turn to.
     * @param newTitle An optional new title for the menu. If null, the title of the target page will be used.
     */
    fun turnPage(viewer: Player, page: Int, newTitle: Component? = null) {
        val session = MenuSession.getSession(viewer)
        val (_, menu, receptacle) = session

        check(menu == this) {
            "Player ${viewer.name} is not viewing this menu."
        }
        check(page in 0..<pages.size) {
            "Page $page does not exist in this menu."
        }

        var titleToSet = newTitle ?: pages[page].title

        val event = PageTurnEvent(session, viewer, this, session.page, page, titleToSet)
        if (!event.callEvent()) return

        titleToSet = event.title

        receptacle!!.clear()
        session.page = page

        session.updatePlayerSlots()
        val shouldUpdateTitle: Boolean = titleToSet != receptacle.title

        pages[page].loadInPage(session)

        if (shouldUpdateTitle) {
            receptacle.title(titleToSet, true)
            return
        }

        receptacle.refresh()
    }

    /**
     * Translate a key to an ItemStack using the icon mapper.
     *
     * @param key The key to translate.
     * @return The corresponding ItemStack, or null if the key does not exist.
     */
    fun translateIcon(key: String): ItemStack? = iconMapper[key]

    /**
     * Update the ItemStack associated with a specific key in the icon mapper and refresh the viewers' inventories accordingly.
     *
     * @param key The key to update.
     * @param item The new ItemStack to associate with the key, or null to remove it.
     * @param refresh Whether to refresh the viewers' inventories after updating the icon.
     */
    fun updateIconForKey(
        key: String,
        item: ItemStack?,
        refresh: Boolean,
        callback: ((event: MenuInteractEvent) -> Unit)? = null
    ) {
        iconMapper[key] = item ?: ItemStack.empty()
        for (viewer in viewers) {
            val session = MenuSession.querySession(viewer)
            if (session?.menu != this) continue

            val page = pages[session.page]

            page.keyToSlots[key]?.apply {
                forEach { session.setIcon(it, item, false) }

                if (refresh) {
                    session.refresh(singleOrNull() ?: -1)
                }
            }
        }

        if (callback == null) return

        for (singlePage in pages) {
            singlePage.keyToSlots[key]?.forEach {
                singlePage.clickCallbacks[it] = callback
            }
        }
    }

    /**
     * Set a click callback for a specific slot on a specific page.
     *
     * @param page The page number.
     * @param slot The slot number.
     * @param callback The callback function to execute on click.
     */
    fun setSlotCallback(
        page: Int,
        slot: Int,
        callback: ((event: MenuInteractEvent) -> Unit)
    ) {
        check(page in 0..<size) {
            "Page $page does not exist in this menu."
        }
        val pageInstance = pages[page]
        pageInstance.clickCallbacks[slot] = callback
    }

    /**
     * Override the item in a specific slot on a specific page and refresh the viewers' inventories accordingly.
     *
     * @param page The page number.
     * @param slot The slot number.
     * @param item The new ItemStack to set in the slot, or null to clear it.
     * @param refresh Whether to refresh the viewers' inventories after overriding the slot.
     * @param callback An optional callback function to execute on click for the overridden slot.
     */
    fun overrideSlot(
        page: Int,
        slot: Int,
        item: ItemStack?,
        refresh: Boolean,
        callback: ((event: MenuInteractEvent) -> Unit)? = null
    ) {
        if (page !in 0..<size) return
        val pageInstance = pages[page]
        pageInstance.slotOverrides[slot] = item ?: ItemStack.empty()

        for (viewer in viewers) {
            val session = MenuSession.querySession(viewer)
            if (session?.menu != this || session.page != page) continue

            try {
                session.setIcon(slot, item, false)
            } catch (e: IndexOutOfBoundsException) {
                throw IndexOutOfBoundsException("Slot $slot is out of bounds for the receptacle.").apply {
                    initCause(e)
                }
            }

            if (refresh) {
                session.refresh(slot)
            }
            if (callback != null) {
                pageInstance.clickCallbacks[slot] = callback
            }
        }
    }

    /**
     * Insert a new page into the menu at the specified position.
     * @param page The position to insert the new page. If out of bounds, it will be clamped.
     * @param layoutPattern The layout pattern for the new page.
     * @param title The title for the new page. If null, the default title from properties will be used.
     * @param playerInventoryPattern The player inventory pattern for the new page. If null, an empty pattern will be used.
     */
    fun insertPage(
        page: Int?,
        layoutPattern: List<String>?,
        title: Component?,
        playerInventoryPattern: List<String>?
    ) {
        val pageInstance = Page(
            inventoryType,
            title ?: properties.defaultTitle,
            layoutPattern ?: properties.defaultLayout,
            playerInventoryPattern ?: listOf(),
            properties
        )

        if (page == null) {
            pages.add(pageInstance)
            return
        }

        pages.add(page.coerceIn(0, size), pageInstance)
    }

    override fun toString() =
        "Menu(id=${id ?: "<unnamed>"}, properties=$properties, inventoryType=$inventoryType, viewers=$viewers, pages=$pages, iconMapper=$iconMapper)"

    companion object {

        /**
         * A map of all menus by their unique identifiers.
         */
        val menus: MutableMap<String, Menu> = ConcurrentHashMap()
    }
}
