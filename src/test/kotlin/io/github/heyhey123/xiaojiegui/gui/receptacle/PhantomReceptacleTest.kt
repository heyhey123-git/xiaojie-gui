package io.github.heyhey123.xiaojiegui.gui.receptacle

import io.github.heyhey123.xiaojiegui.gui.PacketHelper
import io.github.heyhey123.xiaojiegui.gui.utils.TaskUtil
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class PhantomReceptacleTest {

    @Test
    fun `a hidden player inventory leaves the slots past the container to the menu`() {
        // With the player's inventory hidden there is nothing to mirror into the lower half of a phantom
        // window, so those slots are the menu's own space: an icon a script puts there has to come back out
        // of it, and not be replaced by whatever the player happens to carry in that inventory slot.
        val receptacle = PhantomReceptacle(Component.text("Hidden"), ViewLayout.Chest.GENERIC_9X3).apply {
            hidePlayerInventory = true
        }
        val icon = mockk<ItemStack>(relaxed = true)

        receptacle.setElement(30, icon)

        assertEquals(icon, receptacle.getElement(30))
    }

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
                player,
                114,
                layout.type,
                title
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
                player,
                114,
                layout.type,
                title
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
    fun `an icon a page lays out below the container is cleared while the inventory is shown`() {
        // The rows below the container are the player's own while the inventory is shown, so what arrives
        // there has to be the player's items and nothing else: `contents` is what a whole-window refresh
        // sends, and an icon left in it would appear in every cell the player happens to leave empty.
        val player = mockk<Player>(relaxed = true)
        val inventory = mockk<PlayerInventory>(relaxed = true)
        val inventoryContents = arrayOfNulls<ItemStack?>(36)

        val title = Component.text("Phantom Receptacle Test")
        val layout = ViewLayout.Chest.GENERIC_9X3
        val receptacle = PhantomReceptacle(title, layout)

        val pageIcon = mockk<ItemStack>()
        receptacle.setElement(layout.containerSize, pageIcon) // the first cell below the container

        every { player.uniqueId } returns UUID.randomUUID()
        every { player.inventory } returns inventory
        every { inventory.contents } returns inventoryContents
        every { mockedPacketHelper.sendOpenScreen(player, 114, layout.type, any()) } just Runs
        every { mockedPacketHelper.sendContainerSetContent(player, 114, any()) } just Runs

        receptacle.open(player)

        // The player carries nothing, so the cell has to arrive empty rather than carrying the page's icon.
        verify {
            mockedPacketHelper.sendContainerSetContent(
                player,
                114,
                match<Array<ItemStack?>> { items -> items[layout.containerSize] == null }
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
                player,
                114,
                layout.type,
                any()
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
                player,
                114,
                layout.type,
                newTitle
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
