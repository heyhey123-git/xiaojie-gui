package io.github.heyhey123.xiaojiegui.gui.menu

import io.github.heyhey123.xiaojiegui.XiaojieGUI
import io.github.heyhey123.xiaojiegui.gui.event.MenuCloseEvent
import io.github.heyhey123.xiaojiegui.gui.menu.component.Page
import io.github.heyhey123.xiaojiegui.gui.receptacle.Receptacle
import io.github.heyhey123.xiaojiegui.gui.receptacle.StaticReceptacle
import io.github.heyhey123.xiaojiegui.listener.BukkitInventoryListener
import io.github.heyhey123.xiaojiegui.listener.PlayerQuitListener
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import net.kyori.adventure.text.Component.text
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * What a script can read inside `on menu close`, and therefore what it can save.
 *
 * A backpack that remembers is written as "read the window in `on menu close`, put it back on open" (the
 * cookbook's recipe), which only works while the window is still there when the event runs. Every path
 * that ends a window therefore has to fire the event *before* the container is dropped, or the recipe
 * silently saves nothing. This holds that order for the paths that end a window without the player
 * closing it -- quitting, dying, changing world -- because those are the ones where a mistake is
 * invisible: the close paths a player triggers themselves run through `ViewReceptacle.close`, whose
 * `onClose` callback fires the event before `shut` drops anything.
 */
class WindowCloseOrderTest {

    private lateinit var server: ServerMock
    private lateinit var player: Player

    @BeforeEach
    fun setUp() {
        server = MockBukkit.mock()
        player = server.addPlayer()
        MenuSession.clearSessions()
        // `Menu` reads the plugin for the registry it registers itself in, the way `MenuTest` sets it up.
        mockkObject(XiaojieGUI.Companion)
        val plugin = mockk<XiaojieGUI>(relaxed = true)
        XiaojieGUI.instance = plugin
        every { plugin.isEnabled } returns true
    }

    @AfterEach
    fun tearDown() {
        MenuSession.clearSessions()
        MockBukkit.unmock()
        unmockkAll()
    }

    /** A window opened for [player], holding one diamond, and what the close event saw when it ran. */
    private class OpenWindow(val session: MenuSession, val receptacle: StaticReceptacle) {
        var seenInEvent: ItemStack? = null
        var eventCount = 0
    }

    private fun openWindowWithADiamond(): OpenWindow {
        val properties = MenuProperties(
            text("Shop"),
            false,
            Receptacle.Mode.STATIC,
            50,
            1,
            listOf("AAAAAAAAA", "AAAAAAAAA", "AAAAAAAAA"),
            false
        )
        val menu = Menu("close-order", properties, InventoryType.CHEST)
        menu.pages.add(
            Page(InventoryType.CHEST, text("Shop"), properties.defaultLayout, emptyList(), properties)
        )
        menu.open(player)

        val session = MenuSession.querySession(player)!!
        session.setIcon(0, ItemStack(Material.DIAMOND, 3), refresh = false)

        val window = OpenWindow(session, session.receptacle as StaticReceptacle)
        server.pluginManager.registerEvents(
            object : Listener {
                @EventHandler
                fun onClose(event: MenuCloseEvent) {
                    window.eventCount++
                    window.seenInEvent = window.session.getIcon(0)
                }
            },
            MockBukkit.createMockPlugin()
        )
        return window
    }

    private fun assertTheEventSawTheWindow(window: OpenWindow, how: String) {
        assertEquals(1, window.eventCount, "$how must fire `on menu close` exactly once")
        assertEquals(
            Material.DIAMOND,
            window.seenInEvent?.type,
            "$how must fire the event while the container still holds what the player left in it"
        )
        assertNull(
            window.receptacle.getElement(0),
            "and the container must be gone once the event has run, so the items cannot outlive the window"
        )
    }

    @Test
    fun `closing the menu lets the event read the container`() {
        val window = openWindowWithADiamond()
        window.session.close()
        assertTheEventSawTheWindow(window, "closing the menu")
    }

    @Test
    fun `quitting lets the event read the container`() {
        val window = openWindowWithADiamond()
        PlayerQuitListener.onQuit(
            PlayerQuitEvent(player, text("bye"), PlayerQuitEvent.QuitReason.DISCONNECTED)
        )
        assertTheEventSawTheWindow(window, "quitting")
    }

    @Test
    fun `dying lets the event read the container`() {
        val window = openWindowWithADiamond()
        val death = mockk<PlayerDeathEvent>(relaxed = true)
        every { death.entity } returns player
        BukkitInventoryListener.onPlayerDeath(death)
        assertTheEventSawTheWindow(window, "dying")
    }

    @Test
    fun `leaving the world lets the event read the container`() {
        val window = openWindowWithADiamond()
        val change = mockk<PlayerChangedWorldEvent>(relaxed = true)
        every { change.player } returns player
        BukkitInventoryListener.onPlayerChangeWorld(change)
        assertTheEventSawTheWindow(window, "changing world")
    }
}
