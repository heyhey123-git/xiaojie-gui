# Conditions

[中文](conditions.zh-CN.md) | English

## Whether the player inventory is hidden

```
player inventory of %menu% is hidden
player inventory of %menu% (isn't|is not) hidden
```

```skript
if player inventory of {_menu} is hidden:
    send "This menu hides the player inventory." to player
else:
    send "This menu shows the player inventory." to player
```

It reads exactly the `hide player inventory` flag: `create menu … with hide player inventory`,
`hide player inventory: true` in `build a menu`, and `hide player inventory of {_menu}` all change it.
A **player layout** changes it too, because it sets that flag: a page laid out for the rows below the
container is a page that needs them hidden (see [Pages](pages.md#player-layout)).

To change it, use the effects, in either of two forms:

```skript
hide player inventory of {_menu}
show player inventory of {_menu}
```

Showing it again on a menu whose page lays the rows below the container out (`player layout`) is **refused**,
with a line in the console saying so: those rows are the page's own space there, so the two states cannot
both be had, and a window that was half the page's icons and half the player's items is the one window this
addon does not build. Destroy the menu and build it again without a player layout to give the player that
half back.

Only two spellings are accepted: `isn't` and `is not` (the pattern is `(isn't|is not)`); anything else
(`isnt`, `isn't not`) means nothing. To test for "shown", write
`if player inventory of {_menu} is not hidden:`.

When the menu is "no value" this condition is **false** (not an error):
`if player inventory of {_menu} is hidden:` does not hold when `{_menu}` is not set, and the script
carries on.

## Whether a menu is destroyed

```
[the] [menu] %menu% (is|was) destroyed
[the] [menu] %menu% (isn't|is not|wasn't|was not) destroyed
```

```skript
if the menu with id "main_menu" is destroyed:
    send "The main menu has been destroyed and cannot be used." to player

if {_menu} is not destroyed:
    open menu {_menu} for player
```

When it is destroyed:

- someone runs `create menu` again with the same id (the old one is destroyed);
- you run `destroy the menu {_menu}` / `destroy the menu with id "main_menu"`;
- the plugin is disabled, or Skript reloads scripts (**every** menu is destroyed).

A destroyed menu **cannot** be opened, turned, or have its contents changed — those operations give
readable errors. This condition is there to hold them off in advance:

```skript
if {_menu} is not destroyed:
    open menu {_menu} for player
else:
    send "The menu is not built yet, one moment." to player
```

`destroy` itself is idempotent: destroying an already destroyed menu does nothing.

## Testing "does the player have a menu open"

There is no dedicated syntax; use a session:

```skript
if the menu session of player is not set:
    send "You do not have a menu open yet." to player
```

This is what replaces the old `if player has a gui open:` in 2.0.0 (the old `gui` syntax is removed).
