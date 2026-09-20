# Pages

[中文](pages.zh-CN.md) | English

## A menu has one page by default

The `layout` given when the menu is created is **always page 1**, and everything you map onto it lives
on that page. Most menus end here and never have to think about page numbers.

Paging is a feature you add when you need it. If all you want is to change what a page shows, keep one
page, swap what is in the slots with `override slot` / `map key`, and keep the "page number" in a script
variable of your own — that is the common 1.x approach, and 2.0.0 still supports it.

## Page numbers start at 1

`page 0` **does not exist**. Wherever older documentation writes `page 0`, it is wrong: `in page 1` is
the first page, and `insert page 1` means "insert at position 1". A page number out of range gives a
readable error (`Page 0 does not exist` and the like) instead of throwing inside Skript.

## `with page N` only decides which page is shown first

```skript
create a phantom menu with chest inventory titled "Paged" with layout "AAAAAAAAA" with id "paged" with page 2:
```

The menu this line builds has **only 1 page** (that layout), and its `default page` is 2. It means "show
page 2 when the menu is opened", not "create 2 pages first". So this menu fails on `open menu`, because
page 2 does not exist.

`with page N` / `default page: N` **never** changes how many pages a menu has. It used to stop the
plugin from creating any page at all, and a menu with no pages cannot be opened — that is,
`create menu … with layout … with page 2` used to produce a menu that could not be opened. This version
fixes that: the layout always becomes page 1.

To read this value: `the default page of {_menu}`; to change it:
`set the default page of {_menu} to 2` (a value below 1 is clamped to 1).

## Adding a page: `insert page`

**The effect form** (all on one line):

```
insert page [%-numbers%] [to %-menu%]
    [with layout %-strings%]
    [with player layout %-strings%]
    [with [new] title <title>]
```

```skript
insert page 1 to {_menu} with layout "xxxxxxxxx", "xooooooxx", "xxxxxxxox" with player layout "ooooooooo", "oooooooox", "xxxxxxxxx" with title "New Page"
```

**The section form** (the three entries `layout:` / `player layout:` / `title:`, all
optional):

```skript
insert page 1 into menu {_menu}:
    layout: "xxxxxxxxx", "xooooooxx", "xxxxxxxox"
    player layout: "ooooooooo", "oooooooox", "xxxxxxxxx"
    title: "New Page"
```

Where it is inserted:

| Written as | Result |
|---|---|
| `insert page …` | appended at the end |
| `insert page 1 …` | inserted at **position 1**: the original layout page shifts to page 2 |
| `insert page 2 …` | inserted after page 1 |
| a page number out of range | clamped to `1..pages+1`, which is the same as appending |

A new page without `layout:` inherits the menu's **default layout** (`the default layout of {_menu}`,
which `set the default layout of {_menu} to …` changes); without `title:` it inherits the default title
(`the default title of {_menu}`).

**Inside a menu event the menu may be left out**, because the current event carries it:

```skript
on menu interact:
    insert page with layout "BBBBBBBBB"
```

Outside an event you **must** name the menu, otherwise the line does not parse at all
(`insert page … to %-menu%`).

## Turning pages

```skript
turn to page 2 for player
turn to page 2 for player with new title "Chapter Two"
```

One `turn to page` fires `on page turn`, and the event can read the page it came from and the page it
goes to. You can also get the same result with `open menu {_menu} for player and go to page 2`; opening
a menu for a player who already has it open does not report an error, it sends him to the page you name.

If you want to turn the page without changing the title, leave out `with new title`. If you do write a
title, that title is only for **this one display** and is not written back to the page's title — to
change the title permanently use `update title of page N in {_menu} to "…"`.

Turning a page **clears only the slots the page being left owns** (the ones its layout names a key for,
plus the ones `override slot` covered); every other slot is left exactly as it is. That is what lets a
backpack menu keep what the player put in its empty slots across a page turn.

## A two-page menu

```skript
on load:
    create a phantom menu with chest inventory titled "&6Shop" with layout "#########", "#AAAAAAA#", "#########" with id "shop":
        insert page 2 with layout "#########", "#BBBBBBB#", "#########" with title "&6Second Page"
        map key "A" to icon diamond named "&bPage one item" for {_menu}
        map key "B" to icon emerald named "&aPage two item" for {_menu}
        map key ">" to icon arrow named "&eNext page" for {_menu} and when clicked:
            if the page is less than the page number of {_menu}:
                turn to page (the page + 1) for player
        map key "<" to icon arrow named "&ePrevious page" for {_menu} and when clicked:
            if the page is greater than 1:
                turn to page (the page - 1) for player

command /shop:
    trigger:
        set {_menu} to the menu with id "shop"
        open menu {_menu} for player
```

The two characters `>` and `<` appear on **both pages**, so `map key` maps them onto both: **without
`on page` the scope is every page** (including pages added later by `insert page`). That is why the
page-turn buttons work on every page, while `A` only has slots on page 1 and `B` only on page 2, without
interfering with each other.

## Reading page numbers

| What you want | How to write it |
|---|---|
| how many pages the menu has | `the page number of {_menu}` |
| which page a session is on | `the page of the menu session of player` |
| which page a player is on | `player's current menu page` |
| the title of a page | `the title of page 1 in {_menu}` |
| which slots a key occupies in a layout | `the slots of key "A" in page 1 of {_menu}` |
| which key a slot is | `the key of slot 1 in page 1 of {_menu}` |

`the page number of {_menu}` is the number of pages, not "the current page". `the page` inside a menu
event is the page the event happened on.

## `player layout`

This is the layout of the **4 rows below the container** (3 main inventory rows + 1 hotbar row), which
are the player's half of the window. Up to 4 strings are used, and missing rows are filled with empty
ones. It follows the same rules as the main layout: one character per slot, a space stays empty. Slot 27
is the first cell of the lower half of a three-row chest, so a key above the container's rows is 0 to 26
and a key below them starts at 27.

**Asking for a player layout hides the player's inventory**, and that is not a second switch to remember
but the same statement: those rows are the menu's own space only while the player's inventory is hidden,
because the client draws that half from the last content packet either way. A menu that hides it can put
its own icons there and a click on one of them arrives like any other click; a menu that shows it is
showing the player's own items.

```skript
create a phantom menu with chest inventory titled "&6Backpack" with layout "AAA      ", "AAA      ", "AAA      " with player layout "B        ", "         ", "         ", "         " with id "backpack":
    map key "B" to icon chest named "&eYour bag" for menu with id "backpack"
```

The half belongs to the menu rather than to a page — the player's inventory is either there or it is not —
so a player layout written on **any** page hides it for the whole menu. Pages that have no player layout
of their own simply leave those 36 slots empty; page 1 keeps what its own pattern gave it.

A menu that wants the lower half hidden but empty asks for that alone, with `with hide player inventory`:
the 36 slots are the menu's then, and `set icon in slot N of {_window}` fills one at runtime.

The half is one thing or the other, never both: `show player inventory of {_menu}` on a menu whose page
lays those rows out is **refused** (with a line saying so), because the page's icons for them would land on
the player's own items. To give the player that half back, destroy the menu and build it again without a
player layout.

`static` mode has no player layout: that window *is* the player's real inventory, so a player layout there
is reported with a warning when the menu is created and changes nothing.
