package io.github.heyhey123.xiaojiegui.gui.interact

/**
 * A drag, as the protocol sends it: not one packet but a run of `QUICK_CRAFT` click packets, with the
 * phase and the kind of drag packed into the packet's button, because the packet had no field left for
 * them:
 *
 * - the low two bits are the phase: `0` starts, `1` continues, `2` ends;
 * - the high bits are the kind: `0` left, `1` right, `2` middle.
 *
 * So a left drag is the buttons `0, 1, 1, ..., 2` and a right drag `4, 5, 5, ..., 6`. Vanilla's own names
 * for the kinds say what each does: `CHARITABLE` (left, share the stack over the slots), `GREEDY` (right,
 * one item per slot) and `CLONE` (middle, a full stack per slot, creative only).
 *
 * Two consequences that decide how drags are handled everywhere else:
 *
 * - the start packet's slot is ignored (the server collects slots from the continue packets), so a start
 *   followed straight by an end drags over nothing and does nothing at all;
 * - a drag only ever *puts* the cursor's items into slots. There is no phase that takes items out, so a
 *   script that accounts for what a player took only has to watch clicks; a drag can only add.
 *
 * This is why `ClickType` has no drag values: a drag that touched one slot is delivered by the server as
 * an ordinary click, and one that touched several is reported as a single interaction with a slot list.
 */
object QuickCraft {

    const val START = 0
    const val CONTINUE = 1
    const val END = 2

    private const val LEFT = 0
    private const val RIGHT = 1

    /** The phase of the drag this packet belongs to. */
    fun header(button: Int): Int = button and 3

    /** The kind of drag: 0 left, 1 right, 2 middle. */
    fun kind(button: Int): Int = button shr 2

    /** The click type a script reads for this drag. */
    fun clickType(button: Int): ClickType = when (kind(button)) {
        LEFT -> ClickType.LEFT
        RIGHT -> ClickType.RIGHT
        else -> ClickType.MIDDLE
    }
}
