# Events and Event Values

[中文](events.zh-CN.md) | English

## The four events

| Event | When it fires | Cancellable |
|---|---|---|
| `on menu open` | before a menu opens for a player | yes |
| `on menu interact` | a player clicked a slot in the menu | yes |
| `on page turn` | a player turns a page | yes |
| `on menu close` | the menu closes (the player closes it, code closes it, it is destroyed) | no |

```skript
on menu open:
    send "You opened the menu, and you are on page %the page%." to player

on menu interact:
    if the event-clicktype is left mouse button:
        send "Left click on slot %the clicked slot%." to player

on page turn:
    send "Turning from page %the page% to page %the future page%." to player

on menu close:
    send "The menu closed." to player
```

`cancel event` in `on menu open` and `on page turn` **really works**: cancel the open and the menu does
not appear; cancel the turn and the player stays on the page he was on.

## The two ways to write an event value

This plugin's own event values are all written the way a script would say them, **without the `event-`
prefix**:

| Value | Meaning | In which events |
|---|---|---|
| `the clicked slot` | the number of the slot that was clicked (0-based) | `on menu interact` |
| `the clicked icon` | the item in the slot that was clicked | `on menu interact` |
| `the pressed number key` | the number key that was pressed, 1–9; nothing for any other click | `on menu interact` |
| `the page` | which page the event happened on | `on menu open`, `on menu interact`, `on page turn` (the page it is leaving) |
| `the future page` | which page it is turning to | `on page turn` |
| `the title` | the title of the page being turned to (settable) | `on page turn` |
| `the menu` | the event's menu | all four |
| `the session` | the event's menu session | all four |

The `the event-` forms (except `the event-slot`, which collides with Skript's own `event-slot`) are kept
as aliases, for scripts already written that way: `the event-page`, `the event-title` and
`the event-icon` all work, with exactly the values above.

`the clicked icon` also has the shorter historical alias `the icon`. `the pressed number key` has no
`event-` form — `the event-pressed number key` is not something a person says.

### The boundaries of `the future page`

`the future page` **only has a value in `on page turn`**. In `on menu open` and `on menu interact` it is
nothing, rather than "the current page" — the latter would leave a script unable to tell a page-turn
event from an open event. To get a page number in an open event, use `the page`.

`the title` is the same: it is readable and writable only in `on page turn`.

## Click types

The click type in `on menu interact` is **Skript's own click type**, the one `on inventory click` gives
you, written with Skript's words and its literals:

```skript
on menu interact:
    if the click type is left mouse button:
        send "Left click!" to player
    if the event-clicktype is left mouse button with shift:
        send "Shift + left click." to player
```

`the click type` and `the event-clicktype` are **the same value** in `on menu interact`, and both work.

- `the click type` itself belongs to Skript's `on inventory click` and refuses to parse in other events;
  the plugin registers this property for its own event, so the form without the prefix also gets an
  answer here.
- `the event-clicktype` is Skript's general event-value form and goes through the converter the plugin
  registers (turning the plugin's finer-grained click type — which knows the key and the click mode —
  into Bukkit's).

The available literals are Skript's set: `left mouse button`, `right mouse button`,
`left mouse button with shift`, `right mouse button with shift`, `middle mouse button`, `number key`,
`double click`, `drop key`, `swap offhand key` and so on. Do **not** write the plugin's `LEFT` or
`LEFT_CLICK` any more: the old `menu click type` / `menu click mode` types were removed in 2.0.0, and
they printed as names like `LEFT` that cannot be written in a script.

### Number keys: `the pressed number key`

The one piece of information Skript's click type cannot carry is **which number key was pressed** — all
nine keys report as the same `number key`. `the pressed number key` fills that in:

```skript
on menu interact:
    if the pressed number key is 1:
        send "You pressed 1!" to player
```

On a `number key` click it is 1 to 9, and on any other click it is nothing. Together with
`when slot … is clicked` that makes shortcuts: leave a slot in the layout without an item mapped to it,
attach an invisible button with `when slot 9 in page 1 of {_menu} is clicked:`, and then test
`the pressed number key`.

## Who is watching this event

`player` is the event's player, the same one as the session's `viewer`:

```skript
on menu interact:
    send "You clicked %the clicked slot%." to player
    loop the menu viewers of the event-menu:
        send "Someone clicked the menu." to loop-player
```

Note that a menu can be **open for several people at once**, and each of them can be on a different
page. `on menu interact` fires only for the person who clicked. To change what others see, use
`the menu viewers of {_menu}` or one of the `and refresh` forms that notify every viewer, see
[Sessions](sessions.md). (`the viewers of …` is answered by Skript's own property on 2.16, see that
page.)

## Cancelling

```skript
on menu open:
    if player doesn't have permission "shop.open":
        cancel event
        send "You do not have permission to open this menu." to player
```

A cancelled `on menu interact` does not run the slot callbacks that are already attached — put the
other way round, the callback runs **first** and the event is decided **after**, so a callback cannot
"block and then let through"; to block it, `stop` inside the callback yourself.
