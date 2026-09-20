package io.github.heyhey123.xiaojiegui.gui.menu

import io.github.heyhey123.xiaojiegui.XiaojieGUI
import io.github.heyhey123.xiaojiegui.gui.event.MenuInteractEvent
import io.github.heyhey123.xiaojiegui.gui.event.MenuOpenEvent
import io.github.heyhey123.xiaojiegui.gui.event.PageTurnEvent
import io.github.heyhey123.xiaojiegui.gui.menu.component.IconProducer
import io.github.heyhey123.xiaojiegui.gui.menu.component.Page
import io.github.heyhey123.xiaojiegui.gui.receptacle.Receptacle
import io.github.heyhey123.xiaojiegui.gui.receptacle.ViewReceptacle
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import java.util.UUID
import kotlin.test.Test

class MenuTest {
    @AfterEach
    fun tearDown() {
        MenuSession.clearSessions()
        unmockkAll()
    }

    private lateinit var properties: MenuProperties

    /**
     * Helper function to create a chest page with given properties and title.
     */
    private fun chestPage(props: MenuProperties, title: String = "Title"): Page =
        Page(
            InventoryType.CHEST,
            Component.text(title),
            listOf("aaaaaaaaa"),
            emptyList(),
            props
        )

    @BeforeEach
    fun setUp() {
        val props = mockk<MenuProperties>(relaxed = true)
        every { props.mode } returns Receptacle.Mode.STATIC
        every { props.hidePlayerInventory } returns true
        every { props.defaultPage } returns 1
        every { props.defaultTitle } returns Component.text("Default")
        every { props.defaultLayout } returns listOf("aaaaaaaaa")
        mockkObject(XiaojieGUI.Companion)
        val plugin = mockk<XiaojieGUI>(relaxed = true)
        XiaojieGUI.instance = plugin
        every { plugin.isEnabled } returns true
        properties = props
    }

    @Test
    fun `open function - create receptacle & load in page & open view`() {
        val menu = Menu(null, properties, InventoryType.CHEST)

        val page = chestPage(properties)
        menu.pages.add(page)

        val receptacle = mockk<ViewReceptacle>(relaxed = true)
        mockkObject(ViewReceptacle)
        every { ViewReceptacle.create(any(), any(), any()) } returns receptacle
        every { receptacle.open(any()) } just Runs

        mockkConstructor(MenuOpenEvent::class)
        every { anyConstructed<MenuOpenEvent>().callEvent() } returns true

        val viewer = mockk<Player>(relaxed = true)
        val vid = UUID.randomUUID()
        every { viewer.uniqueId } returns vid

        // Act
        menu.open(viewer, 1)

        val session = MenuSession.getSession(viewer)
        assertSame(menu, session.menu)
        assertEquals(1, session.page)
        assertSame(receptacle, session.receptacle)
        assertTrue(menu.viewers.contains(vid))
        verify(exactly = 1) { receptacle.open(viewer) }
    }

    @Test
    fun `open function - action cancelled by event`() {
        val menu = Menu(null, properties, InventoryType.CHEST)

        val page = chestPage(properties)
        menu.pages.add(page)

        mockkConstructor(MenuOpenEvent::class)
        every { anyConstructed<MenuOpenEvent>().callEvent() } returns false

        val viewer = mockk<Player>(relaxed = true)
        val vid = UUID.randomUUID()
        every { viewer.uniqueId } returns vid

        // Act
        menu.open(viewer, 1)

        val session = MenuSession.getSession(viewer)
        assertSame(null, session.menu)
        assertEquals(-1, session.page)
        assertSame(null, session.receptacle)
        assertTrue(!menu.viewers.contains(vid))
    }

    @Test
    fun `turnPage function - turn to specific page & update title & refresh receptacle`() {
        val menu = Menu(null, properties, InventoryType.CHEST)

        val page1 = chestPage(properties, "Page 1")
        val page2 = chestPage(properties, "Page 2")
        menu.pages.addAll(listOf(page1, page2))

        val receptacle = mockk<ViewReceptacle>(relaxed = true)
        mockkObject(ViewReceptacle)
        every { ViewReceptacle.create(any(), any(), any()) } returns receptacle
        every { receptacle.title(any(), any()) } just Runs

        mockkConstructor(MenuOpenEvent::class)
        every { anyConstructed<MenuOpenEvent>().callEvent() } returns true

        val viewer = mockk<Player>(relaxed = true)
        val vid = UUID.randomUUID()
        every { viewer.uniqueId } returns vid

        // Open initial page
        menu.open(viewer, 1)
        val session = MenuSession.getSession(viewer)

        mockkConstructor(PageTurnEvent::class)
        every { anyConstructed<PageTurnEvent>().callEvent() } returns true

        // Act: turn to page 2
        menu.turnPage(viewer, 2)

        assertEquals(2, session.page)
        verify(exactly = 1) { receptacle.title(eq(Component.text("Page 2")), true) }
    }

    @Test
    fun `updateIconForKey - update items & set callbacks`() {
        val menu = Menu(null, properties, InventoryType.CHEST)
        val page = chestPage(properties)
        menu.pages.add(page)

        val icon = mockk<ItemStack>(relaxed = true)

        val v1 = mockk<Player>(relaxed = true)
        val v2 = mockk<Player>(relaxed = true)
        val id1 = UUID.randomUUID()
        val id2 = UUID.randomUUID()
        every { v1.uniqueId } returns id1
        every { v2.uniqueId } returns id2

        val r1 = mockk<ViewReceptacle>(relaxed = true)
        val r2 = mockk<ViewReceptacle>(relaxed = true)
        every { r1.setElement(any(), any()) } just Runs
        every { r2.setElement(any(), any()) } just Runs
        every { r1.refresh(any()) } just Runs
        every { r2.refresh(any()) } just Runs

        val s1 = MenuSession.getSession(v1).apply {
            this.menu = menu
            this.page = 1
            this.receptacle = r1
        }
        val s2 = MenuSession.getSession(v2).apply {
            this.menu = menu
            this.page = 1
            this.receptacle = r2
        }
        // register the viewers so updateIconForKey iterates over them
        menu.viewers.addAll(listOf(id1, id2))

        // provide the callback
        val cb: (MenuInteractEvent) -> Unit = {}

        menu.updateIconForKey(
            "a",
            IconProducer.SingleIconProducer(icon),
            refresh = true,
            callback = cb
        )

        // nine 'a' in one row, slots 0..8 should all be updated
        for (slot in 0..8) {
            verify(atLeast = 1) { r1.setElement(eq(slot), eq(icon)) }
            verify(atLeast = 1) { r2.setElement(eq(slot), eq(icon)) }
        }
        // multiple slots -> refresh(-1)
        verify(atLeast = 1) { r1.refresh(-1) }
        verify(atLeast = 1) { r2.refresh(-1) }

        // every slot has a callback configured
        assertTrue((0..8).all { it in page.clickCallbacks.keys })
    }

    @Test
    fun `overrideSlot - override slots & set callbacks`() {
        val menu = Menu(null, properties, InventoryType.CHEST)
        val page = chestPage(properties)
        menu.pages.add(page)

        val viewer = mockk<Player>(relaxed = true)
        val id = UUID.randomUUID()
        every { viewer.uniqueId } returns id
        val receptacle = mockk<ViewReceptacle>(relaxed = true)
        every { receptacle.setElement(any(), any()) } just Runs
        every { receptacle.refresh(any()) } just Runs

        MenuSession.getSession(viewer).apply {
            this.menu = menu
            this.page = 1
            this.receptacle = receptacle
        }
        menu.viewers.add(id)

        val item = mockk<ItemStack>(relaxed = true)
        val slot = 3
        val cb: (MenuInteractEvent) -> Unit = {}

        menu.overrideSlot(1, slot, item, refresh = true, callback = cb)

        verify(exactly = 1) { receptacle.setElement(slot, item) }
        verify(exactly = 1) { receptacle.refresh(slot) }
        assertSame(item, page.slotOverrides[slot])
        assertTrue(page.clickCallbacks.containsKey(slot))
    }

    @Test
    fun `open function - opening the menu again goes to that page instead of failing`() {
        val menu = Menu(null, properties, InventoryType.CHEST)
        menu.pages.addAll(listOf(chestPage(properties, "Page 1"), chestPage(properties, "Page 2")))

        val receptacle = mockk<ViewReceptacle>(relaxed = true)
        mockkObject(ViewReceptacle)
        every { ViewReceptacle.create(any(), any(), any()) } returns receptacle
        every { receptacle.open(any()) } just Runs
        every { receptacle.title(any(), any()) } just Runs

        mockkConstructor(MenuOpenEvent::class)
        every { anyConstructed<MenuOpenEvent>().callEvent() } returns true
        mockkConstructor(PageTurnEvent::class)
        every { anyConstructed<PageTurnEvent>().callEvent() } returns true

        val viewer = mockk<Player>(relaxed = true)
        every { viewer.uniqueId } returns UUID.randomUUID()

        menu.open(viewer, 1)
        // Act: the same menu again, at another page. This used to throw "already viewing this menu".
        menu.open(viewer, 2)

        assertEquals(2, MenuSession.getSession(viewer).page)
        // The window is not opened a second time: the player is already in it.
        verify(exactly = 1) { receptacle.open(viewer) }
    }

    @Test
    fun `sessionsOn - only the sessions looking at that page`() {
        val menu = Menu(null, properties, InventoryType.CHEST)
        val otherMenu = Menu(null, properties, InventoryType.CHEST)

        val onPageOne = mockk<Player>(relaxed = true)
        val onPageTwo = mockk<Player>(relaxed = true)
        val onAnotherMenu = mockk<Player>(relaxed = true)
        val id1 = UUID.randomUUID()
        val id2 = UUID.randomUUID()
        val id3 = UUID.randomUUID()
        every { onPageOne.uniqueId } returns id1
        every { onPageTwo.uniqueId } returns id2
        every { onAnotherMenu.uniqueId } returns id3

        val expected = MenuSession.getSession(onPageOne).apply {
            this.menu = menu
            this.page = 1
        }
        MenuSession.getSession(onPageTwo).apply {
            this.menu = menu
            this.page = 2
        }
        MenuSession.getSession(onAnotherMenu).apply {
            this.menu = otherMenu
            this.page = 1
        }
        menu.viewers.addAll(listOf(id1, id2, id3))

        assertEquals(listOf(expected), menu.sessionsOn(1))
    }

    @Test
    fun `destroyAll - every menu goes with the scripts that built it`() {
        val first = Menu(null, properties, InventoryType.CHEST)
        val second = Menu(null, properties, InventoryType.CHEST)
        first.pages.add(chestPage(properties))
        second.pages.add(chestPage(properties))

        Menu.destroyAll()

        assertTrue(first.isDestroyed)
        assertTrue(second.isDestroyed)
    }

    @Test
    fun `insertPage - insert the specific page properly`() {
        val menu = Menu(null, properties, InventoryType.CHEST)

        assertEquals(0, menu.pages.size)
        menu.insertPage(
            page = null,
            layoutPattern = listOf("aaaaaaaaa"),
            title = Component.text("X"),
            playerLayoutPattern = emptyList()
        )

        assertEquals(1, menu.pages.size)
    }
}
