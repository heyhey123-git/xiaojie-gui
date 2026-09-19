# Sessions

[中文](sessions.zh-CN.md) | English

A **menu** (the menu itself) and a **session** (one player's one opening of that menu) are two
different things. The menu is a shared definition; the session is each person's own state: which page
he is on, what his window title is, and what is in every slot he sees.

One menu can be open for ten people at once, each on a different page, each seeing different contents
— that is why sessions exist.

## Getting a session

```skript
set {_session} to the menu session of player
if {_session} is not set:
    send "You do not have a menu open right now." to player
```

`the menu session of %player%` returns **no value** when there is no session. In Skript, testing it
means going through a variable and then `is not set` (`is not null` is not how Skript writes that
condition).

Inside a menu event, `the session` is it directly, with no lookup needed.

## What a session has on it

| What you want | How to write it |
|---|---|
| the player the session belongs to | `the viewer of {_session}` |
| the menu the session belongs to | `the menu of {_session}` |
| which page the session is on | `the page of {_session}` |
| the session window's title | `the title of {_session}` |
| the item in a slot of the session | `icon in slot 5 of {_session}` |
| the player the session belongs to (short form) | `the menu session of player` |

The other way round, you can also get the page number straight from the player:
`player's current menu page`.

## Changing what one player sees

Titles and icons on a session affect **that one player only**; that is the whole of how "each player
sees different contents" is implemented.

```skript
on menu interact:
    if the clicked slot is 4:
        # retitle for the one who clicked
        update title of the menu session of player to "&aYou clicked the middle!"
        # change one slot for the one who clicked
        set icon in slot 5 of the menu session of player to red stained glass pane
```

- `update title of [the] [menu] [session] %menusession% to "…" [and refresh]` — without
  `and refresh` it only changes the model, and the screen does not change right away.
- `set icon in slot %numbers% of %menusession% to <item>` — pushed to that player immediately (here
  `set` refreshes even without `refresh`, because it goes through the session's
  `setIcons(…, true)`).
- `delete icon in slot %numbers% of %menusession%` — clears those slots.
- It can also be written `set the icon of %menusession% in slot %numbers% to …`.

A session's icon only changes "the window this player sees" and **does not touch the menu itself**:
everyone else still sees the original, and a page turn or refresh overwrites it with the menu's contents
again. To change it for everyone, use `override slot` / `map key` (see [Filling Menus](filling-menus.md)).

Both `set the title of {_session} to "…"` and `update title of the menu session … to "…"` change the
title; the first changes the value directly, the second adds an `and refresh` switch.

## Operations on a session

```skript
close the menu session of player
close the menu session {_session}

refresh the slot 5 in menu session {_session}
refresh the menu session {_session}

clear the menu session of player
clear the menu session of player and refresh
```

| Syntax | What it does |
|---|---|
| `close [the] [menu] [session] %menusession%` | closes this session, the window disappears, and `on menu close` fires |
| `refresh [the slot %-number% in] [the] [menu] [session] %menusession%` | resends contents: with a slot it sends only that one slot, otherwise the whole window |
| `clear [the] [menu] [session] %menusession% [and refresh]` | empties the window's contents without **closing** the window |

`refresh` does nothing in `static` mode — there the contents are held by a real server-side inventory,
and the client sees the latest state on its own.

The menu can also be operated from the player's side: `close the menu for player` (equivalent to
closing his session; when he has no session it reports a readable one-line error).

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
through him once. To be sure he is still there, check `the menu session of loop-player` again inside
the loop.

## When a session goes away

| Situation | What happens |
|---|---|
| the player closes the window | `on menu close` fires and the session ends |
| `close the menu …` / `destroy the menu …` | the same; `destroy` closes everyone who is looking at this menu |
| the player switches to another menu | the old menu fires `on menu close` once, and he is no longer in the old menu's `viewers` |
| the player leaves the server | the session is cleared |
| the plugin is disabled / Skript reloads scripts | **every menu is destroyed** and every player is thrown out of his window |

The last one matters: a menu holds callbacks from the script that built it, and after a script reload
those callbacks point at triggers that no longer exist. That is why the plugin destroys every menu in
`PreScriptLoadEvent` and the scripts build them again when they reload — and why menus are almost
always built in `on load:`.

Building the same menu again with a fixed id inside `on load` is safe: a new menu with the same id
**destroys** the old one, so two of them never coexist.
