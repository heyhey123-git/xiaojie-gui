package io.github.heyhey123.xiaojiegui.gui

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.wrapper.PacketWrapper
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerCloseWindow
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowItems
import io.github.heyhey123.xiaojiegui.gui.layout.LayoutType
import io.papermc.paper.adventure.PaperAdventure
import net.kyori.adventure.text.Component
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.entity.Player

private typealias PacketEventsItemStack = com.github.retrooper.packetevents.protocol.item.ItemStack

/**
 * Packet helper.
 *
 * The helper is used to send packets to players.
 *
 */
abstract class PacketHelper {

    companion object {
        /**
         * The singleton instance of PacketHelper.
         */
        val instance: PacketHelper by lazy { PacketHelperImpl() }
    }

    /**
     * Generate and return the next container ID for the player.
     *
     * @param player the player to generate the next container ID for
     * @return the next container ID
     */
    abstract fun generateNextContainerId(player: Player): Int

    //resource: container_close
    abstract fun sendContainerClose(player: Player)

    //resource: open_screen
    abstract fun sendOpenScreen(player: Player, windowId: Int, windowType: LayoutType, title: Component)

    // resource: container_set_content
    abstract fun sendContainerSetContent(player: Player, windowId: Int, items: Array<PacketEventsItemStack>)

    // resource: container_set_slot
    abstract fun sendContainerSetSlot(player: Player, windowId: Int, slot: Int, item: PacketEventsItemStack)

    // resource: container_set_data
//    fun sendContainerSetData(player: Player, windowId: Int, property: Int, value: Int) {
//        val packet = WrapperPlayServerWindowProperty(windowId, property, value)
//        player.sendPacket(packet)
//    }
}

/**
 * Packet helper implementation.
 *
 * The helper is used to send packets to players.
 * Why don't we just use object PacketHelper directly?
 * Because in testing we may want to mock the PacketHelper instance,
 * but mocking objects ALWAYS causes issues in Mockk(invoke original method unexpectedly).
 *
 */
private class PacketHelperImpl : PacketHelper() {

    override fun generateNextContainerId(player: Player): Int {
        val serverPlayer = (player as CraftPlayer).handle
        return serverPlayer.nextContainerCounter()
    }

    private fun <T : PacketWrapper<T>> Player.sendPacket(packet: PacketWrapper<T>) =
        PacketEvents.getAPI().playerManager.sendPacket(this, packet)

    override fun sendContainerClose(player: Player) {
        val packet = WrapperPlayServerCloseWindow(0)
        //by wiki: The vanilla client disregards the provided window ID and closes any active window.
        player.sendPacket(packet)
    }

    override fun sendOpenScreen(player: Player, windowId: Int, windowType: LayoutType, title: Component) {
        val serverPlayer = (player as CraftPlayer).handle
        serverPlayer.connection.send(
            ClientboundOpenScreenPacket(
                windowId,
                windowType.toNMSType(),
                PaperAdventure.asVanilla(title)
            )
        ) // PacketEvents causes issues when processing Components.
    }

    override fun sendContainerSetContent(player: Player, windowId: Int, items: Array<PacketEventsItemStack>) {
        val packet = WrapperPlayServerWindowItems(
            windowId,
            -1,
            items.toList(),
            null
        )
        player.sendPacket(packet)
    }

    override fun sendContainerSetSlot(player: Player, windowId: Int, slot: Int, item: PacketEventsItemStack) {
        val packet = WrapperPlayServerSetSlot(windowId, -1, slot, item)
        player.sendPacket(packet)
    }
}
