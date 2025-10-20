@file:Suppress("unused", "functionName")

package io.github.heyhey123.xiaojiegui.gui.layout

import kotlin.test.Test
import kotlin.test.assertEquals


private class TestLayout(range: IntRange) : Layout(range)

class LayoutTest {

    @Test
    fun `3x9 chest like layout ranges and sizes`() {
        val layoutRange = 0..26
        val layout = TestLayout(layoutRange)

        assertEquals(
            (27..53).toList(),
            layout.mainInvSlotRange,
            "Main inventory slot range should be correct"
        )

        assertEquals(
            (54..62).toList(),
            layout.hotBarSlotRange,
            "Hotbar slot range should be correct"
        )

        assertEquals(
            (0..26).toList(),
            layout.containerSlotRange,
            "Container slot range should be correct"
        )

        assertEquals(
            27,
            layout.containerSize,
            "Container size should be correct"
        )

        assertEquals(
            (0..62).toList(),
            layout.totalSlotRange,
            "Total slot range should be correct"
        )

        assertEquals(
            63,
            layout.totalSize,
            "Total size should be correct"
        )
    }

    fun `3x3 crafter like layout ranges and sizes`() {
        // actually the slot 45 is also used in crafting table, but we ignore it here for simplicity
        val layoutRange = 0..8
        val layout = TestLayout(layoutRange)

        assertEquals(
            (9..35).toList(),
            layout.mainInvSlotRange,
            "Main inventory slot range should be correct"
        )

        assertEquals(
            (36..44).toList(),
            layout.hotBarSlotRange,
            "Hotbar slot range should be correct"
        )

        assertEquals(
            (0..8).toList(),
            layout.containerSlotRange,
            "Container slot range should be correct"
        )

        assertEquals(
            9,
            layout.containerSize,
            "Container size should be correct"
        )

        assertEquals(
            (0..44).toList(),
            layout.totalSlotRange,
            "Total slot range should be correct"
        )

        assertEquals(
            45,
            layout.totalSize,
            "Total size should be correct"
        )
    }

}

