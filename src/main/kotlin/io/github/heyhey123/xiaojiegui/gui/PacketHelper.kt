package io.github.heyhey123.xiaojiegui.gui

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.wrapper.PacketWrapper
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerCloseWindow
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowItems
import io.github.heyhey123.xiaojiegui.gui.layout.LayoutType
import io.github.retrooper.packetevents.util.SpigotConversionUtil
import io.papermc.paper.adventure.PaperAdventure
import net.kyori.adventure.text.Component
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket
import org.bukkit.craftbukkit.entity.CraftPlayer
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

//    override fun getCurrentContainerId(player: Player): Int {
//        val serverPlayer = (player as CraftPlayer).handle
//        return Reflection.SERVER_PLAYER.getContainerCounter(serverPlayer)
//    }

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

    override fun sendContainerSetContent(player: Player, windowId: Int, items: Array<ItemStack?>) {
        val packet = WrapperPlayServerWindowItems(
            windowId,
            -1,
            items.map { item ->
                SpigotConversionUtil.fromBukkitItemStack(item)
            },
            null
        )
        player.sendPacket(packet)
    }

    override fun sendContainerSetSlot(player: Player, windowId: Int, slot: Int, item: ItemStack?) {
        val packet = WrapperPlayServerSetSlot(
            windowId,
            -1,
            slot,
            SpigotConversionUtil.fromBukkitItemStack(item)
        )
        player.sendPacket(packet)
    }

//    /**
//     * Proxy for reflection access to obfuscated Minecraft server code.
//     */
//    object Reflection {
//        val SERVER_PLAYER: ServerPlayerProxy = run {
//            val remapper = ReflectionRemapper.forReobfMappingsInPaperJar()
//            val proxyFactory = ReflectionProxyFactory.create(remapper, Reflection::class.java.classLoader);
//            return@run proxyFactory.reflectionProxy(ServerPlayerProxy::class.java)
//        }
//
//        @Proxies(ServerPlayer::class)
//        interface ServerPlayerProxy {
//
//            @FieldGetter("containerCounter")
//            fun getContainerCounter(instance: ServerPlayer): Int
//        }
//    }
}
