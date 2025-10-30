package io.github.heyhey123.xiaojiegui.gui.receptacle

import io.github.heyhey123.xiaojiegui.gui.StaticInventory.staticInventory
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Material
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.InventoryHolder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import org.mockbukkit.mockbukkit.entity.PlayerMock
import org.mockbukkit.mockbukkit.inventory.InventoryMock
import org.mockbukkit.mockbukkit.inventory.ItemStackMock
import kotlin.test.Test
import kotlin.test.assertEquals

@Suppress("DEPRECATION")
private class MockServerExtension : ServerMock() {
    override fun createInventory(
        owner: InventoryHolder?,
        type: InventoryType,
        title: Component
    ): InventoryMock = this.createInventory(
        owner,
        type,
        LegacyComponentSerializer.legacySection().serialize(title)
    )

    override fun createInventory(
        owner: InventoryHolder?,
        size: Int,
        title: Component
    ): InventoryMock = this.createInventory(
        owner,
        size,
        LegacyComponentSerializer.legacySection().serialize(title)
    )
}

class StaticReceptacleTest {

    private lateinit var server: ServerMock
    private lateinit var player: PlayerMock

    @BeforeEach
    fun setUp() {
        server = MockBukkit.mock(MockServerExtension())
        player = server.addPlayer()
    }

    @AfterEach
    fun tearDown() {
        MockBukkit.unmock()
    }

    @Test
    fun `open receptacle`() {
        val title = Component.text("Test Receptacle")
        val layout = ViewLayout.Chest.GENERIC_9X4
        val receptacle = StaticReceptacle(title, layout)
        receptacle.open(player)

        val apple = ItemStackMock(Material.GOLDEN_APPLE, 60)
        val diamond = ItemStackMock(Material.DIAMOND)
        receptacle.setElement(0, apple)
        receptacle.setElement(27, diamond)

        assertEquals(
            player.openInventory.topInventory.holder,
            player.staticInventory?.holder,
            "The static inventory holder should match the opened inventory holder."
        )

        assertEquals(
            apple,
            receptacle.getElement(0),
            "The item in slot 0 should be the given golden apple."
        )

        assertEquals(
            diamond,
            receptacle.getElement(27),
            "The item in slot 27 should be the given diamond."
        )
    }

    @Test
    fun `close receptacle`() {
        val title = Component.text("Test Receptacle")
        val layout = ViewLayout.Chest.GENERIC_9X4
        val receptacle = StaticReceptacle(title, layout)
        receptacle.open(player)

        receptacle.close(true)

        assertEquals(
            null,
            player.staticInventory,
            "The static inventory should be null after closing the receptacle."
        )
    }

//    @Test
//    fun `set title for the opened receptacle`() {
//        val title = Component.text("Test Receptacle")
//        val layout = ViewLayout.Chest.GENERIC_9X4
//        val receptacle = StaticReceptacle(title, layout)
//        receptacle.open(player)
//
//        val newTitle = Component.text("New Title")
//        receptacle.title(newTitle, true)
//
//        @Suppress("DEPRECATION")
//        assertEquals(
//            "New Title",
//            player.openInventory.title,
//            "The inventory title should be updated to the new title."
//        )
//    }
}
