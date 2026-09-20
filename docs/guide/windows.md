# Windows

[中文](windows.zh-CN.md) | English

A **menu** is the definition (its pages, layouts and icons, shared by everyone). A **window** is the one
copy a player sees after opening it: which page he is on, what his title says, what is in every slot he
looks at. One menu can be open for ten people at once, each on a different page with different contents --
that is why windows exist.

The word a script uses is **window**: `the menu window of player`. The type itself is still `menusession`
(its noun is `menu window`), so `session` parses as well and means the same thing; this page says window
throughout.

**Most scripts never need to name one.** Naming a window is for exactly one job: changing what **one player**
sees.

| What you want | How | Window needed? |
|---|---|---|
| Do something after a click or a drag | `the clicked slot` / `the dragged slots` in `on menu interact:` | no |
| Open a menu for a player, or close it | `open menu {_menu} for player` / `close the menu for player` | no |
| Turn a player's page | `turn to page 2 for player` | no |
| Change the shelf everybody sees | `override slot 5 in page 1 to X for {_menu}` | no (that is the **menu**) |
| Change **this player's** slots, title or list | `set icon in slot 5 of the menu window of player to X` and the like | yes |

## Getting a window

```skript
set {_window} to the menu window of player
if {_window} is not set:
    send "You do not have a menu open right now." to player
```

`the menu window of %player%` (also written `the menu session of %player%`) returns **no value** when he has
no window. In Skript, testing it means going through a variable and then `is not set` (`is not null` is not
how Skript writes that condition).

Inside a menu event, the window that event is about is **`the event-menu window`**. The `event-` prefix
cannot be left out here: that is the form Skript registers the event value under, and `the session`,
`the window` and `the menu window` are not expressions at all -- writing one only earns a "can't understand
this expression". Asking by player works too, since `player` inside an event is the one it happened to:
`the menu window of player` and `the event-menu window` are the same window.

## What a window has on it

`{_window}` below is either `the menu window of player` or `the event-menu window` from an event.

| What you want | How to write it |
|---|---|
| which player this window belongs to | `the viewer of {_window}` |
| which menu this window belongs to | `the menu of {_window}` |
| which page this window is on | `the page of {_window}` |
| this window's title | `the title of {_window}` |
| the item in a slot of the window | `icon in slot 5 of {_window}` |
| the slots of its container that hold something | `the occupied slots of {_window}` |
| everything its container holds, in slot order | `the menu contents of {_window}` |
| a player's window, in one step | `the menu window of player` |

The other way round, you can also get the page number straight from the player:
`player's current menu page`.

The last two are what saving a window is made of: `the occupied slots of {_window}` says which slots hold
something, and `the menu contents of {_window}` is those items themselves, in slot order. Both read the
**container** only -- the slots the layout describes -- because the lower half of a `static` menu belongs to
the player. Saving a backpack is then:

```skript
on menu close:
    loop the occupied slots of the menu window of player:
        set {backpack::%loop-value%} to icon in slot loop-value of the menu window of player
```

The `menu` in the name is not decoration: Skript's own `contents of %inventory%` is registered before this
addon, so a shorter name would be answered by Skript, exactly as with `menu viewers`.

## Changing what one player sees

Titles and icons on a window affect **that one player only**; that is the whole of how "each player sees
different contents" is implemented.

```skript
on menu interact:
    if the clicked slot is 4:
        # retitle for the one who clicked
        update title of the event-menu window to "&aYou clicked the middle!"
        # change one slot for the one who clicked
        set icon in slot 5 of the event-menu window to red stained glass pane
```

- `update title of [the] [menu] [(session|window)] %menusession% to "…" [and refresh]` — without
  `and refresh` it only changes the model, and the screen does not change right away.
- `set icon in slot %numbers% of %menusession% to <item>` — pushed to that player immediately (here
  `set` refreshes even without `refresh`, because it goes through the window's `setIcons(…, true)`).
- `delete icon in slot %numbers% of %menusession%` — clears those slots.
- It can also be written `set the icon of %menusession% in slot %numbers% to …`.

An icon on a window only changes "the copy this player sees" and **does not touch the menu itself**:
everyone else still sees the original. A page turn re-draws that page into the window, so a temporary icon
in a slot the page owns is overwritten by it; a `refresh` is not -- it re-sends what the window *currently*
holds, so the icon stays. To change it for everyone, use `override slot` / `map key` (see
[Filling Menus](filling-menus.md)).

Both `set the title of {_window} to "…"` and `update title of the menu window … to "…"` change the
title; the first changes the value directly, the second adds an `and refresh` switch.

## Operations on a window

```skript
close the menu window of player
close the menu window {_window}

refresh the slot 5 in menu window {_window}
refresh the menu window {_window}

clear the menu window of player
clear the menu window of player and refresh
```

| Syntax | What it does |
|---|---|
| `close [the] [menu] [(session|window)] %menusession%` | closes this window, it disappears, and `on menu close` fires |
| `refresh [the slot %-number% in] [the] [menu] [(session|window)] %menusession%` | resends contents: with a slot it sends only that one slot, otherwise the whole window |
| `clear [the] [menu] [(session|window)] %menusession% [and refresh]` | empties the window's contents without **closing** it |

`refresh` does nothing in `static` mode — there the contents are held by a real server-side inventory,
and the client sees the latest state on its own.

The menu can also be operated from the player's side: `close the menu for player` (equivalent to
closing his window; when he has none it reports a readable one-line error).

## Everyone

```skript
set {_viewers::*} to the menu viewers of {_menu}
loop the menu viewers of {_menu}:
    send "You are looking at a %the inventory type of {_menu}% menu right now." to loop-player
broadcast "There are currently %size of the menu viewers of {_menu}% people looking at this menu."
```

`the menu viewers of {_menu}` returns the players **currently looking at** this menu. Closing it
themselves, switching to another menu or leaving the server takes them out of it; that holds in
`static` mode too.

**Why it is called `menu viewers` and not `viewers`**: Skript 2.16 registers a `viewer[s]` property of
its own, declared on **any object**, and registers it before the plugin, so `the viewers of {_menu}` is
answered by Skript and returns nothing (one of the forms that silently stopped working after upgrading
to 2.16). Skript has no name `menu viewers`, so it is reliably available.

Other forms: `{_menu}'s menu viewers`, `all players viewing {_menu}`.

Note that this is a snapshot: if someone closes the menu after you took the list, `loop` still goes
through him once. To be sure he is still there, check `the menu window of loop-player` again inside
the loop.

## When a window goes away

| Situation | What happens |
|---|---|
| the player closes the window | `on menu close` fires and the window ends |
| `close the menu …` / `destroy the menu …` | the same; `destroy` closes everyone who is looking at this menu |
| the player switches to another menu | the old menu fires `on menu close` once, and he is no longer in the old menu's `viewers` |
| the player leaves the server | the window is cleared |
| the plugin is disabled / Skript reloads scripts | **every menu is destroyed** and every player is thrown out of his window |

The last one matters: a menu holds callbacks from the script that built it, and after a script reload
those callbacks point at triggers that no longer exist. That is why the plugin destroys every menu in
`ScriptLoader.ScriptPreInitEvent`, registered through Skript's lifecycle event registry, before the batch
of scripts is parsed. The scripts build them again when they reload — which is why menus are almost
always built in `on load:`.

Building the same menu again with a fixed id inside `on load` is safe: a new menu with the same id
**destroys** the old one, so two of them never coexist.
