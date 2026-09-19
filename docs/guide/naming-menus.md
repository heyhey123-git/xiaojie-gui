# Referring to Menus in Syntax

[中文](naming-menus.zh-CN.md) | English

One script can have a dozen menus at once, so every piece of syntax has to know which one you mean.
There are three ways, one for each situation.

## The three ways

**Inside a menu section: write nothing.**

```skript
create a phantom menu with chest inventory titled "Menu" with layout "AAAAA" with id "main":
    override slot 4 in page 1 to diamond named "Special Item" for menu with id "main"
```

Inside the section body of `create menu` / `build a menu` / `edit menu`, the "current menu" is the one
that was just built or is being edited, so `insert page …` and the like can leave the menu out
entirely.

**Through a variable: `for {_menu}`.**

```skript
set {_menu} to the menu with id "main"
map key "A" to icon stone for {_menu}
override slot 2 in page 1 to diamond for {_menu}
set {_slots::*} to the slots of key "A" in page 1 of {_menu}
set {_key} to the key of slot 1 in page 1 of {_menu}
destroy the menu {_menu}
```

The word `menu` **is now optional** in the groups where it was already optional, so both
`for {_menu}` and `for menu {_menu}` parse. Both are correct; write whichever one reads better.

**Through an id: `with id "main"`.**

```skript
set {_menu} to the menu with id "main"
map key "A" to icon stone for menu with id "main"
destroy the menu with id "main"
```

`menu with id "main"` is an **expression** that evaluates to that menu; it can be used anywhere a menu
is needed. When there is no menu with that id, it returns nothing.

`destroy` deliberately has two patterns, because a pattern that has already eaten the word `menu`
cannot let the menu expression start at `with id` — without the second one you would have to write
`menu` twice.

## One easy trap: do not write `menu` in a property expression

```skript
set {_id} to the id of menu {_menu}     # wrong: read as "look a menu up by id"
set {_id} to the id of {_menu}          # right
```

`menu {_menu}` parses as "use `{_menu}` as an id to find a menu", not as "the menu named `{_menu}`".
Every `the … of {_menu}` in [Creating Menus](menus.md) follows that rule:

```skript
set {_title} to the default title of {_menu}
set the default title of {_menu} to "&aMy Menu"
set {_page} to the default page of {_menu}
set {_delay} to the minimum click delay of {_menu}
set {_mode} to the mode of {_menu}
set {_type} to the inventory type of {_menu}
set {_pages} to the page number of {_menu}
set {_layout::*} to the default layout of menu {_menu}   # the one exception, see below
```

`default layout` has `[(menu|gui)]` in its pattern, so both
`the default layout of menu {_menu}` and `the default layout of {_menu}` work.

## Menus without an id

`with id` is optional in `create menu`. A menu without an id **still works**, as long as you have the
value: `build a menu {_menu}:` stores it in a variable, and inside the section body of `create menu`
you can refer to it directly.

The price is two things:

- It does not appear in `all menus` / `all menu ids` (those two only list menus that have an id).
- It cannot be found again with `menu with id "…"`.

`destroy the menu {_menu}` works for both cases.

## Details of finding a menu by id

```skript
set {_menu} to the menu with id "main"
if {_menu} is not set:
    send "There is no menu 'main'." to player
```

- The id part of `menu with id` is a **string expression**, so `menu with id {_id}` works too.
- When it finds nothing it returns "no value"; in Skript, testing an expression that has no value means
  going through a variable and then `is not set` (`is not null` is not how Skript writes that
  condition).
- Creating another menu with the same id **destroys** the old one, so rebuilding inside `on load` is
  safe: the old one is closed, and no player is left stuck in a window that no longer exists.

Buttons (`define button` / `map key … to button "id"`) are also indexed by id, in a separate namespace:
`all buttons` lists every button id. Buttons and menus do not affect each other.
