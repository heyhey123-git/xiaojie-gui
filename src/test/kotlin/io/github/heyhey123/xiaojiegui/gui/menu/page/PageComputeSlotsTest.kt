package io.github.heyhey123.xiaojiegui.gui.menu.page

import io.github.heyhey123.xiaojiegui.gui.menu.MenuProperties
import io.github.heyhey123.xiaojiegui.gui.menu.component.IconProducer
import io.github.heyhey123.xiaojiegui.gui.menu.component.Page
import io.github.heyhey123.xiaojiegui.gui.receptacle.Receptacle
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import net.kyori.adventure.text.Component
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.ItemStack
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import kotlin.test.Test

class PageComputeSlotsTest {

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    // 1) STATIC mode tests
    @Test
    fun `STATIC - fills container and clones per slot`() {
        val properties = mockk<MenuProperties>(relaxed = true).apply {
            every { mode } returns Receptacle.Mode.STATIC
        }
        val page = Page(
            InventoryType.CHEST,
            Component.text("Title"),
            listOf("aaaaaaaaa"),
            emptyList(),
            properties
        )

        val iconA = mockk<ItemStack>(relaxed = true)
        val iconProducerA = IconProducer.SingleIconProducer(iconA)
        page.iconMapper["a"] = iconProducerA to null
        var cloneCount = 0
        every { iconA.clone() } answers { mockk<ItemStack>(relaxed = true, name = "a#${cloneCount++}") }

        val slots = page.computeSlots()

        assertEquals(page.layout.containerSize, slots.size, "Slots array size should match container size")
        val nonNullSlots = slots.filterNotNull()
        assertEquals(page.layout.containerSize, nonNullSlots.size, "All container slots should be filled")
        // 每个槽位应为独立 clone 实例
        assertEquals(
            nonNullSlots.size,
            nonNullSlots.toSet().size,
            "Each slot should contain a cloned instance of the icon"
        )
    }

    // 2) STATIC mode with multi-char keys
    @Test
    fun `STATIC - backtick multi-char keys are mapped to single visual slot`() {
        val properties = mockk<MenuProperties>(relaxed = true).apply {
            every { mode } returns Receptacle.Mode.STATIC
        }
        // 可视位：0:`gold` 1:`gold` 2:a 其余为空格
        val page = Page(
            InventoryType.CHEST,
            Component.text("Title"),
            listOf("`gold``gold`a      "),
            emptyList(),
            properties
        )

        val iconGold = mockk<ItemStack>(relaxed = true)
        val iconA = mockk<ItemStack>(relaxed = true)
        every { iconGold.clone() } answers { mockk<ItemStack>(relaxed = true) }
        every { iconA.clone() } answers { mockk<ItemStack>(relaxed = true) }
        page.iconMapper += mapOf(
            "gold" to (IconProducer.SingleIconProducer(iconGold) to null),
            "a" to (IconProducer.SingleIconProducer(iconA) to null)
        )

        val slots = page.computeSlots()

        assertNotNull(slots[0])
        assertNotNull(slots[1])
        assertNotNull(slots[2])
        for (i in 3..<page.layout.containerSize) {
            assertNull(slots[i])
        }
    }

    // 3) PHANTOM mode tests
    @Test
    fun `PHANTOM - includes player inventory when not hidden (nulls for space key)`() {
        val properties = mockk<MenuProperties>(relaxed = true).apply {
            every { mode } returns Receptacle.Mode.PHANTOM
            every { hidePlayerInventory } returns false
        }
        val page = Page(
            InventoryType.CHEST,
            Component.text("Title"),
            listOf("aaaaaaaaa"),
            emptyList(),
            properties
        )

        val iconA = mockk<ItemStack>(relaxed = true)
        every { iconA.clone() } answers { mockk<ItemStack>(relaxed = true) }
        page.iconMapper["a"] = IconProducer.SingleIconProducer(iconA) to null

        val slots = page.computeSlots()

        assertEquals(page.layout.totalSize, slots.size)
        // 容器区非空，玩家背包区为空
        val containerSize = page.layout.containerSize
        for (i in 0..<containerSize) assertNotNull(slots[i])
        for (i in containerSize..<slots.size) assertNull(slots[i])

        verify { iconA.clone() }
    }

    // 4) PHANTOM mode with hidden player inventory
    @Test
    fun `PHANTOM - hides player inventory so only container slots are filled`() {
        val properties = mockk<MenuProperties>(relaxed = true).apply {
            every { mode } returns Receptacle.Mode.PHANTOM
            every { hidePlayerInventory } returns true
        }
        val page = Page(
            InventoryType.CHEST,
            Component.text("Title"),
            listOf("aaaaaaaaa"),
            emptyList(),
            properties
        )

        val iconA = mockk<ItemStack>(relaxed = true)
        every { iconA.clone() } answers { mockk<ItemStack>(relaxed = true) }
        page.iconMapper["a"] = IconProducer.SingleIconProducer(iconA) to null

        val slots = page.computeSlots()

        // 仍返回 totalSize，但不会对玩家背包键进行解析
        assertEquals(page.layout.totalSize, slots.size)
        val containerSize = page.layout.containerSize
        for (i in 0..<containerSize) assertNotNull(slots[i])
        for (i in containerSize..<slots.size) assertNull(slots[i])

        verify { iconA.clone() }
    }
}
