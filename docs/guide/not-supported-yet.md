# Not Supported Yet

[中文](not-supported-yet.zh-CN.md) | English

This page lists the things that are **deliberately not in 2.0.0**, and the known boundaries. When you
hit "why does this line not parse", look here first.

## There is no declarative page section

Pages can only come about like this:

- the `layout` given when the menu is created (it is always page 1);
- `insert page …` (the effect or the section form).

There is **no** `page:` section, and no way to "declare a set of pages for a menu". One page = one
layout + one title, and that is deliberate: a page is not an object that needs long-term upkeep, it is
shorthand for "layout + title".

## There is no syntax for deleting a page

There is no `delete page` / `remove page`. To have fewer pages, the only way is to destroy the whole
menu and build it again as needed:

```skript
on load:
    # Destroy before rebuilding, and the page count starts again from the layout it was created with.
    destroy the menu with id "main"
    create a phantom menu with chest inventory titled "Rebuilt" with layout "#########" with id "main":
        insert page with layout "#########" with title "Second Page"
```

`insert page N` can change the page order (inserting at position N, with the rest shifting along), but
**moving a page away** cannot be done.

## There is no `set slot N of {_menu}` and the like

Only two pieces of syntax change a slot's contents:

```skript
override slot 10 in page 1 to diamond for menu with id "main_menu"
map key "A" to icon stone for {_menu}
```

`set slot 4 in page 1 of menu with id "main" to diamond` **does not parse** (it is neither an effect
nor a condition). The `set slot …` in older documentation never belonged to this plugin.

## The old `gui` syntax is removed

`create a gui …`, `%players% (has|have) a gui [open]` and `the player's gui` come from the earlier
skript-gui plugin, and 2.0.0 **removes** them rather than fixing them. The comparison table:

| Removed | Write this instead |
|---|---|
| `create a gui with {_inv} with id "main":` | `create a phantom menu with chest inventory titled "…" with layout "…" with id "main":`, or a `build a menu:` section |
| `the player's gui` | `the menu of the menu session of player` |
| `if player has a gui open:` | `if the menu session of player is set:` |
| `create a gui … with removable items` | `create a static menu …`, or `mode: static` in a `build a menu` |

The old `create gui` numbered pages from 0 and crashed as soon as the inventory passed to it contained
an item; the mode it hard-coded (`removable items`, that is `static`) now has a proper spelling.

## Some container types have no window

`composter`, `chiseled bookshelf`, `decorated pot`, `shelf` and `jukebox` are not "not done yet", they
**cannot** be done: Minecraft has no menu for them, so there is no window for the client to draw (Bukkit's
inventory types for them have no menu type and cannot be created either). `create menu` can only report
`Unsupported inventory type`.

The player's own 2×2 crafting grid (`crafting table inventory`) is the same story: the window the client
draws for it is a crafting table, with a different slot count, so it is not supported either — use
`workbench inventory` for a crafting-table window.

## No commands, and no permissions

The plugin registers no commands and no permission nodes. Every entry point is Skript syntax, and the
script itself decides how menus are opened and closed, and what the buttons do.

## The dependencies are hard

- The server must be **Paper 26.2** or a compatible fork: `plugin.yml` declares `api-version: 26.2`,
  and an older server refuses to load outright.
- **Skript 2.16.2** and **PacketEvents 2.13.0** are `depend`; without them the plugin does not enable.
- **SkBee** is a `softdepend`: only the "use a text component as a title" path needs it, see
  [Titles](titles.md).

## How far clicks and items are verified on a real client

Clicks on the server side have always been verified; the real-client layer (`clientTest`) now verifies
more than "the window title changed":

- **Items**: the icon types of the four keys on page 1 (`stone` / `diamond` / `emerald` / `clock`), the
  `apple` on page 2, and the `custom_name` on every icon are all read out and asserted by the client
  itself. The type is the stronger of the two — it says "which item the menu mapped to this key", while
  `custom_name` only says "what name this item was given", so an implementation that maps the wrong item
  but copies the name can pass the latter and not the former. The name is still kept, because it is the
  only piece of identity that does not depend on the version's registry.
- **Slot alignment, page numbers and titles**: the slot number the client sees and the clicked slot
  `11-client.sk` reports have to agree, and the title and the items after a page turn are asserted on
  the client side too.

**But this is not the same as "a real 26.2 client saw these packets".** The client's
mineflayer/minecraft-data only goes up to 26.1 (protocol 775), while the server is 26.2 (protocol 776),
so at this layer `clientTest` installs ViaVersion + ViaBackwards: the client truthfully declares itself
26.1, the server translates 26.2 into 26.1 before sending, and what the client reads is the
**translated view**. So what this layer proves is:

- the plugin really sent these packets — Via only translates what it is given, and a packet the plugin
  did not send cannot appear here;
- packet headers/fields that only 26.2 has are still parseable by a real client after translation, and
  the item registry is translated back to 26.1, so the types can be asserted.

What it **cannot** prove is that the bytes a real 26.2 client sees directly are correct. The shape Via
rewrote and the plugin's original 26.2 shape are the same thing at this layer, so "the plugin got the
26.2 bytes right" and "Via can translate the bytes the plugin sent" cannot be told apart here;
conversely, Via's own translation problems look like the plugin's problems at this layer. Telling them
apart needs an actual 26.2 client.

Client behaviour that is not covered yet:

- **"Ghost items" that exist only on the client** (in `static` mode, where the server did not change a
  slot but the client moved it itself).
- **Dragging**: `clientTest` only does single clicks, Shift clicks, number keys and page turns; there
  is no drag across slots.
- **A 26.2 client itself**, see above.

When you find a mismatch, report it; do not say "it probably works".

## Some boundary behaviours

These are not bugs, but they are surprising the first time:

- **The click cooldown silently drops clicks.** When the gap between two clicks is smaller than
  `click delay`, the event does not fire and the slot callbacks do not run, with no message at all. The
  default is 50 milliseconds for `create menu` and 5 milliseconds for `build a menu`.
- **Without `in page`, `override slot` uses the "default page"** (`with page N` / `default page:`), not
  a hard-coded 1. When the default page is greater than the number of pages that exist, it reports
  `Page N does not exist in this menu.` at runtime. If you built only one page but set `with page 2`,
  write `in page 1` explicitly.
- **Without `on page`, `map key` applies to every page**, including pages added later with
  `insert page`. To apply it to one page only, write `on page 1`.
- **`the pressed number key` has a value only on a `number key` click**; on any other click it is
  "no value".
- **`the future page` has a value only in `on page turn`**; in `on menu open` / `on menu interact` it
  is nothing.
- **A menu without an id does not appear in `all menus` / `all menu ids`.**
- **Skript reloading scripts destroys every menu**, and players are thrown out of their windows;
  building them again in `on load` is enough.
- **`the viewers of …` no longer points at this plugin on 2.16.** Skript registers a `viewer[s]`
  property of its own, declared on any object, and registers it before the plugin, so this form is
  answered by Skript and returns empty. For a menu's viewers write `the menu viewers of {_menu}` (or
  `all players viewing {_menu}`).
- **With SkBee installed, `the id of {_menu}` can be taken by SkBee's `id of %bounds%`** (a variable of
  undecided type matches it too). If you want a menu id and have SkBee, use the **look a menu up by
  id** side, or store the menu in a variable and keep the id yourself.
