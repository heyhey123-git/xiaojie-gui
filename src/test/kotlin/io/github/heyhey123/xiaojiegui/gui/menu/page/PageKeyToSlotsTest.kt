package io.github.heyhey123.xiaojiegui.gui.menu.page

import io.github.heyhey123.xiaojiegui.gui.menu.MenuProperties
import io.github.heyhey123.xiaojiegui.gui.menu.component.Page
import io.github.heyhey123.xiaojiegui.gui.receptacle.Receptacle
import net.kyori.adventure.text.Component
import org.bukkit.event.inventory.InventoryType
import org.junit.jupiter.api.Test
import kotlin.test.*

class PageKeyToSlotsTest {

    /**
     * Helper function to create a Page with specified parameters
     */
    private fun page(
        layout: List<String>,
        player: List<String> = listOf(),
        mode: Receptacle.Mode = Receptacle.Mode.PHANTOM,
        hidePI: Boolean = false,
        type: InventoryType = InventoryType.CHEST
    ): Page {
        val props = MenuProperties(
            defaultTitle = Component.text("T"),
            hidePlayerInventory = hidePI,
            mode = mode,
            minClickDelay = 0,
            defaultPage = 1,
            defaultLayout = listOf()
        )
        return Page(
            inventoryType = type,
            title = Component.text("T"),
            layoutPattern = layout,
            playerInventoryPattern = player,
            properties = props
        )
    }

    // 综合测试：多行、多键、玩家背包、偏移、聚合
    @Test
    fun `keyToSlots parse the keys of a series strings with backquote`() {
        val layoutPattern = listOf(
            "xxxxoxxxx",
            "x       x",
            "xx `pre` `close` `next` x",
        )

        val playerInventoryPattern = listOf(
            "`corner a`aaaaaaaa",
            "bbbbbbbbb",
            "ccccccccc",
            "dddddddd`corner d`",
        )

        val inventoryType = InventoryType.CHEST

        val p = page(
            layout = layoutPattern,
            player = playerInventoryPattern,
            mode = Receptacle.Mode.PHANTOM,
            hidePI = false,
            type = inventoryType
        )

        val expectedKeyToSlots = mapOf(
            "pre" to setOf(21),
            "close" to setOf(23),
            "next" to setOf(25),
            "corner a" to setOf(27),
            "corner d" to setOf(62)
        )

        val actualKeyToSlots = p.keyToSlots

        for ((key, expectedSlots) in expectedKeyToSlots) {
            val actualSlots = actualKeyToSlots[key]
            assertTrue(actualSlots != null, "Key '$key' not found in actual keyToSlots")
            assertEquals(expectedSlots, actualSlots, "Slots for key '$key' do not match")
        }

    }

    // 1) 反引号块计 1 列，同键聚合
    @Test
    fun `aggregates same key multiple times in line`() {
        val p = page(layout = listOf("a`x`a`y`a`z`a"))
        val actual = p.keyToSlots["a"]
        assertNotNull(actual, "Expected key 'a' to be mapped")
        assertEquals(setOf(0, 2, 4, 6), actual, "Slots for key 'a' should be [0,2,4,6]")
    }

    // 2) 宽度截断(越界忽略)：超过 9 列（CHEST）后的内容不应计入；反引号块开始于第 10 列被截断
    @Test
    fun `respects width limit and truncates overflow`() {
        val p = page(layout = listOf("1234567890`k`"))
        val map = p.keyToSlots
        assertFalse(map.containsKey("k"), "key 'k' should be truncated beyond width")
    }

    // 3) 不成对反引号 -> 记作 '`'
    @Test
    fun `unpaired backquote is literal key`() {
        val p = page(layout = listOf("ab`cd"))
        val slots = p.keyToSlots["`"]
        assertNotNull(slots, "Expected literal '`' key to be mapped when unpaired")
        assertEquals(setOf(2), slots, "Unpaired backquote should map at index 2")
    }

    // 4) 玩家背包在被隐藏时不参与映射
    @Test
    fun `player inventory ignored when hidden`() {
        val p = page(
            layout = listOf("         "),
            player = listOf("`pi`      "),
            mode = Receptacle.Mode.PHANTOM,
            hidePI = true
        )
        assertFalse(p.keyToSlots.containsKey("pi"), "Player inventory should be ignored when hidden")
    }

    // 5) 玩家背包在 PHANTOM 模式显示时，偏移从 size 开始（偏移正确）
    @Test
    fun `player inventory mapped with correct offset when visible`() {
        val p = page(
            layout = listOf("         "), // 1 行 CHEST => container size = 9
            player = listOf("`pi`      "),
            mode = Receptacle.Mode.PHANTOM,
            hidePI = false
        )
        val slots = p.keyToSlots["pi"]
        assertNotNull(slots, "Expected key 'pi' to be mapped in player inventory")
        assertEquals(setOf(9), slots, "Key 'pi' should start at base index = size (9)") // 基址 size=9，行=0，列=0
    }

    // 6) 边界内的反引号块仍映射
    @Test
    fun `backquote block at last visual column is mapped`() {
        val p = page(layout = listOf("12345678`K`X"))
        val slotsOfk = p.keyToSlots["K"]

        assertNotNull(slotsOfk, "Expected key 'K' to be mapped at last visual column")
        assertEquals(setOf(8), slotsOfk, "'K' should be mapped at index 8")
        // 'X' 超过宽度被截断，不再映射
        val slotsOfX = p.keyToSlots["X"]
        assertNull(slotsOfX, "'X' should be truncated and not mapped")
    }

    // 7) 非 CHEST：HOPPER 宽度为 5，反引号块占 1 列
    @Test
    fun `hopper width is 5 and backquote counts as one`() {
        val p = page(
            layout = listOf("a`pi`cde"), // a(0) `pi`(1) c(2) d(3) e(4)
            player = listOf(),
            mode = Receptacle.Mode.PHANTOM,
            hidePI = false,
            type = InventoryType.HOPPER
        )
        val slots = p.keyToSlots["pi"]
        assertNotNull(slots, "Expected key 'pi' to be mapped on HOPPER row")
        assertEquals(setOf(1), slots, "Key 'pi' should be mapped at index 1 on HOPPER")
    }

    // 8) 跨区合并同键
    @Test
    fun `merges same key across layout and player inventory`() {
        val p = page(
            layout = listOf("`k`       "), // 槽位 0
            player = listOf("`k`       "), // 槽位 size(=9) + 0 => 9
            mode = Receptacle.Mode.PHANTOM,
            hidePI = false
        )
        val slots = p.keyToSlots["k"]
        assertNotNull(slots, "Expected key 'k' to be mapped in both regions")
        assertEquals(setOf(0, 9), slots, "Key 'k' should merge to slots [0, 9]")
    }
}
