package io.github.heyhey123.xiaojiegui.gui.event

import io.github.heyhey123.xiaojiegui.gui.interact.ClickType
import io.github.heyhey123.xiaojiegui.gui.receptacle.Receptacle
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import org.bukkit.inventory.ItemStack

/**
 * A player interacted with a receptacle.
 *
 * One interaction is one event, whether the player clicked a slot or dragged across several. A drag that
 * only touched one slot arrives as an ordinary click: that is not a simplification here, it is what the
 * server does with it, so a script never has to tell the two apart unless it wants to.
 *
 * @property player the player who interacted
 * @property receptacle the receptacle that was interacted with
 * @property clickType the click that was made: for a drag, the button it was made with
 * @property slot the first slot of [slots]
 * @property slots every slot this interaction touches, in ascending order; more than one only for a drag
 * @property cursor what the player was holding, or null when the server did not say (phantom mode)
 */
class ReceptacleInteractEvent(
    val player: Player,
    val receptacle: Receptacle,
    val clickType: ClickType,
    val slot: Int,
    val slots: List<Int> = listOf(slot),
    val cursor: ItemStack? = null
) : Event(), Cancellable {

    companion object {
        private val HANDLERS = HandlerList()

        @JvmStatic
        fun getHandlerList() = HANDLERS
    }

    /** Whether this interaction is a drag, that is, one that touches more than one slot. */
    val isDrag: Boolean
        get() = slots.size > 1

    private var cancelled = false

    override fun getHandlers() = HANDLERS

    override fun isCancelled(): Boolean = cancelled

    override fun setCancelled(cancel: Boolean) {
        this.cancelled = cancel
    }
}
