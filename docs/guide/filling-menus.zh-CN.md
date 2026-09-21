# 填充菜单

[English](filling-menus.md) | 简体中文

布局字符串为格子指定 key；放入什么图标、点击后执行什么操作，则由本页介绍的语法决定。

## 两条语法，两种思路

| | `map key` | `override slot` |
|---|---|---|
| 定位方式 | 布局中的 **key** | 直接指定**槽位编号** |
| 影响范围 | 使用该 key 的所有格子 | 指定的格子 |
| 典型用途 | 按布局批量设置图标 | 修改具体位置的内容 |

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

- `icon` 与 `item` 含义相同，单复数形式均可使用。
- 提供**多个物品**时，使用同一 key 的格子按顺序取用，数量不足时循环使用。
- 省略 `for …` 时，从菜单创建、编辑段落或菜单事件中取得当前菜单；没有菜单上下文时，必须显式指定。
- 不写 `on page …` 时，作用范围是**所有页**，包括之后用 `insert page` 加进来的页。
- `and refresh` 立刻把改动推给正在看的玩家。不写的话，`phantom` 菜单要等下次刷新/翻页才会出现；`static`
  菜单本来就会显示，因为改动落进了真实物品栏。
- `and when clicked` 要求你**必须**跟一段段落，否则注册时就会报
  `You must provide a section to handle the click event when using 'and when clicked'.`

<a id="列表页把一页填成一列物品"></a>

## 列表页：按顺序展示物品

物品浏览器通常需要按顺序展示一组结果，并在点击时识别所选项。先**把物品列表映射给一个 key**，
该 key 对应的格子便成为列表区域，按布局顺序填充：

```skript
build a menu {_menu}:
    inventory type: chest inventory
    title: "&6物品浏览器"
    layout: "LLLLLLLLL", "LLLLLLLLL", "LLLLLLLLL", "LLLLLLLLL", "LLLLLLLLL", "  P   N  "
    id: "browser"
    edit:
        map key "L" to icon {results::*} for {_menu}
        map key "P" to icon arrow named "&e上一页" for {_menu}
        map key "N" to icon arrow named "&e下一页" for {_menu}
```

随后可为每个玩家的窗口单独设置列表，**一次调用即可**：

```skript
set the menu list of the menu session of player to {results::*}
```

- 超出列表区域容量的项目不会显示，未填满的格子会被清空。例如上面的 45 格区域只有 3 条结果时，会显示 3 个图标，其余 42 格留空。
- **整个窗口只更新一次**：45 个格子通过一次调用和一次刷新完成，无需逐格刷新。

用 `the menu list index` 读取点击的是第几个结果。**列表索引从 1 开始**，与从 0 开始的槽位编号不同：

```skript
on menu interact:
    set {_picked} to {results::%the menu list index%}
    if {_picked} is set:
        send "&a你选了 %{_picked}%" to player
        stop
    # 不是列表格：那按到的就是上一页/下一页之类的按钮
```

点击非列表格子时，`the menu list index` **没有值**，因此同一个事件中也能区分列表项目与翻页按钮。

列表分页需要预先安排页面，窗口本身也不提供文字输入。这些限制见[尚未支持](not-supported-yet.zh-CN.md)。

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

- 槽位编号从 0 开始，按容器布局排列；箱子从左上角开始，每行 9 格。
- `in page …` 可指定多个页码，也可省略。**省略时使用菜单的默认页码**，即 `with page N` / `default page: N` 的值，默认 1。
  如果默认页不存在，运行时会报 `Page N does not exist in this menu.`。例如设置了 `with page 2` 却只创建一页时，应显式写 `in page 1`。
- `to` 后面一定要有东西，`for` 一定要有菜单：`override slot 10 of diamond` 和
  `override slot 10 to diamond` 都不解析。
- 和 `map key` 一样，`and when clicked` 必须有段落体。
- 想清空一个格子：`override slot 5 in page 1 to air for {_menu}`。

## 点击回调的三种写法

### 一、写在 `override slot` / `map key` 后面

回调跟着那条映射走，只在**那个字符的格子**或**那个格子**上触发：

```skript
build a menu {_menu}:
    inventory type: chest inventory
    title: "Main Menu"
    layout: "AAA", "ABA", "AAA"
    id: "main_menu"
    edit:
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

**必须提供段落体**，否则注册时会报 `You must provide a section to handle the slot click event.`
这种写法也能为**没有图标**的格子设置回调，做成“隐形按钮”：格子虽然为空，点击仍会执行操作。

### 三、按钮：定义一次，多处复用

```
define [a] button %string%:
    icon: <物品>
    when clicked:
        <代码>
```

```skript
define button "my_button":
    icon: stone
    when clicked:
        send "You clicked the button!" to player

on load:
    build a menu {_menu}:
        inventory type: chest inventory
        title: "&6菜单"
        layout: "#####", "# A #", "#####"
        id: "main"
        edit:
            map key "A" to button "my_button" for {_menu}
```

`define button %string%` 的 `icon:` 是必需的，`when clicked:` 也是必需的。同 id 再定义一次会**覆盖**
旧的。`all buttons` 列出所有按钮 id。

按钮的回调是**共享**的：任何菜单、任何格子只要映射了这个按钮，点击都跑同一段代码。想在回调里知道
是哪个菜单，用菜单事件里的 `the event-menu`。

注意：`override slot … to button "x" for …:` 同时附带段落体时，会使用按钮定义，**忽略段落并发出警告**
（`Both a button ID and a section were provided …`）。按钮与内联回调应二选一。

## 在回调里能拿到什么

回调段落跑在菜单交互事件的上下文里，所以[事件](events.zh-CN.md)里的值都能用：

```skript
map key "A" to icon stone for {_menu} and when clicked:
    send "你在第 %the page% 页点了第 %the clicked slot% 格。" to player
    if the clicked icon is diamond:
        send "而且那是一颗钻石。" to player
```

`player` 是点击的人，`the event-menu` / `the event-menu window` 是这次交互的菜单和窗口。

## 动态改内容

`override slot` 和 `map key` 会在执行时修改页面内容，也可在 `on menu open` 或 `on menu interact` 中动态调用。
是否立即同步到客户端，则取决于菜单模式与刷新选项。

```skript
on menu interact:
    override slot 4 in page 1 to (the clicked icon) for the menu and refresh
```

在 `phantom` 下，不带 `and refresh` 时，内容已修改，但客户端仍显示旧内容，直到下一次刷新或翻页。
在 `static` 下，修改直接作用于真实物品栏，无论是否附带刷新选项，都会同步显示。

**`and refresh` 决定何时同步显示，不决定修改影响谁。** `override slot` 和 `map key` 修改的是共享页面；
使用 `and refresh` 会立即刷新**所有正在查看该页的玩家**的对应格子。

如果只想改**某个玩家**的窗口（例如给他自己显示一个高亮），用**窗口**的图标，见
[窗口](windows.zh-CN.md)：

```skript
set icon in slot 5 of the menu session of player to red stained glass pane
```
