package io.github.heyhey123.xiaojiegui.listener

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketListenerPriority
import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow.WindowClickType
import io.github.heyhey123.xiaojiegui.gui.interact.ClickType
import io.github.heyhey123.xiaojiegui.gui.menu.MenuSession
import io.github.heyhey123.xiaojiegui.gui.receptacle.PhantomReceptacle
import io.github.heyhey123.xiaojiegui.gui.receptacle.Receptacle
import io.github.heyhey123.xiaojiegui.gui.utils.TaskUtil

object ReceptaclePacketListener :
    PacketListenerAbstract(PacketListenerPriority.NORMAL), BaseListener {

    override fun register() {
        PacketEvents.getAPI().eventManager.registerListener(this)
    }

    override fun unregister() {
        PacketEvents.getAPI().eventManager.unregisterListener(this)
    }

    override fun onPacketReceive(event: PacketReceiveEvent) {
        val playerId = event.user.uuid ?: return
        val receptacle = MenuSession.querySession(playerId)?.receptacle ?: return
        if (receptacle.mode != Receptacle.Mode.PHANTOM) return

        when (event.packetType) {
            PacketType.Play.Client.CLICK_WINDOW -> {
                val packet = WrapperPlayClientClickWindow(event)
                val slot = packet.slot
                if (packet.windowClickType == WindowClickType.QUICK_CRAFT) {
                    // A drag arrives as a run of these packets, with the phase packed into the button, and
                    // none of them means anything on its own: the window is ours, so the run is collected
                    // here and reported as one interaction when it ends. See `QuickCraft`.
                    val phantom = receptacle as? PhantomReceptacle ?: return
                    TaskUtil.sync { phantom.dragPacket(slot, packet.button) }
                    event.isCancelled = true
                    return
                }
                // A client can send a click this addon has no name for. It is dropped rather than thrown:
                // the window it belongs to is the server's own, so ignoring it is exactly what an
                // unimplemented click should do, and a packet listener that throws fills the log with
                // stack traces for input that is not a bug in the plugin.
                val clickType = ClickType.from(
                    mode = packet.windowClickType.ordinal,
                    button = packet.button,
                    slot = slot
                ) ?: return
                TaskUtil.sync {
                    receptacle.clicked(clickType, slot, null)
                }
                event.isCancelled = true
            }

            PacketType.Play.Client.CLOSE_WINDOW -> {
                TaskUtil.sync { receptacle.closed() }
                event.isCancelled = true
            }

            else -> return
        }
    }
}
