package io.github.heyhey123.xiaojiegui.gui.receptacle

import io.github.heyhey123.xiaojiegui.gui.PacketHelper
import io.github.heyhey123.xiaojiegui.gui.event.ReceptacleInteractEvent
import io.github.heyhey123.xiaojiegui.gui.interact.ClickType
import io.github.heyhey123.xiaojiegui.utils.TaskUtil
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack

class PhantomReceptacle(title: Component, layout: ViewLayout) : ViewReceptacle(title, layout, Mode.PHANTOM) {

    /**
     * The contents of the receptacle.
     */
    private val contents = arrayOfNulls<ItemStack?>(layout.totalSize)

    private var windowId: Int = -1

    override fun getElement(slot: Int): ItemStack? {
        setupPlayerInventory()
        // just follow trm, if this method called frequently, may cause performance issue
        return contents.getOrNull(slot)
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

    override fun clear() {
        contents.fill(null)
        refresh()
    }

    override fun title(title: Component, render: Boolean) {
        this.title = title
        if (!render) return
        TaskUtil.sync(delay = 3L) {
            viewer ?: return@sync
            initializationPackets()
        }
    }

    override fun interruptItemDrag(event: ReceptacleInteractEvent) {
        if (event.clickType.isItemMoveable()) {
            refresh()
        } else {
            refresh(event.slot)
        }

        if (event.clickType == ClickType.SWAP_OFFHAND) {
            PacketHelper.instance.sendContainerSetSlot(
                player = viewer!!,
                windowId = 0,
                slot = 45,
                item = viewer!!.equipment?.itemInOffHand
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
            title,
        )
        PacketHelper.instance.sendContainerSetSlot(
            viewer!!,
            windowId = 0,
            slot = 45,
            item = null
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
        event.callEvent()

        if (viewer != null) {
            onClick(event)
        }
    }

}
