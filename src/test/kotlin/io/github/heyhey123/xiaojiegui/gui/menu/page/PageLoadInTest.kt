package io.github.heyhey123.xiaojiegui.gui.menu.page

import io.github.heyhey123.xiaojiegui.gui.event.MenuInteractEvent
import io.github.heyhey123.xiaojiegui.gui.event.ReceptacleInteractEvent
import io.github.heyhey123.xiaojiegui.gui.interact.ClickType
import io.github.heyhey123.xiaojiegui.gui.menu.Menu
import io.github.heyhey123.xiaojiegui.gui.menu.MenuProperties
import io.github.heyhey123.xiaojiegui.gui.menu.MenuSession
import io.github.heyhey123.xiaojiegui.gui.menu.component.Cooldown
import io.github.heyhey123.xiaojiegui.gui.menu.component.Page
import io.github.heyhey123.xiaojiegui.gui.receptacle.Receptacle
import io.github.heyhey123.xiaojiegui.gui.receptacle.ViewReceptacle
import io.mockk.*
import net.kyori.adventure.text.Component
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PageLoadInTest {

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `loadInPage sets title, hides player inventory, registers handlers, fills and overrides slots, and invokes click callback`() {
        // Arrange menu properties
        val properties = mockk<MenuProperties>(relaxed = true)
        every { properties.mode } returns Receptacle.Mode.STATIC
        every { properties.hidePlayerInventory } returns true

        // Page with 1 row chest layout, 9 columns all 'a'
        val page = Page(
            InventoryType.CHEST,
            Component.text("Title"),
            listOf("aaaaaaaaa"),
            emptyList(),
            properties
        )

        // Mock Menu and its behaviors
        val menu = mockk<Menu>(relaxed = true)
        every { menu.properties } returns properties
        every { menu.pages } returns mutableListOf(page)

        // Icon item & clone behavior
        val icon = mockk<ItemStack>(relaxed = true)
        every { icon.clone() } returns icon
        every { menu.translateIcon("a") } returns icon

        // Mock Receptacle and capture onClick callback
        val receptacle = mockk<ViewReceptacle>(relaxed = true)
        val onClickSlot = slot<(ReceptacleInteractEvent) -> Unit>()
        every { receptacle.title(any(), any()) } just Runs
        every { receptacle.setElement(any(), any()) } just Runs
        every { receptacle.onClose(any()) } just Runs
        every { receptacle.onClick(capture(onClickSlot)) } just Runs
        every { receptacle.getElement(any()) } returns null // not important, just returns null

        // Mock session and destructuring parts used by Page
        val session = mockk<MenuSession>(relaxed = true)
        every { session.menu } returns menu
        every { session.component1() } returns mockk(relaxed = true)
        every { session.component2() } returns menu
        every { session.component3() } returns receptacle

        // Slot override: index 0 will be overridden by this item
        val overrideItem = mockk<ItemStack>(relaxed = true)
        page.slotOverrides[0] = overrideItem

        // Cooldown: always allow
        val cooldown = mockk<Cooldown>(relaxed = true)
        every { menu.cooldownManager } returns cooldown
        every { cooldown.tryConsumeCooldown(any()) } returns true

        // Click callback for index 0 — 直接写入真实 map
        var callbackWasCalled = false
        page.clickCallbacks[0] = { callbackWasCalled = true }

        // Mock MenuInteractEvent.callEvent() to return true
        mockkConstructor(MenuInteractEvent::class)
        every { anyConstructed<MenuInteractEvent>().callEvent() } returns true

        // Act
        page.loadInPage(session)

        // Assert: title set
        verify(exactly = 1) { receptacle.title(any(), false) }

        // Assert: hide player inventory propagated
        verify(exactly = 1) { receptacle.hidePlayerInventory = true }

        // Assert: event handlers registered
        verify(exactly = 1) { receptacle.onClose(any()) }
        verify(exactly = 1) { receptacle.onClick(any()) }

        // Assert: all 9 indices initially filled (0..8)
        for (i in 0..8) verify(atLeast = 1) { receptacle.setElement(eq(i), any()) }
        // Assert: indices 1..8 用 icon（克隆后同一引用）填充
        for (i in 1..8) verify(atLeast = 1) { receptacle.setElement(eq(i), eq(icon)) }
        // Assert: index 0 最终被覆盖为 overrideItem
        verify(atLeast = 1) { receptacle.setElement(0, overrideItem) }

        // 额外断言：行数和布局宽度符合预期
        assertEquals(1, page.rows)
        assertEquals(9, page.width)

        // Simulate a click on slot 0 to verify callback invocation
        val clickedEvent = mockk<ReceptacleInteractEvent>(relaxed = true)
        every { clickedEvent.player } returns mockk(relaxed = true)
        every { clickedEvent.receptacle } returns receptacle
        every { clickedEvent.slot } returns 0
        every { clickedEvent.clickType } returns ClickType.LEFT

        // Act: 触发 onClick 中注册的 lambda
        onClickSlot.captured.invoke(clickedEvent)

        // 验证 Page.clickCallbacks[0] 被调用
        assertTrue(callbackWasCalled)
        // 验证 MenuInteractEvent 被创建并调用
        verify { anyConstructed<MenuInteractEvent>().callEvent() }
    }
}
