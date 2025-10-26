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
# Pseudo-code: consult the Skript elements in 'skript/*' for exact syntax.

command /openexample:
  trigger:
    # Open a predefined menu for the player
    open menu "example" to player

on inventory click:
  # You can inspect session/menu properties or cancel clicks as needed
  if slot is 5:
    cancel event
```

## Contributing

Feel free to dive in! Open an issue or submit pull requests (PRs).

## Disclaimer

APIs may evolve. For exact usage, consult the source files in 'src/main/kotlin' and the unit tests in 'src/test/kotlin'.

## License

Copyright (c) 2025 heyhey123, All rights reserved.
This project is licensed under the AGPL-3.0 License. See the LICENSE file for details.
