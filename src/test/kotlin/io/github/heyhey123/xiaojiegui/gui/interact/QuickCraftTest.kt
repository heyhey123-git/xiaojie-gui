package io.github.heyhey123.xiaojiegui.gui.interact

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The button of a `QUICK_CRAFT` packet carries the drag's phase and the button it was made with, and
 * everything else in the addon reads those two facts from here. Getting a bit range wrong would not
 * fail loudly: it would make a right drag look like a left one, or a drag's *start* look like its end.
 */
class QuickCraftTest {

    @Test
    fun `phase is the low two bits of the button`() {
        for (button in 0..11) {
            assertEquals(button and 3, QuickCraft.header(button), "header of button $button")
        }
    }

    @Test
    fun `the button is the high bits`() {
        assertEquals(0, QuickCraft.kind(0), "left button")
        assertEquals(1, QuickCraft.kind(4), "right button")
        assertEquals(2, QuickCraft.kind(8), "middle button")

        // The three buttons of one button's drag share their kind, and only then their phase.
        assertEquals(listOf(0, 0, 0), (0..2).map { QuickCraft.kind(it) }, "left drag")
        assertEquals(listOf(1, 1, 1), (4..6).map { QuickCraft.kind(it) }, "right drag")
        assertEquals(listOf(2, 2, 2), (8..10).map { QuickCraft.kind(it) }, "middle drag")
    }

    @Test
    fun `every button of a drag is a left, right or middle click, never a drag of its own`() {
        assertEquals(ClickType.LEFT, QuickCraft.clickType(0), "start of a left drag")
        assertEquals(ClickType.LEFT, QuickCraft.clickType(1), "continue of a left drag")
        assertEquals(ClickType.LEFT, QuickCraft.clickType(2), "end of a left drag")

        assertEquals(ClickType.RIGHT, QuickCraft.clickType(4), "start of a right drag")
        assertEquals(ClickType.RIGHT, QuickCraft.clickType(5), "continue of a right drag")
        assertEquals(ClickType.RIGHT, QuickCraft.clickType(6), "end of a right drag")

        assertEquals(ClickType.MIDDLE, QuickCraft.clickType(8), "start of a middle drag")
        assertEquals(ClickType.MIDDLE, QuickCraft.clickType(9), "continue of a middle drag")
        assertEquals(ClickType.MIDDLE, QuickCraft.clickType(10), "end of a middle drag")
    }
}
