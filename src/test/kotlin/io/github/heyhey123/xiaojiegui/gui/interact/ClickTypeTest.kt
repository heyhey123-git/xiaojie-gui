package io.github.heyhey123.xiaojiegui.gui.interact

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import kotlin.test.Test

class ClickTypeTest {

    @Test
    fun `number key - the digit behind each swap`() {
        assertEquals(1, ClickType.NUMBER_KEY_1.numberKey)
        assertEquals(5, ClickType.NUMBER_KEY_5.numberKey)
        assertEquals(9, ClickType.NUMBER_KEY_9.numberKey)
    }

    @Test
    fun `number key - clicks that are not a number key report nothing`() {
        // A number key that arrived with a button outside 0..8, and swapping with the offhand, are
        // swaps as well, but neither of them is a key from 1 to 9.
        assertNull(ClickType.NUMBER_KEY_INVALID.numberKey)
        assertNull(ClickType.SWAP_OFFHAND.numberKey)
        assertNull(ClickType.LEFT.numberKey)
        assertNull(ClickType.SHIFT_LEFT.numberKey)
        assertNull(ClickType.DOUBLE_CLICK.numberKey)
    }
}
