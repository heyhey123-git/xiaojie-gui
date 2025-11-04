package io.github.heyhey123.xiaojiegui.listener

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketListenerPriority
import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow
import io.github.heyhey123.xiaojiegui.gui.interact.ClickType
import io.github.heyhey123.xiaojiegui.gui.menu.MenuSession
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
                val clickType = ClickType.from(
                    mode = packet.windowClickType.ordinal,
                    button = packet.button,
                    slot = slot
                )
                    ?: throw IllegalArgumentException("Unknown click type: ${packet.windowClickType} with button ${packet.button} at slot $slot")
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
