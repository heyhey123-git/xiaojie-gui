# Creating Menus

[中文](menus.zh-CN.md) | English

## Two ways to write it

**The section form** (`create menu`) puts every property on one first line, and the contents in the
section:

```skript
create a static menu with chest inventory titled "Main Menu" with id "main_menu" with layout "AAA" and "ABA" and "AAA" with 100 ms click delay:
    override slot 4 in page 1 to diamond named "Special Item" for menu with id "main_menu"
```

**The entry form** (`build a menu`) writes the properties as `key: value` and puts the contents in an
`edit:` section:

```skript
build a menu {_menu}:
    mode: phantom
    inventory type: chest inventory
    title: "Main Menu"
    layout: "AAA", "ABA", "AAA"
    id: "main_menu"
    click delay: 100
    hide player inventory: true
    edit:
        override slot 4 in page 1 to diamond named "Special Item" for {_menu}
```

`build a menu {_menu}:` **stores the menu it builds in that variable**, which is also why the
`for {_menu}` below it works. `build a menu` is a section, so its first line needs the colon at the end.

## The parts of `create menu`

```
create [a] [phantom|static] menu
    with %inventorytype%
    titled <title>
    with layout %strings%
    [with player layout %strings%]
    [with id %string%]
    [with page %number%]
    [with %number% ms click delay]
    [with|without hide player inventory]
    [with|without locked icons]
```

The order is fixed: `with player layout` comes **after** `with layout`, `with id` comes after it, and
`with page` comes after `with id`. If you do not write `phantom` / `static`, it is `phantom`.

| Part | Required | Notes |
|---|---|---|
| `with %inventorytype%` | yes | the container type, see the next section |
| `titled …` | yes | the title, see [Titles](titles.md) |
| `with layout %strings%` | yes | the layout, see the next section |
| `with player layout %strings%` | no | the 4 rows below the container; giving one **hides the player's inventory**, see [Pages](pages.md#player-layout) |
| `with id %string%` | no | names the menu; **a menu with an id that already exists destroys the old one** |
| `with page %number%` | no | only decides which page `open menu` shows first, see [Pages](pages.md) |
| `with %number% ms click delay` | no | the minimum interval between two clicks, 50 milliseconds by default |
| `with hide player inventory` | no | hides the player inventory area below the menu |
| `with locked icons` | no | the slots the layout gives an icon to belong to the menu: they cannot be taken, filled, swapped or dropped, and a drag that touches one is refused as a whole |

`without hide player inventory` is the default; writing it out is for readability. It **only means anything in
a `phantom` menu**: a `static` window shows the player's real inventory, so there is nothing to hide, and
writing it on a static menu warns once instead of producing a menu that quietly looks wrong.

`with player layout` is the layout of the rows below the container, and writing it **turns the hide flag on
by itself**: those rows are the menu's own space only while the player's inventory is hidden, so asking for
the layout is asking for the half. `with hide player inventory` on its own is still the way to get that half
hidden but empty, and on a `static` menu a player layout warns once and changes nothing.

`with locked icons` makes **the slots the layout gives an icon to** the menu's own: no click, shift click,
number key, offhand swap, drop or double-click collect can take from them or put anything in, and a
**drag that touches one of them is refused as a whole** (the protocol cannot apply half a drag). The slots
the layout leaves empty stay entirely the player's, which is why a shop (icons) and a backpack (a page of
nothing but empty slots) are the same mechanism.

Which slots count as "has an icon" is deliberate: **the ones the layout names a key for**, plus the ones
`override slot` has covered. An item a script puts into one player's window at runtime with
`set icon in slot N of {_session}` does **not** count -- the first two are the menu's furniture, the same
for everyone, and the last one is that player's own content, which is usually exactly what they are meant
to be able to take.

**The player's own half of a `static` window is not the menu's, so only the operations that would land on a
menu slot are refused** -- the whole half is not closed off. A menu that really is designed that way is a
buying shop: the player puts what they are selling into the container, with a cancel button on the left of
the bottom row and a confirm button on the right:

```skript
create a static menu with chest inventory titled "&6Sell" with layout "         " and "         " and "C       " and "        S" with locked icons:
```

Only `C` and `S` are locked here. The empty slots in between are the buying area: the player clicks items
in, and **shift clicks** a whole stack in from their backpack, as usual. The decision is:

- **shift click** (from the backpack into the container): the game merges into a matching stack first and
  only then uses an empty slot, so it is refused only when **one of the menu's own slots holds a matching
  item that could still take more** -- which is where it would have gone -- and allowed otherwise.
  "Matching" means the same type and the same components: a different name is a different item, and the
  game would not merge them either;
- **double-click collect**: it gathers that item out of every slot that holds one, so it is refused only
  when **one of the menu's own slots really holds it**;
- everything else (a plain click, a right click, a number key, a drop, ...) touches nothing in the
  container and is allowed.

Phantom mode is a different story: that window is the server's own invention and nothing can move in it
anyway, so `locked icons` there only says "these slots are not somewhere to put things".

To flip that switch **at runtime**, use `lock the icons of %menu%` / `unlock the icons of %menu%`, and read
it with `if the icons of {_menu} are locked:`. It is the same switch the creation-time `with locked icons`
sets, and it takes effect on the **next** interaction (nothing on screen changes because of it):

```skript
edit menu with id "main_menu":
    unlock the icons of the menu with id "main_menu"
```

## Inventory types

`%inventorytype%` uses Skript's own inventory type, so the names are the names in Skript:

| Written as | Slots | Notes |
|---|---|---|
| `chest inventory` | rows × 9, see below | the only type whose size **comes from the layout** |
| `dropper inventory` | 9 | 3×3 |
| `dispenser inventory` | 9 | 3×3, and not the same window as dropper |
| `crafter inventory` | 9 | the crafter block. Like `workbench` it is 3×3, but the client draws a different window, so both exist |
| `workbench inventory` | 10 | the workbench: 1 result slot + the 3×3 grid |
| `anvil inventory` | 3 | |
| `beacon inventory` | 1 | only the slot the payment goes in |
| `blast furnace inventory` | 3 | |
| `furnace inventory` | 3 | |
| `smoker inventory` | 3 | |
| `grindstone inventory` | 3 | |
| `smithing inventory` | 4 | template, gear, addition, result |
| `loom inventory` | 4 | |
| `cartography table inventory` | 3 | **not** `cartography inventory` |
| `brewing stand inventory` | 5 | |
| `hopper inventory` | 5 | |
| `lectern inventory` | 1 | |
| `stonecutter inventory` | 2 | |
| `enchanting table inventory` | 2 | **not** `enchanting inventory` |
| `merchant inventory` | 3 | `villager inventory` also works. **Phantom menus only**, see below |
| `barrel inventory` | 27 | |
| `ender chest inventory` | 27 | |
| `shulker box inventory` | 27 | |

With `chest inventory` the number of rows is **the number of layout strings you give** (at most 6), so
`"AAA", "ABA", "AAA"` is a 3-row, 27-slot chest. Every other type has a fixed size, and the layout
strings only decide which slots are used.

A slot count is **the number of slots the client really draws for that window**, with the player's 27
inventory slots and 9 hotbar slots right after it. Getting it wrong does not raise an error — it moves
everything in the player's inventory and every click by a few slots — so this table is pinned to the same
table in a unit test, and a mistake there fails the build.

`merchant inventory` works in `phantom` mode, where the server draws the window itself. It does not work
as `static`: Bukkit will not create a merchant inventory (the trades come from the merchant API), and
`create a static menu with merchant inventory ...` says so and stops.

A type other than those above reports `Unsupported inventory type`. `crafting table inventory` (the
player's own 2×2 crafting grid) is deliberately one of them: no client menu draws that window, so opening
it would draw a crafting table instead; use `workbench inventory` for a crafting-table window.

## What a layout string is

A layout is **one string per row**, and each character is one slot, starting at the top left, going left
to right and top to bottom.

Rows are joined with `and`. A comma-separated list means exactly the same thing, but Skript prints
`List is missing 'and' or 'or', defaulting to 'and'` while such a script loads, so the examples here use
`and`; to keep the commas, write `suppress missing conjunction warnings` on the line before, or set
`disable variable missing and/or warnings: true` in `plugins/Skript/config.sk`.

```skript
with layout "#########" and "#  A  B #" and "#########"
```

- One string = one row, no longer than 9 characters (for anything other than a chest, the container's
  width decides).
- Every character occupies one slot, and **a space is a slot too**; it just is not mapped to any key, so
  it stays empty. In the example above the second row is nine characters: `#`, space, space, `A`, space,
  space, `B`, space, `#`.
- A character is only a name and has no built-in meaning: `#` does not turn into glass by itself, and
  `A` does not turn into a diamond. What decides the appearance is `map key` (see
  [Filling Menus](filling-menus.md)).
- The same character may appear more than once in a layout, and those slots are mapped together and
  changed together.

**How slot numbers are counted**: row 1 is 0–8, row 2 is 9–17, row 3 is 18–26, and so on. So the `A` in
`"#########", "#  A  B #"` is slot 12.

**To use a long name as a key**, wrap the name in backticks; the whole `` `name` `` occupies one slot:

```skript
with layout "`previous`AAA`next`"
map key "previous" to icon paper named "&ePrevious" for {_menu}
```

One character is one key, so the character in the layout and the string in `map key` have to be
**exactly the same** (case-sensitive). Using a character in `map key` that the layout does not contain
does not report an error, but the item never appears in any slot — that is the most common reason for
"I mapped it and there is nothing there".

## Changing an existing menu with `edit menu`

```skript
edit menu with id "main_menu":
    override slot 0 in page 1 to diamond named "New Item" for menu with id "main_menu"
```

`(edit|change) [the] [(menu|gui)] %menu%` does not change any property by itself; it only provides a
"current menu" context, so that `insert page …` and the like inside the section work without naming the
menu. To change a property, use the matching expression, for example
`set the default title of {_menu} to "…"`.
