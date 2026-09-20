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

        PlayerQuitListener.onQuit(PlayerQuitEvent(player, Component.text("bye"), PlayerQuitEvent.QuitReason.DISCONNECTED))

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

    @Test
    fun `two players have a window each, not one shared chest`() {
        // `static` is a real inventory, so the question that follows is what it is shared with. It is shared
        // with nothing: a receptacle creates its own holder in its constructor, and one receptacle is one
        // window. Two players looking at the same menu therefore have two real inventories, which is also
        // why the guide says two players cannot share a real chest through this addon.
        val layout = ViewLayout.Chest.GENERIC_9X3
        val other = server.addPlayer()
        val mine = StaticReceptacle(Component.text("Shop"), layout)
        val theirs = StaticReceptacle(Component.text("Shop"), layout)
        mine.open(player)
        theirs.open(other)

        mine.setElement(0, ItemStackMock(Material.DIAMOND, 3))

        assertEquals(
            null,
            theirs.getElement(0),
            "What one window's player leaves behind must not appear in another player's window."
        )
    }

    @Test
    fun `closing a window clears it, so a later one starts from the page`() {
        val layout = ViewLayout.Chest.GENERIC_9X3
        val first = StaticReceptacle(Component.text("Shop"), layout)
        first.open(player)
        first.setElement(0, ItemStackMock(Material.DIAMOND, 3))

        first.close(true)

        assertEquals(
            null,
            first.getElement(0),
            "Closing has to clear the container, or its items would outlive the window that held them."
        )

        // A window opened later is a different receptacle with a different inventory, so nothing of the
        // previous player is there to inherit. That is what makes reusing a menu -- the same id, a second
        // player, a reload that rebuilds it -- safe, and what the backpack recipe in the cookbook exists for:
        // anything a script wants back has to be read out before the window closes.
        val second = StaticReceptacle(Component.text("Shop"), layout)
        assertEquals(
            null,
            second.getElement(0),
            "A later window of the same menu must start from the page's own contents."
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
