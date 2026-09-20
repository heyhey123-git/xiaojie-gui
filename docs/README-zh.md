# xiaojie-gui

[English](../README.md) | 简体中文

一个为基于 Bukkit/Paper 的 Minecraft 服务器设计的物品栏 GUI 框架，使用 Kotlin 编写。它提供了一个简洁的 API，用于构建带有布局、页面、冷却和数据包级别容器的菜单用户界面，并包含一流的 Skript 集成。

- **现代化的 API 设计**: 采用 Kotlin 优先的原则，提供富有表现力且简洁的 API，同时保持与 Java 的完全兼容。
- **强大的菜单系统**: 支持声明式布局、多页面分页以及静态和虚拟容器，让复杂的 GUI 构建变得简单。
- **窗口与事件驱动**: 为每个玩家提供独立的菜单窗口，并通过丰富的自定义事件（打开、关闭、交互等）实现精细的逻辑控制。
- **深度 Skript 集成**: 提供一流的 Skript 支持，让不熟悉编程的用户也能通过简单的脚本创建和管理菜单。
- **可靠且经过测试**: 核心功能均经过单元测试，确保框架的稳定性和可靠性。

## 要求

- Paper 服务器或其衍生版 (不保证与 Spigot/Bukkit 的兼容性)
- 与您的服务器运行时兼容的 JDK

依赖插件:
- [Skript](https://github.com/SkriptLang/Skript/releases)
- [PacketEvents](https://github.com/retrooper/packetevents/releases)
- [SkBee](https://github.com/ShaneBeee/SkBee/releases) (可选，用于在菜单标题中使用 textcomponent)

## 构建和测试

- Windows (PowerShell 或命令提示符):
    - 构建并运行测试:
      ```bash
      gradlew.bat :clean :build
      ```
    - 运行测试:
      ```bash
      gradlew.bat :test
      ```
    - 构建（不含测试）:
      ```bash
      gradlew.bat :clean :build -x test
      ```
      或
      ```bash
      gradlew.bat :clean :shadowJar
      ```
- 构建产物位于 `build/dist` 目录中。

## 安装 (作为服务器插件)

1. 构建项目或下载一个发布版的 jar 文件。
2. 将 `build/dist` 目录中生成的 \*.jar 文件复制到您服务器的 `plugins` 文件夹中。
3. 启动/重启服务器。插件已在 `src/main/resources/plugin.yml` 中声明。

## 概念

- **布局 (Layout)**: 声明式的格子排列和键到格子的映射。
- **菜单 (Menu)**: 一个逻辑上的 GUI 定义，结合了布局、页面和行为。
- **页面 (Page)**: 菜单中组件的分页视图。
- **菜单窗口 (MenuSession)**: 每个玩家的窗口状态，具有生命周期 (打开/关闭/刷新)。
- **容器 (Receptacle)**: 底层的物品栏容器抽象 (静态/视图/虚拟)。
- **事件 (Events)**: 打开、关闭、交互、翻页和容器事件。

### 关于页面

菜单**默认就是单页**，这也是最常见的用法：创建菜单时给出的 `layout` 会成为第 1 页，你在它上面映射
的东西就都在这一页里。分页是按需添加的：

- `insert page [N] ... with layout "..."`：新增一页（默认追加到末尾，给了 `N` 就插到该位置）。
- `turn to page N for {_玩家}`：给某个玩家翻页，配合 `on page turn` 事件做后续处理。
- `with page N`（或 `default page`）**只决定 `open menu` 先显示第几页**，不会改变菜单有几页。

如果你想自己管状态，什么都不用改：只保留一页，用 `override slot` / `map key` 换页内容，把"页码"
存在自己的脚本变量里即可。

## Skript 集成

下面这个例子是**真能跑的** —— 测试套件会启动一个带 Skript 的服务端，原样执行它，所以它不会悄悄失效。
它建了一个单页菜单、两个按钮，以及一个打开它的命令：

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

`layout` 的字符串就是菜单的形状，一行一个字符串，每个字符代表一个可以用 `map key` 或
`override slot` 填充的槽位。完整的语法参考在 [wiki](https://github.com/heyhey123-git/xiaojie-gui/wiki)：
每条语句怎么写、参数是什么、以及哪些坑值得提前知道。

## 贡献

欢迎参与！可以提出 issue 或提交拉取请求 (PR)。构建、测试与代码风格说明见 `CONTRIBUTION.md`。

## 免责声明

API 可能会发生变化。要了解确切用法，请查阅 `src/main/kotlin` 中的源文件和 `src/test/kotlin` 中的单元测试。

## 许可证

Copyright (c) 2025 heyhey123, All rights reserved.
本项目采用 AGPL-3.0 许可证。详情请见 LICENSE 文件。

## 鸣谢

- [TrMenu](https://github.com/CoderKuo/TrMenu)：为插件的菜单系统提供灵感，揭示了实现 GUI 操作的底层细节。
- [Kotlin](https://github.com/JetBrains/kotlin)：本项目使用的编程语言。
- [PaperMC](https://github.com/PaperMC/Paper)：本框架面向的服务器平台。

没有这些项目，本框架无法实现。感谢所有贡献者与维护者！
