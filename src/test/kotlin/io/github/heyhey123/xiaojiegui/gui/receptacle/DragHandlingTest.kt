package io.github.heyhey123.xiaojiegui.gui.receptacle

import io.github.heyhey123.xiaojiegui.gui.PacketHelper
import io.github.heyhey123.xiaojiegui.gui.event.ReceptacleInteractEvent
import io.github.heyhey123.xiaojiegui.gui.interact.ClickType
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import net.kyori.adventure.text.Component
import org.bukkit.event.inventory.InventoryDragEvent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * A drag is a run of packets on one side and a single bukkit event on the other, and a script has to see
 * the same thing either way. These tests pin the two readings to each other: several slots become one
 * interaction with a slot list, one slot becomes an ordinary click (which is what the game itself does
 * with it), and a drag that reached nothing is not reported at all.
 */
class DragHandlingTest {

    private lateinit var server: ServerMock
    private lateinit var packetHelper: PacketHelper

    @BeforeEach
    fun setUp() {
        server = MockBukkit.mock()
        packetHelper = mockk<PacketHelper>()
        mockkObject(PacketHelper.Companion)
        every { PacketHelper.instance } returns packetHelper
        every { packetHelper.generateNextContainerId(any()) } returns 114
        every { packetHelper.sendOpenScreen(any(), any(), any(), any()) } just Runs
        every { packetHelper.sendContainerSetContent(any(), any(), any()) } just Runs
        every { packetHelper.sendContainerSetSlot(any(), any(), any(), any()) } just Runs
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(PacketHelper.Companion)
        MockBukkit.unmock()
    }

    @Test
    fun `a phantom drag over several slots is one interaction carrying all of them`() {
        val receptacle = openPhantom()
        val interactions = mutableListOf<ReceptacleInteractEvent>()
        receptacle.onClick { interactions += it }

        // A left drag over slots 4 and 5: start, one continue per slot, end.
        receptacle.dragPacket(4, 0)
        receptacle.dragPacket(4, 1)
        receptacle.dragPacket(5, 1)
        receptacle.dragPacket(4, 2)

        assertEquals(1, interactions.size, "a drag is one interaction, not one per packet")
        val interaction = interactions.single()
        assertEquals(listOf(4, 5), interaction.slots)
        assertEquals(4, interaction.slot, "the first touched slot")
        assertEquals(ClickType.LEFT, interaction.clickType)
        assertTrue(interaction.isDrag)
    }

    @Test
    fun `a phantom drag that touched one slot is reported as a click`() {
        val receptacle = openPhantom()
        val interactions = mutableListOf<ReceptacleInteractEvent>()
        receptacle.onClick { interactions += it }

        // One item can only fill one slot, so the game turns this drag into a plain click.
        receptacle.dragPacket(4, 0)
        receptacle.dragPacket(4, 1)
        receptacle.dragPacket(4, 2)

        assertEquals(1, interactions.size)
        val interaction = interactions.single()
        assertEquals(listOf(4), interaction.slots)
        assertFalse(interaction.isDrag, "a one-slot drag is a click, and a script should not have to know")
    }

    @Test
    fun `a phantom drag over nothing is not reported`() {
        val receptacle = openPhantom()
        val interactions = mutableListOf<ReceptacleInteractEvent>()
        receptacle.onClick { interactions += it }

        // A start followed straight by an end collects no slot: the start's own slot is ignored.
        receptacle.dragPacket(7, 0)
        receptacle.dragPacket(7, 2)

        assertEquals(emptyList(), interactions)
    }

    @Test
    fun `the three buttons of a drag survive as the button the drag was made with`() {
        for ((button, expected) in listOf(0 to ClickType.LEFT, 4 to ClickType.RIGHT, 8 to ClickType.MIDDLE)) {
            val receptacle = openPhantom()
            val interactions = mutableListOf<ReceptacleInteractEvent>()
            receptacle.onClick { interactions += it }

            receptacle.dragPacket(4, button)
            receptacle.dragPacket(4, button + 1)
            receptacle.dragPacket(5, button + 1)
            receptacle.dragPacket(4, button + 2)

            assertEquals(expected, interactions.single().clickType, "drag started with button $button")
        }
    }

    @Test
    fun `a cancelled static drag cancels the bukkit event`() {
        val receptacle = StaticReceptacle(Component.text("Drag"), ViewLayout.Chest.GENERIC_9X3)
        receptacle.open(server.addPlayer())
        receptacle.onClick { it.isCancelled = true }

        val dragEvent = mockk<InventoryDragEvent>(relaxed = true)
        receptacle.dragged(ClickType.LEFT, listOf(4, 5), null, dragEvent)

        verify { dragEvent.isCancelled = true }
    }

    private fun openPhantom(): PhantomReceptacle {
        val receptacle = PhantomReceptacle(Component.text("Drag"), ViewLayout.Chest.GENERIC_9X3).apply {
            hidePlayerInventory = true
        }
        receptacle.open(server.addPlayer())
        return receptacle
    }
}
