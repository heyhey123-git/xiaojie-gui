package io.github.heyhey123.xiaojiegui.gui.receptacle

import io.github.heyhey123.xiaojiegui.gui.event.ReceptacleCloseEvent
import io.github.heyhey123.xiaojiegui.gui.event.ReceptacleInteractEvent
import io.github.heyhey123.xiaojiegui.gui.interact.ClickType
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import java.util.*

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
        // if render is true, the receptacle is being closed by the plugin
        // if render is false, the receptacle is being closed by the player
        viewer ?: return
        onClose(viewer!!, this)
        viewer!!.removeViewingReceptacle()
        if (render) {
            doClose()
        }
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
     * Handle the receptacle being closed (by player), triggering the appropriate events and actions.
     *
     */
    internal fun closed() {
        close(false)
        ReceptacleCloseEvent(viewer!!, this).callEvent()

//        async(delay = 1L) {
//            // from trm:
//            // 防止关闭菜单后, 动态标题频率过快出现的卡假容器
//            // maybe not necessary here?
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
