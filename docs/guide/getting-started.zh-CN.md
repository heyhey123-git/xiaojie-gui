# 快速上手

[English](getting-started.md) | 简体中文

这一页让你在五分钟内看到一个能用的菜单。其它页面把每一部分拆开讲。

## 安装

1. 把 `xiaojiegui-<版本>.jar` 放进服务端的 `plugins` 目录。
2. 服务端需要 **Paper 26.2**（或与之兼容的分支）和 **Java 25**。
3. 依赖插件：**Skript 2.16.2**、**PacketEvents 2.13.0**。可选：**SkBee**（只为了在标题里用文本组件）。
4. 启动服务端。没有任何需要配置的东西，所以插件不会生成配置文件。

要求是硬性的：`plugin.yml` 里声明了 `api-version: 26.2`，更旧的服务端会**直接拒绝加载**本插件，
而不是加载成功后在运行时报奇怪的错。

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

这段脚本做四件事：

- `build a menu {_menu}:` 建出一个菜单、**把它存进 `{_menu}`**，并且**布局字符串里的每个字符就是一个格子**。
- `map key "A" to icon …` 把布局里的字符 `A` 和一件物品绑起来，`key` 就是那个字符。
- `for {_menu} and when clicked:` 里的段落是这个图标被点击时执行的代码。
- `open menu {_menu} for player` 把菜单发给玩家。

`with id "main"` 给菜单起了一个名字，之后可以用 `the menu with id "main"` 找回来。**没有 id 的菜单
只能靠变量传递**，而且不会出现在 `all menus` 里。

## 三个最容易踩的坑

**一、`menu` 这个词在这一版是可选的，位置要放对。**

```skript
map key "A" to icon stone for {_menu}          # 可以
map key "A" to icon stone for menu {_menu}     # 也可以
override slot 2 in page 1 to diamond for {_menu}
```

但在属性表达式里**不能**多写 `menu`：`the id of menu {_menu}` 会被读成"按 id 查找菜单"。
写 `the id of {_menu}`。

**二、不再有 `page 0`。** 页码从 1 开始，创建菜单时给的布局永远是第 1 页。

**三、标题里不要写 `string:` 或 `component:`。** 那是插件 pattern 内部的分支标记，不是脚本写法。
写了之后整段文字（含前缀）会被当成标题，2.0.0 起会明确报错。

## 接下来读哪一页

- 想知道菜单和窗口到底是怎么跑起来的：[窗口到底是怎么工作的](how-it-works.zh-CN.md) 把模型讲清楚，
  之后每一页都更好读。
- 想知道每个语法长什么样：从[创建菜单](menus.zh-CN.md)开始，顺着侧栏往下看；建第一个菜单之前先定下
  [两种模式](modes.zh-CN.md)。
- 想直接抄一个多页菜单：看[页面](pages.zh-CN.md)和[填充菜单](filling-menus.zh-CN.md)。
- 遇到了奇怪的现象：先看[尚未支持](not-supported-yet.zh-CN.md)，那里列了已知的边界。
