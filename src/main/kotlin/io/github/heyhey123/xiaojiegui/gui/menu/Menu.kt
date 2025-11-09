package io.github.heyhey123.xiaojiegui.gui.menu

import io.github.heyhey123.xiaojiegui.XiaojieGUI
import io.github.heyhey123.xiaojiegui.gui.event.MenuCloseEvent
import io.github.heyhey123.xiaojiegui.gui.event.MenuInteractEvent
import io.github.heyhey123.xiaojiegui.gui.event.MenuOpenEvent
import io.github.heyhey123.xiaojiegui.gui.event.PageTurnEvent
import io.github.heyhey123.xiaojiegui.gui.menu.component.Cooldown
import io.github.heyhey123.xiaojiegui.gui.menu.component.IconProducer
import io.github.heyhey123.xiaojiegui.gui.menu.component.Page
import io.github.heyhey123.xiaojiegui.gui.receptacle.ViewReceptacle
import io.github.heyhey123.xiaojiegui.gui.utils.WeakHashSet
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.ItemStack
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.function.Consumer


/**
 * A menu that can be opened by players.
 *
 * @property id The unique identifier of the menu.
 * When a new menu with the same ID is created, the old one will be destroyed.
 * @property properties The properties of the menu.
 * @property inventoryType The type of inventory for the menu.
 */
class Menu(
    val id: String?,
    val properties: MenuProperties,
    val inventoryType: InventoryType
) {

    init {
        check(XiaojieGUI.instance.isEnabled) {
            "Cannot create a menu when the plugin is disabled."
        }

        id?.let {
            if (id in menusWithId) {
                menusWithId[id]!!.destroy()
            }
            menusWithId[it] = this
        }

        menus.add(this)
    }

    /**
     * Whether the menu has been destroyed.
     */
    var isDestroyed: Boolean = false
        private set

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
     * The default icon mapper for the menu.
     */
    val defaultIconMapper: MutableMap<String, Pair<IconProducer, ((event: MenuInteractEvent) -> Unit)?>> =
        ConcurrentHashMap()

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
        checkDestroyed()

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
            session.shutTemporarily()
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
        checkDestroyed()

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
     * Update the ItemStack associated with a specific key in the icon mapper and refresh the viewers' inventories accordingly.
     *
     * @param key The key to update.
     * @param itemProducer The new IconProducer to associate with the key, or null to remove it.
     * @param pages The collection of page numbers to update. If null, all pages will be updated.,
     * and the default icon mapper will be updated.
     * @param refresh Whether to refresh the viewers' inventories after updating the icon.
     */
    fun updateIconForKey(
        key: String,
        itemProducer: IconProducer,
        refresh: Boolean,
        pages: Collection<Int>? = null,
        callback: ((event: MenuInteractEvent) -> Unit)? = null
    ) {
        checkDestroyed()

        if (pages == null) {
            defaultIconMapper[key] = itemProducer to callback
        }

        val targetPages = pages?.apply {
            forEach {
                check(it in 0..<size) {
                    "Page $it does not exist in this menu."
                }
            }
        } ?: (0..<size).toList()

        for (targetPage in targetPages) {
            val pageInstance = this.pages[targetPage]
            pageInstance.iconMapper[key] = itemProducer to callback
            pageInstance.keyToSlots[key]?.forEach { slot ->
                pageInstance.clickCallbacks[slot] = callback ?: {}
            }
        }

        for (viewer in viewers) {
            val session = MenuSession.querySession(viewer)
            if (session?.menu != this) continue
            if (session.page !in targetPages) continue

            val currentPage = this.pages[session.page]
            currentPage.keyToSlots[key]?.apply {
                forEach { slot ->
                    session.setIcon(slot, itemProducer.produceNext(), false)
                }
                if (refresh) {
                    session.refresh(singleOrNull() ?: -1)
                }
            }
        }
    }

    /**
     * Update the ItemStack associated with a specific key in the icon mapper and refresh the viewers' inventories accordingly.
     * This version uses a Consumer for the callback.
     *
     * @param key The key to update.
     * @param itemProducer The new IconProducer to associate with the key, or null to remove it.
     * @param pages The collection of page numbers to update. If null, all pages will be updated.,
     * and the default icon mapper will be updated.
     * @param refresh Whether to refresh the viewers' inventories after updating the icon.
     * @param callback A Consumer callback to execute on click.
     */
    fun updateIconForKey(
        key: String,
        itemProducer: IconProducer,
        pages: Collection<Int>?,
        refresh: Boolean,
        callback: Consumer<MenuInteractEvent>
    ) = updateIconForKey(key, itemProducer, refresh, pages) { event -> callback.accept(event) }

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
        checkDestroyed()

        check(page in 0..<size) {
            "Page $page does not exist in this menu."
        }
        val pageInstance = pages[page]
        pageInstance.clickCallbacks[slot] = callback
    }

    /**
     * Set a click callback for a specific slot on a specific page.
     * This version uses a Consumer for the callback.
     *
     * @param page The page number.
     * @param slot The slot number.
     * @param callback A Consumer callback to execute on click.
     */
    fun setSlotCallback(
        page: Int,
        slot: Int,
        callback: Consumer<MenuInteractEvent>
    ) = setSlotCallback(page, slot) { event -> callback.accept(event) }

    /**
     * Override the item in a specific slot on specific pages and refresh the viewers' inventories accordingly.
     *
     * @param pages The collection of page numbers.
     * @param slot The slot number.
     * @param item The new ItemStack to set in the slot, or null to clear it.
     * @param refresh Whether to refresh the viewers' inventories after overriding the slot.
     * @param callback An optional callback function to execute on click for the overridden slot.
     */
    fun overrideSlot(
        pages: Collection<Int>?,
        slot: Int,
        item: ItemStack?,
        refresh: Boolean,
        callback: ((event: MenuInteractEvent) -> Unit)? = null
    ) {
        checkDestroyed()

        val targetPages = pages?.apply {
            forEach {
                check(it in 0..<size) {
                    "Page $it does not exist in this menu."
                }
            }
        } ?: (0..<size).toList()

        targetPages.forEach { page ->

            val pageInstance = this.pages[page]
            pageInstance.slotOverrides[slot] = item ?: ItemStack.empty()
            pageInstance.clickCallbacks[slot] = callback ?: {}

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
            }
        }
    }

    /**
     * Override the item in a specific slot on specific pages and refresh the viewers' inventories accordingly.
     * This version uses a Consumer for the callback.
     *
     * @param pages The collection of page numbers.
     * @param slot The slot number.
     * @param item The new ItemStack to set in the slot, or null to clear it.
     * @param refresh Whether to refresh the viewers' inventories after overriding the slot.
     * @param callback A Consumer callback to execute on click for the overridden slot.
     */
    fun overrideSlot(
        pages: Collection<Int>,
        slot: Int,
        item: ItemStack?,
        refresh: Boolean,
        callback: Consumer<MenuInteractEvent>
    ) = overrideSlot(pages, slot, item, refresh) { event -> callback.accept(event) }


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
    ) = overrideSlot(listOf(page), slot, item, refresh, callback)

    /**
     * Override the item in a specific slot on a specific page and refresh the viewers' inventories accordingly.
     * This version uses a Consumer for the callback.
     *
     * @param page The page number.
     * @param slot The slot number.
     * @param item The new ItemStack to set in the slot, or null to clear it.
     * @param refresh Whether to refresh the viewers' inventories after overriding the slot.
     * @param callback A Consumer callback to execute on click for the overridden slot.
     */
    fun overrideSlot(
        page: Int,
        slot: Int,
        item: ItemStack?,
        refresh: Boolean,
        callback: Consumer<MenuInteractEvent>
    ) = overrideSlot(listOf(page), slot, item, refresh) { event -> callback.accept(event) }

    /**
     * Override the items in specific slots on specific pages and refresh the viewers' inventories accordingly.
     *
     * @param pages The page number.
     * @param slots The slot numbers.
     * @param item The new ItemStack to set in the slot, or null to clear it.
     * @param refresh Whether to refresh the viewers' inventories after overriding the slot.
     * @param callback An optional callback function to execute on click for the overridden slot.
     */
    fun overrideSlots(
        pages: Collection<Int>?,
        slots: Collection<Int>,
        item: ItemStack?,
        refresh: Boolean,
        callback: ((event: MenuInteractEvent) -> Unit)? = null
    ) {
        require(slots.isNotEmpty()) {
            "Slots cannot be empty."
        }

        slots.forEach { slot ->
            overrideSlot(pages, slot, item, refresh, callback)
        }
    }

    /**
     * Override the items in specific slots on specific pages and refresh the viewers' inventories accordingly.
     * This version uses a Consumer for the callback.
     *
     * @param pages The page numbers.
     * @param slots The slot numbers.
     * @param item The new ItemStack to set in the slot, or null to clear it.
     * @param refresh Whether to refresh the viewers' inventories after overriding the slot.
     * @param callback A Consumer callback to execute on click for the overridden slot.
     */
    fun overrideSlots(
        pages: Collection<Int>?,
        slots: Collection<Int>,
        item: ItemStack?,
        refresh: Boolean,
        callback: Consumer<MenuInteractEvent>
    ) = overrideSlots(pages, slots, item, refresh) { event -> callback.accept(event) }

    /**
     * Override the items in specific slots on a specific page and refresh the viewers' inventories accordingly.
     *
     * @param pages The page number.
     * @param slots The slot numbers.
     * @param item The new ItemStack to set in the slot, or null to clear it.
     * @param refresh Whether to refresh the viewers' inventories after overriding the slot.
     * @param callback An optional callback function to execute on click for the overridden slot.
     */
    fun overrideSlots(
        page: Int,
        slots: Collection<Int>,
        item: ItemStack?,
        refresh: Boolean,
        callback: ((event: MenuInteractEvent) -> Unit)? = null
    ) = overrideSlots(listOf(page), slots, item, refresh, callback)

    /**
     * Override the items in specific slots on a specific page and refresh the viewers' inventories accordingly.
     * This version uses a Consumer for the callback.
     *
     * @param pages The page number.
     * @param slots The slot numbers.
     * @param item The new ItemStack to set in the slot, or null to clear it.
     * @param refresh Whether to refresh the viewers' inventories after overriding the slot.
     * @param callback A Consumer callback to execute on click for the overridden slot.
     */
    fun overrideSlots(
        page: Int,
        slots: Collection<Int>,
        item: ItemStack?,
        refresh: Boolean,
        callback: Consumer<MenuInteractEvent>
    ) = overrideSlots(listOf(page), slots, item, refresh) { event -> callback.accept(event) }


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
        checkDestroyed()

        val pageInstance = Page(
            inventoryType,
            title ?: properties.defaultTitle,
            layoutPattern ?: properties.defaultLayout,
            playerInventoryPattern ?: listOf(),
            properties
        )

        pageInstance.iconMapper += defaultIconMapper

        pageInstance.keyToSlots.forEach { (key, slots) ->
            val callback = defaultIconMapper[key]?.second ?: return@forEach
            slots.forEach { slot -> pageInstance.clickCallbacks[slot] = callback }
        }

        if (page == null) {
            pages.add(pageInstance)
            return
        }

        pages.add(page.coerceIn(0, size), pageInstance)
    }

    /**
     * Destroy the menu, closing it for all viewers and cleaning up resources.
     * If the menu has already been destroyed, this method does nothing.
     *
     */
    fun destroy() {
        if (isDestroyed) return

        isDestroyed = true

        id?.let { menusWithId.remove(id) }

        for (viewer in viewers) {
            val session = MenuSession.querySession(viewer)
            if (session?.menu != this) continue

            session.close()
        }
    }

    /**
     * Check if the menu has been destroyed and throw an exception if it has.
     *
     */
    private fun checkDestroyed() {
        check(!isDestroyed) {
            "This menu has been destroyed, so it can no longer be used."
        }
    }

    override fun toString() =
        "Menu(id=${id ?: "<unnamed>"}, properties=$properties, inventoryType=$inventoryType, viewers=$viewers, pages=$pages, defaultIconMapper=$defaultIconMapper)"

    companion object {

        /**
         * A map of all menus by their unique identifiers.
         */
        val menusWithId: MutableMap<String, Menu> = ConcurrentHashMap()

        /**
         * A set of all menus, used for cleanup on plugin disable.
         */
        val menus: WeakHashSet<Menu> = WeakHashSet()
    }
}
