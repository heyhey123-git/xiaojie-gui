# xiaojie-gui

[English](../README.md) | 简体中文

xiaojie-gui 是一个面向 Paper 服务器的物品栏菜单插件。你可以用 Skript 编写菜单。

从一个按钮到多页商店，菜单都由布局、页面和交互逻辑组成：

- **布局与分页**：用字符串安排格子，按需添加页面。
- **两种模式**：使用真实物品栏，或只向客户端展示虚拟窗口。
- **独立窗口**：同一个菜单可以同时打开给多位玩家，各自保留窗口状态。
- **事件与按钮**：响应打开、关闭、点击和翻页，也可以定义可复用的按钮。

项目包含单元测试和服务端测试；下方的 Skript 示例也会在测试中运行。

## 运行要求

- Paper 服务器或其衍生版，不保证兼容 Spigot/Bukkit。
- 与服务器运行环境兼容的 JDK。

所需插件：
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

## 安装

1. 下载发布版 jar，或自行构建项目。
2. 将下载的 jar（自行构建时位于 `build/dist`）放入服务器的 `plugins` 文件夹，并安装所需依赖。
3. 启动或重启服务器。

## 先认识几个概念

- **布局（Layout）**：规定格子如何排列，以及每个字符对应哪些格子。
- **菜单（Menu）**：定义整个菜单的布局、页面和交互逻辑。
- **页面（Page）**：菜单中的一页，保存这一页的布局和内容。
- **菜单窗口（MenuSession）**：记录某位玩家当前打开的菜单及其状态。
- **容器（Receptacle）**：负责底层物品栏窗口的展示和操作。
- **事件（Events）**：让脚本在打开、关闭、交互或翻页时作出响应。

### 关于页面

菜单默认只有一页。创建时填写的 `layout` 就是第 1 页的布局，映射的物品也放在这一页。需要更多页面时，再添加即可：

- `insert page [N] ... with layout "..."`：新增一页（默认追加到末尾，给了 `N` 就插到该位置）。
- `turn to page N for {_玩家}`：给某个玩家翻页，配合 `on page turn` 事件做后续处理。
- `with page N`（或 `default page`）**只决定 `open menu` 先显示第几页**，不会改变菜单有几页。

也可以只保留一页，用 `override slot` 或 `map key` 替换显示内容，再用脚本变量记录当前“页码”。这种做法由脚本自行管理分页。

## Skript 集成

下面的示例创建一个带有两个按钮的单页菜单，并通过 `/menu` 命令打开。服务端测试会原样执行这段脚本：

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

`layout` 中的每个字符串对应一行，每个字符对应一个格子。用 `map key` 按字符填充，或用 `override slot` 指定格子。

更多语法、参数和使用注意事项，请参阅 [wiki](https://github.com/heyhey123-git/xiaojie-gui/wiki)。

## 贡献

欢迎参与！可以提出 issue 或提交拉取请求 (PR)。构建、测试与代码风格说明见 `CONTRIBUTION.md`。

## 免责声明

API 可能会发生变化。要了解确切用法，请查阅 `src/main/kotlin` 中的源文件和 `src/test/kotlin` 中的单元测试。

## 许可证

Copyright (c) 2025 heyhey123, All rights reserved.
本项目采用 AGPL-3.0 许可证。详情请见 LICENSE 文件。

## 鸣谢

- [TrMenu](https://github.com/CoderKuo/TrMenu)：为菜单设计和底层实现提供参考。
- [Kotlin](https://github.com/JetBrains/kotlin)：本项目使用的编程语言。
- [PaperMC](https://github.com/PaperMC/Paper)：本框架面向的服务器平台。
- [Mineflayer](https://github.com/PrismarineJS/mineflayer)：为客户端自动化测试提供支持，帮助检查菜单显示与交互。

感谢这些项目的贡献者与维护者，也感谢每一位使用、反馈和改进本插件的人。
