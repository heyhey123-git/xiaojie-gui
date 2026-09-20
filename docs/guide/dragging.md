# How drags actually work

[中文](dragging.zh-CN.md) | English

A player holding the left button and sweeping across a few slots looks like one action. In the protocol it
is a **run of packets**, and the server turns that run into three completely different outcomes. Knowing
this is what makes a menu with "these slots are goods and cannot be dragged away" or "dragging payment in
counts as paying" possible; not knowing it produces "the player dragged something and my script never
heard about it".

This page covers three things: what a drag is underneath, what it becomes by the time a script sees it, and
why we shaped the interface the way we did.

## The short version

1. **One drag is one interaction.** `on menu interact` runs once, and `the dragged slots` is every slot it
   reached.
2. **`the click type` stays `left/right/middle mouse button`.** A drag is not a click type of its own.
3. **`cancel event` is all or nothing**: for a click, that slot does not move; for a drag, none of it
   happens (the protocol cannot apply half of one).
4. **A drag only ever puts items into slots, it never takes any out.** So when you account for things: what
   a player took, watch clicks; what a player put, watch drags.

## Underneath: the phase lives in the button

A drag is not a packet of its own. The client sends a run of ordinary container click packets with
`clickType = QUICK_CRAFT`, and, because the packet had no field left for it, the phase is packed into the
**button**:

| Phase | Left | Right | Middle (creative) |
|---|---|---|---|
| start | `0` | `4` | `8` |
| continue | `1` | `5` | `9` |
| end | `2` | `6` | `10` |

The low two bits are the phase and the high bits are the button the drag was made with. Vanilla's names for
the three buttons say what each one does:

- left = `CHARITABLE`: **share** the cursor's stack over the slots it reached;
- right = `GREEDY`: **one item** per slot;
- middle = `CLONE`: **a whole stack** per slot (creative only).

**The start packet's slot is ignored**: the server collects slots from the continue packets, one slot per
packet.

## The three outcomes on the server

When the end packet arrives, the server looks at how many slots it collected:

| Collected | What the server does | What the plugin and the script see |
|---|---|---|
| **0 slots** | nothing | **no event at all** |
| **exactly 1** | rewrites it into an **ordinary click** | a plain click: `the clicked slot` is that slot, `the dragged slots` is empty |
| **2 or more** | applies all of it at once and fires **one** `InventoryDragEvent` | one interaction, `the dragged slots` holds every slot it reached |

Two more rules decide what gets collected at all:

- dragging with an **empty cursor** resets everything and does nothing (no event);
- a slot is collected only if it can **take** the cursor's item *and* the cursor still has items left to
  spread (`count <= collected` skips it). So **one item can only ever fill one slot** and two can fill two;
  dragging one stone across three empty stone slots collects exactly one.

### Why a drag can only put, never take

The end packet spreads the cursor's stack over the collected slots and subtracts it from the cursor. **No
phase of a drag ever moves an item from a slot to the cursor.** So:

- taking something out is always a click (plain click, shift click, number key, double-click collect, Q,
  offhand swap) — every one of those reaches a script as a click;
- a drag can only add. That is good news for shop and backpack bookkeeping: **who took what, look at
  clicks.**

## What a script sees

```skript
on menu interact:
    # One interaction, one run, whether the player clicked or swept over ten slots.
    set {_type} to the click type            # left / right / middle mouse button
    set {_slots::*} to the dragged slots     # empty for a click; every slot it reached for a drag
    set {_held} to the cursor item           # what the player is holding (for a drag, the stack it spread)

    # Slots 0 and 1 are goods and slot 5 is reserved: touching one refuses the whole interaction.
    if {_slots::*} is not set:
        set {_slots::*} to the clicked slot
    loop {_slots::*}:
        if loop-value is 0 or 1 or 5:
            cancel event
            stop

    # The free slots: a drag into them is a placement like any other, so settle each slot it reached.
    loop {_slots::*}:
        if loop-value is 11 or 12 or 13:
            settle(player, {_held}, loop-value)
```

The details that matter:

- **`the clicked slot` is the first slot touched** (for a drag); `the dragged slots` is all of them. Code
  that cares about one slot should read `the dragged slots`.
- **A `when clicked` slot callback runs for a drag too**, once per touched slot, and inside it
  `the clicked slot` is its own slot. A callback written for clicks therefore keeps working when a player
  drags over that slot instead.
- **`the cursor item` is only set in `static` mode.** In phantom mode the window is the server's own
  invention and there is no real stack in the player's hand.
- **Slot numbers are window slots**: `0` is the top left of the container, and the player's 27 inventory
  slots and 9 hotbar slots follow it, in both modes. In `static` mode the player's half of the window is
  not the menu's (it holds the player's own items), so putting icons there does nothing.

## What a drag means in phantom mode

The fact first: in a phantom menu a player **cannot pick anything up**. The window is the server's own
invention (every click packet is stopped by the plugin, which then re-sends the contents), so a drag cannot
change a real item any more than a click can. So why handle it at all?

1. **Not handling it leaves ghost items.** The client always predicts a container interaction locally, and
   only the server's re-send can undo that. The drag's **start and end packets had no click type here**, so
   they threw and were **neither cancelled nor followed by a re-send**: the slots the player swept over
   stayed wrong on the client's side (exactly the ghost items warned about elsewhere).
2. **The script's intent is real.** Without a real inventory a menu is still a button panel: which slots the
   player swept over is the only thing a script can read. There are two sensible uses — "select several at
   once" (a menu as a picker), or refusing it outright
   (`if the dragged slots is set: cancel event`). Neither was possible before.
3. **"One interaction, one event" is just as hard a rule here.** Items do not land anywhere, but a script's
   **side effects** are real: messages, charging a player, opening another menu, counters. A drag that runs
   a handler N times charges N times.
4. **One way to write a script for both modes.** Changing a menu from `static` to `phantom` should not mean
   rewriting it.

A real client can barely produce a drag in a phantom menu at all: it only drags what it is holding, and it
cannot hold anything in a phantom window (the contents are re-sent after every click). It still happens —
in the short "predicted" window between picking something up and our re-send arriving, or with a modified
client or another plugin in the way. So this is an input that has to be caught, not one that never arrives.

**One deliberate difference**: `the dragged slots` means something slightly different in the two modes,
because the two modes care about different things. In `static` it is **the slots the game would really
fill** (slots that cannot take the item, and slots the cursor no longer has items for, are skipped, and
`the cursor item` is the stack being spread); in `phantom` it is **every slot the player swept over**, since
nothing is filled there and the intent is all there is. In neither mode does a plain click have
`the dragged slots` (a one-slot drag is delivered by the game as a click).

One last limit: `the cursor item` is **empty in phantom mode**. Since 1.21 the packet's cursor field is only
a **hash** of the item rather than the item itself, so the server cannot see it, and this addon does not
guess.

## Using a drag as "select several" (phantom mode)

Nothing can be moved in a phantom menu, so the only thing a drag carries is the **gesture** itself: "I swept
over these slots". Used as a picker, one click selects one item and one sweep selects a row of them, with **no
second code path for drags**:

```skript
on menu interact:
    set {_slots::*} to the dragged slots
    if {_slots::*} is not set:
        set {_slots::*} to the clicked slot      # a click: the one slot it touched
    loop {_slots::*}:
        if loop-value is not between 0 and 4:
            continue                             # buttons and the like are not part of the selection
        if {picked::%uuid of player%::*} contains loop-value:
            remove loop-value from {picked::%uuid of player%::*}
        else:
            add loop-value to {picked::%uuid of player%::*}
```

A complete, runnable version is `server-test/skript/13-phantom-picker.sk` (with a confirm button that reports
which slots were picked), and it is parsed by `serverTest` so it cannot quietly stop working. It uses
`set icon in slot N of {_session}` to draw the selection: an icon set at runtime is **not** one of the slots
`locked icons` protects, which is exactly what makes per-player content possible.

## Measured: one gesture, four results

These were run against a real server with a real client (sending raw packets, because mineflayer does not
model drags):

| Gesture | Collected | Event | Result |
|---|---|---|---|
| a plain left click | — | one `InventoryClickEvent` | the item moves |
| **one** item swept over 2 slots | 1 slot | one `InventoryClickEvent` | **exactly like a click** (it lands in the first slot) |
| **two** items swept over 2 slots | 2 slots | **only** one `InventoryDragEvent` | one item in each |
| an empty cursor, or a start straight into an end | 0 slots | **no event at all** | nothing happens |
| sweeping over a slot that cannot take it (stone onto an emerald) | that slot is skipped | depends on the rest | that slot does not move |

That last row is the easy trap: **you think the drag reached two slots and the server collected one**, which
turns it back into a click.

## How this was found

Three sources, checked against each other:

1. **The protocol**: mineflayer's own window sync sends `mode: 5, mouseButton: 2` with the comment *"end of
   a drag that never started"*;
2. **NMS bytecode**: `AbstractContainerMenu.doClick` reads exactly those two bit ranges
   (`getQuickcraftHeader` / `getQuickcraftType`), and `getQuickCraftPlaceCount` is the sharing formula above;
   that class is the **only** one in the whole server jar that mentions `InventoryDragEvent`;
3. **CraftBukkit above it**: the click-packet handler jumps over the code that builds an
   `InventoryClickEvent` when the packet is `QUICK_CRAFT` — which is why a many-slot drag has no click event
   — while the single-slot case is rebuilt as a click.

The tests that hold it in place:

- `src/test/.../gui/interact/QuickCraftTest.kt`: the phase, button and click type of all nine buttons;
- `src/test/.../gui/receptacle/DragHandlingTest.kt`: one drag → one interaction with its slot list, a
  one-slot drag → a click, a drag over nothing → no event, cancelling → the whole drag event is cancelled;
- `server-test/skript/12-client-drag.sk` with `server-test/client/bot.mjs`: a **real client sending raw drag
  packets**, checking that a refused drag moves nothing and an accepted one fills every slot it reached.
