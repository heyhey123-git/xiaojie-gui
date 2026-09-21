# Filling Menus

[中文](filling-menus.zh-CN.md) | English

A layout string only decides **which slots exist**; what actually goes in them and what a click does is
decided by the syntax below.

## Two pieces of syntax, two ways of thinking

| | `map key` | `override slot` |
|---|---|---|
| what it goes by | the **characters** in the layout | **slot numbers** directly |
| what it affects | every slot where that character appears | the slots you write |
| typical use | filling things in across a whole layout | putting one thing in a specific place |

Either of them can carry a click callback, and either can go without one.

## `map key … to icon/items`

```
map key %string% to ((icon|item)[s] %-itemstacks%|button %-string%)
    [for [(menu|gui)] %-menu%] [on page[s] %-numbers%]
    [and (refresh|update)] [and when clicked]
```

```skript
map key "A" to icon stone for {_menu}
map key "A" to items {_allSkull::*} for {_menu}
map key "B" to button "my_button" for {_menu}
map key "special_item" to item diamond named "Special Item" for menu {_menu} and refresh and when clicked:
    send "You clicked the special item." to player
```

- `icon` and `item` are synonyms, and singular or plural is up to you.
- When you give **several** items, each slot with that key takes one in order (cycling through them).
- Without `for …` the menu comes from the current event (a `create menu` section body, an `edit menu`
  section body, any menu event). Outside an event it has to be written out.
- Without `on page …` the scope is **every page**, including pages added later with `insert page`.
- `and refresh` pushes the change to the players who are watching right away. Without it, a `phantom` menu
  only shows the change on the next refresh or page turn; a `static` menu shows it anyway, because the
  change is a real item in the real inventory.
- `and when clicked` **requires** a section body, otherwise registration reports
  `You must provide a section to handle the click event when using 'and when clicked'.`

## List pages: filling a page with a column of items

A browser wants "this page shows these N results, and clicking the third one is the third one". The way to
do that is to **map a list of items to one key**: the cells of that key are the page's list cells, in layout
order:

```skript
build a menu {_menu}:
    inventory type: chest inventory
    title: "&6Item browser"
    layout: "LLLLLLLLL", "LLLLLLLLL", "LLLLLLLLL", "LLLLLLLLL", "LLLLLLLLL", "  P   N  "
    id: "browser"
    edit:
        map key "L" to icon {results::*} for {_menu}
        map key "P" to icon arrow named "&ePrevious" for {_menu}
        map key "N" to icon arrow named "&eNext" for {_menu}
```

Every player then fills their own page with **one call**:

```skript
set the menu list of the menu session of player to {results::*}
```

- Extra items are dropped and slots with nothing left are cleared, so "only three results" is three icons
  and 42 empty cells without any extra handling;
- **the window is updated once**: one call and one refresh for 45 cells, not 45 of each.

Which result was clicked is `the menu list index` (1-based, like every other number here):

```skript
on menu interact:
    set {_picked} to {results::%the menu list index%}
    if {_picked} is set:
        send "&aYou picked %{_picked}%" to player
        stop
    # not a list cell, so this is the previous/next button
```

`the menu list index` is **empty** when the clicked slot is not one of the list cells, which is what lets one
event handler tell a button from a result.

The two things a browser still cannot do -- a page count that shrinks, and typing inside the window -- are in
[Not Supported Yet](not-supported-yet.md).

## `override slot … to … for …`

```
(override|set) slot %numbers%
    [in page %-numbers%]
    to (%-itemstack%|button %-string%)
    for [the] [(menu|gui)] %-menu%
    [(and|with) (refresh|update)]
    [and when (clicked|interacted|pressed)]
```

```skript
override slot 10 in page 1 to diamond named "Clicked Item" for menu with id "main_menu" and refresh and when clicked:
    send "You clicked the overridden slot!" to player
```

- Slot numbers start at 0, at the container's top left, 9 per row.
- `in page …` accepts several page numbers, or nothing at all. **Without it the default is the "page"
  entry** — that is, the value of `with page N` / `default page: N`, which defaults to 1. **There is a
  trap here**: if that default page is greater than the number of pages that exist, this syntax reports
  `Page N does not exist in this menu.` at runtime. So if you set `with page 2` and built only one page,
  write `in page 1` explicitly.
- Something has to follow `to`, and `for` has to name a menu: neither `override slot 10 of diamond` nor
  `override slot 10 to diamond` parses.
- As with `map key`, `and when clicked` requires a section body.
- To empty a slot: `override slot 5 in page 1 to air for {_menu}`.

## The three ways to write a click callback

### One: write it after `override slot` / `map key`

The callback goes with that mapping and fires only on **the slots of that character** or **that slot**:

```skript
build a menu {_menu}:
    inventory type: chest inventory
    title: "Main Menu"
    layout: "AAA", "ABA", "AAA"
    id: "main_menu"
    edit:
        map key "A" to icon stone for {_menu} and when clicked:
            send "You clicked an A slot." to player
        override slot 4 in page 1 to diamond named "Middle" for {_menu} and when clicked:
            send "You clicked the middle." to player
```

### Two: a `when … clicked` section, attaching a callback to a slot on its own

```
(when|on) slot %numbers%
    [in page [(number|index)] %-numbers%]
    [of [(menu|gui)] %-menu%]
    [is] (clicked|interacted|pressed)
```

```skript
when slot 4 in page 1 of menu with id "main_menu" is clicked:
    send "You clicked slot 4!" to player

when slot 4 and 5 in page 1 of the menu {_menu} is clicked:
    send "You clicked 4 or 5." to player
```

**This one must have a section body**, otherwise registration reports
`You must provide a section to handle the slot click event.` Its advantage is that it can attach a
callback to a slot that **has no item mapped to it** — the so-called "invisible button": there is
nothing in the slot, but clicking it does something.

### Three: buttons, defined once and reused in many places

```
define [a] button %string%:
    icon: <item>
    when clicked:
        <code>
```

```skript
define button "my_button":
    icon: stone
    when clicked:
        send "You clicked the button!" to player

on load:
    build a menu {_menu}:
        inventory type: chest inventory
        title: "&6Menu"
        layout: "#####", "# A #", "#####"
        id: "main"
        edit:
            map key "A" to button "my_button" for {_menu}
```

In `define button %string%`, both `icon:` and `when clicked:` are required. Defining the same id again
**overwrites** the old one. `all buttons` lists every button id.

A button's callback is **shared**: any menu and any slot that maps this button runs the same code on a
click. To find out which menu it was inside the callback, use `the event-menu` in the menu event.

**One warning**: when `override slot … to button "x" for …:` carries a section body, the button takes
effect and the section is **ignored with a warning**
(`Both a button ID and a section were provided …`). Pick one.

## What a callback can read

A callback section runs in the context of the menu interact event, so every value in
[Events](events.md) is available:

```skript
map key "A" to icon stone for {_menu} and when clicked:
    send "You clicked slot %the clicked slot% on page %the page%." to player
    if the clicked icon is diamond:
        send "And it was a diamond." to player
```

`player` is the one who clicked, and `the event-menu` / `the event-menu window` are the menu and the window
of this interaction.

## Changing contents on the fly

`override slot` and `map key` take effect **when they run**: running them inside `on menu open` or
`on menu interact` changes what the player sees.

```skript
on menu interact:
    override slot 4 in page 1 to (the clicked icon) for the menu and refresh
```

In a `phantom` menu, without `and refresh` the change only goes into the model, and the screen still shows
the old contents; in a `static` menu the change is already in the real inventory, so the screen shows it
either way. This flag is exactly the difference between **what others see** and **what you see**:
`and refresh` refreshes the matching slots for **every player currently looking at that page**.

If you want to change **one player's** window only (showing him a highlight, say), use the **window's**
icon, see [Windows](windows.md):

```skript
set icon in slot 5 of the menu session of player to red stained glass pane
```
