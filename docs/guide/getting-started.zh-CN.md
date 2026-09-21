# 快速上手

[English](getting-started.md) | 简体中文

不妨从一个能用的小菜单起步，边写边熟悉其中的细节。不必一开始就记住所有语法；等第一个按钮有了回应，再循序渐进地添上你需要的功能。

## 安装

1. 把 `xiaojiegui-<版本>.jar` 放进服务端的 `plugins` 目录。
2. 服务端需要 **Paper 26.2**（或与之兼容的分支）和 **Java 25**。
3. 安装依赖插件：**Skript 2.16.2**、**PacketEvents 2.13.0**。如果要在标题中使用文本组件，还需安装 **SkBee**。
4. 启动服务端。插件无需配置，也不会生成配置文件。

请留意服务端版本：`plugin.yml` 声明了 `api-version: 26.2`，更旧的服务端会**直接拒绝加载**本插件，
不会等到运行时才报错。

## 第一个菜单

```skript
on load:
    build a menu {_menu}:
        inventory type: chest inventory
        title: "&6主菜单"
        layout: "#########", "#  A  B #", "#########"
        id: "main"
        edit:
            map key "A" to icon diamond named "&b打个招呼" for {_menu} and when clicked:
                send "你好！" to player
            map key "B" to icon clock named "&e关闭" for {_menu} and when clicked:
                close the menu for player

command /menu:
    trigger:
        set {_menu} to the menu with id "main"
        open menu {_menu} for player
```

这段脚本的关键在于：

- `build a menu {_menu}:` 创建菜单，并**把它存进 `{_menu}`**。其中，**布局字符串里的每个字符代表一个格子**。
- `map key "A" to icon …` 为布局中的字符 `A` 指定图标；这里的 `key` 就是布局字符。
- `for {_menu} and when clicked:` 后的代码会在玩家点击该图标时执行。
- `open menu {_menu} for player` 为玩家打开菜单。

`id: "main"` 给菜单取了一个名字，之后可以用 `the menu with id "main"` 找到它。没有 id 时，
可以将菜单存入变量，或在支持当前菜单上下文的语法中引用；它不会出现在 `all menus` 中。

## 三个最容易踩的坑

**一、部分语法可以省略 `menu`，但不能随意添加。**

```skript
map key "A" to icon stone for {_menu}          # 可以
map key "A" to icon stone for menu {_menu}     # 也可以
override slot 2 in page 1 to diamond for {_menu}
```

属性表达式里则**不能**多写 `menu`：`the id of menu {_menu}` 会被解析为“按 id 查找菜单”。
要读取菜单的 id，请写 `the id of {_menu}`。

**二、不再有 `page 0`。** 页码从 1 开始，创建菜单时给的布局永远是第 1 页。

**三、标题里不要写 `string:` 或 `component:`。** 它们是插件语法定义内部的分支标记，不属于脚本语法。
旧版会把包括前缀在内的整段文字当成标题；从 2.0.0 起，这种写法会明确报错。

## 接下来读哪一页

- 想理解菜单与窗口的关系：先读[窗口如何工作](how-it-works.zh-CN.md)，再看具体语法会更容易。
- 想逐项学习语法：先了解[两种模式](modes.zh-CN.md)，再从[创建菜单](menus.zh-CN.md)开始，沿侧栏往下读。
- 想参考多页菜单的写法：看[页面](pages.zh-CN.md)和[填充菜单](filling-menus.zh-CN.md)。
- 遇到不符合预期的行为：先看[尚未支持](not-supported-yet.zh-CN.md)，了解目前的功能边界。
