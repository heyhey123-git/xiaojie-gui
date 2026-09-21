# xiaojie-gui

English | [简体中文](docs/README-zh.md)

Inventory GUI framework for Bukkit/Paper-based Minecraft servers, written in Kotlin. It provides a clean API for building menu UIs with layouts, pages, cooldowns, and packet-level receptacles, and includes first-class Skript integration.

- **Modern API Design**: A Kotlin-first approach provides an expressive and concise API while maintaining full compatibility with Java.
- **Powerful Menu System**: Simplifies complex GUI construction with support for declarative layouts, multipage pagination, and both static and virtual receptacles.
- **Window-based & Event-driven**: Gives each player an independent menu window and allows for fine-grained logical control through a rich set of custom events (open, close, interaction, etc.).
- **Deep Skript Integration**: Offers first-class Skript support, enabling users without programming experience to create and manage menus through simple scripts.
- **Reliable & Tested**: Core functionalities are unit-tested to ensure the framework's stability and reliability.

## Requirements

- Paper server or maybe Paper's fork (Spigot/Bukkit compatibility is not guaranteed)
- JDK compatible with your server runtime

plugins:
- [Skript](https://github.com/SkriptLang/Skript/releases)
- [PacketEvents](https://github.com/retrooper/packetevents/releases)
- [SkBee](https://github.com/ShaneBeee/SkBee/releases) (optional, for using textcomponent in menu titles)

## Build and test

- Windows (PowerShell or Command Prompt):
    - Build and run tests:
      ```bash
      gradlew.bat :clean :build
      ```
    - Run tests:
      ```bash
      gradlew.bat :test
      ```
    - Build without tests:
      ```bash
      gradlew.bat :clean :build -x test
      ```
      or
      ```bash
      gradlew.bat :clean :shadowJar
      ```
- Artifacts are in 'build/dist'.

## Installation (as a server plugin)

1. Build the project or download a release jar.
2. Copy the generated \*.jar from 'build/dist' to your server's 'plugins' folder.
3. Start/restart the server. The plugin is declared in 'src/main/resources/plugin.yml'.

## Concepts

- Layout: declarative slot arrangement and key-to-slot mapping.
- Menu: a logical GUI definition combining layout, pages, and behaviors.
- Page: paginated view of components within a menu.
- MenuSession: per-player window state with lifecycle (open/close/refresh).
- Receptacle: low-level inventory container abstraction (static/view/phantom).
- Events: open, close, interact, page turn, and receptacle events.

### Pages

A menu is a single page by default, and that is the normal case: the `layout` you give when creating it
becomes page 1, and everything you map onto it lives there. Paging is something you add when you need it:

- `insert page [N] ... with layout "..."` adds a page (at the end, or at position `N`).
- `turn to page N for {_player}` moves a viewer, and `on page turn` is the event to hook.
- `with page N` (or `default page`) only decides which page `open menu` shows first; it never changes how
  many pages exist.

If you would rather manage the state yourself, change nothing: keep one page and swap what is in it with
`override slot` / `map key`, keeping the "page number" in a script variable of your own.

## Skript integration

An example that runs — the test suite boots a server with Skript and executes exactly this, so it cannot
quietly stop working. It builds a one-page menu with two buttons and a command that opens it:

```skript
on load:
    build a menu {_menu}:
        inventory type: chest inventory
        title: "&6Main Menu"
        layout: "#########", "#  A  B #", "#########"
        id: "main"
        edit:
            map key "A" to icon diamond named "&bSay hello" for {_menu} and when clicked:
                send "Hello!" to player
            map key "B" to icon clock named "&eClose" for {_menu} and when clicked:
                close the menu for player

command /menu:
    trigger:
        set {_menu} to the menu with id "main"
        open menu {_menu} for player
```

The layout strings are the menu's shape, one string per row, and each character names a slot you can fill
with `map key` or `override slot`. The [wiki](https://github.com/heyhey123-git/xiaojie-gui/wiki) is the
syntax reference: every statement, what it takes, and the pitfalls worth knowing.

## Contributing

Feel free to dive in! Open an issue or submit pull requests (PRs). `CONTRIBUTION.md` explains how this
repository is built, tested and styled.

## Disclaimer

APIs may evolve. For exact usage, consult the source files in 'src/main/kotlin' and the unit tests in 'src/test/kotlin'.

## License

Copyright (c) 2025 heyhey123, All rights reserved.
This project is licensed under the AGPL-3.0 License. See the LICENSE file for details.

## Acknowledgements

- [TrMenu](https://github.com/CoderKuo/TrMenu): Inspiration for menu systems in Minecraft plugins, providing us the details of underlying implementations of the operations to a GUI.
- [Kotlin](https://github.com/JetBrains/kotlin): The programming language used for development.
- [PaperMC](https://github.com/PaperMC/Paper): The server platform for which this framework is designed.
- [Mineflayer](https://github.com/PrismarineJS/mineflayer): Powers automated client tests that help check menu rendering and interactions.

Without these projects, this framework would not be possible. Thank you to all the contributors and maintainers of these projects!
