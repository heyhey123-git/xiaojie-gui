# xiaojie-gui

[English](../README.md)|简体中文

一个为基于 Bukkit/Paper 的 Minecraft 服务器设计的物品栏 GUI 框架，使用 Kotlin 编写。它提供了一个简洁的 API，用于构建带有布局、页面、冷却和数据包级别容器的菜单用户界面，并包含一流的 Skript 集成。

- Kotlin 优先的 API，兼容 Java
- 基于物品栏的菜单，支持页面和布局
- 静态和虚拟容器
- 与玩家绑定的菜单会话和自定义事件
- 用于菜单组件的冷却工具
- 用于无代码使用的 Skript 效果/表达式
- 针对布局、页面和容器进行了单元测试
- 使用 `src/main` 和 `src/test` 分离的 Gradle 构建

## 要求

- Paper 服务器或其衍生版 (不保证与 Spigot/Bukkit 的兼容性)
- 与您的服务器运行时兼容的 JDK

## 构建和测试

- Windows (PowerShell 或命令提示符):
    - 构建并运行测试:
      ```bash
      gradlew.bat clean build
      ```
    - 运行测试:
      ```bash
      gradlew.bat test
      ```
    - 构建（不含测试）:
      ```bash
      gradlew.bat clean build -x test
      ```
      或
      ```bash
      gradlew.bat clean shadowJar
      ```
- 构建产物位于 `build/dist` 目录中。

## 安装 (作为服务器插件)

1. 构建项目或下载一个发布版的 jar 文件。
2. 将 `build/dist` 目录中生成的 \*.jar 文件复制到您服务器的 `plugins` 文件夹中。
3. 根据需要配置 `src/main/resources/config.yml`。
4. 启动/重启服务器。插件已在 `src/main/resources/plugin.yml` 中声明。

## 概念

- **布局 (Layout)**: 声明式的格子排列和键到格子的映射。
- **菜单 (Menu)**: 一个逻辑上的 GUI 定义，结合了布局、页面和行为。
- **页面 (Page)**: 菜单中组件的分页视图。
- **菜单会话 (MenuSession)**: 每个玩家的会话状态，具有生命周期 (打开/关闭/刷新)。
- **容器 (Receptacle)**: 底层的物品栏容器抽象 (静态/视图/虚拟)。
- **事件 (Events)**: 打开、关闭、交互、翻页和容器事件。

## Skript 集成

示例 (伪 Skript 代码):
```skript
# 伪代码：确切语法请参考 'skript/*' 中的 Skript 元素。

command /openexample:
  trigger:
    # 为玩家打开一个预定义的菜单
    open menu "example" to player

on inventory click:
  # 您可以根据需要检查会话/菜单属性或取消点击事件
  if slot is 5:
    cancel event
```

## 贡献

欢迎参与！可以提出 issue 或提交拉取请求 (PR)。

## 免责声明

API 可能会发生变化。要了解确切用法，请查阅 `src/main/kotlin` 中的源文件和 `src/test/kotlin` 中的单元测试。

## 许可证

Copyright (c) 2025 heyhey123, All rights reserved.
本项目采用 AGPL-3.0 许可证。详情请见 LICENSE 文件。
