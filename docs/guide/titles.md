# Titles

[中文](titles.zh-CN.md) | English

Everywhere a title can be written — `create menu … titled …`, `insert page … with title …`,
`update title of page N in {_menu} to …`, `update title of the menu session … to …`,
`turn to page N for player with new title …`, and `title:` in `build a menu` — the usage is
**exactly the same**: just give the value.

## Plain text

```skript
create a phantom menu with chest inventory titled "Main Menu" with layout "#########" with id "main":
update title of page 1 in {_menu} to "Page One"
```

## Do not write `string:` or `component:`

```skript
update title of page 1 in {_menu} to "Page One"          # right
update title of page 1 in {_menu} to string:"Page One"   # wrong
```

`string:` and `component:` are **branch markers inside the plugin's own patterns**, not script syntax.
Of the input, only what follows the colon is the title; if you hand-write the prefix, the whole text
(prefix included) becomes the title. Older documentation used that form; since 2.0.0 it is an
**explicit error** instead of rendering a title that begins with `string:`.

## `&` colour codes

A title is text, so the `&` colour codes inside it are formatting, **available just as they are
anywhere else in Skript**:

```skript
create a phantom menu with chest inventory titled "&6&lMain Menu" with layout "#########" with id "main":
```

`§` works as well (`&` is translated into it internally). The accepted characters are Spigot's set:
`0`-`9` and `a`-`f` are colours, `k`-`o` are formats, and `r` resets.

**Side effect**: an `&` followed by a colour character is taken as a colour code. So to show a
**literal** `&B` in a title, `&` cannot do it — exactly as with Spigot's item names.

## Hexadecimal colours

Use Adventure's `&x&r&r&g&g&b&b` form, **with every digit carrying its own `&`**:

```skript
create a phantom menu with chest inventory titled "&x&f&f&5&5&0&0Orange Title" with layout "#########" with id "main":
```

`&#ff5500` is **not** part of this syntax; writing it gives the literal `&#ff5500`.

## Text components (requires SkBee)

With SkBee installed, a title can be a text component directly:

```skript
set {_title} to a new text component from "Main Menu"
create a phantom menu with chest inventory titled {_title} with layout "#########" with id "skbee_title":
```

This path makes two differences:

- A component can carry the full formatting SkBee provides (gradients, click events, fonts, ...),
  without the limits of the `&` set.
- A title **read back** is also a SkBee component (`the title of page 1 in {_menu}` and the like),
  which prints as its legacy text form.

The `textcomponent` type is **not** written into the pattern: an unregistered type in a pattern makes
the whole pattern fail to compile, and then a server without SkBee could not even use a title like
`"plain text"`. So the title slot's type is `%-object%`, and whether a string or a component arrives is
decided by the plugin at runtime.

Without SkBee, anything other than a string is not a title: `titled {_someItem}` reports
`Valid Menu title is required.` at runtime (`create menu`) or
`The given menu title is not a textcomponent, and cannot be converted to string.` (the other syntax).

## Per-page titles vs per-session titles

| What you want to change | How to write it | What it affects |
|---|---|---|
| one page's title (permanently) | `update title of page 1 in {_menu} to "…"` | that page; adding `and refresh` notifies the players looking at **that page** |
| one session's window title (one person only) | `update title of the menu session of player to "…" and refresh` | that one player's window |
| the value directly | `set the title of page 1 in {_menu} to "…"` | the model only, with no push |

`update title of page N …` **only** retitles the players who are looking at page N; someone on page 2
keeps seeing page 2's title and is not overwritten by another page's title. Add `and refresh` to see
the effect immediately, otherwise it waits for the next refresh or page turn.

There is also a `the title` in `on page turn`: it is "the title of the page being turned to", it **can
be set** in the event, and it applies to this one turn only:

```skript
on page turn:
    set the title to "Chapter %the future page%"
```

## Reading titles

```skript
set {_title} to the default title of {_menu}
set {_pageTitle} to the title of page 1 in {_menu}
send "The page 1 title is %{_pageTitle}%" to player
```

`the default title of {_menu}` is "what pages added later are called by default"; it is the value
`titled …` gave.

**Without SkBee** these expressions return a string, and `%…%` prints it as text with `§` in it;
**with SkBee** they return a component, and `%…%` prints its legacy text form. Neither is "the plain
colour stripped out", so be careful when comparing them.
