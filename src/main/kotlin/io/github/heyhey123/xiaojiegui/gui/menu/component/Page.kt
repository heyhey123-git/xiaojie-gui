package io.github.heyhey123.xiaojiegui.gui.menu.component

import io.github.heyhey123.xiaojiegui.gui.event.MenuCloseEvent
import io.github.heyhey123.xiaojiegui.gui.event.MenuInteractEvent
import io.github.heyhey123.xiaojiegui.gui.event.ReceptacleInteractEvent
import io.github.heyhey123.xiaojiegui.gui.interact.ClickType
import io.github.heyhey123.xiaojiegui.gui.menu.MenuProperties
import io.github.heyhey123.xiaojiegui.gui.menu.MenuSession
import io.github.heyhey123.xiaojiegui.gui.menu.MenuSession.Companion.querySession
import io.github.heyhey123.xiaojiegui.gui.receptacle.Receptacle
import io.github.heyhey123.xiaojiegui.gui.receptacle.ViewLayout
import net.kyori.adventure.text.Component
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.MenuType
import kotlin.math.min

/**
 * A page in a menu, defined by its layout pattern and title.
 *
 * @param inventoryType The type of inventory for the page.
 * @param title The title of the page.
 * @param layoutPattern The layout pattern of the page, represented as a list of strings.
 *                      Each character represents a slot, and spaces represent empty slots.
 * @param playerLayoutPattern The layout pattern for the rows below the container, represented as a list of
 *                     strings. Each character represents a slot, and spaces represent empty slots. Those
 *                     rows are the menu's own only while the player's inventory is hidden, so this pattern
 *                     is only used in PHANTOM mode with the player inventory hidden.
 * @param properties The properties of the menu that this page belongs to.
 */
class Page(
    inventoryType: InventoryType,
    var title: Component,
    val layoutPattern: List<String>,
    playerLayoutPattern: List<String>,
    val properties: MenuProperties
) {
    /**
     * The number of rows in the inventory, constrained by the inventory type.
     */
    val rows: Int = when {
        inventoryType == InventoryType.CHEST -> min(6, layoutPattern.size) // [0, 6]
        inventoryType.menuType == MenuType.GENERIC_3X3 -> 3
        inventoryType.defaultSize % 9 == 0 -> inventoryType.defaultSize / 9
        else -> 1
    }

    /**
     * The layout pattern of the rows below the container, ensured to have exactly 4 rows.
     */
    val playerLayoutPattern: MutableList<String> =
        MutableList(4) { playerLayoutPattern.getOrNull(it) ?: "         " } // 9*4

    /**
     * Whether this page lays the rows below the container out, which is to say whether it asks for the
     * player's inventory to be hidden.
     *
     * The pattern is read once, when the page is built, so this is what a page was declared with rather than
     * what the flag says now. It is what `EffHidePlayerInv` refuses to contradict: those rows are this page's
     * own space, so a menu that has such a page cannot be shown the player's inventory, and neither state
     * can ever meet the other.
     */
    val hasPlayerLayout: Boolean = playerLayoutPattern.any { it.isNotBlank() }

    /**
     * The layout of the inventory, determined by its type and number of rows.
     */
    val layout: ViewLayout = when {
        inventoryType == InventoryType.CHEST -> {
            when (rows) {
                1 -> ViewLayout.Chest.GENERIC_9X1
                2 -> ViewLayout.Chest.GENERIC_9X2
                3 -> ViewLayout.Chest.GENERIC_9X3
                4 -> ViewLayout.Chest.GENERIC_9X4
                5 -> ViewLayout.Chest.GENERIC_9X5
                6 -> ViewLayout.Chest.GENERIC_9X6
                else -> throw IllegalArgumentException("Invalid number of rows for chest layout: $rows")
            }
        }

        else -> ViewLayout.fromInventoryType(inventoryType)
    }

    /**
     * A mapping of slot indices to their click callback functions.
     * The callback function takes a MenuInteractEvent as a parameter.
     */
    val clickCallbacks: MutableMap<Int, (event: MenuInteractEvent) -> Unit> =
        mutableMapOf()

    /**
     * A mapping of slot indices to their overridden item stacks.
     */
    val slotOverrides: MutableMap<Int, ItemStack> = mutableMapOf()

    /**
     * The width of the inventory, derived from its default size.
     */
    val width: Int = when (inventoryType.defaultSize) {
        27 -> 9
        5 -> 5
        else -> 3
    } // ??

    /**
     * The container size of the layout.
     */
    val size: Int = layout.containerSize

    val keyToSlots: MutableMap<String, MutableSet<Int>> = run {
        val mapping = mutableMapOf<String, MutableSet<Int>>()

        fun processLine(line: String, yIndex: Int, baseIndex: Int, rowWidth: Int = width) {
            fun addKey(key: String, visualX: Int) {
                mapping.computeIfAbsent(key) { mutableSetOf() }
                    .add(baseIndex + yIndex * rowWidth + visualX)
            }

            fun addKey(ch: Char, visualX: Int) = addKey(ch.toString(), visualX)

            var i = 0
            var visualX = 0 // the x coordinate inside the container, which is what a slot is counted in

            while (i < line.length && visualX < rowWidth) {
                val ch = line[i]
                if (ch == '`') {
                    val closing = line.indexOf('`', i + 1)
                    if (closing == -1) {
                        // Not a pair: a lone backquote is an ordinary key of its own.
                        addKey('`', visualX)
                        i += 1
                        visualX += 1
                        continue
                    }
                    val keyName = line.substring(i + 1, closing)
                    addKey(keyName, visualX)
                    visualX += 1 // a whole backquoted block is one visible cell
                    i = closing + 1
                } else {
                    addKey(ch, visualX)
                    i += 1
                    visualX += 1
                }
            }
        }

        // The page's own layout.
        layoutPattern.asSequence()
            .take(rows)
            .forEachIndexed { rowIndex, patternLine ->
                processLine(patternLine, rowIndex, 0)
            }

        // The rows below the container are the menu's own space only while the player's inventory is hidden:
        // the client draws that half of the window from the last content packet either way, so a menu that
        // hid it can put icons down there and they are buttons like any other. With the inventory shown the
        // same 36 slots are a copy of the player's real items, and a menu laying icons over them would be
        // neither the player's inventory nor its own half. So the two go together -- declaring a player
        // layout hides the inventory (see the syntax) -- and what a page lays out here is what the menu
        // owns. In `static` mode the lower half is the player's real inventory and never the menu's.
        // A lectern's client menu is one slot and has no player half at all (`ViewLayout` says so), so a page
        // that laid icons out below the container would be writing into slots that window does not have --
        // `contents` is sized from the layout, and the write would land past its end. Skip it, the way a
        // static menu's player layout is skipped, instead of indexing out of the window.
        if (
            properties.mode == Receptacle.Mode.PHANTOM &&
            properties.hidePlayerInventory &&
            layout.hasPlayerInventory
        ) {
            this.playerLayoutPattern.asSequence()
                .take(4)
                .forEachIndexed { rowIndex, patternLine ->
                    processLine(patternLine, rowIndex, size, rowWidth = 9)
                }
        }

        mapping
    }

    /**
     * The icon mapper for the menu, mapping keys to ItemStacks and optional click callbacks.
     */
    val iconMapper: MutableMap<String, Pair<IconProducer, ((MenuInteractEvent) -> Unit)?>> = mutableMapOf()

    /**
     * The slots this page puts an icon in: the ones its layout maps a key to, plus the ones overridden
     * with an item.
     *
     * These are the page's own slots. They are what `locked icons` protects, and they are the only slots
     * a page turn clears, so a slot the layout left empty keeps whatever the player put there -- which is
     * what a paging backpack is.
     */
    fun iconSlots(): Set<Int> {
        val slots = mutableSetOf<Int>()
        for ((key, mapped) in keyToSlots) {
            if (iconMapper[key]?.first != null) slots.addAll(mapped)
        }
        for ((slot, item) in slotOverrides) {
            if (!item.isEmpty) slots.add(slot)
        }
        return slots
    }

    /**
     * Clear the slots this page owns and leave every other slot alone.
     *
     * @param session the session to clear them in
     */
    fun clearIcons(session: MenuSession) {
        iconSlots().forEach { session.setIcon(it, null, false) }
    }

    /**
     * The slots this page fills from a list, in the order a list is written into them.
     *
     * A page declares them by mapping a whole *list* of items to one key -- `map key "L" to icon {_items::*}`
     * -- which is what makes a page a list page without any extra syntax: the key that was given several
     * items is the key a list goes into. Its slots are in layout order, the same order the layout itself
     * fills them in.
     */
    fun listSlots(): List<Int> {
        for ((key, slots) in keyToSlots) {
            if (iconMapper[key]?.first is IconProducer.MultipleIconProducer) return slots.sorted()
        }
        return emptyList()
    }

    /**
     * The 1-based position of a slot in this page's list, or null when the slot is not one of them.
     *
     * @param slot the slot to look for
     */
    fun listIndex(slot: Int): Int? = listSlots().indexOf(slot).takeIf { it >= 0 }?.plus(1)

    /**
     * Write items into this page's list slots, in order.
     *
     * The window is refreshed once for the whole page rather than once per slot, which is the difference
     * between a browser turning a page in one packet and in forty-five.
     *
     * @param session the session to write them in
     * @param items the items to write; a slot with no item left is cleared
     */
    fun setList(session: MenuSession, items: List<ItemStack?>) {
        val slots = listSlots()
        if (slots.isEmpty()) return
        slots.forEachIndexed { index, slot -> session.setIcon(slot, items.getOrNull(index), false) }
        session.refresh()
    }

    /**
     * Whether `locked icons` refuses this interaction.
     *
     * An interaction that touches one of the page's own slots is refused: that slot is the menu's, so
     * nothing about it may change. For the player's own half the question is not which half it happened in
     * but where it would land. A shop that buys from the player needs a shift click to reach the container,
     * and a player tidying their own inventory should be able to do that, so only the slots the menu owns
     * have to stay out of the way:
     *
     * - a **shift click** moves the item into the container, merging into a matching stack first wherever
     *   it is and only then using an empty slot, so it is refused when a matching stack in one of the
     *   page's own slots could take it;
     * - a **double click** collects the item out of every slot that holds one, so it is refused when one
     *   of the page's own slots holds that item at all;
     * - anything else in that half changes nothing in the container and is left to the player.
     */
    private fun refusesInteraction(
        event: ReceptacleInteractEvent,
        iconSlots: Set<Int>,
        session: MenuSession
    ): Boolean {
        if (event.slots.any { it in iconSlots }) return true
        val ownHalfOnly = properties.mode == Receptacle.Mode.STATIC && event.slots.all { it >= size }
        if (!ownHalfOnly) return false

        // A shift click moves what is in the clicked slot; a double click collects what is on the cursor,
        // or what the click picked up when the cursor was empty.
        val moved = when {
            event.clickType.isShiftClick() -> event.clickedItem
            event.clickType == ClickType.DOUBLE_CLICK -> event.cursor ?: event.clickedItem
            else -> null
        } ?: return false

        val ownItems = iconSlots.mapNotNull { session.getIcon(it) }
        return when {
            event.clickType.isShiftClick() ->
                ownItems.any { it.isSimilar(moved) && it.amount < it.maxStackSize }

            event.clickType == ClickType.DOUBLE_CLICK -> ownItems.any { it.isSimilar(moved) }

            else -> false
        }
    }

    /**
     * Load in the page into the given menu session.
     *
     * @param session The menu session to load the page into.
     */
    fun loadInPage(session: MenuSession) {
        val (_, menu, receptacle) = session
        menu ?: return
        receptacle ?: return

        receptacle.title(title, false)

        receptacle.hidePlayerInventory = menu.properties.hidePlayerInventory

        for (icon in iconMapper.values) {
            (icon.first as? IconProducer.MultipleIconProducer)?.reset()
        } // ??

        receptacle.onClose { player, _ ->
            querySession(player)!!.run {
                MenuCloseEvent(this, player, menu).callEvent()
                shut()
            }
        }

        receptacle.onClick { event ->
            event.receptacle.interruptItemDrag(event)

            val doCancel = {
                // cancel if cooldown not passed
                event.isCancelled = true
            }

            val player = event.player

            if (!menu.cooldownManager.tryConsumeCooldown(player)) {
                doCancel()
                return@onClick
            }

            // One interaction is one event, however many slots it touched. A drag is one thing the player
            // did, so a handler that pays out, gives an item or logs must not run once per slot: the slots
            // it touched are `the dragged slots`, and the first of them is `the clicked slot`.
            val menuEvent = MenuInteractEvent(
                session,
                viewer = player,
                menu,
                // The session's page, not `menu.pages.indexOf(this)`: that is a 0-based list index, and
                // every page number a script sees is 1-based, including the one `on page turn` reports.
                session.page,
                event.slot,
                event.receptacle.getElement(event.slot),
                event.clickType,
                event.slots,
                event.cursor
            )

            // A slot callback belongs to one slot, so it is handed an event whose slot and icon are its
            // own: a callback written for a click keeps working when the player drags over that slot
            // instead. A click's one slot makes the two the same event, so nothing changes for clicks.
            var callbackCancelled = false
            for (slot in event.slots) {
                // Only the menu's own cells can be buttons, never the player's real items.
                val ownsLowerSlot = properties.mode == Receptacle.Mode.PHANTOM &&
                    receptacle.hidePlayerInventory &&
                    slot in size until layout.totalSize
                if (slot !in 0 until size && !ownsLowerSlot) {
                    continue
                }
                val callback = clickCallbacks[slot] ?: continue
                val slotEvent = if (event.slots.size == 1) {
                    menuEvent
                } else {
                    MenuInteractEvent(
                        session,
                        viewer = player,
                        menu,
                        session.page,
                        slot,
                        event.receptacle.getElement(slot),
                        event.clickType,
                        event.slots,
                        event.cursor
                    )
                }

                callback(slotEvent)

                if (slotEvent.isCancelled) {
                    callbackCancelled = true
                    break
                }
            }

            // Per-slot events are callback contexts, not additional global interactions.
            if (callbackCancelled) menuEvent.isCancelled = true
            if (!menuEvent.callEvent() || callbackCancelled) {
                doCancel()
            }

            // `locked icons`: an interaction that touches a slot this page gave an icon to changes
            // nothing. The callbacks above have already run by now, which is the point -- a shop's "buy"
            // callback fires and the goods stay exactly where they are.
            if (menu.properties.lockedIcons && refusesInteraction(event, iconSlots(), session)) {
                doCancel()
            }
        }

        val slots = computeSlots()
        // Only mapped slots belong to this page. Nulls in unmapped slots must not erase
        // items the player deposited in a static menu; null list entries still clear owned slots.
        for (index in iconSlots()) {
            if (index in slots.indices) receptacle.setElement(index, slots[index])
        }

        for ((index, item) in slotOverrides) {
            receptacle.setElement(index, item)
        }
    }

    /**
     * Compute the item stacks for each slot in the menu session's receptacle.
     *
     * @return An array of item stacks representing the items in each slot, or null if no item is set for a slot.
     */
    fun computeSlots(): Array<ItemStack?> {
        val size = when (properties.mode) {
            Receptacle.Mode.STATIC -> layout.containerSize
            Receptacle.Mode.PHANTOM -> layout.totalSize
        }
        val slots: Array<ItemStack?> = arrayOfNulls(size)

        for ((key, slotSet) in keyToSlots) {
            val itemProducer = iconMapper[key]?.first ?: continue
            slotSet.forEach { slot ->
                val item = itemProducer.produceNext() ?: return@forEach
                slots[slot] = item.clone()
            }
        }

        return slots
    }

    override fun toString() =
        "Page(title=$title, layoutPattern=$layoutPattern)"
}
