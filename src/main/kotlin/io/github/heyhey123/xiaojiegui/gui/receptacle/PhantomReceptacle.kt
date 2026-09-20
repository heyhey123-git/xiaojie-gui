package io.github.heyhey123.xiaojiegui.gui.receptacle

import io.github.heyhey123.xiaojiegui.gui.PacketHelper
import io.github.heyhey123.xiaojiegui.gui.event.ReceptacleInteractEvent
import io.github.heyhey123.xiaojiegui.gui.interact.ClickType
import io.github.heyhey123.xiaojiegui.gui.interact.QuickCraft
import io.github.heyhey123.xiaojiegui.gui.utils.TaskUtil
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.inventory.ItemStack

class PhantomReceptacle(title: Component, layout: ViewLayout) : ViewReceptacle(title, layout, Mode.PHANTOM) {

    companion object {
        /**
         * How many ticks the title re-open is deferred by.
         *
         * The protocol has no "set title": the only way a window's title changes is a second open-screen
         * packet, which is what [title] sends (with the window's whole contents behind it).
         *
         * Where this used to come from, and why it is 0 now: TrMenu's `WindowReceptacle` changed a title
         * with `submit(delay = 3, async = true) { initializationPackets() }` -- three *ticks* (TabooLib
         * documents its delay as ticks) on an *asynchronous* task. Both halves of that say "get out of the
         * call stack that changed the title": an async task is not the tick or the packet handler that asked
         * for it. `TaskUtil.sync` with delay 0 already is the next tick, which is the same thing, and a real
         * 26.2 client was used to check it: opening a menu and retitling it in the same command
         * (`/acc sametick` in `docs/manual-acceptance.sk`), a page turn, and a per-page title refresh all
         * show the new title with the extra ticks removed. The old value was a margin, not a measurement.
         *
         * Note that TrMenu sent real incrementing state ids with its content packets (`stateId = stateId`)
         * while this addon sends -1, which the client treats as "no state check": that ordering hazard is
         * another reason a plugin with real state ids may want to defer the re-open, and not one this code
         * shares.
         *
         * What *is* pinned is that the re-open has to happen at all: the client test waits for the window of
         * page 2 after a page turn and times out when this packet is skipped, and a page turn is exactly what
         * schedules it (the receptacle being left carries the new page's title to the client).
         */
        internal const val TITLE_REOPEN_DELAY_TICKS = 0L
    }

    /**
     * The contents of the receptacle.
     */
    private val contents = arrayOfNulls<ItemStack?>(layout.totalSize)

    private var windowId: Int = -1

    /**
     * The slots the drag in progress has collected, or null when no drag is in progress.
     */
    private var draggedSlots: MutableSet<Int>? = null

    override fun getElement(slot: Int): ItemStack? {
        // Past the container the window holds either the player's own items or the menu's, and which of the
        // two it is, is exactly what hiding the player's inventory decides. Hidden, those slots are the
        // menu's own space and `contents` is where a script's icons are; shown, they are a copy of the
        // player's real inventory, and reading that live costs one lookup instead of copying all 36.
        if (!hidePlayerInventory && slot !in layout.containerSlotRange) return playerHalfItem(slot)
        return contents.getOrNull(slot)
    }

    /**
     * The player's own item in a window slot past the container, or null for a slot that is not theirs.
     *
     * `PlayerInventory` numbers the hotbar 0..8 and the main inventory 9..35, and a window puts the main
     * inventory before the hotbar, so the two ranges are mapped back to those numbers. A menu whose client
     * window has no player inventory has neither range, so every slot past the container belongs to
     * nobody and this answers null for all of them.
     */
    private fun playerHalfItem(slot: Int): ItemStack? {
        val player = viewer ?: return null
        val index = when {
            slot in layout.hotBarSlotRange -> slot - layout.hotBarSlotRange.first()
            slot in layout.mainInvSlotRange -> 9 + (slot - layout.mainInvSlotRange.first())
            else -> return null
        }
        return player.inventory.getItem(index)
    }

    override fun setElement(slot: Int, item: ItemStack?) {
        contents[slot] = item
    }

    override fun doOpen(player: Player) {
        windowId = PacketHelper.instance.generateNextContainerId(player)
        initializationPackets()
    }

    override fun doClose() {
        PacketHelper.instance.sendContainerClose(viewer!!)
    }

    override fun clear(render: Boolean) {
        contents.fill(null)
        if (!render) return
        refresh()
    }

    override fun title(title: Component, render: Boolean) {
        this.title = title
        if (!render) return
        // Delayed, because the client ignores a title change in the same tick it opened the window.
        // The viewer is re-checked when the task runs: ViewReceptacle.close clears it, and without that
        // check the open-screen packet below would put a window back on screen that the server no longer
        // has a container for.
        //
        // Do not "harden" this into "only while this is still the session's receptacle": a page turn
        // schedules this update on the receptacle it is leaving, and that delayed open-screen packet is
        // what carries the new page's title to the client -- the session holds a different receptacle by
        // the time the task runs. Guarding on identity makes the client sit on the old title forever
        // (the client test waits for the page 2 window and times out).
        TaskUtil.sync(delay = TITLE_REOPEN_DELAY_TICKS) {
            viewer ?: return@sync
            initializationPackets()
        }
    }

    override fun interruptItemDrag(event: ReceptacleInteractEvent) {
        if (event.clickType.isItemMoveable()) {
            refresh()
        } else if (event.isDrag) {
            // A drag touched several slots, so putting one of them back is not enough.
            event.slots.forEach { refresh(it) }
        } else {
            refresh(event.slot)
        }

        if (event.clickType == ClickType.SWAP_OFFHAND) {
            PacketHelper.instance.sendContainerSetSlot(
                player = viewer!!,
                windowId = 0,
                slot = 45,
                item = viewer!!.equipment.itemInOffHand
            )
        } else {
            PacketHelper.instance.sendContainerSetSlot(
                player = viewer!!,
                windowId = -1,
                slot = -1,
                item = null
            )
        }
    }

    override fun refresh(slot: Int) {
        viewer ?: return
        setupPlayerInventory()
        if (slot >= 0) {
            PacketHelper.instance.sendContainerSetSlot(viewer!!, windowId, slot, contents[slot])
            return
        }
        PacketHelper.instance.sendContainerSetContent(viewer!!, windowId, contents)
    }

    /**
     * Send initialization packets to the player to set up the receptacle view.
     */
    fun initializationPackets() {
        viewer ?: return
        PacketHelper.instance.sendOpenScreen(
            viewer!!,
            windowId,
            (layout as ViewLayout).type,
            title
        )
        refresh()
    }

    /**
     * Set up the player's inventory, copying items from the player's actual inventory
     * into the receptacle's contents if the player inventory is not to be hidden.
     *
     * What is below the container is then the player's, and only the player's: the rows are cleared first,
     * because `contents` is what a whole-window refresh sends, and an icon a page laid out for those rows
     * would otherwise arrive in every cell the player happens to leave empty. That is what makes "the page's
     * icons drawn over the player's items" impossible rather than merely discouraged.
     *
     * A window whose client menu has no player inventory stops here, and that is also what makes
     * `hide player inventory` / `show player inventory` meaningless on such a menu rather than dangerous:
     * the client menu has no player slots to fill or hide, so the flag is documented as having no effect
     * rather than refused -- there is no state to contradict, and the empty ranges are what would have
     * made the walk below index past the end of a list.
     */
    fun setupPlayerInventory() {
        if (!layout.hasPlayerInventory || hidePlayerInventory || viewer == null) return

        for (slot in layout.containerSize until contents.size) {
            contents[slot] = null
        }

        viewer!!.inventory.contents.forEachIndexed { index, itemStack ->
            if (itemStack != null) {
                val slot =
                    when (index) { // in player inventory, 0-8 hotbar, 9-35 main inv (not same sorting as receptacle)
                        in 0..8 -> layout.hotBarSlotRange[index]
                        in 9..35 -> layout.mainInvSlotRange[index - 9]
                        else -> -1
                    } // ensure slot is valid
                if (slot >= 0) {
                    contents[slot] = itemStack
                }
            }
        }
    }

    override fun clicked(clickType: ClickType, slot: Int, staticInventoryEvent: InventoryClickEvent?) {
        val event = ReceptacleInteractEvent(viewer!!, this, clickType, slot)
        onClick(event)

        event.callEvent()
    }

    override fun dragged(
        clickType: ClickType,
        slots: List<Int>,
        cursor: ItemStack?,
        dragEvent: InventoryDragEvent?
    ) {
        val event = ReceptacleInteractEvent(viewer!!, this, clickType, slots.first(), slots, cursor)
        onClick(event)

        // There is no bukkit event to cancel here: the window is the server's own invention, and the
        // refresh the interaction already did is what puts the client's own drag back.
        event.callEvent()
    }

    /**
     * One `QUICK_CRAFT` packet of a drag in progress.
     *
     * The protocol sends a drag as a run of click packets whose *button* carries the phase, and nothing
     * here can act on a single one of them: the slots are only known once the run ends, and the server
     * itself ignores a drag that collected fewer than two of them. So they are collected here and
     * reported as one interaction on the end packet, which is what a script sees.
     *
     * @param slot the slot the packet names, which the start packet's phase ignores
     * @param button the packet's button: its phase and the button the drag was made with
     */
    fun dragPacket(slot: Int, button: Int) {
        when (QuickCraft.header(button)) {
            QuickCraft.START -> draggedSlots = linkedSetOf()

            QuickCraft.CONTINUE -> draggedSlots?.let {
                val ownsLowerSlot = hidePlayerInventory && slot in layout.containerSize until layout.totalSize
                if (slot in layout.containerSlotRange || ownsLowerSlot) {
                    it.add(slot)
                }
            }

            QuickCraft.END -> {
                val slots = draggedSlots ?: return
                draggedSlots = null
                val clickType = QuickCraft.clickType(button)
                when {
                    // A drag over nothing is what a start followed by an end is: no slots were collected.
                    slots.isEmpty() -> return
                    // A drag that collected one slot is what the server rewrites into a plain click, so
                    // it goes down the same path a click does rather than becoming a drag of one slot.
                    slots.size == 1 -> clicked(clickType, slots.first(), null)
                    else -> dragged(clickType, slots.toList(), null, null)
                }
            }
        }
    }
}
