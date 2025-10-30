package io.github.heyhey123.xiaojiegui.gui.menu

import io.github.heyhey123.xiaojiegui.XiaojieGUI
import io.github.heyhey123.xiaojiegui.gui.event.MenuInteractEvent
import io.github.heyhey123.xiaojiegui.gui.event.MenuOpenEvent
import io.github.heyhey123.xiaojiegui.gui.event.PageTurnEvent
import io.github.heyhey123.xiaojiegui.gui.menu.component.Page
import io.github.heyhey123.xiaojiegui.gui.receptacle.Receptacle
import io.github.heyhey123.xiaojiegui.gui.receptacle.ViewReceptacle
import io.mockk.*
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import java.util.*
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
        every { props.defaultPage } returns 0
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
        menu.open(viewer, 0)

        val session = MenuSession.getSession(viewer)
        assertSame(menu, session.menu)
        assertEquals(0, session.page)
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
        menu.open(viewer, 0)

        val session = MenuSession.getSession(viewer)
        assertSame(null, session.menu)
        assertEquals(-1, session.page)
        assertSame(null, session.receptacle)
        assertTrue(!menu.viewers.contains(vid))
    }

    @Test
    fun `turnPage function - turn to specific page & update title & refresh receptacle`() {
        val menu = Menu(null, properties, InventoryType.CHEST)

        val page0 = chestPage(properties, "Page 0")
        val page1 = chestPage(properties, "Page 1")
        menu.pages.addAll(listOf(page0, page1))

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
        menu.open(viewer, 0)
        val session = MenuSession.getSession(viewer)

        mockkConstructor(PageTurnEvent::class)
        every { anyConstructed<PageTurnEvent>().callEvent() } returns true

        // Act: turn to page 1
        menu.turnPage(viewer, 1)

        assertEquals(1, session.page)
        verify(exactly = 1) { receptacle.title(eq(Component.text("Page 1")), true) }
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
            this.page = 0
            this.receptacle = r1
        }
        val s2 = MenuSession.getSession(v2).apply {
            this.menu = menu;
            this.page = 0;
            this.receptacle = r2
        }
        // 注册观众以便被 updateIconForKey 遍历
        menu.viewers.addAll(listOf(id1, id2))

        // 提供回调
        val cb: (MenuInteractEvent) -> Unit = {}

        menu.updateIconForKey("a", icon, refresh = true, callback = cb)

        // 一行 9 个 'a'，槽位 0..8 均应更新
        for (slot in 0..8) {
            verify(atLeast = 1) { r1.setElement(eq(slot), eq(icon)) }
            verify(atLeast = 1) { r2.setElement(eq(slot), eq(icon)) }
        }
        // 多槽位 -> refresh(-1)
        verify(atLeast = 1) { r1.refresh(-1) }
        verify(atLeast = 1) { r2.refresh(-1) }

        // 所有槽位均配置了回调
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

        val session = MenuSession.getSession(viewer).apply {
            this.menu = menu; this.page = 0; this.receptacle = receptacle
        }
        menu.viewers.add(id)

        val item = mockk<ItemStack>(relaxed = true)
        val slot = 3
        val cb: (MenuInteractEvent) -> Unit = {}

        menu.overrideSlot(0, slot, item, refresh = true, callback = cb)

        verify(exactly = 1) { receptacle.setElement(slot, item) }
        verify(exactly = 1) { receptacle.refresh(slot) }
        assertSame(item, page.slotOverrides[slot])
        assertTrue(page.clickCallbacks.containsKey(slot))
    }

    @Test
    fun `insertPage - insert the specific page properly`() {
        val menu = Menu(null, properties, InventoryType.CHEST)

        assertEquals(0, menu.pages.size)
        menu.insertPage(
            page = null,
            layoutPattern = listOf("aaaaaaaaa"),
            title = Component.text("X"),
            playerInventoryPattern = emptyList()
        )

        assertEquals(1, menu.pages.size)
    }
}
