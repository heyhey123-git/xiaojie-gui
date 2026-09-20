# Cookbook

[中文](cookbook.zh-CN.md) | English

Whole recipes, using only the syntax the rest of the documentation describes. Each one is a problem, a
script, and a few lines on why it is written that way.

Every recipe here also exists as a script under [`server-test/skript/`](../../server-test/skript/), which
`serverTest` parses on a real Paper server on each run: a recipe that stops parsing, or stops doing what it
claims, fails the build instead of going stale in a page. The copies and the page differ in two ways on
purpose: the shop and the browser assert the slots, the mode and the arithmetic they claim (that layer has no
player, so it cannot click anything), and the backpack copy is the script the server itself runs. This page
shows the part of each recipe that matters; the whole script is in the copy.

## A shop

The goods sit in one row with a price button directly under each one. The menu is `phantom`, which is what
keeps a player from taking the goods at all: nothing in a phantom window can move, and any click that tries
is undone by the addon. This shop therefore carries no `with locked icons` -- that flag protects the slots of
a `static` menu, where the items are real. The price row is one key mapped to a list, so a click on it says
which good it is through `the menu list index`.

```skript
on load:
    # One price button per good. Five items under one key are what makes that row a *list*:
    # `set the menu list` fills it, and `the menu list index` says which position a click landed on.
    set {shop::prices::1} to stone named "&eBuy 1"
    set {shop::prices::2} to stone named "&eBuy 2"
    set {shop::prices::3} to stone named "&eBuy 3"
    set {shop::prices::4} to stone named "&eBuy 4"
    set {shop::prices::5} to stone named "&eBuy 5"

    build a menu {_menu}:
        inventory type: chest inventory
        title: "&6Shop"
        layout: "12345    ", "ppppp    ", "         "
        id: "shop"
        edit:
            # Five goods, one key each, so every good owns a slot of its own. A key mapped to a single item
            # is not a list key, which leaves the price row below as the page's only list.
            map key "1" to icon diamond named "&bDiamond" for {_menu}
            map key "2" to icon emerald named "&aEmerald" for {_menu}
            map key "3" to icon gold ingot named "&6Gold ingot" for {_menu}
            map key "4" to icon iron ingot named "&7Iron ingot" for {_menu}
            map key "5" to icon redstone named "&cRedstone" for {_menu}
            # The list key: the five buttons, in the layout order of the five `p` cells.
            map key "p" to icon {shop::prices::*} for {_menu}

command /shop:
    trigger:
        open menu (the menu with id "shop") for player

# The price callback lives in the event: every click arrives here, and only a price cell yields a list index.
on menu interact:
    if the event-menu is not the menu with id "shop":
        stop

    set {_buy} to {shop::goods::%the menu list index%}
    set {_price} to {shop::cost::%the menu list index%}
    # On a goods cell `the menu list index` is empty, so those two variables are empty too: not a purchase.
    if {_buy} is not set:
        stop

    if {coins::%uuid of player%} is less than {_price}:
        send "&cYou need %{_price}% coins for that." to player
        stop

    remove {_price} from {coins::%uuid of player%}
    give {_buy} to player
    send "&aYou bought %{_buy}% for %{_price}% coins." to player
```

- A goods cell is a slot the layout **gives a key**. In a `static` menu that is exactly what
  `with locked icons` protects -- no click, shift click, number key, offhand swap, drop or double-click
  collect can take from it, and a drag that touches one is refused as a whole -- while a `phantom` menu needs
  no flag, because its window cannot move anything in the first place; see [Menus](menus.md) and
  [How it works](how-it-works.md). The slots the layout leaves empty stay the player's, which is how a shop
  and a backpack are the same mechanism.
- One interaction is decided in a fixed order: the slot callbacks run first, then the event fires, and in a
  `static` menu `locked icons` is applied last. So the "buy" callback has run by the time the goods are
  protected, and nothing has to put them back; see
  [How a click is decided](how-it-works.md#how-a-click-is-decided).
- The price row is one key because a page has room for exactly one list key: both `set the menu list` and
  `the menu list index` go by the **first** key the page mapped several items to. Give each good a key and
  the whole price row one key, and the index is the good's number.
- The callback reads `the event-menu`: `the menu`, `the window` and `the session` are not event values at
  all, so a shorter form does not parse; the two ways of writing an event value are in [Events](events.md).
  `{shop::goods::*}` and `{shop::cost::*}` are the script's own goods table, not shown here.

### Sold out: for everyone, or for this buyer

When a good runs out, replace its price button. The two scopes differ by one line and mean different things:

```skript
# Everyone: this changes the page, so every player looking at it sees the barrier and it survives a reopen.
override slot 9 in page 1 to barrier named "&cSold out" for menu with id "shop" and refresh

# This buyer only: this changes his window. Others still see the button, and a page turn redraws over it.
set icon in slot 9 of the menu window of player to barrier named "&cSold out"
```

- `override slot … for menu …` writes the **page**: it is pushed to everyone looking at that page
  (`and refresh`) and is still there when the menu is opened again. That is the one a real shop wants -- the
  shelf is shared, so one buyer taking the last one has to be visible to the next; see
  [Filling Menus](filling-menus.md).
- `set icon in slot N of the menu window of player` writes **this player's window**: it changes the copy he
  sees and does not touch the menu itself. A page turn redraws that page into the window, so a temporary icon
  in a slot the page owns is overwritten by it; see [Windows](windows.md).
- A sold-out cell is still a list cell as far as the list is concerned: `override slot` writes the page's own
  icon over that cell, but the cell keeps its place in the list, so `the menu list index` still names it and a
  later `set the menu list` would write a button back into it. What refuses the sale is the callback's own
  stock check -- the picture is a courtesy, the check is the rule.

## A browser for a thousand items

45 results to a page, one result per cell, and two cells for the previous and next page. The whole page is
filled with **one call**, made **after** the window is open or the page has turned; and whether a click hit a
result or an arrow is decided by whether `the menu list index` is empty.

```skript
# One page of the browser: put 45 items into the window's list cells. `set the menu list` drops the items
# that do not fit and clears the cells left without one, so "only three results" needs no extra handling.
# What is on screen is remembered per player: two players on different pages of one menu must not read each
# other's 45 items when they click.
function fillBrowser(p: player, page: number):
    set {_window} to the menu window of {_p}
    set {_first} to ({_page} - 1) * 45
    delete {browser::shown::%uuid of {_p}%::*}
    loop 45 times:
        set {_one} to {browser::all::%{_first} + loop-number%}
        if {_one} is set:
            set {browser::shown::%uuid of {_p}%::%loop-number%} to {_one}
    set the menu list of {_window} to {browser::shown::%uuid of {_p}%::*}

on load:
    # Every page of a browser looks the same, so the layout is written once and used for all of them.
    set {_layout::*} to "LLLLLLLLL" and "LLLLLLLLL" and "LLLLLLLLL" and "LLLLLLLLL" and "LLLLLLLLL" and "P       N"
    build a menu {_menu}:
        inventory type: chest inventory
        title: "&6Item browser"
        layout: {_layout::*}
        id: "browser"
        edit:
            map key "L" to icon {browser::all::*} for {_menu}
            map key "P" to icon arrow named "&ePrevious" for {_menu} and when clicked:
                set {_page} to the page of the menu window of player
                if {_page} is greater than 1:
                    turn to page {_page} - 1 for player
                    # Fill after the turn: the page is loaded after its event, and loading it writes the
                    # key's own icons into the cells, which would overwrite a list written before it.
                    fillBrowser(player, {_page} - 1)
            map key "N" to icon arrow named "&eNext" for {_menu} and when clicked:
                set {_page} to the page of the menu window of player
                if {_page} is less than the page number of {_menu}:
                    turn to page {_page} + 1 for player
                    fillBrowser(player, {_page} + 1)
    # The layout above is the first page; the others have to be asked for, because the page count is fixed
    # here, when the menu is made -- `turn to page` cannot go past the last page. A thousand items are 23
    # pages of 45, so 22 more.
    loop 22 times:
        insert page to {_menu} with layout {_layout::*}

command /browse:
    trigger:
        # A real script fills this from its own source; a thousand items is the example here.
        delete {browser::all::*}
        loop 1000 times:
            set {browser::all::%loop-number%} to stone named "&7Item %loop-number%"
        open menu (the menu with id "browser") for player
        # After the window is open, never from `on menu open`: that event runs before there is a window to
        # write into. The last bullet below says why both events are the wrong place.
        fillBrowser(player, 1)

on menu interact:
    set {_picked} to {browser::shown::%uuid of player%::%the menu list index%}
    if {_picked} is set:
        send "&aYou picked %{_picked}%" to player
    # Empty: this cell is not a result, so it is a button such as an arrow.
```

- The result cells are **one key**: the list in `map key "L" to icon {browser::all::*}` is what gives the page
  its list cells, in layout order. With 45 of them, the first item of page `p` is `(p - 1) * 45 + 1`; a
  thousand items are 23 pages -- the `loop 22 times` above is what makes them, since a menu's page count is
  fixed when it is made -- and the last page holds 10. See [Filling Menus](filling-menus.md).
- `set the menu list` is **one call and one update** for the whole page: 45 cells in one go rather than 45,
  and extra items are dropped while cells with nothing left are cleared, so a short page needs no extra code.
- `the menu list index` is **1-based**, and it is empty when the clicked cell is not a list cell. That is how
  one event tells a result from the previous/next arrow; the slice on screen is kept per player
  (`{browser::shown::%uuid of player%::*}`), because two players looking at one menu can be on different pages.
  See [List pages](filling-menus.md#list-pages-filling-a-page-with-a-column-of-items).
- Fill **after** the window or the page is there, never from `on menu open` or `on page turn`: `on menu open`
  fires before the window exists, and a page turn loads the page *after* its event, so a list written inside
  either one is refused or overwritten. The two arrow callbacks and `/browse` are the places this browser
  turns a page, so they are the places it fills; a script that turns this menu's page anywhere else has to
  fill there too, or the cells keep showing the icon the key was mapped to. The page count is fixed when the
  menu is created, so a shorter search does not mean fewer pages -- that boundary is in
  [Not Supported Yet](not-supported-yet.md).

## A backpack that remembers

A window exists only while it is open, and in a `static` menu the empty cells are real items, so what a player
left in them has to be read out before the window ends and put back cell by cell the next time it is opened.
`the occupied slots of` says which cells hold something and `icon in slot N of` says what they hold.

```skript
on load:
    # `build a menu` is a section, so its first line needs the colon; the properties under it are its
    # body, so there is no empty section for Skript to warn about.
    build a menu {_menu}:
        mode: static
        inventory type: chest inventory
        title: "&6Backpack"
        layout: "         ", "         ", "         "
        id: "backpack"
        click delay: 0

command /backpack:
    trigger:
        set {_menu} to the menu with id "backpack"
        open menu {_menu} for player
        set {_window} to the menu window of player
        # Put back what this player left behind last time. The stored keys are slot numbers, so an item
        # returns to the cell it was in.
        loop {backpack::%uuid of player%::*}:
            set {_slot} to loop-index parsed as number
            set icon in slot {_slot} of {_window} to {backpack::%uuid of player%::%loop-index%}

on menu close:
    if the event-menu is not the menu with id "backpack":
        stop
    set {_window} to the menu window of player
    # Deleting first matters: a player who took something out would otherwise keep a stale copy of it
    # in the stored list and get it back for free next time.
    delete {backpack::%uuid of player%::*}
    loop the occupied slots of {_window}:
        set {backpack::%uuid of player%::%loop-value%} to icon in slot loop-value of {_window}
```

- A window's container is **cleared when the window ends**, so saving has to happen inside `on menu close`.
  That event fires on **every** path that ends a window -- the player closing it, `close`, `destroy`,
  quitting, dying, changing world -- and the container is still readable inside it, because the event runs
  before the inventory is cleared. What is not saved is only what the script never reads. What it saves goes
  into the script's own storage (a variable here, a database through something like skript-orm on a real
  server): there is no "save the window" element; see [Windows](windows.md).
- `the occupied slots of {_window}` reads the **container** only -- the cells the layout describes. A `static`
  menu's lower half is the player's own inventory and never the menu's, so it follows the player around and
  needs no saving; see [What a window has on it](windows.md#what-a-window-has-on-it).
- This is only worth writing in `static` mode: a `phantom` menu's cells are all the menu's own, so a player
  cannot put anything in them in the first place. The two modes are in [The Two Modes](modes.md).
- The `delete` before the `loop` in `on menu close` is not decoration: without it the item a player took out
  is still in the old list, and he gets it back for free.

## One player's highlight

`set icon in slot N of the menu window of player` marks that cell for **that one player**;
`override slot` marks it for everyone. The two are written separately because one writes a window and the
other writes a page.

```skript
on menu interact:
    # For the one who clicked: everyone else's window still shows the original.
    set icon in slot 5 of the menu window of player to red stained glass pane

    # For everyone looking at this page, and still there when the menu is opened again.
    override slot 5 in page 1 to red stained glass pane for the event-menu and refresh
```

- An icon on a window changes the copy **that player** sees and does not touch the menu itself. What he sees
  changes; the shelf does not. A change to the page reaches everyone looking at that page; see
  [Changing what one player sees](windows.md#changing-what-one-player-sees).
- A page turn redraws that page into the window, so a temporary highlight in a cell the page owns is
  overwritten by it. A `refresh` is not: it re-sends what the window currently holds. To make a highlight
  survive a turn, set it again after the turn.
- An icon put into a window with `set icon in slot N of {_window}` is **not** one of the slots `locked icons`
  protects, which is exactly what makes per-player content possible; see
  [Which slots belong to whom](how-it-works.md#which-slots-belong-to-whom).
- The scope rule is that one line: a window is **one player's copy**, a page is the **shared definition**.
  [Windows](windows.md) opens with the table of which operations need a window named and which do not.
