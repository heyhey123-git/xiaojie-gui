# Configuration

[中文](configuration.zh-CN.md) | English

The plugin has no commands and no permission nodes. The only thing that can be configured is
`plugins/xiaojie-gui/config.yml`, generated automatically on the first start, with just two keys:

```yaml
enable-async-check: true

# Print the banner in full colour. The colours are written as escape sequences by the plugin itself, so
# Paper's own idea of what the console supports does not decide it. Set this to false on a console that
# would print the escapes as text; the banner is then printed without colour.
force-truecolor: true
```

| Key | Default | What it does |
|---|---|---|
| `enable-async-check` | `true` | checks whether menu operations run on the main thread, and reports a readable one-line error instead of letting the server crash when they do not |
| `force-truecolor` | `true` | whether the startup banner is printed with true-colour escape sequences |

## `enable-async-check`

When it is on, the operations below write a line of explanation to the console if they are called on an
**asynchronous thread** (and **give up that operation**), instead of carrying on with illegal state:

- `open menu …` / `show menu …`
- `close the menu for …` / `close the menu session …`
- `turn to page …`
- `update title of the menu session …`
- `set icon in slot … of %menusession%`

What it prints looks like this (it includes the current statement):

```
Menu can only be opened from the main server thread, but got called from an asynchronous thread: ...
```

**Keeping it `true` is recommended.** There are not many places in Skript that cause an asynchronous
call, but they are all subtle (a delayed `run task later` inside an asynchronous `execute`, another
plugin's asynchronous callback, a custom event declared `on … async`). Setting it to `false` only turns
the check off; it does not make an asynchronous call legal.

## `force-truecolor`

The plugin writes ANSI true-colour escape sequences into the banner itself instead of leaving it to
Paper — Paper decides whether the console can show colour, and when it decides it cannot, the banner
turns grey.

**If the console prints the escape sequences as they are** (some older Windows terminals, or a log
redirected to a file and opened later), set this to `false`: the banner is printed in a colourless
version and nothing else changes.

## These keys used to be broken

`enable-async-check` and `force-truecolor` used to be read from Spigot's own configuration file, so the
values written in the plugin's `config.yml` **did nothing at all**. Since 2.0.0 they are read from
`plugins/xiaojie-gui/config.yml`. **The key names have not changed**, so if your server has had these
two keys all along, they **only start taking effect** after the upgrade — if you had
`force-truecolor: false` in your configuration and kept seeing a coloured banner, this is why.

Restart the server after changing them: the two values are read once, the first time they are used
after the plugin enables, and not again.
