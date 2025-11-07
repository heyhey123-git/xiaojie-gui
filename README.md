# xiaojie-gui

English | [简体中文](docs/README-zh.md)

Inventory GUI framework for Bukkit/Paper-based Minecraft servers, written in Kotlin. It provides a clean API for building menu UIs with layouts, pages, cooldowns, and packet-level receptacles, and includes first-class Skript integration.

- **Modern API Design**: A Kotlin-first approach provides an expressive and concise API while maintaining full compatibility with Java.
- **Powerful Menu System**: Simplifies complex GUI construction with support for declarative layouts, multipage pagination, and both static and virtual receptacles.
- **Session-based & Event-driven**: Provides independent menu sessions for each player and allows for fine-grained logical control through a rich set of custom events (open, close, interaction, etc.).
- **Deep Skript Integration**: Offers first-class Skript support, enabling users without programming experience to create and manage menus through simple scripts.
- **Reliable & Tested**: Core functionalities are unit-tested to ensure the framework's stability and reliability.

## Requirements

- Paper server or maybe Paper's fork (Spigot/Bukkit compatibility is not guaranteed)
- JDK compatible with your server runtime

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
3. Configure 'src/main/resources/config.yml' as needed.
4. Start/restart the server. The plugin is declared in 'src/main/resources/plugin.yml'.

## Concepts

- Layout: declarative slot arrangement and key-to-slot mapping.
- Menu: a logical GUI definition combining layout, pages, and behaviors.
- Page: paginated view of components within a menu.
- MenuSession: per-player session state with lifecycle (open/close/refresh).
- Receptacle: low-level inventory container abstraction (static/view/phantom).
- Events: open, close, interact, page turn, and receptacle events.

## Skript integration

Example (pseudo-Skript):
```skript
on load:
    #> -------------
    #> 菜单 Shape
    #> -------------
    add "#########" to {_shape::*}
    add "#AAAAAAA#" to {_shape::*}
    add "#AAAAAAA#" to {_shape::*}
    add "#AAAAAAA#" to {_shape::*}
    add "#AAAAAAA#" to {_shape::*}
    add "##<#x#>##" to {_shape::*}
    create phantom menu with (chest inventory) titled "<white>awa" with layout {_shape::*}:
        #> -------------------
        #> 无需物品的回调执行
        #> -------------------
        when slot 5 in page 0 is clicked:
            send "不要再戳我啦!" to event-player
        #> ------
        #> 翻页
        #> ------
        if {_page} > 1:
            map key "<" to icon paper named "&eprevious page" for event-menu:
                turn to page {_page} - 1 for {_p}
        else if {_page} = 1:
            map key "<" to icon gray stained glass pane for event-menu
        if {_page} < {_maxpage}:
            map key ">" to icon paper named "&enext page" for event-menu:
                turn to page {_page} + 1 for {_p}
        else:
            map key ">" to icon gray stained glass pane for event-menu
        #> -------------------
        #> Fill specifit area with items
        #> -------------------
        map key "A" to items {-_allSkull::*} for event-menu:
            set {_teleporter} to "%{_player}%"
            set {_server} to server of vplayer {_teleporter}
            functionawa({_teleporter},{_server})
        map key "x" to icon clock named "&ereturn to menu" for event-menu:
            close menu for {_p}
        #> when you "Create menu", If the "Default Page" didn't set, we will insert "Page 0" as default.
        open menu event-menu to {_p}
```

## Contributing

Feel free to dive in! Open an issue or submit pull requests (PRs).

## Disclaimer

APIs may evolve. For exact usage, consult the source files in 'src/main/kotlin' and the unit tests in 'src/test/kotlin'.

## License

Copyright (c) 2025 heyhey123, All rights reserved.
This project is licensed under the AGPL-3.0 License. See the LICENSE file for details.

## Acknowledgements

- [TrMenu](https://github.com/CoderKuo/TrMenu): Inspiration for menu systems in Minecraft plugins, providing us the details of underlying implementations of the operations to a GUI.
- [Kotlin](https://github.com/JetBrains/kotlin): The programming language used for development.
- [PaperMC](https://github.com/PaperMC/Paper): The server platform for which this framework is designed.

Without these projects, this framework would not be possible. Thank you to all the contributors and maintainers of these projects!
