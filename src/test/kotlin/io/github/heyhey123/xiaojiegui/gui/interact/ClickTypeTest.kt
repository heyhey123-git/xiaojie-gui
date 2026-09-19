package io.github.heyhey123.xiaojiegui.gui.interact

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.bukkit.event.inventory.ClickType as Bukkit

/**
 * Every click a player can make, and what a script is told it was.
 *
 * There are two ways in. A `phantom` menu receives container click packets, so its clicks are named by
 * the protocol's mode and button ([ClickType.from]); a `static` menu receives bukkit events, which name
 * the click themselves ([ClickType.fromBukkit]). Both have to end up with the same answer for the same
 * gesture, because a script is written once and a menu's mode is a line in its creation.
 *
 * The value a script reads is `the click type`, which is Skript's own, so the assertions below are mostly
 * about `bukkitClickType`: every click type has to be one Skript can name, or a script has nothing to
 * compare against.
 */
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

    @Test
    fun `every click type is one a script can name`() {
        // `the click type` is Skript's click type, which is the bukkit one. A click type without one
        // would leave a script unable to write it or compare it, so there is none: a drag, which the
        // protocol sends as a click whose button carries its phase, is reported as a left/right/middle
        // click with a slot list instead of as a click type of its own.
        ClickType.entries.forEach { clickType ->
            assertTrue(
                clickType.bukkitClickType != null,
                "$clickType has no Skript click type to report"
            )
        }
    }

    @Test
    fun `a packet names the click it is`() {
        val expected = listOf(
            Triple(ClickMode.PICK_UP, 0, ClickType.LEFT),
            Triple(ClickMode.PICK_UP, 1, ClickType.RIGHT),
            Triple(ClickMode.QUICK_MOVE, 0, ClickType.SHIFT_LEFT),
            Triple(ClickMode.QUICK_MOVE, 1, ClickType.SHIFT_RIGHT),
            Triple(ClickMode.SWAP, 0, ClickType.NUMBER_KEY_1),
            Triple(ClickMode.SWAP, 8, ClickType.NUMBER_KEY_9),
            Triple(ClickMode.SWAP, 40, ClickType.SWAP_OFFHAND),
            Triple(ClickMode.CLONE, 2, ClickType.MIDDLE),
            Triple(ClickMode.THROW, 0, ClickType.DROP),
            Triple(ClickMode.THROW, 1, ClickType.CONTROL_DROP),
            Triple(ClickMode.PICK_UP_ALL, 0, ClickType.DOUBLE_CLICK)
        )
        for ((mode, button, clickType) in expected) {
            assertEquals(clickType, ClickType.from(mode.id, button), "$mode with button $button")
        }
    }

    @Test
    fun `a swap that named no hotbar slot is still a swap`() {
        // Bukkit reports the hotbar slot as the button of a swap; anything outside 0..8 is a swap that
        // did not come with a key, which a script sees as `number key` with nothing in `the pressed key`.
        assertEquals(ClickType.NUMBER_KEY_INVALID, ClickType.from(ClickMode.SWAP.id, 9))
        assertEquals(ClickType.NUMBER_KEY_INVALID, ClickType.from(ClickMode.SWAP.id, -1))
    }

    @Test
    fun `a drag is never a click type of its own`() {
        // `QUICK_CRAFT` is a drag, and its packets are collected into one interaction before this is
        // asked; a drag that reached one slot is delivered by the game as a click and arrives as one.
        assertNull(ClickType.from(ClickMode.QUICK_CRAFT.id, 0))
        assertNull(ClickType.from(ClickMode.QUICK_CRAFT.id, 1))
        assertNull(ClickType.from(ClickMode.QUICK_CRAFT.id, 2))
    }

    @Test
    fun `a packet this addon has no name for is nothing rather than a guess`() {
        // A client can send anything at all. The listener drops what it cannot name instead of throwing.
        assertNull(ClickType.from(ClickMode.PICK_UP.id, 5))
        assertNull(ClickType.from(99, 0))
    }

    @Test
    fun `a click outside the window is still the button it was made with`() {
        // Dropping the cursor on the background: the packet says so with the outside slot, and the two
        // buttons are two different clicks. `DROP_*_CURSOR` actions arrive this way too, which is why the
        // four cases below are the ones a drop can be.
        assertEquals(ClickType.OUTSIDE_LEFT, ClickType.from(ClickMode.PICK_UP.id, 0, ClickType.OUTSIDE_SLOT))
        assertEquals(ClickType.OUTSIDE_RIGHT, ClickType.from(ClickMode.PICK_UP.id, 1, ClickType.OUTSIDE_SLOT))
        assertEquals(ClickType.LEFT_DROP, ClickType.from(ClickMode.THROW.id, 0, ClickType.OUTSIDE_SLOT))
        assertEquals(ClickType.RIGHT_DROP, ClickType.from(ClickMode.THROW.id, 1, ClickType.OUTSIDE_SLOT))
        assertEquals(ClickType.UNKNOWN, ClickType.from(ClickMode.QUICK_MOVE.id, 0, ClickType.OUTSIDE_SLOT))
    }

    @Test
    fun `a bukkit event names the click it is`() {
        val expected = listOf(
            Bukkit.LEFT to ClickType.LEFT,
            Bukkit.RIGHT to ClickType.RIGHT,
            Bukkit.SHIFT_LEFT to ClickType.SHIFT_LEFT,
            Bukkit.SHIFT_RIGHT to ClickType.SHIFT_RIGHT,
            Bukkit.SWAP_OFFHAND to ClickType.SWAP_OFFHAND,
            Bukkit.MIDDLE to ClickType.MIDDLE,
            Bukkit.DROP to ClickType.DROP,
            Bukkit.CONTROL_DROP to ClickType.CONTROL_DROP,
            Bukkit.DOUBLE_CLICK to ClickType.DOUBLE_CLICK,
            Bukkit.UNKNOWN to ClickType.UNKNOWN
        )
        for ((bukkit, clickType) in expected) {
            assertEquals(clickType, ClickType.fromBukkit(bukkit, 0), "$bukkit in a slot")
        }
    }

    @Test
    fun `a dropped item is a drop, and a click on the background is a click`() {
        // The regression this table exists for: reading the event's *action* as well as its click type
        // reported `Q` over a slot as a click on the background, and a click on the background as a drop.
        assertEquals(ClickType.DROP, ClickType.fromBukkit(Bukkit.DROP, 5))
        assertEquals(ClickType.CONTROL_DROP, ClickType.fromBukkit(Bukkit.CONTROL_DROP, 5))

        assertEquals(ClickType.OUTSIDE_LEFT, ClickType.fromBukkit(Bukkit.LEFT, ClickType.OUTSIDE_SLOT))
        assertEquals(ClickType.OUTSIDE_RIGHT, ClickType.fromBukkit(Bukkit.RIGHT, ClickType.OUTSIDE_SLOT))
        assertEquals(ClickType.LEFT_DROP, ClickType.fromBukkit(Bukkit.DROP, ClickType.OUTSIDE_SLOT))
        assertEquals(ClickType.RIGHT_DROP, ClickType.fromBukkit(Bukkit.CONTROL_DROP, ClickType.OUTSIDE_SLOT))

        // And what a script reads for each of them, since that is the value it compares against.
        assertEquals(Bukkit.LEFT, ClickType.OUTSIDE_LEFT.bukkitClickType)
        assertEquals(Bukkit.DROP, ClickType.DROP.bukkitClickType)
        assertEquals(Bukkit.DROP, ClickType.LEFT_DROP.bukkitClickType)
    }

    @Test
    fun `a bukkit number key carries its hotbar slot in the slot argument`() {
        // The listener hands over the event's hotbar button for a number key, so the digit comes back.
        assertEquals(ClickType.NUMBER_KEY_1, ClickType.fromBukkit(Bukkit.NUMBER_KEY, 0))
        assertEquals(ClickType.NUMBER_KEY_9, ClickType.fromBukkit(Bukkit.NUMBER_KEY, 8))
        assertEquals(ClickType.NUMBER_KEY_INVALID, ClickType.fromBukkit(Bukkit.NUMBER_KEY, 9))
        assertEquals(1, ClickType.fromBukkit(Bukkit.NUMBER_KEY, 0).numberKey)
        assertEquals(9, ClickType.fromBukkit(Bukkit.NUMBER_KEY, 8).numberKey)
    }

    @Test
    fun `the predicates a phantom menu refreshes by are the interesting clicks`() {
        // `isItemMoveable` decides whether a phantom menu puts back one slot or the whole window after a
        // click, so a click that moves an item between slots has to say so here.
        for (clickType in listOf(
            ClickType.SHIFT_LEFT,
            ClickType.SHIFT_RIGHT,
            ClickType.NUMBER_KEY_1,
            ClickType.SWAP_OFFHAND,
            ClickType.MIDDLE,
            ClickType.DOUBLE_CLICK
        )) {
            assertTrue(clickType.isItemMoveable(), "$clickType can move an item between slots")
        }
        for (clickType in listOf(ClickType.LEFT, ClickType.RIGHT, ClickType.DROP, ClickType.CONTROL_DROP, ClickType.OUTSIDE_LEFT)) {
            assertFalse(clickType.isItemMoveable(), "$clickType only touches what it names")
        }
    }

    @Test
    fun `the button predicates agree with the bukkit click they carry`() {
        assertTrue(ClickType.LEFT.isLeftClick())
        assertTrue(ClickType.RIGHT.isRightClick())
        assertTrue(ClickType.SHIFT_LEFT.isShiftClick())
        assertTrue(ClickType.SHIFT_RIGHT.isRightClick())
        assertTrue(ClickType.NUMBER_KEY_3.isKeyboardClick())
        assertTrue(ClickType.NUMBER_KEY_3.isNumberKeyClick())
        // Swapping with the offhand is a key press, but not a number key: it carries no digit.
        assertTrue(ClickType.SWAP_OFFHAND.isKeyboardClick())
        assertFalse(ClickType.SWAP_OFFHAND.isNumberKeyClick())
        // Bukkit counts the drop keys as keyboard clicks as well, which is why `isItemMoveable` above
        // does not use that predicate.
        assertTrue(ClickType.DROP.isKeyboardClick())
        assertTrue(ClickType.LEFT.isMouseClick())
        assertFalse(ClickType.LEFT.isRightClick())
        assertFalse(ClickType.LEFT.isShiftClick())
        // The middle button and a double click are mouse clicks in Bukkit's sense; the drop keys and the
        // offhand swap are the keyboard ones (see the assertions above).
        assertTrue(ClickType.MIDDLE.isMouseClick())
        assertTrue(ClickType.DOUBLE_CLICK.isMouseClick())
        assertFalse(ClickType.MIDDLE.isKeyboardClick())
    }
}
