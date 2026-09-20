# Configuration

[中文](configuration.zh-CN.md) | English

The plugin has no commands, no permission nodes and no options, so it writes no config file:
`plugins/xiaojie-gui/` holds only what a script puts there.

## The main-thread check

Menu operations change real inventories, so they have to run on the server's main thread. If one of the
operations below is called on an **asynchronous thread**, it writes a line of explanation to the console
(and **gives up that operation**) instead of carrying on with illegal state:

- `open menu …` / `show menu …`
- `close the menu for …` / `close the menu session …`
- `turn to page …`
- `update title of the menu session …`
- `set icon in slot … of %menusession%`

What it prints looks like this (it includes the current statement):

```
Menu can only be opened from the main server thread, but got called from an asynchronous thread: ...
```

The check cannot be turned off, because an asynchronous call here is never legal -- and it is only ever
subtle (a delayed `run task later` inside an asynchronous `execute`, another plugin's asynchronous
callback, a custom event declared `on … async`).

## The banner

The banner carries its own true-colour escape sequences, unless the console detection Paper uses -- the
`net.kyori.ansi.colorLevel` system property -- says the console would print them as text, and then the
banner is plain. The log file is always plain: Paper's file appender drops the sequences on the way into
`logs/latest.log`.
