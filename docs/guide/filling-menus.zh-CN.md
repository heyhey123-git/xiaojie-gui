# 填充菜单

[English](filling-menus.md) | 简体中文

布局字符串只决定**哪些格子存在**，真正放什么、点击做什么，是下面这几条语法决定的。

## 两条语法，两种思路

| | `map key` | `override slot` |
|---|---|---|
| 认什么 | 布局里的**字符** | 直接给**格子编号** |
| 影响范围 | 所有出现那个字符的格子 | 你写的那几个格子 |
| 典型用途 | 按布局批量铺物品 | 往具体位置塞一个东西 |

两条都可以带一段点击回调，也都可以不带。

## `map key … to icon/items`

```
map key %string% to ((icon|item)[s] %-itemstacks%|button %-string%)
    [for [(menu|gui)] %-menu%] [on page[s] %-numbers%]
    [and (refresh|update)] [and when clicked]
```

```skript
map key "A" to icon stone for {_menu}
map key "A" to items {_allSkull::*} for {_menu}
map key "B" to button "my_button" for {_menu}
map key "special_item" to item diamond named "Special Item" for menu {_menu} and refresh and when clicked:
    send "你点了特殊物品。" to player
```

- `icon` 和 `item` 是同义词，单复数也随意。
- 给了**多个**物品时，同名的每个格子按顺序取一个（循环使用）。
- 不写 `for …` 时，菜单来自当前事件（`create menu` 段落体、`edit menu` 段落体、任何菜单事件）。
  事件之外必须写出来。
- 不写 `on page …` 时，作用范围是**所有页**，包括之后用 `insert page` 加进来的页。
- `and refresh` 立刻把改动推给正在看的玩家。不写的话，改动只在下次刷新/翻页时出现。
- `and when clicked` 要求你**必须**跟一段段落，否则注册时就会报
  `You must provide a section to handle the click event when using 'and when clicked'.`

## 列表页：把一页填成一列物品

浏览器要的是"这一页显示这 N 个结果，点了第几个就是第几个"。做法是**把一个列表映射给一个 key**——那个
key 的格子就是这一页的列表格，按布局顺序：

```skript
create a phantom menu with chest inventory titled "&6物品浏览器" with layout "LLLLLLLLL" and "LLLLLLLLL" and "LLLLLLLLL" and "LLLLLLLLL" and "LLLLLLLLL" and "  P   N  " with id "browser":
    map key "L" to icon {results::*} for menu with id "browser"
    map key "P" to icon arrow named "&e上一页" for menu with id "browser"
    map key "N" to icon arrow named "&e下一页" for menu with id "browser"
```

然后每个玩家各自填内容，**一次调用**：

```skript
set the menu list of the menu session of player to {results::*}
```

- 多出来的物品会被丢掉，没填满的格子会被清空——所以"只剩 3 条结果"就是 3 个图标加 42 个空格，不需要额外处理。
- **整个窗口只更新一次**：45 个格子一次调用加一次刷新，而不是 45 次。

点到了第几个结果，用 `the menu list index`（1 基，和其他所有编号一致）：

```skript
on menu interact:
    set {_picked} to {results::%the menu list index%}
    if {_picked} is set:
        send "&a你选了 %{_picked}%" to player
        stop
    # 不是列表格：那按到的就是上一页/下一页之类的按钮
```

`the menu list index` 在点到的不是列表格时**没有值**，所以"点按钮"和"点结果"用同一个事件就能分开。

浏览器剩下的两件事——页数在创建时固定、窗口里没有文字输入——写在[尚未支持](not-supported-yet.zh-CN.md)。

## `override slot … to … for …`

```
(override|set) slot %numbers%
    [in page %-numbers%]
    to (%-itemstack%|button %-string%)
    for [the] [(menu|gui)] %-menu%
    [(and|with) (refresh|update)]
    [and when (clicked|interacted|pressed)]
```

```skript
override slot 10 in page 1 to diamond named "Clicked Item" for menu with id "main_menu" and refresh and when clicked:
    send "You clicked the overridden slot!" to player
```

- 格子编号是 0 起的，从容器左上角开始，一行 9 格。
- `in page …` 可以给多个页码，也可以完全不写。**不写时默认是"页面"那一项** —— 也就是
  `with page N` / `default page: N` 的值，默认 1。**这里有个坑**：如果那个默认页大于实际页数，
  这条语法会在运行时报 `Page N does not exist in this menu.`。所以设了 `with page 2` 又只建了一页时，
  请显式写 `in page 1`。
- `to` 后面一定要有东西，`for` 一定要有菜单：`override slot 10 of diamond` 和
  `override slot 10 to diamond` 都不解析。
- 和 `map key` 一样，`and when clicked` 必须有段落体。
- 想清空一个格子：`override slot 5 in page 1 to air for {_menu}`。

## 点击回调的三种写法

### 一、写在 `override slot` / `map key` 后面

回调跟着那条映射走，只在**那个字符的格子**或**那个格子**上触发：

```skript
create a phantom menu with chest inventory titled "Main Menu" with layout "AAA" and "ABA" and "AAA" with id "main_menu":
    map key "A" to icon stone for {_menu} and when clicked:
        send "你点了 A 区域。" to player
    override slot 4 in page 1 to diamond named "中间" for {_menu} and when clicked:
        send "你点了中间。" to player
```

### 二、`when … clicked` 段落，单独给格子挂回调

```
(when|on) slot %numbers%
    [in page [(number|index)] %-numbers%]
    [of [(menu|gui)] %-menu%]
    [is] (clicked|interacted|pressed)
```

```skript
when slot 4 in page 1 of menu with id "main_menu" is clicked:
    send "You clicked slot 4!" to player

when slot 4 and 5 in page 1 of the menu {_menu} is clicked:
    send "点了 4 或 5。" to player
```

**这一段必须有段落体**，否则注册时报 `You must provide a section to handle the slot click event.`
它的好处是给一个**没有映射任何物品**的格子挂回调 —— 也就是常说的"隐形按钮"：格子里什么都没有，
但点上去有反应。

### 三、按钮：定义一次，多处复用

```skript
define button "my_button":
    icon: stone
    when clicked:
        send "You clicked the button!" to player

on load:
    create a phantom menu with chest inventory titled "&6菜单" with layout "#####" and "# A #" and "#####" with id "main":
        map key "A" to button "my_button" for {_menu}
```

`define button %string%` 的 `icon:` 是必需的，`when clicked:` 也是必需的。同 id 再定义一次会**覆盖**
旧的。`all buttons` 列出所有按钮 id。

按钮的回调是**共享**的：任何菜单、任何格子只要映射了这个按钮，点击都跑同一段代码。想在回调里知道
是哪个菜单，用菜单事件里的 `the menu`。

**一个提醒**：`override slot … to button "x" for …:` 带段落体时，按钮会生效，段落会被**忽略并警告**
（`Both a button ID and a section were provided …`）。二选一。

## 在回调里能拿到什么

回调段落跑在菜单交互事件的上下文里，所以[事件](events.zh-CN.md)里的值都能用：

```skript
map key "A" to icon stone for {_menu} and when clicked:
    send "你在第 %the page% 页点了第 %the clicked slot% 格。" to player
    if the clicked icon is diamond:
        send "而且那是一颗钻石。" to player
```

`player` 是点击的人，`the menu` / `the session` 是这次交互的菜单和会话。

## 动态改内容

`override slot` 和 `map key` 是**当时执行、当时生效**的：在 `on menu open` 或 `on menu interact` 里
执行，就会改动玩家看到的东西。

```skript
on menu interact:
    override slot 4 in page 1 to (the clicked icon) for the menu and refresh
```

不带 `and refresh` 时改动只进了模型，屏幕上还是旧的。**给别人看的**和**给自己看的**区别就在这个
标志上：`and refresh` 会刷新**所有正在看这一页的玩家**的对应格子。

如果只想改**某个玩家**的窗口（例如给他自己显示一个高亮），用**会话**的图标，见
[会话](sessions.zh-CN.md)：

```skript
set icon in slot 5 of the menu session of player to red stained glass pane
```
