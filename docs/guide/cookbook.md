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

The goods sit in one row with a price button directly under each one. The goods cells belong to the menu, so
`with locked icons` keeps a player from taking them; the price row is one key mapped to a list, so a click on
it says which good it is through `the menu list index`.

```skript
on load:
    # One price button per good. Five items under one key are what makes that row a *list*:
    # `set the menu list` fills it, and `the menu list index` says which position a click landed on.
    set {shop::prices::1} to stone named "&eBuy 1"
    set {shop::prices::2} to stone named "&eBuy 2"
    set {shop::prices::3} to stone named "&eBuy 3"
    set {shop::prices::4} to stone named "&eBuy 4"
    set {shop::prices::5} to stone named "&eBuy 5"

    create a phantom menu with chest inventory titled "&6Shop" with layout "12345    " and "ppppp    " and "         " with id "shop" with locked icons:
        # Five goods, one key each, so every good owns a slot of its own. A key mapped to a single item is
        # not a list key, which leaves the price row below as the page's only list.
        map key "1" to icon diamond named "&bDiamond" for menu with id "shop"
        map key "2" to icon emerald named "&aEmerald" for menu with id "shop"
        map key "3" to icon gold ingot named "&6Gold ingot" for menu with id "shop"
        map key "4" to icon iron ingot named "&7Iron ingot" for menu with id "shop"
        map key "5" to icon redstone named "&cRedstone" for menu with id "shop"
        # The list key: the five buttons, in the layout order of the five `p` cells.
        map key "p" to icon {shop::prices::*} for menu with id "shop"

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

- A goods cell is a slot the layout **gives a key**, and that is what `with locked icons` protects: no click,
  shift click, number key, offhand swap, drop or double-click collect can take from it, and a drag that
  touches one is refused as a whole. The slots the layout leaves empty stay the player's -- which is how a
  shop and a backpack are the same mechanism; see [Menus](menus.md) and [How it works](how-it-works.md).
- One interaction is decided in a fixed order: the slot callbacks run first, then the event fires, and
  `locked icons` is applied last. So the "buy" callback has run by the time the goods are protected, and
  nothing has to put them back; see [How a click is decided](how-it-works.md#how-a-click-is-decided).
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
- Once a slot has been `override slot … to barrier`, it is no longer a list cell, so `the menu list index` no
  longer names its position -- the sold-out cell stops being a price button, and the next `set the menu list`
  does not write a button back into it.

## A browser for a thousand items

45 results to a page, one result per cell, and two cells for the previous and next page. The whole page is
filled with **one call**; it is filled on the page turn rather than in the arrow buttons; and whether a click
hit a result or an arrow is decided by whether `the menu list index` is empty.

```skript
# One page of the browser: put 45 items into the window's list cells. `set the menu list` drops the items
# that do not fit and clears the cells left without one, so "only three results" needs no extra handling.
function fillBrowser(window: menusession, page: number):
    set {_first} to ({_page} - 1) * 45
    delete {browser::shown::*}
    loop 45 times:
        set {_one} to {browser::all::%{_first} + loop-number%}
        if {_one} is set:
            set {browser::shown::%loop-number%} to {_one}
    set the menu list of {_window} to {browser::shown::*}

on load:
    create a phantom menu with chest inventory titled "&6Item browser" with layout "LLLLLLLLL" and "LLLLLLLLL" and "LLLLLLLLL" and "LLLLLLLLL" and "LLLLLLLLL" and "P       N" with id "browser":
        map key "L" to icon {browser::all::*} for menu with id "browser"
        map key "P" to icon arrow named "&ePrevious" for menu with id "browser" and when clicked:
            set {_page} to the page of the menu window of player
            if {_page} is greater than 1:
                turn to page {_page} - 1 for player
        map key "N" to icon arrow named "&eNext" for menu with id "browser" and when clicked:
            set {_page} to the page of the menu window of player
            if {_page} is less than the page number of the menu with id "browser":
                turn to page {_page} + 1 for player

command /browse:
    trigger:
        # A real script fills this from its own source; a thousand items is the example here.
        delete {browser::all::*}
        loop 1000 times:
            set {browser::all::%loop-number%} to stone named "&7Item %loop-number%"
        open menu (the menu with id "browser") for player

on menu open:
    if the event-menu is the menu with id "browser":
        fillBrowser(the menu window of player, 1)

# Filling on the page turn rather than in the arrow button keeps the content right whichever way the page
# was turned -- the button, `turn to page` from another script, or anything added later.
on page turn:
    if the event-menu is the menu with id "browser":
        fillBrowser(the menu window of player, the future page)

on menu interact:
    set {_picked} to {browser::shown::%the menu list index%}
    if {_picked} is set:
        send "&aYou picked %{_picked}%" to player
    # Empty: this cell is not a result, so it is a button such as an arrow.
```

- The result cells are **one key**: the list in `map key "L" to icon {browser::all::*}` is what gives the page
  its list cells, in layout order. With 45 of them, the first item of page `p` is `(p - 1) * 45 + 1`; a
  thousand items are 23 pages, and the last page holds 10. See [Filling Menus](filling-menus.md).
- `set the menu list` is **one call and one update** for the whole page: 45 cells in one go rather than 45,
  and extra items are dropped while cells with nothing left are cleared, so a short page needs no extra code.
- `the menu list index` is **1-based**, and it is empty when the clicked cell is not a list cell. That is how
  one event tells a result from the previous/next arrow; see
  [List pages](filling-menus.md#list-pages-filling-a-page-with-a-column-of-items).
- Filling in `on page turn` rather than in the arrow callback covers every way a page can be turned, and
  `the future page` is the page being turned to; it has a value only in `on page turn`, as
  [Events](events.md) and [Pages](pages.md) explain. The page count is fixed when the menu is created, so a
  shorter search does not mean fewer pages -- that boundary is in [Not Supported Yet](not-supported-yet.md).

## A backpack that remembers

A window exists only while it is open, and in a `static` menu the empty cells are real items, so what a player
left in them has to be read out before the window ends and put back cell by cell the next time it is opened.
`the occupied slots of` says which cells hold something and `icon in slot N of` says what they hold.

```skript
on load:
    # No colon: the menu needs no body, and a section with nothing under it is an empty section,
    # which Skript warns about while the script loads.
    create a static menu with chest inventory titled "&6Backpack" with layout "         " and "         " and "         " with id "backpack" with 0 ms click delay

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
