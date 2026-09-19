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

### Configuration actually applies

`enable-async-check` and `force-truecolor` were read from Spigot's own configuration, so the values in
this plugin's `config.yml` had no effect at all. They are read from `plugins/xiaojie-gui/config.yml` now,
which is created on first start. No keys were renamed.

### Behaviour that users can see

- `cancel event` inside `on menu open` and `on page turn` now works. One of the two ignored the argument
  and cancelled regardless, the other never cancelled.
- `viewers of menu` no longer keeps reporting players who left that menu for another one.
- Static menus no longer send the whole window twice on every page turn, and a menu closed by the player
  is not reopened by a title update that was still in flight.
- `expr` used outside a menu event, a page number out of range, or a null menu now report something
  readable instead of throwing inside Skript (`Page 0 does not exist`, `index out of bounds`, …).
- `update title of page N` retitles only the players looking at page N. It used to retitle the window of
  every viewer of the menu, so a player on another page saw that page's title until they turned a page.
- `open menu {_menu} for player` for a player who already has that menu open now goes to the page you
  asked for instead of failing with `already viewing this menu`.
- `barrel`, `ender chest`, `dispenser`, `enchanting` and `cartography` menus can be created; they used to
  report `Unsupported inventory type`, because no layout existed for them. Skript's names for the last
  two are `enchanting table inventory` and `cartography table inventory`, not `enchanting inventory` and
  `cartography inventory`.

### Known issues

- Clicks are tested on the server side. Anything that depends on the client — how slots line up, items
  that appear to exist client-side only, dragging — is verified separately with a real client.
- `insert page` and `with page` are the only ways to create a page; there is no declarative page block.
  A page is a layout plus a title, and that is deliberate.

### Reliability

The addon now has three test layers, and the third one is new: unit tests, and a real Paper 26.2 server
that is booted with Skript and PacketEvents, runs a set of Skript scripts against the addon, and has its
log checked. That layer is what found the title crash above, the page contract and several pattern
problems — failures that a server otherwise reports as one line of prose while it happily keeps running.

Two pieces of the console output no longer go through Skript's or Adventure's internals:

- The syntax is registered through Skript's addon API instead of the static `Skript.register…` calls,
  which Skript 2.16 deprecated for removal. Nothing a script writes changes; what changes is that the
  registration no longer depends on calls that are on their way out.
- The banner writes its own colour codes instead of reaching into Adventure's ANSI serializer through
  reflection. That reflection could fail while the plugin was enabling — a bad moment for a banner — and
  a console Paper had judged colourless showed the banner grey. `force-truecolor: false` still prints it
  without colour.
