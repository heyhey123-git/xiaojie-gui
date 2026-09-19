package io.github.heyhey123.xiaojiegui.gui.menu.component

import io.github.heyhey123.xiaojiegui.gui.event.MenuCloseEvent
import io.github.heyhey123.xiaojiegui.gui.event.MenuInteractEvent
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
 * @param playerInventoryPattern The layout pattern for the player's inventory, represented as a list of
 *                     strings. Each character represents a slot, and spaces represent empty slots.
 *                     This pattern is only used if the menu's mode is PHANTOM.
 * @param properties The properties of the menu that this page belongs to.
 */
class Page(
    inventoryType: InventoryType,
    var title: Component,
    val layoutPattern: List<String>,
    playerInventoryPattern: List<String>,
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
     * The layout pattern of the player's inventory, ensured to have exactly 4 rows.
     */
    val playerInventoryPattern: MutableList<String> =
        MutableList(4) { playerInventoryPattern.getOrNull(it) ?: "         " } // 9*4

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

        fun computeSlot(visualX: Int, yIndex: Int, baseIndex: Int): Int =
            baseIndex + yIndex * width + visualX

        fun addKey(key: String, visualX: Int, yIndex: Int, baseIndex: Int) {
            mapping.computeIfAbsent(key) { mutableSetOf() }
                .add(computeSlot(visualX, yIndex, baseIndex))
        }

        fun addKey(ch: Char, visualX: Int, yIndex: Int, baseIndex: Int) {
            addKey(ch.toString(), visualX, yIndex, baseIndex)
        }

        fun processLine(line: String, yIndex: Int, baseIndex: Int) {
            var i = 0
            var visualX = 0 // the x coordinate inside the container, which is what a slot is counted in

            while (i < line.length && visualX < width) {
                val ch = line[i]
                if (ch == '`') {
                    val closing = line.indexOf('`', i + 1)
                    if (closing == -1) {
                        // Not a pair: a lone backquote is an ordinary key of its own.
                        addKey('`', visualX, yIndex, baseIndex)
                        i += 1
                        visualX += 1
                        continue
                    }
                    val keyName = line.substring(i + 1, closing)
                    addKey(keyName, visualX, yIndex, baseIndex)
                    visualX += 1 // a whole backquoted block is one visible cell
                    i = closing + 1
                } else {
                    addKey(ch, visualX, yIndex, baseIndex)
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

        // The player's inventory, for a phantom page that shows it: it starts after the container.
        if (properties.mode == Receptacle.Mode.PHANTOM && !properties.hidePlayerInventory) {
            this.playerInventoryPattern.asSequence()
                .take(4)
                .forEachIndexed { rowIndex, patternLine ->
                    processLine(patternLine, rowIndex, size)
                }
        }

        mapping
    }

    /**
     * The icon mapper for the menu, mapping keys to ItemStacks and optional click callbacks.
     */
    val iconMapper: MutableMap<String, Pair<IconProducer, ((MenuInteractEvent) -> Unit)?>> = mutableMapOf()

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
            for (slot in event.slots) {
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

                if (!slotEvent.callEvent()) {
                    doCancel()
                    return@onClick
                }
            }

            if (!menuEvent.callEvent()) {
                doCancel()
            }
        }

        val slots = computeSlots()
        for ((index, item) in slots.withIndex()) {
            receptacle.setElement(index, item)
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
