package io.github.heyhey123.xiaojiegui.gui

import io.github.heyhey123.xiaojiegui.gui.layout.LayoutType
import io.papermc.paper.adventure.PaperAdventure
import net.kyori.adventure.text.Component
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.common.ClientCommonPacketListener
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket
import net.minecraft.network.protocol.game.ClientboundSetCursorItemPacket
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

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

//    /**
//     * Get the current container counter value for the player.
//     *
//     * @param player the player to get the container counter for
//     * @return the current container counter value
//     */
//    fun getCurrentContainerId(player: Player): Int

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
    abstract fun sendContainerSetContent(player: Player, windowId: Int, items: Array<ItemStack?>)

    // resource: container_set_slot
    abstract fun sendContainerSetSlot(player: Player, windowId: Int, slot: Int, item: ItemStack?)
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

    private fun <T : ClientCommonPacketListener> Player.sendPacket(packet: Packet<T>) {
        val serverPlayer = (this as CraftPlayer).handle
        serverPlayer.connection.send(packet) // The ItemStacks conversion in PacketEvents causes performance problem.
    }

    private fun  ItemStack?.asNMSCopy(): net.minecraft.world.item.ItemStack =
        CraftItemStack.asNMSCopy(this)

    override fun sendContainerClose(player: Player) {
        val packet = ClientboundContainerClosePacket(0)
        //by wiki: The vanilla client disregards the provided window ID and closes any active window.
        player.sendPacket(packet)
    }

    override fun sendOpenScreen(player: Player, windowId: Int, windowType: LayoutType, title: Component) {
        val packet = ClientboundOpenScreenPacket(
            windowId,
            windowType.toNMSType(),
            PaperAdventure.asVanilla(title)
        ) // PacketEvents causes issues when processing Components.
        player.sendPacket(packet)
    }

    override fun sendContainerSetContent(player: Player, windowId: Int, items: Array<ItemStack?>) {
        val packet = ClientboundContainerSetContentPacket(
            windowId,
            -1,
            items.map { item -> item.asNMSCopy() },
            ItemStack.empty().asNMSCopy()
        )
        player.sendPacket(packet)
    }

    override fun sendContainerSetSlot(player: Player, windowId: Int, slot: Int, item: ItemStack?) {
        val setSlotPacket = ClientboundContainerSetSlotPacket(
            windowId,
            -1,
            slot,
            item.asNMSCopy()
        )
        player.sendPacket(setSlotPacket)

        val setCursorPacket = ClientboundSetCursorItemPacket(ItemStack.empty().asNMSCopy())
        player.sendPacket(setCursorPacket)
    }
}
