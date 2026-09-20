package io.github.heyhey123.xiaojiegui.gui.menu.page

import io.github.heyhey123.xiaojiegui.gui.event.MenuInteractEvent
import io.github.heyhey123.xiaojiegui.gui.event.ReceptacleInteractEvent
import io.github.heyhey123.xiaojiegui.gui.interact.ClickType
import io.github.heyhey123.xiaojiegui.gui.menu.Menu
import io.github.heyhey123.xiaojiegui.gui.menu.MenuProperties
import io.github.heyhey123.xiaojiegui.gui.menu.MenuSession
import io.github.heyhey123.xiaojiegui.gui.menu.component.IconProducer
import io.github.heyhey123.xiaojiegui.gui.menu.component.Page
import io.github.heyhey123.xiaojiegui.gui.receptacle.Receptacle
import io.github.heyhey123.xiaojiegui.gui.receptacle.ViewReceptacle
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.AfterEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PageAuditRegressionTest {
    @AfterEach
    fun tearDown() = unmockkAll()

    private class Fixture(val mode: Receptacle.Mode = Receptacle.Mode.STATIC, hidden: Boolean = false) {
        val properties = mockk<MenuProperties>(relaxed = true)
        val menu = mockk<Menu>(relaxed = true)
        val receptacle = mockk<ViewReceptacle>(relaxed = true)
        val player = mockk<Player>(relaxed = true)
        val session = MenuSession(player, menu, 1, receptacle)
        val contents = mutableMapOf<Int, ItemStack?>()
        val handler = slot<(ReceptacleInteractEvent) -> Unit>()
        val page: Page

        init {
            every { properties.mode } returns mode
            every { properties.hidePlayerInventory } returns hidden
            every { properties.lockedIcons } returns true
            every { menu.properties } returns properties
            every { menu.cooldownManager.tryConsumeCooldown(any()) } returns true
            every { receptacle.hidePlayerInventory } returns hidden
            every { receptacle.onClick(capture(handler)) } just Runs
            every { receptacle.setElement(any(), any()) } answers { contents[firstArg()] = secondArg() }
            every { receptacle.getElement(any()) } answers { contents[firstArg()] }
            mockkConstructor(MenuInteractEvent::class)
            every { anyConstructed<MenuInteractEvent>().callEvent() } returns true
            page = Page(InventoryType.CHEST, Component.text("Audit"), listOf("a        "), emptyList(), properties)
        }

        fun interact(vararg slots: Int): ReceptacleInteractEvent =
            ReceptacleInteractEvent(player, receptacle, ClickType.LEFT, slots.first(), slots.toList()).also {
                handler.captured(it)
            }
    }

    @Test
    fun `loading a page preserves free static slots and clears exhausted owned list slots`() {
        val f = Fixture()
        val item = mockk<ItemStack>(relaxed = true)
        f.contents[0] = item
        f.contents[5] = item
        f.page.iconMapper["a"] = IconProducer.MultipleIconProducer(emptyList()) to null
        f.page.loadInPage(f.session)
        assertSame(item, f.contents[5])
        assertNull(f.contents[0])
        verify(exactly = 0) { f.receptacle.setElement(5, any()) }
    }

    @Test
    fun `click callback emits exactly one global event`() {
        val f = Fixture()
        var callbacks = 0
        f.page.clickCallbacks[0] = { callbacks++ }
        f.page.loadInPage(f.session)
        f.interact(0)
        assertEquals(1, callbacks)
        verify(exactly = 1) { anyConstructed<MenuInteractEvent>().callEvent() }
    }

    @Test
    fun `drag callbacks keep slot contexts and cancellation but emit one global event`() {
        val f = Fixture()
        val slots = mutableListOf<Int>()
        f.page.clickCallbacks[0] = { slots += it.slot }
        f.page.clickCallbacks[1] = {
            slots += it.slot
            it.isCancelled = true
        }
        f.page.loadInPage(f.session)
        val event = f.interact(0, 1)
        assertEquals(listOf(0, 1), slots)
        assertTrue(event.isCancelled)
        verify(exactly = 1) { anyConstructed<MenuInteractEvent>().callEvent() }
    }

    @Test
    fun `cancelled callback stops later callbacks and still dispatches one global event`() {
        val f = Fixture()
        var later = false
        f.page.clickCallbacks[0] = { it.isCancelled = true }
        f.page.clickCallbacks[1] = { later = true }
        f.page.loadInPage(f.session)
        assertTrue(f.interact(0, 1).isCancelled)
        assertFalse(later)
        verify(exactly = 1) { anyConstructed<MenuInteractEvent>().callEvent() }
    }

    @Test
    fun `static player half and outside slots cannot invoke slot callbacks`() {
        val f = Fixture()
        var callbacks = 0
        for (slot in listOf(-999, 9, 44, 45)) f.page.clickCallbacks[slot] = { callbacks++ }
        f.page.loadInPage(f.session)
        f.interact(-999, 9, 44, 45)
        assertEquals(0, callbacks)
        verify(exactly = 1) { anyConstructed<MenuInteractEvent>().callEvent() }
    }

    @Test
    fun `live overrides gain and lose locked protection without reloading`() {
        val f = Fixture()
        f.page.loadInPage(f.session)
        val item = mockk<ItemStack>(relaxed = true)
        f.page.slotOverrides[5] = item
        assertTrue(f.interact(5).isCancelled)
        val air = mockk<ItemStack>()
        every { air.isEmpty } returns true
        f.page.slotOverrides[5] = air
        assertFalse(f.interact(5).isCancelled)
        f.page.iconMapper["a"] = IconProducer.SingleIconProducer(item) to null
        assertTrue(f.interact(0).isCancelled)
        every { f.properties.lockedIcons } returns false
        assertFalse(f.interact(0).isCancelled)
    }

    @Test
    fun `shown phantom lower half cannot invoke saved or direct callbacks`() {
        val f = Fixture(Receptacle.Mode.PHANTOM, hidden = true)
        var callbacks = 0
        f.page.slotOverrides[10] = mockk(relaxed = true)
        f.page.clickCallbacks[10] = { callbacks++ }
        f.page.clickCallbacks[11] = { callbacks++ }
        f.page.loadInPage(f.session)
        // Reopen with player items shown; saved menu callbacks must not become invisible buttons.
        every { f.properties.hidePlayerInventory } returns false
        every { f.receptacle.hidePlayerInventory } returns false
        f.page.loadInPage(f.session)
        f.interact(10, 11)
        assertEquals(0, callbacks)
        verify(exactly = 1) { anyConstructed<MenuInteractEvent>().callEvent() }
    }

    @Test
    fun `lower layouts use nine columns even below hopper and dispenser`() {
        for (type in listOf(InventoryType.HOPPER, InventoryType.DISPENSER)) {
            val f = Fixture(Receptacle.Mode.PHANTOM, hidden = true)
            val page = Page(type, Component.empty(), listOf("aaa"), List(4) { "bbbbbbbb`last`" }, f.properties)
            assertEquals(setOf(page.size + 8, page.size + 17, page.size + 26, page.size + 35), page.keyToSlots["last"]?.toSet())
            assertEquals(32, page.keyToSlots["b"]!!.size)
        }
    }
}
