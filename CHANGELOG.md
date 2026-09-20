# Changelog

## 2.0.0

This release moves the addon to Minecraft 26.2 / Skript 2.16 and fixes the menu syntax that did not work.
Most of it is invisible in day-to-day use; the sections below say what a script has to change and why.

### Requirements

| | Before | Now |
|---|---|---|
| Server | Paper 1.21.8 | **Paper 26.2** |
| Skript | 2.13.1 | **2.16.2** |
| PacketEvents | 2.9.5 | **2.13.0** |
| Java | 21 | **25** |

Skript 2.16.2 supports Paper up to 26.2, and Paper 26.2 itself needs Java 25. The plugin declares
`api-version: 26.2`, so a server older than that refuses to load it instead of failing later.

### The title syntax used to crash, and the forms it accepts

Every syntax that takes a menu title — `create menu … titled …`, `insert page … with title …`,
`update title …`, `update session title …`, `turn to page … with title …` — threw an internal error the
moment a script ran it, so a menu created that way was never actually created. That is fixed.

The accepted forms are simply the value:

```skript
create a phantom menu with chest inventory titled "Main Menu" with layout "#########" with id "main":
update title of page 1 in {_menu} to "Page One"
```

Do **not** write `string:"Main Menu"` or `component:"Main Menu"`. Those prefixes came from old
documentation and are not part of the syntax: they are markers this addon uses inside its own patterns,
where the input is only what follows the colon. Writing them makes the *whole* text, prefix included,
the title — and 2.0.0 says so out loud instead of rendering a title that starts with `string:`.

A title is text, so colour codes in it are formatting: `&a`, `&l` and the rest work the way they work
everywhere else in Skript, and `§` works as well. The title reaches the client as a text component
rather than a plain string, which is why it carries colour and formats at all. Hex colours work in
Adventure's `&x&f&f&5&5&0&0` form, where each digit is written with its own `&`; `&#ff5500` is not part
of that syntax.

One consequence, and it is the same one item names have always had: an `&` that is followed by a code
letter is read as a code. A title that must show a literal `&B` cannot say so with `&`, exactly as in
Spigot.

### Pages

- **Pages are numbered from 1.** `page 0` no longer exists anywhere, and old documentation that used it
  was wrong: `in page 1` is the first page.
- **The layout you create a menu with always becomes page 1.** `with page N` (or
  `default page`) only decides which page `open menu` shows first. Before, setting it skipped creating a
  page at all, and a menu with no pages cannot be opened — so `create menu … with layout … with page 2`
  used to produce a menu that refused to open.
- `insert page 1 …` puts a page *in front of* page 1 (your layout page becomes page 2); `insert page 2 …`
  puts it after page 1; `insert page …` without a number appends.
- A menu is a single page unless you add more. Paging is a feature you opt into, not something you have
  to think about: keep one page and swap its contents with `override slot` / `map key` if that is all you
  want.

`update title of page N …` is meant to retitle what the players looking at page N see. It currently
retitles the window of every viewer of the menu, whichever page they are on — see Known issues.

### Naming a menu in a syntax

The literal word `menu` is used where it reads naturally, and it is optional inside optional groups, so
these now parse:

```skript
map key "A" to icon stone for {_menu}
override slot 2 in page 1 to diamond for {_menu}
set {_slots::*} to the slots of key "A" in page 1 of {_menu}
set {_key} to the key of slot 1 in page 1 of {_menu}
```

and `destroy` accepts the id form, which used to need the word `menu` twice:

```skript
destroy the menu {_menu}
destroy the menu with id "main"
```

### The old `gui` syntax is gone

`create a gui …`, `%players% (has|have) a gui [open]` and `the player's gui` came from the older
skript-gui addon. They were never maintained and they did not follow the rest of the addon: `create gui`
indexed pages from 0, so it crashed as soon as the inventory it was given had an item in it, and the mode
it hard-coded (static, for `removable items`) has a proper form now. They are removed rather than fixed,
so a 2.0.0 script uses the menu syntax:

| Removed | Use instead |
|---|---|
| `create a gui with {_inv} with id "main":` | `create a phantom menu with chest inventory titled "…" with layout "…" with id "main":`, or the `build a menu:` section |
| `the player's gui` | `the menu of the menu session of player` |
| `if player has a gui open:` | `if the menu session of player is set:` |
| `create a gui … with removable items` | `create a static menu …`, or `mode: static` in `build a menu` |

### A menu's mode is text

`the mode of {_menu}` returns `phantom` or `static` — the same two words `build a menu` takes in its
`mode:` entry — so it compares as ordinary text:

```skript
if the mode of {_menu} is "phantom":
```

The `receptaclemode` type is gone with it, and so is a trap worth knowing about: Skript parses a literal
for an addon's enum from its *language files*, which an addon has to ship as a versioned `default.lang`.
An addon without one leaves the generated fallback as the only literal that parses — here
`static receptacle mode` — and the bare word `phantom` belongs to Minecraft's phantom entity, so
`is phantom` silently compared against an entity type and never matched. Text needs none of that
machinery.

The same trap was waiting in `on menu interact`. The addon used to publish its own `menu click type` and
`menu click mode` types, and neither could be named in a script; what they printed was `LEFT`. They are
gone. The click type a script sees is Skript's own click type — the one `on inventory click` hands out,
with Skript's words and its literals:

```skript
on menu interact:
    if the event-clicktype is left mouse button:
        send "Left click!" to player
```

That works through the converter the addon registers from its own, richer click type (which knows the
button and the click mode) to Bukkit's: Skript resolves an event value of one type through registered
converters. Registered as a type of our own, the same value was only the worse name for it.

Inside `on menu interact` both `the click type` and `the event-clicktype` work, and they are the same
value. Skript's own `the click type` belongs to its `on inventory click` event and refuses to parse
anywhere else, but the addon registers the property for its own event, so the plain form is answered
here.

The addon's own event values are written the way a script says them, with no `event-` prefix — `the
clicked slot`, `the clicked icon`, `the page`, `the pressed number key` — and the `event-` forms stay as
aliases for scripts that already used them.

One thing Skript's click type cannot carry is *which* number key was pressed: all nine of them arrive as
the single `number key`. That is what the addon's nine `NUMBER_KEY_*` types are for, so the digit is
readable on its own:

```skript
on menu interact:
    if the pressed number key is 1:
        send "You pressed 1!" to player
```

`the pressed number key` is 1 to 9 for a `number key` click and nothing for every other click, which is
what a menu needs to use the keys as shortcuts.

### There is no configuration

`enable-async-check` and `force-truecolor` are gone, and so is the `config.yml` they lived in. The plugin
has no commands, no permission nodes and no options, so it writes no file into `plugins/xiaojie-gui/`:
what is there is what a script put there. The main-thread check cannot be turned off, because a menu
operation on an asynchronous thread is never legal, and the banner asks the console whether it can print
colour instead of being told. `docs/guide/configuration.md` is where both are described now.

### Behaviour that users can see

- `cancel event` inside `on menu open` and `on page turn` now works. One of the two ignored the argument
  and cancelled regardless, the other never cancelled.
- `the page` inside `on menu interact` reports 1 for the first page. It used to report the page's 0-based
  position in the menu's list, so it disagreed with every other page number a script sees.
- `viewers of menu` no longer keeps reporting players who left that menu for another one. The syntax is
  `the menu viewers of {_menu}` now: Skript 2.16 registers a `viewer[s]` property of its own over any
  object and registers it before this addon, so `the viewers of {_menu}` was answered by Skript and
  returned nothing. `all players viewing {_menu}` is unchanged.
- Static menus no longer send the whole window twice on every page turn, and a menu closed by the player
  is not reopened by a title update that was still in flight.
- `expr` used outside a menu event, a page number out of range, or a null menu now report something
  readable instead of throwing inside Skript (`Page 0 does not exist`, `index out of bounds`, …).
- `update title of page N` retitles only the players looking at page N. It used to retitle the window of
  every viewer of the menu, so a player on another page saw that page's title until they turned a page.
- `open menu {_menu} for player` for a player who already has that menu open now goes to the page you
  asked for instead of failing with `already viewing this menu`.
- `barrel`, `ender chest`, `dispenser`, `enchanting`, `cartography` and `crafter` menus can be created;
  they used to report `Unsupported inventory type`, because no layout existed for them. Skript's names for
  two of them are `enchanting table inventory` and `cartography table inventory`, not `enchanting
  inventory` and `cartography inventory`.
- The fixed-size layouts now count the slots the client really draws for that window: `workbench` has 10
  (not 9), `beacon` 1 (not 3), `enchanting table` 2 (not 3), `lectern` 1 (not 2), `smithing` 4 (not 3) and
  `cartography table` 3 (not 4). A count that is off does not report anything — it moves the player's
  inventory and every click in the window by a few slots. A unit test now holds every layout to Bukkit's
  own size for its inventory type, so this cannot drift again.
- `crafting table inventory` (the player's own 2×2 grid) is no longer a layout. No client menu draws that
  window, so it came out as a crafting table whose 10 slots were mapped onto our 5, and Bukkit refuses to
  create that inventory at all. `workbench inventory` is the crafting-table window.
- `create a static menu with merchant inventory ...` now stops with a sentence naming the type instead of
  Bukkit's own exception. Merchant windows come from the merchant API and only work in `phantom` mode.
- Dragging works. A drag used to reach the addon only when it happened to touch a single slot, which the
  game itself delivers as a click; a real drag, the kind that spreads a stack over several slots, changed
  the window with `on menu interact` never running, so a shop could not refuse it and a backpack could not
  see it. A drag is now one interaction, with every slot it reached in `the dragged slots` and the stack
  the player carried in `the cursor item`, and `cancel event` refuses all of it at once — the protocol
  cannot apply half a drag. Every detail, including why a drag can only put items and never take them, is
  in the new wiki page *How drags work*.
- A click in the player's own half of a `static` window is reported with the window slot the client used.
  The old number was relative to that half, so it named a container slot the player never touched and ran
  a callback for it.
- The click a script is told about is the click the player made. Reading the event's *action* as well as
  its click type reported a `Q` drop in a slot as a click on the background, and a click on the background
  as a drop; `the click type` is now what Skript reports for the same gesture, inside the window and out.
- A container click the addon has no name for is dropped rather than thrown from the packet listener. The
  window a `phantom` menu shows is the server's own, so an unimplemented click should do nothing instead of
  filling the log with stack traces.
- A `phantom` menu puts the whole window back for the clicks that can move an item between slots, and that
  set no longer includes the drop keys: a `Q` drop takes the item out of the window, so the slot it names
  is the only one that changed.
- `with locked icons` (and `locked icons: true` in `build a menu`) makes the slots a layout gives an icon
  to the menu's own: no click, shift click, number key, offhand swap, drop or double-click collect can take
  from them or put anything into them, and a drag that touches one is refused as a whole. The empty slots
  stay the player's, so a shop and a backpack are the same mechanism, and a lock does not stop a slot
  callback: a shop's "buy" callback still fires while the goods stay exactly where they are. `override
  slot` counts as the menu's furniture and `set icon in slot N of {_session}` as that player's own content,
  which is the difference between a shop's goods and a backpack's items. The player's own half is only
  guarded where it would reach the menu: a shift click is refused when a matching stack in one of the
  menu's own slots could take the item, and a double click when one of them holds it, so a shop that buys
  from the player can still have him shift things into its buying area.
- A page turn clears only the slots the page being left owns, instead of the whole window. Clearing
  everything threw away whatever the player had put in a slot the layout left empty, which is to say it
  made a paging backpack impossible.
- The same switch can be flipped while the server runs: `lock the icons of %menu%` /
  `unlock the icons of %menu%` set it (inside `edit menu …:` or anywhere else), and
  `if the icons of {_menu} are locked:` reads it. It takes effect on the next interaction.
- A player who quits, dies or changes world with a `static` menu open leaves no inventory behind any more.
  Their entry used to stay in memory with the items in it for as long as the server ran, one per player.
- `hide player inventory` on a `static` menu now warns once when the menu is created. That window is the
  player's real inventory, so the flag cannot hide anything, and a menu that silently looks wrong is worse
  than a line in the console.
- A page can be a **list**: map a list of items to one key (`map key "L" to icon {results::*}`) and the cells
  of that key become the page's result cells. `set the menu list of %menusession% to %itemstacks%` fills them
  for one player -- extra items dropped, leftover cells cleared, the window updated **once** instead of once
  per cell -- and `the menu list index` is which result was clicked, 1-based, empty for a slot that is not
  one of them. That is what makes an item browser a layout plus one call plus one index, and it is why the
  two things a browser still cannot do (a page count that shrinks, and typing inside a window) are written
  down in the guide instead.
- What a player has open is called a **window** in everything a script reads: `the menu window of player`,
  and `the event-menu window` inside a menu event. `the menu session of player` and `session` still parse and mean the
  same thing, so no script has to change, and the type's own description no longer reads like a database
  manual. The guide's page on it now opens by saying what a window is -- and that most scripts never name
  one, with a table of the things that need it and the many that do not.
- With `hide player inventory` in a `phantom` menu, the 36 slots below the container are the **menu's own
  space**, not the player's: nothing is mirrored into them, a script can put icons there and clicks on them
  arrive like any other slot's. That was already true of what a script wrote into them, and it is now also
  true of what a script reads back out: `a phantom window reads the player's own half live` must not read
  the player's inventory there.
- `player inventory layout` is now **`player layout`**, and it is the layout of the rows below the
  container rather than of a display copy of the player's inventory. Those rows are the menu's own only
  while the player's inventory is hidden, so **giving a player layout hides it**: one keyword says "this
  half is mine", and the layout can no longer quietly do nothing. Under the old rule it described a copy of
  the player's rows, which the player's own items were drawn over -- that half was neither the player's nor
  the menu's. `with hide player inventory` on its own still means "that half is hidden and empty", and on a
  `static` menu a player layout warns once and changes nothing. A layout belongs to a page and the half
  belongs to the menu, so writing one on any page hides it for the whole menu. The half is one thing or the
  other and never both: `show player inventory of %menu%` on such a menu is refused (with a line saying what
  to do instead), and with the inventory shown the rows below the container are the player's items and
  nothing else -- an icon a page laid out for them is cleared before the window is sent, so it can no longer
  appear in a cell the player happens to leave empty.
- A `phantom` window reads the player's own half live instead of copying their whole inventory on every
  read, which was the one hot spot the performance pass found.
- Two runnable examples now ship in `server-test/`: a list-page browser (`14-browser-list.sk`) and a backpack
  saved on close and put back on open (`15-backpack-persist.sk`). `serverTest` parses every script there on
  every run, so they cannot quietly stop working -- and it already caught a bad loop in the first one.
- `the occupied slots of %menusession%` and `the menu contents of %menusession%` read a window back: the
  slots that hold something, and those items in slot order. Together with `icon in slot N of {_session}`
  they are how a menu that keeps things in its free slots is saved and put back.
- `/skript reload` closes every menu before the new scripts load. A menu holds the callbacks of the scripts
  that built it, so a menu that survived a reload answered the next click with code that was no longer in
  any script. The reloaded scripts build their menus again, as they do on start.
- `on menu close` now fires when a window ends **without** the player closing it -- quitting, dying, changing
  world -- and it fires while the container is still there. Those three paths used to drop the window in
  silence, so a backpack that saves on close lost everything a player was carrying when he left; a unit test
  now pins that order for all four paths.

### Known issues

- Clicks, slot alignment, titles and item types are covered by a real client now (`./gradlew clientTest`):
  a Minecraft client joins the test server, reads the window the server opens for it, clicks in it, and the
  item in each slot is checked by *type*. The client is a 26.1 client and the test server runs
  ViaVersion + ViaBackwards for it, so what that proves is that the packets this addon emits survive a real
  translation layer to a real client — not that a 26.2 client sees the same bytes. Drags and `locked icons`
  are covered there as of this version (the bot writes raw drag packets, which mineflayer cannot build), so
  what stays uncovered is client-side ghost items and two clients side by side.
- `composter`, `chiseled bookshelf`, `decorated pot`, `shelf` and `jukebox` are not supported, and cannot
  be: Minecraft has no menu for them (Bukkit's inventory types for them have no menu type and cannot be
  created), so there is no window to draw. `create menu` reports `Unsupported inventory type` rather than
  guessing a menu shape.
- `insert page` and `with page` are the only ways to create a page; there is no declarative page block.
  A page is a layout plus a title, and that is deliberate.

### Reliability

The addon has four test layers now. Unit tests cover what needs no server; a real Paper 26.2 server is
booted with Skript and PacketEvents, runs a set of Skript scripts against the addon and has its log
checked; every `@Examples` annotation is parsed by that same server, so a documented example that Skript
cannot read fails the build; and `./gradlew clientTest` connects a real Minecraft client, which reads the
window the server opens for it and clicks in it.

What the layers found is the reason they exist: the title crash above, the page contract, several pattern
problems, `the page` reporting a 0-based index in `on menu interact`, and `the viewers of` being claimed by
Skript's own property. A failure Skript reports in prose while the server keeps running is exactly what a
build has to notice.

Two pieces of the console output no longer go through Skript's or Adventure's internals:

- The syntax is registered through Skript's addon API instead of the static `Skript.register…` calls,
  which Skript 2.16 deprecated for removal. Nothing a script writes changes; what changes is that the
  registration no longer depends on calls that are on their way out.
- The banner writes its own colour codes instead of reaching into Adventure's ANSI serializer through
  reflection. That reflection could fail while the plugin was enabling — a bad moment for a banner — and
  a console Paper had judged colourless showed the banner grey. The banner now asks the console itself --
  the `net.kyori.ansi.colorLevel` system property -- and prints without colour when the answer is `none`.
