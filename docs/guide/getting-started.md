# Getting Started

[中文](getting-started.zh-CN.md) | English

This page gets you a working menu in five minutes. The other pages take each part apart.

## Installation

1. Put `xiaojiegui-<version>.jar` in the server's `plugins` directory.
2. The server needs **Paper 26.2** (or a compatible fork) and **Java 25**.
3. Dependencies: **Skript 2.16.2** and **PacketEvents 2.13.0**. Optional: **SkBee** (only to use text
   components in titles).
4. Start the server. There is nothing to configure, so the plugin writes no config file.

The requirements are hard: `plugin.yml` declares `api-version: 26.2`, and an older server **refuses to
load** the plugin outright, rather than loading it and then failing with strange errors at runtime.

## Your first menu

```skript
on load:
    create a phantom menu with chest inventory titled "&6Main Menu" with layout "#########" and "#  A  B #" and "#########" with id "main":
        map key "A" to icon diamond named "&bSay hello" for menu with id "main" and when clicked:
            send "Hello!" to player
        map key "B" to icon clock named "&eClose" for menu with id "main" and when clicked:
            close the menu for player

command /menu:
    trigger:
        set {_menu} to the menu with id "main"
        open menu {_menu} for player
```

This script does four things:

- `create a phantom menu … with layout …` creates a menu, and **every character in a layout string is
  one slot**.
- `map key "A" to icon …` ties the character `A` in the layout to an item; `key` is that character.
- The section under `for menu with id "main" and when clicked:` is the code that runs when this icon is
  clicked.
- `open menu {_menu} for player` sends the menu to the player.

`with id "main"` gives the menu a name, which `the menu with id "main"` can find again later. **A menu
without an id can only be passed around in a variable**, and it does not appear in `all menus`.

## The three easiest traps

**One: the word `menu` is optional in this version, and it has to sit in the right place.**

```skript
map key "A" to icon stone for {_menu}          # fine
map key "A" to icon stone for menu {_menu}     # also fine
override slot 2 in page 1 to diamond for {_menu}
```

But inside a property expression you must **not** add `menu`: `the id of menu {_menu}` is read as "look a
menu up by id". Write `the id of {_menu}`.

**Two: there is no `page 0` any more.** Page numbers start at 1, and the layout given when the menu is
created is always page 1.

**Three: do not write `string:` or `component:` in a title.** Those are branch markers inside the
plugin's own patterns, not script syntax. If you write one, the whole text (prefix included) becomes the
title, and since 2.0.0 that is reported as an explicit error.

## Which page to read next

- To find out how a menu and a window actually run: [How a window actually works](how-it-works.md) lays
  out the model, and every page after it is easier to read.
- To see what each piece of syntax looks like: start at [Creating Menus](menus.md) and follow the
  sidebar down; decide [the two modes](modes.md) before you build the first one.
- To copy a multi-page menu outright: see [Pages](pages.md) and [Filling Menus](filling-menus.md).
- Something behaves strangely: check [Not Supported Yet](not-supported-yet.md) first, which lists the
  known boundaries.
