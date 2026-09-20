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
     * inventory before the hotbar, so the two ranges are mapped back to those numbers.
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
        // The viewer is re-checked when the task runs: ViewReceptacle.close clears it, and without
        // that check the open-screen packet below would put a window back on screen that the server
        // no longer has a container for.
        TaskUtil.sync(delay = 3L) {
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
     */
    fun setupPlayerInventory() {
        if (hidePlayerInventory || viewer == null) return
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
                if (slot in layout.containerSlotRange) it.add(slot)
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
