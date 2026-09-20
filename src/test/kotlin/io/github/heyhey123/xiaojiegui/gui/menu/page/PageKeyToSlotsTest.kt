package io.github.heyhey123.xiaojiegui.gui.menu.page

import io.github.heyhey123.xiaojiegui.gui.menu.MenuProperties
import io.github.heyhey123.xiaojiegui.gui.menu.component.Page
import io.github.heyhey123.xiaojiegui.gui.receptacle.Receptacle
import net.kyori.adventure.text.Component
import org.bukkit.event.inventory.InventoryType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
            playerLayoutPattern = player,
            properties = props
        )
    }

    // combined test: multiple rows, multiple keys, player inventory, offsets, aggregation
    @Test
    fun `keyToSlots parse the keys of a series strings with backquote`() {
        val layoutPattern = listOf(
            "xxxxoxxxx",
            "x       x",
            "xx `pre` `close` `next` x"
        )

        val playerInventoryPattern = listOf(
            "`corner a`aaaaaaaa",
            "bbbbbbbbb",
            "ccccccccc",
            "dddddddd`corner d`"
        )

        val inventoryType = InventoryType.CHEST

        val p = page(
            layout = layoutPattern,
            player = playerInventoryPattern,
            mode = Receptacle.Mode.PHANTOM,
            hidePI = true,
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

    // 1) a backquote block counts as 1 column, the same key aggregates
    @Test
    fun `aggregates same key multiple times in line`() {
        val p = page(layout = listOf("a`x`a`y`a`z`a"))
        val actual = p.keyToSlots["a"]
        assertNotNull(actual, "Expected key 'a' to be mapped")
        assertEquals(setOf(0, 2, 4, 6), actual, "Slots for key 'a' should be [0,2,4,6]")
    }

    // 2) width truncation (out of bounds ignored): content past 9 columns (CHEST) is not counted,
    // and a backquote block starting at column 10 is truncated
    @Test
    fun `respects width limit and truncates overflow`() {
        val p = page(layout = listOf("1234567890`k`"))
        val map = p.keyToSlots
        assertFalse(map.containsKey("k"), "key 'k' should be truncated beyond width")
    }

    // 3) an unpaired backquote is recorded literally as '`'
    @Test
    fun `unpaired backquote is literal key`() {
        val p = page(layout = listOf("ab`cd"))
        val slots = p.keyToSlots["`"]
        assertNotNull(slots, "Expected literal '`' key to be mapped when unpaired")
        assertEquals(setOf(2), slots, "Unpaired backquote should map at index 2")
    }

    // 4) a player layout is not read while the player's own inventory is shown: those slots are the
    // player's, and a menu painting icons over them would be neither half
    @Test
    fun `player layout ignored when the player inventory is shown`() {
        val p = page(
            layout = listOf("         "),
            player = listOf("`pi`      "),
            mode = Receptacle.Mode.PHANTOM,
            hidePI = false
        )
        assertFalse(p.keyToSlots.containsKey("pi"), "Player layout should be ignored while the inventory is shown")
    }

    // 5) with the player's inventory hidden the lower half is the menu's, and the offset starts at size
    @Test
    fun `player layout mapped with correct offset when the inventory is hidden`() {
        val p = page(
            layout = listOf("         "), // 1 row CHEST => container size = 9
            player = listOf("`pi`      "),
            mode = Receptacle.Mode.PHANTOM,
            hidePI = true
        )
        val slots = p.keyToSlots["pi"]
        assertNotNull(slots, "Expected key 'pi' to be mapped in the hidden lower half")
        assertEquals(setOf(9), slots, "Key 'pi' should start at base index = size (9)") // base is size=9, row=0, column=0
    }

    // 6) a backquote block inside the bounds is still mapped
    @Test
    fun `backquote block at last visual column is mapped`() {
        val p = page(layout = listOf("12345678`K`X"))
        val slotsOfk = p.keyToSlots["K"]

        assertNotNull(slotsOfk, "Expected key 'K' to be mapped at last visual column")
        assertEquals(setOf(8), slotsOfk, "'K' should be mapped at index 8")
        // 'X' is past the width, is truncated and no longer mapped
        val slotsOfX = p.keyToSlots["X"]
        assertNull(slotsOfX, "'X' should be truncated and not mapped")
    }

    // 7) non CHEST: HOPPER width is 5, a backquote block takes 1 column
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

    // 8) the same key merges across the container and the hidden lower half
    @Test
    fun `merges same key across layout and hidden lower half`() {
        val p = page(
            layout = listOf("`k`       "), // slot 0
            player = listOf("`k`       "), // slot size(=9) + 0 => 9
            mode = Receptacle.Mode.PHANTOM,
            hidePI = true
        )
        val slots = p.keyToSlots["k"]
        assertNotNull(slots, "Expected key 'k' to be mapped in both regions")
        assertEquals(setOf(0, 9), slots, "Key 'k' should merge to slots [0, 9]")
    }
}
