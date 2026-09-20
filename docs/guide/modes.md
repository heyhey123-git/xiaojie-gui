# The Two Modes: `phantom` and `static`

[中文](modes.zh-CN.md) | English

Every menu has a mode, and there are only two values: `phantom` and `static`.

## How to write it

**When creating** (without it, `phantom`):

```skript
create a phantom menu with chest inventory titled "Virtual Menu" with layout "#########" with id "phantom_menu":
create a static menu with hopper inventory titled "Real Menu" with layout "AAAAA" with id "static_menu" with 250 ms click delay without hide player inventory:
```

**The entry form** uses `mode:`:

```skript
build a menu {_menu}:
    mode: phantom
    inventory type: chest inventory
    title: "Main Menu"
    layout: "AAA", "ABA", "AAA"
```

`mode:` accepts only the two words `phantom` and `static`; anything else reports
`Invalid menu mode: … Must be one of: static, phantom.`

**Reading it back** gives **plain text**, so a text comparison is all you need:

```skript
set {_mode} to the mode of {_menu}
if the mode of {_menu} is "phantom":
    send "This menu is in phantom mode." to player
```

`the mode of {_menu}` returns `"phantom"` or `"static"`.

**Do not** write `if the mode of {_menu} is phantom:` (without quotes). In Skript an enum constant's
literal comes from the plugin's language file, which this plugin does not ship, so the only thing that
parses is a generated name like `static receptacle mode`; and bare `phantom` is Minecraft's phantom
entity, so `is phantom` quietly compares against the **entity type** and never holds. Text has no such
mechanism, which is why the mode is text. The same trap applies to the old `receptaclemode` /
`menu click type` types — both were removed in 2.0.0.

## What the two modes mean for the player

| | `phantom` (virtual) | `static` (real) |
|---|---|---|
| what is underneath | a window the plugin draws itself with packets | an inventory the server really creates |
| the items inside | **fixed**: cannot be picked up, dragged or put in | **can be moved, swapped and dropped normally** |
| the player's mouse | only "a click"; the item always stays where it is | ordinary behaviour in a chest |
| the player inventory area | described by `player inventory layout`, a **read-only mirror** | the player sees his own real inventory |
| refreshing one slot on its own | supported | not supported (`refresh` does nothing) |
| changing an icon for one player | supported | supported (session icons live on the window layer) |

In one line: **`phantom` is a "button board", `static` is a "real chest".**

Almost every UI menu uses `phantom` — it is designed for "a row of buttons, a click does one thing",
and a player cannot drag the decorative blocks out of it. Use `static` only for a container the player
sorts and stores items in (a custom vault, a trade window).

## Behaviour specific to each

**`phantom`**

- Items **cannot be moved by the player**. When the player does pick an item up (drags it onto the
  cursor, takes it to craft), the plugin resends the affected slot, and it looks like "it was just
  picked up and snapped back" — that is how "it does not move" is implemented, and it is where
  `phantom` costs a little more performance than `static`.
- The player inventory below is a **display copy** laid out with `player inventory layout`, and what
  the player does there does not really change his inventory. So a `phantom` menu is not suited to
  interactions that carry items around.
- `hide player inventory` only affects "whether those 4 rows below are there"; it does not affect the
  container part above. **With them hidden those 36 slots are not the player's, they are the menu's own
  space**: nothing is mirrored into them, a script can put icons there with
  `set icon in slot N of {_window}`, and clicks on them arrive like any other slot's. `player inventory
  layout` describes the **display copy when it is shown**, so it does nothing while they are hidden.

**`static`**

- The player moving items **really** happens on the server-side inventory, so slots that
  `override slot` changed can be carried off by the player too.
- `refresh the menu session …` is a no-op (the client syncs the contents itself anyway).
- Clicks are still judged against the menu's cooldown (`click delay`), and `on menu interact` still
  fires.

## The cooldown: both modes have it

`with %number% ms click delay` / `click delay:` is the minimum interval between two clicks; the
default is **50 milliseconds** for `create menu` and **5 milliseconds** for `build a menu`. A click
that comes too early is **silently dropped** (the event does not fire and the slot callbacks do not
run) — when you are chasing "I clicked and nothing happened", look at this value first.

```skript
create a static menu with hopper inventory titled "StaticSelftest" with layout "AAAAA" with id "static_selftest" with 250 ms click delay without hide player inventory:
```

Reading it back: `the minimum click delay of {_menu}`.
