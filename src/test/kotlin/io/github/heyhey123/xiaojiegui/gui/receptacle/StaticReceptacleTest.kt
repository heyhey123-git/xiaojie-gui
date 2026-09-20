package io.github.heyhey123.xiaojiegui.gui.receptacle

import io.github.heyhey123.xiaojiegui.gui.StaticInventory.staticInventory
import io.github.heyhey123.xiaojiegui.listener.PlayerQuitListener
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.event.player.PlayerQuitEvent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import org.mockbukkit.mockbukkit.entity.PlayerMock
import org.mockbukkit.mockbukkit.inventory.ItemStackMock
import kotlin.test.Test
import kotlin.test.assertEquals

class StaticReceptacleTest {

    private lateinit var server: ServerMock
    private lateinit var player: PlayerMock

    @BeforeEach
    fun setUp() {
        server = MockBukkit.mock()
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
    fun `quitting with a static menu open drops its inventory`() {
        // The player is gone, so nothing closes the window through this addon: without the listener's own
        // cleanup the entry, and the items in it, would stay for as long as the server runs.
        val receptacle = StaticReceptacle(Component.text("Test Receptacle"), ViewLayout.Chest.GENERIC_9X3)
        receptacle.open(player)

        PlayerQuitListener.onQuit(PlayerQuitEvent(player, Component.text("bye")))

        assertEquals(
            null,
            player.staticInventory,
            "The static inventory should be dropped when the player quits."
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
