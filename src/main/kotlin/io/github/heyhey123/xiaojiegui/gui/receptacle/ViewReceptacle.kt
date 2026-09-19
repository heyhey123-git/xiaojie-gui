package io.github.heyhey123.xiaojiegui.gui.receptacle

import io.github.heyhey123.xiaojiegui.gui.event.ReceptacleCloseEvent
import io.github.heyhey123.xiaojiegui.gui.event.ReceptacleInteractEvent
import io.github.heyhey123.xiaojiegui.gui.interact.ClickType
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.inventory.ItemStack
import java.util.HashMap
import java.util.UUID

abstract class ViewReceptacle(
    title: Component,
    layout: ViewLayout,
    mode: Mode
) : Receptacle(title, layout, mode) {

    override var onOpen: (player: Player, receptacle: Receptacle) -> Unit = { _, _ -> }

    override var onClose: (player: Player, receptacle: Receptacle) -> Unit = { _, _ -> }

    override var onClick: (event: ReceptacleInteractEvent) -> Unit = { _ -> }

    /**
     * The player currently viewing the receptacle, or null if no player is viewing it.
     */
    protected var viewer: Player? = null

    /**
     * Whether to hide the player's inventory contents when the receptacle is opened.
     */
    var hidePlayerInventory = false

    override fun open(player: Player) {
        viewer = player
        player.viewingReceptacle = this
        onOpen(player, this)
        doOpen(player)
    }

    /**
     * Perform the actual opening of the receptacle for the player.
     *
     * @param player the player to open the receptacle for
     */
    protected abstract fun doOpen(player: Player)

    override fun close(render: Boolean) {
        // render is true when the plugin closes the receptacle and false when the player closed it:
        // only the first case has a packet to send.
        val player = viewer ?: return
        onClose(player, this)
        player.removeViewingReceptacle()
        if (render) {
            doClose()
        }
        player.updateInventory()
        // Cleared last because doClose() reads it. A null viewer is also what keeps a delayed title
        // update from reopening a window the player has already closed.
        viewer = null
    }

    /**
     * Perform the actual closing of the receptacle, triggered by player action.
     */
    protected abstract fun doClose()

    /**
     * Handle a click in the receptacle, triggering the appropriate events and actions.
     *
     * @param clickType the type of click
     * @param slot the slot that was clicked
     * @param staticInventoryEvent the bukkit InventoryClickEvent, or null if not applicable
     */
    internal abstract fun clicked(clickType: ClickType, slot: Int, staticInventoryEvent: InventoryClickEvent?)

    /**
     * Dispatch one interaction that touched several slots at once, which can only be a drag.
     *
     * A drag that touched a single slot never reaches this: the server delivers it as an ordinary click,
     * so it goes through [clicked] and a script sees exactly what it would see for a click.
     *
     * @param clickType the button the drag was made with
     * @param slots every slot the drag touched, in ascending order
     * @param cursor what the player was holding, or null when the server did not say
     * @param dragEvent the bukkit drag event to cancel, or null for a receptacle that cannot be cancelled
     */
    internal abstract fun dragged(
        clickType: ClickType,
        slots: List<Int>,
        cursor: ItemStack?,
        dragEvent: InventoryDragEvent?
    )

    /**
     * Handle the receptacle being closed (by player), triggering the appropriate events and actions.
     *
     */
    internal fun closed() {
        val player = viewer ?: return
        close(false)
        ReceptacleCloseEvent(player, this).callEvent()

//        async(delay = 1L) {
//            // From TrMenu: guards against a ghost container when the title changes too fast after
//            // the menu is closed. Maybe not necessary here?
//            val receptacle = viewer!!.viewingReceptacle
//            if (receptacle == null) {
//                viewer!!.updateInventory()
//            }
//        }
//        async(delay = 4L) {
//            val receptacle = viewer!!.viewingReceptacle
//            if (receptacle == this@ViewReceptacle) {
//                PacketHelper.sendContainerClose(viewer!!)
//            }
//        }
    }

    companion object {

        /**
         * A map of players currently viewing a receptacle to the receptacle they are viewing.
         */
        val viewingReceptacleMap = HashMap<UUID, ViewReceptacle>()

        var Player.viewingReceptacle
            get() = viewingReceptacleMap[this.uniqueId]
            set(value) {
                if (value == null) {
                    viewingReceptacleMap.remove(this.uniqueId)
                } else {
                    viewingReceptacleMap[this.uniqueId] = value
                }
            }

        fun Player.removeViewingReceptacle() {
            viewingReceptacleMap.remove(this.uniqueId)
        }

        /**
         * Create a new ViewReceptacle instance based on the specified mode.
         *
         * @param title the title of the receptacle
         * @param layout the layout of the receptacle
         * @param mode the mode of the receptacle (PHANTOM or STATIC)
         * @return a new ViewReceptacle instance
         */
        fun create(title: Component, layout: ViewLayout, mode: Mode): ViewReceptacle =
            when (mode) {
                Mode.PHANTOM -> PhantomReceptacle(title, layout)
                Mode.STATIC -> StaticReceptacle(title, layout)
            }
    }
}
