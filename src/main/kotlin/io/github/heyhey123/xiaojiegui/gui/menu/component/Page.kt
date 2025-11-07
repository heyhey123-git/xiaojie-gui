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
import java.util.concurrent.ConcurrentHashMap
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
    @Volatile var title: Component,
    val layoutPattern: List<String>,
    playerInventoryPattern: List<String>,
    val properties: MenuProperties
) {
    /**
     * The number of rows in the inventory, constrained by the inventory type.
     */
    val rows: Int = when {
        inventoryType == InventoryType.CHEST -> min(6, layoutPattern.size) // [0, 6]
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
        ConcurrentHashMap()

    /**
     * A mapping of slot indices to their overridden item stacks.
     */
    val slotOverrides: MutableMap<Int, ItemStack> = ConcurrentHashMap()

    /**
     * The width of the inventory, derived from its default size.
     */
    val width: Int = when (inventoryType.defaultSize) {
        27 -> 9
        5 -> 5
        else -> 3
    } //??

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
            var visualX = 0 // 实际上容器里的 x 坐标

            while (i < line.length && visualX < width) {
                val ch = line[i]
                if (ch == '`') {
                    val closing = line.indexOf('`', i + 1)
                    if (closing == -1) {
                        // 不成对，按普通字符处理
                        addKey('`', visualX, yIndex, baseIndex)
                        i += 1
                        visualX += 1
                        continue
                    }
                    val keyName = line.substring(i + 1, closing)
                    addKey(keyName, visualX, yIndex, baseIndex)
                    visualX += 1 // 整个反引号块占 1 个可视格
                    i = closing + 1
                } else {
                    addKey(ch, visualX, yIndex, baseIndex)
                    i += 1
                    visualX += 1
                }
            }
        }

        // 主布局
        layoutPattern.asSequence()
            .take(rows)
            .forEachIndexed { rowIndex, patternLine ->
                processLine(patternLine, rowIndex, 0)
            }

        // 玩家背包（PHANTOM，从 containerSize 之后开始）
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
    val iconMapper: ConcurrentHashMap<String, Pair<IconProducer, ((MenuInteractEvent) -> Unit)?>> = ConcurrentHashMap()

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

        receptacle.onClose { player, _ ->
            querySession(player)!!.run {
                MenuCloseEvent(this, player, menu).callEvent()
                shut()
            }
        }

        receptacle.onClick { event ->
            event.receptacle.interruptItemDrag(event)

            val doCancel = { // cancel if cooldown not passed
                event.isCancelled = true
            }

            val player = event.player

            if (!menu.cooldownManager.tryConsumeCooldown(player)) {
                doCancel()
                return@onClick
            }

            val menuEvent = MenuInteractEvent(
                session,
                viewer = player,
                menu,
                menu.pages.indexOf(this),
                event.slot,
                event.receptacle.getElement(event.slot),
                event.clickType
            )

            clickCallbacks[event.slot]?.invoke(menuEvent)

            if (!menuEvent.callEvent()) {
                doCancel()
                return@onClick
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

