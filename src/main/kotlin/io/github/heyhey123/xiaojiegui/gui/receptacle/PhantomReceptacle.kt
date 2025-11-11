package io.github.heyhey123.xiaojiegui.gui.receptacle

import io.github.heyhey123.xiaojiegui.gui.PacketHelper
import io.github.heyhey123.xiaojiegui.gui.event.ReceptacleInteractEvent
import io.github.heyhey123.xiaojiegui.gui.interact.ClickType
import io.github.heyhey123.xiaojiegui.gui.utils.TaskUtil
import io.github.retrooper.packetevents.util.SpigotConversionUtil
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack

private typealias PacketEventsItemStack = com.github.retrooper.packetevents.protocol.item.ItemStack

class PhantomReceptacle(title: Component, layout: ViewLayout) : ViewReceptacle(title, layout, Mode.PHANTOM) {

    /**
     * The contents of the receptacle.
     */
    private val contents = arrayOfNulls<ItemStack?>(layout.totalSize)

    private val convertedContent: Array<PacketEventsItemStack> =
        Array(layout.totalSize) { PacketEventsItemStack.EMPTY }

    /**
     * A cache of current item in the player's off-hand.
     */
    private var currentOffHand: Pair<ItemStack, PacketEventsItemStack>? = null

    private var windowId: Int = -1

    override fun getElement(slot: Int): ItemStack? {
        setupPlayerInventory()
        // just follow trm, if this method called frequently, may cause performance issue
        return contents.getOrNull(slot)
    }

    override fun setElement(slot: Int, item: ItemStack?) {
        contents[slot] = item
        convertedContent[slot] = SpigotConversionUtil.fromBukkitItemStack(item)
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
            val offhand = viewer!!.equipment.itemInOffHand
            if (currentOffHand == null || currentOffHand!!.first != offhand) {
                currentOffHand = Pair(offhand, SpigotConversionUtil.fromBukkitItemStack(offhand))
            }

            val item = currentOffHand!!.second
            PacketHelper.instance.sendContainerSetSlot(
                player = viewer!!,
                windowId = 0,
                slot = 45,
                item
            )
        } else {
            PacketHelper.instance.sendContainerSetSlot(
                player = viewer!!,
                windowId = -1,
                slot = -1,
                item = PacketEventsItemStack.EMPTY
            )
        }
    }

    override fun refresh(slot: Int) {
        viewer ?: return
        setupPlayerInventory()
        if (slot >= 0) {
            PacketHelper.instance.sendContainerSetSlot(viewer!!, windowId, slot, convertedContent[slot])
            return
        }
        PacketHelper.instance.sendContainerSetContent(viewer!!, windowId, convertedContent)
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
            item = PacketEventsItemStack.EMPTY
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
                if (slot >= 0 && contents[slot] != itemStack) {
                    setElement(slot, itemStack)
                }
            }
        }
    }

    override fun clicked(clickType: ClickType, slot: Int, staticInventoryEvent: InventoryClickEvent?) {
        val event = ReceptacleInteractEvent(viewer!!, this, clickType, slot)
        onClick(event)

        event.callEvent()
    }

}
