package io.github.heyhey123.xiaojiegui.gui.receptacle

import io.github.heyhey123.xiaojiegui.gui.PacketHelper
import io.github.heyhey123.xiaojiegui.gui.utils.TaskUtil
import io.mockk.*
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.util.*
import kotlin.test.Test

class PhantomReceptacleTest {

    private lateinit var mockedPacketHelper: PacketHelper

    @BeforeEach
    fun setUp() {
        mockedPacketHelper = mockk<PacketHelper>()
        mockkObject(PacketHelper.Companion)
        every { PacketHelper.instance } returns mockedPacketHelper
        every { mockedPacketHelper.generateNextContainerId(any()) } returns 114
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(PacketHelper.Companion)
    }

    @Test
    fun `open receptacle and send init packets to player`() {
        val player = mockk<Player>(relaxed = true)
        every { player.uniqueId } returns UUID.randomUUID()

        val title = Component.text("Phantom Receptacle Test")
        val layout = ViewLayout.Chest.GENERIC_9X3

        every {
            mockedPacketHelper.sendOpenScreen(
                player, 114, layout.type, title
            )
        } just Runs
        every { mockedPacketHelper.sendContainerSetContent(player, 114, any()) } just Runs

        val receptacle = PhantomReceptacle(title, layout).apply {
            hidePlayerInventory = true // jump over player inventory setup
        }
        receptacle.open(player)

        verify(exactly = 1) { mockedPacketHelper.generateNextContainerId(player) }
        verify(exactly = 1) {
            mockedPacketHelper.sendOpenScreen(
                player, 114, layout.type, title
            )
        }
        verify(exactly = 1) { mockedPacketHelper.sendContainerSetContent(player, 114, any()) }
    }

    @Test
    fun `set up player inventory slots and open`() {
        val player = mockk<Player>(relaxed = true)
        val inventory = mockk<PlayerInventory>(relaxed = true)
        val inventoryContents = arrayOfNulls<ItemStack?>(36)

        val apple = mockk<ItemStack>()
        val bread = mockk<ItemStack>()
        inventoryContents[0] = apple // hotbar slot: 0, slot index: 30
        inventoryContents[17] = bread // main inv slot:17, slot index: 11

        val title = Component.text("Phantom Receptacle Test")
        val layout = ViewLayout.FixedContainer.ANVIL
        val receptacle = PhantomReceptacle(title, layout).apply {
            hidePlayerInventory = false
        }

        every { player.uniqueId } returns UUID.randomUUID()
        every { player.inventory } returns inventory
        every { inventory.contents } returns inventoryContents

        every { mockedPacketHelper.sendOpenScreen(player, 114, layout.type, any()) } just Runs
        every { mockedPacketHelper.sendContainerSetContent(player, 114, any()) } just Runs

        receptacle.open(player)

        // Verify that the player inventory slots are set up correctly
        verify {
            mockedPacketHelper.sendContainerSetContent(
                player,
                114,
                match<Array<ItemStack?>> { items -> items[30] == apple && items[11] == bread }
            )
        }
    }

    @Test
    fun `retitle receptacle and send update to player`() {
        val player = mockk<Player>(relaxed = true)
        every { player.uniqueId } returns UUID.randomUUID()

        val initialTitle = Component.text("Initial Title")
        val newTitle = Component.text("New Title")
        val layout = ViewLayout.Chest.GENERIC_9X3

        every {
            mockedPacketHelper.sendOpenScreen(
                player, 114, layout.type, any()
            )
        } just Runs
        every { mockedPacketHelper.sendContainerSetContent(player, 114, any()) } just Runs

        val receptacle = PhantomReceptacle(initialTitle, layout).apply {
            hidePlayerInventory = true // jump over player inventory setup
        }
        receptacle.open(player)

        mockkObject(TaskUtil)
        every { TaskUtil.sync(period = 0L, now = false, delay = 3L, any()) } answers {
            receptacle.initializationPackets()
            mockk()
        }

        // Retitle the receptacle
        receptacle.title(newTitle, render = true)

        // Verify that the retitle packet was sent
        verify(exactly = 1) {
            mockedPacketHelper.sendOpenScreen(
                player, 114, layout.type, newTitle
            )
        }

        unmockkObject(TaskUtil)
    }

    @Test
    fun `send close packet on close with render true`() {
        val player = mockk<Player>(relaxed = true)
        every { player.uniqueId } returns UUID.randomUUID()

        val title = Component.text("Phantom Receptacle Test")
        val layout = ViewLayout.Chest.GENERIC_9X3

        every { mockedPacketHelper.sendOpenScreen(player, 114, layout.type, any()) } just Runs
        every { mockedPacketHelper.sendContainerSetContent(player, 114, any()) } just Runs
        every { mockedPacketHelper.sendContainerClose(player) } just Runs

        val receptacle = PhantomReceptacle(title, layout).apply {
            hidePlayerInventory = true // jump over player inventory setup
        }
        receptacle.open(player)

        // Close the receptacle with render = true
        receptacle.close(render = true)

        // Verify that the close packet was sent
        verify(exactly = 1) { mockedPacketHelper.sendContainerClose(player) }
    }

}
