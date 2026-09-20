# How a window actually works

[中文](how-it-works.zh-CN.md) | English

A player types `/shop`, a chest appears with four buttons in it, and clicking one runs a few lines of
script. Underneath, four separate things had to agree for that to happen: a **definition** nobody is ever
shown, a per-player **copy** of it, a window that is either drawn with packets or is a real inventory, and
a click that is classified before Skript hears about it. Most "why does it behave like that" questions turn
out to be one of those four.

This page takes them one at a time: what the addon does underneath, then what a script therefore sees. A
drag is the same story told in full on a page of its own -- one interaction, whatever it touched -- so it
gets one paragraph here and a link: [How drags work](dragging.md).

## The short version

1. **A menu is a definition; a window is one player's copy of it.** Ten players can look at one menu, each
   on his own page with his own icons.
2. **Two modes, on the wire**: `phantom` draws the window with packets, `static` is a real server
   inventory. That is what decides who owns an item.
3. **Everything sent is one of three packets**: the whole window, one slot, or "open the window again" --
   the last one is what a title change is, because the protocol has no "set the title".

## A menu is a definition; a window is one player's copy of it

Underneath, a menu is three objects, and keeping them apart is what makes per-player content possible:

- `Menu` is the definition: the pages (each a layout plus a title), the icon mappings, the click callbacks,
  the cooldown, and the set of players currently looking at it. It has no title and no contents of its own.
- `ViewReceptacle` is **one window**: one title, one layout, one contents array, one viewer. It is created
  when a player opens a menu and thrown away when he closes it.
- `MenuSession` is the pair for one player: his `menu`, his `page` and his `receptacle`.

`open menu` makes the copy: it fires `on menu open`, builds a receptacle for the page being shown, loads
that page into it, and sends it. Two players opening the same menu therefore get two receptacles that
happen to have been built from the same definition.

What a script sees:

- `the menu window of player` is his window (nothing -- no value -- when he has none), and
  `the event-menu window` inside a menu event is the window the event happened on.
- Anything on the window is **that player's**: `set icon in slot 5 of {_window}` and
  `update title of the menu window of player to …` change one person's window and nobody else's (the
  second one reaches his screen at once only with `and refresh`).
- Anything on the **page** is shared: `override slot` and `map key` reach everybody who is looking at that
  page, because they change the page the receptacles were built from.
- A window's own icons are not the menu's: whatever the **page** writes afterwards -- a page turn, an
  `override slot`, a `map key` -- lands in the slots that page owns, over what that player's window had
  there. That is the whole difference between "this player's window" and "the menu".

```skript
on menu interact:
    # the page: every player looking at page 1 gets this
    override slot 5 in page 1 to diamond for {_menu}
    # the window: only the one who clicked
    set icon in slot 5 of the menu window of player to diamond
```

The word a script uses is **window**; the type behind it is still `menusession`, so `session` parses
everywhere `window` does and means exactly the same thing. Naming a window is only needed when the change
is for **one** player -- [Windows](windows.md) has the table of what does and does not need it.

## Two modes, on the wire

`phantom` and `static` are not two styles of the same thing; they are two different mechanisms, and which
one a menu uses decides who owns an item.

**`phantom` -- the addon draws the window.** It picks a container id, sends an open-screen packet and a
whole-window content packet, and then **cancels every container click packet** the player sends, putting
the contents back on screen afterwards. There is no server-side inventory anywhere: the items a player
sees live in the plugin's own contents array. So the slot contents are the menu's, a player cannot take
anything out, and the "block" he seems to pick up is a client-side prediction that the next packet undoes.

**`static` -- the server really creates one.** A real Bukkit inventory is opened for the player, and the
addon listens to Bukkit's own `InventoryClickEvent`, `InventoryDragEvent` and `InventoryCloseEvent`. What
is in the container slots is a real item in a real inventory, so the player can move, swap and drop it, and
`override slot` furniture can be carried off too.

Two consequences that show up in syntax:

- **Who owns the lower half.** In `static` those 36 slots are the player's own inventory and the menu owns
  nothing there -- `setElement` even refuses a slot past the container with a sentence saying so. In
  `phantom` they are the player's only while his inventory is shown (the window mirrors them live); with it
  hidden they are the menu's own space, which is what a `player layout` describes.
- **`refresh` has nothing to do in `static`.** The client is kept in step by the server's real inventory,
  so `refresh the menu window …` is a no-op there; and `the cursor item` only has a value in `static`,
  because a `phantom` click packet does not carry the item itself.

The last difference is the one that matters most while you write a script: **a `phantom` menu is a button
board, a `static` menu is a real chest.** [The two modes](modes.md) has the table and the flags.

## How a window gets drawn, and why `refresh` exists

Opening a window is two packets, in this order: **open the screen** (container id, the window type the
layout stands for, the title), then **the whole window's contents**, one item or nothing per slot, in
window-slot order.

The items in that second packet come from the page being shown: every key the page maps an icon to puts
that icon in the slots its layout gives that key, and what `override slot` wrote is applied after. So the
window a player first sees is "the page, rendered". The array is as long as the client's window --
container slots, then the player's half, which the same packet fills either with the player's items
(`phantom`, inventory shown) or leaves to the menu (hidden).

**Why an item can seem to snap back.** A client predicts every container interaction locally, before any
server answer. In `phantom` there is no real inventory to correct it, so the addon has to: it cancels the
click packet and re-sends the part of the window the client got wrong.

- A click that can only change the slot it names: that one slot.
- A click that can move an item **between** slots -- shift click, number key, offhand swap, middle click,
  double click: the whole window.
- A drag: every slot it reached, because putting one back is not enough.

That is the whole of "the item picked up and snapped back", and it is where `phantom` costs a little more
than `static`. It happens *before* the click cooldown is even consulted, so even a click the cooldown drops
leaves the client's prediction corrected.

**Where `refresh` comes in.** The repair above is also what a script needs when it changes contents behind
the client's back. `refresh the menu window …` re-sends the whole window; `refresh the slot N in …`
re-sends one slot; in `static` it does nothing, because the server's real inventory is already the truth.
`set icon in slot N of {_window}` sends the window itself, without `refresh`: a `set` goes through the
`setIcons(…, true)` path, which ends in a whole-window send.

## Why changing a title re-opens the window

**The protocol has no "set the title" packet.** The only way a window's title changes is a second
open-window packet, so that is what a title change is:

- in `phantom`, opening it again: the open-screen packet plus the whole contents behind it, deferred by
  `TaskUtil.sync(delay = 0)` -- the **next tick**, because the client ignores a title change in the same
  tick it opened the window;
- in `static`, the same shape done by hand for the real container menu: the container's title is set and a
  fresh open-screen packet is sent for it, in the same tick.

One tick is the whole of the `phantom` delay; it is a boundary the client imposes, not a network guess.

Three things follow, and each of them explains a script's behaviour:

- **A title change re-sends the contents.** Since the second packet is a whole open, whatever the window
  held at that moment is what goes back on screen -- which is why the array behind the title matters.
- **It does nothing once the window is closed.** The deferred task re-checks its viewer first, and closing
  a window clears it, so a title update that was still in flight cannot put a window back on screen that
  the player has already closed. (There is a client test for exactly this ordering.)
- **The screen can be a tick behind the model.** `the title of {_window}` reads the window's own title,
  which is set the moment the statement runs; the window the **client** is holding still carries the old
  title until the second open-screen packet arrives. So a title written and read back in the same tick
  gives the new value, while a read of the client's window in that same tick gives the old one -- which is
  what the manual pass checks with a one-command "open and retitle".

[Windows](windows.md) has the syntax (`and refresh`, per-page titles); [Titles](titles.md) has the text
and colour forms.

## How a click is decided

The click arrives by one of two completely different roads, and it is classified before a script sees
anything:

- **`phantom`**: the click is a container-click packet. It is read on the network thread, translated into
  the addon's own click type from the packet's *mode*, *button* and *slot*, and then handed to the main
  thread. A click the addon has no name for is **dropped**, not thrown: the window it belongs to is the
  server's own invention, so ignoring it is the right answer.
- **`static`**: the click is a Bukkit `InventoryClickEvent` (`InventoryDragEvent` for a drag), and
  Bukkit's own click type decides. The slot recorded is the **raw** slot, not `getSlot()`, because inside
  the player's own half `getSlot()` is numbered relative to the inventory that was clicked and would name a
  container slot the player never touched -- and run a callback for it.

Everything after that happens in one fixed order, which is worth knowing because it explains two
surprises:

1. **the client's prediction is repaired** (see the previous section -- this runs even if the rest of the
   interaction is going to be refused);
2. **the click cooldown is asked**. If the gap since this player's last accepted click is smaller than
   `click delay`, the interaction is **silently dropped**: no event, no slot callbacks, no message. The
   default is 50 ms for `create menu` and 5 ms for `build a menu`;
3. **the slot callbacks run**, one per slot the interaction touched (for a drag, one per slot it reached);
4. **`on menu interact` fires** -- after the callbacks, so a `cancel event` there refuses the interaction
   but cannot un-run a callback that has already fired;
5. **`locked icons` is applied last**, which is why a shop's "buy" callback fires while the goods stay
   exactly where they are.

`the click type` is **Skript's own click type**, with Skript's words (`left mouse button`, `number key`,
`drop key`, `swap offhand key`, …) and its literals; `the event-clicktype` is the same value. The one thing
it cannot carry is *which* number key was pressed -- all nine are the single `number key` -- so that digit
is read from the packet's button and offered as `the pressed number key`, 1 to 9 on a number key click and
nothing on any other click.

`cancel event` refuses **the interaction**, all of it: in `static` the Bukkit event is cancelled so the
item does not move, and in `phantom` there was never anything to move -- the repair is what the client
sees. What a cancelled click does *not* do is refuse a slot callback that already ran; to stop that, `stop`
inside the callback yourself.

**A drag is one interaction.** It arrives as a run of click packets whose button carries the phase, it is
collected and reported once, at the end, with every slot it reached in `the dragged slots` -- and a drag
that collected a single slot is delivered as a plain click. The mechanism, the three outcomes and what a
script should do with it are all in [How drags work](dragging.md).

## Every menu operation runs on the main thread

A menu changes real inventories and real windows, so every menu operation has to run on the server's main
thread. That line is invisible while a script is written, and the ways to cross it are indirect: a
`run task later` inside an asynchronous `execute`, another plugin's asynchronous callback, a custom event
declared `on … async`.

What the plugin checks is the moment of execution, not the syntax: a call from another thread is **given up**
with a line that names the operation -- opening, closing, turning a page, retitling, refreshing, filling a
list and writing an icon each say their own thing, for example
`Menu can only be opened from the main server thread, …`.

That check has no switch, because a call it stops is never legal. The plugin has no options at all: it writes
no config file, and the colour of the startup banner is decided by asking the console
(`net.kyori.ansi.colorLevel`) rather than by a setting -- see [Not Supported Yet](not-supported-yet.md).

## Which slots belong to whom

Slot numbers are **window slots**: `0` is the top left of the container, and the player's half follows it --
27 main inventory slots, then 9 hotbar slots. That is true in both modes, so a layout and a script agree on
numbers. The half is derived, not written down per layout: it starts right after the container's range,
which is why a layout whose slot range is one short does not fail but shifts every player slot by one. (The
lectern is the single exception: its client menu is one slot with no player half at all, so its layout
carries none, and the content packet is exactly the length the client's menu accepts.)

Within the container, the slots a page gives an icon to are the page's own -- the layout's keys plus what
`override slot` wrote. That set is what `with locked icons` protects: no click, shift click, number key,
offhand swap, drop or double-click collect can take from those slots or put anything into them, and a drag
that touches one is refused as a whole. The slots a layout leaves **empty** stay free, which is what makes
a shop (icons that are the menu's) and a backpack (empty slots that are the player's) the same mechanism.

The lower half is decided by mode and by `hide player inventory`:

| | 36 slots below the container |
|---|---|
| `phantom`, inventory shown | the player's own items, mirrored live |
| `phantom`, inventory hidden | the menu's own space; `player layout` describes it, `set icon in slot N of {_window}` fills it, clicks on it arrive like any other slot's |
| `static` | the player's real inventory; never the menu's, and a player layout warns and changes nothing |

An icon a page lays out for the lower half while that half is the player's is **cleared before the window
is sent**, so it cannot appear in a cell the player happens to leave empty. And `set icon in slot N of
{_window}` is that player's own content rather than the menu's furniture -- which is why `locked icons`
does not protect it, and what makes per-player highlights possible.

[Pages](pages.md#player-layout) describes `player layout` itself; [Filling menus](filling-menus.md) is the
other half -- what writes icons into those slots.

## What a page turn sends

A page turn is not "redraw the window". In order:

1. **The target page's layout is checked first**, before anything changes. A page with a different layout
   (a different chest row count, or a different container entirely) is refused with a message, and the page
   number and the contents are left exactly as they were -- the receptacle has an immutable layout, so a
   turn cannot migrate items into a different-shaped window.
2. **`on page turn` fires** and is cancellable; `the title` in it is the title of the page being turned
   *to* and can be changed for this one turn.
3. **Only the slots the page being left owns are cleared** -- the slots its layout maps a key to, plus the
   ones `override slot` covered. Every other slot is left alone. That is what lets a backpack keep what the
   player put in its empty slots across a page turn; clearing the whole window would have made a paging
   backpack impossible.
4. **The target page is loaded into the window** (its icons, its overrides, its list).
5. **The title is applied** if it differs from what the window has, which is the "open the window again"
   packet of the previous section and carries the contents with it; only when the title is unchanged is a
   plain `refresh` sent instead. That is why a titleless page turn and a page turn that retitles do not
   look the same on the wire, and why `static` no longer sends the whole window twice per turn.

Each page carries its own title, so a turn normally changes the title too;
`turn to page 2 for player with new title "…"` supplies one for **that display only** and does not write it
back to the page. The syntax and the page-number rules are in [Pages](pages.md).

## When a window goes away

| What happens | What it means underneath |
|---|---|
| the player closes it (ESC, or the close button) | `phantom`: a window-close packet; `static`: a Bukkit close event. Either way the window's close handler fires `on menu close`, and the window is dropped |
| `close the menu …` / `close the menu window …` | the plugin closes it: the close packet is sent and the window is dropped |
| `destroy the menu …` | every player looking at that menu is closed out of it; the menu itself can no longer be opened or interacted with |
| the player switches to another menu | the old menu fires `on menu close` once, and he stops being counted among its `menu viewers` |
| the player quits, dies or changes world | `on menu close` fires first, while the container is still there, so a script can save what it holds; then the window is dropped and a `static` menu's real inventory is **cleared** with it rather than kept -- otherwise it would sit in memory with the player's items for the rest of the server's life |
| a script reload (`/skript reload`) | **every menu is destroyed** before the new scripts are parsed |

The last row is the one to remember while writing scripts: a menu holds the **callbacks of the script that
built it**, so a menu that outlived a reload would answer the next click with code that is no longer in any
script. The addon therefore destroys every menu in Skript's pre-init event, before the batch of scripts is
parsed, and the reloaded scripts build their menus again -- which is why menus are almost always built in
`on load:`. A menu with a fixed id that is built again destroys the previous one, so two of them never
coexist.

[Windows](windows.md) has the same list from the script's side, plus the `menu viewers` details.

## How this was checked

Three layers, and this section says which one holds which claim.

**The unit tests** pin the parts that need no server:

- `ViewLayoutTest` holds every layout's container size to Bukkit's own size for its inventory type, and
  names the two totals that are not "container + 36": the lectern's window is **1** slot, the beacon's is
  **37**;
- `PhantomReceptacleTest` pins the packet shape of the three operations on this page: opening sends exactly
  one open-screen and one whole-window packet, a retitle sends a **second** open-screen, closing sends the
  close packet -- plus that the lectern's content packet holds its one slot, and that an icon laid out for
  the player's half is cleared while that half is the player's;
- `ViewReceptacleTest` pins that closing clears the viewer, which is the thing that keeps a late title
  update from reopening a closed window;
- `MenuTest` pins that a page turn preserves the contents of the slots the page does not own, that an
  incompatible layout is rejected before anything changes, and that `destroyAll` takes every menu with it;
- `PageLockedIconsTest`, `PageAuditRegressionTest` and `CooldownTest` cover the rest of the click rules:
  every way of taking from an icon slot is refused, a drag that touches one is refused as a whole, a shift
  click out of the player's own half still reaches a free slot, a callback emits exactly one global event,
  and the cooldown refuses a click that comes too early.

**The server test** (`./gradlew serverTest`) boots a real Paper 26.2 server with Skript and PacketEvents and
runs the addon against scripts: that every element registers, that every script parses, that the native
`assert`s of a set of behaviour scripts hold, and that the log has the shape it should (runtime messages on
the runtime channel, no `Severe Error`, no parse shape that used to be a bug). Every `@Examples` annotation
is parsed there too, so a documented example Skript cannot read fails the build.

**The client test** (`./gradlew clientTest`) drives a real Minecraft client, which reads the window back
from the packets: its title, its type, and the item in each known slot by **type** as well as by name. It
asserts the window after a page turn (which is where the second open-screen packet is exercised end to end),
the lower half belonging to the page rather than to the player, the lectern's one-slot window and the
beacon's 1 + 36, the dropper's three-column layout, the drags the bot writes as raw packets, and that a
window retitled and then closed in the same tick does not come back. The client is a 26.1 client behind
ViaVersion + ViaBackwards, so what that layer proves is that the packets this addon emits survive a real
translation layer to a real client -- [Not Supported Yet](not-supported-yet.md) is precise about that
boundary.

**The manual pass** (`docs/manual-acceptance.zh-CN.md`) is what only a person in front of a real 26.2 client
can settle, and the three mechanisms on this page are exactly the ones that need it:

- **the lectern's one-slot window** (`/acctype lectern`): that the window is the one the client draws for
  that block, and that a slot count which is off would shift the whole player inventory;
- **a title arriving after a re-open** (`/acc sametick`: one command opens a window and retitles it): the
  automated layer asserts the value, the person confirms the screen actually shows it;
- **a drag's phases** (`/acc drag`): the client test writes real drag packets with an item in hand, so the
  manual pass adds the empty cursor, and a person's eye on where each item lands and on what a refused
  drag leaves behind.

What the automated layers leave uncovered is written down there too: client-side ghost items, and two
clients looking at one menu at the same time (the client test has one bot).
