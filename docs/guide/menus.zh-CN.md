# 创建菜单

[English](../../README.md) | 简体中文

## 两种写法

**段落式**（`create menu`）一次性把所有属性写在第一行，段落里放内容：

```skript
create a static menu with chest inventory titled "Main Menu" with id "main_menu" with layout "AAA", "ABA", "AAA" with 100 ms click delay with hide player inventory:
    override slot 4 in page 1 to diamond named "Special Item" for menu with id "main_menu"
```

**条目式**（`build a menu`）把属性写成 `键: 值`，内容放进 `edit:` 段落：

```skript
build a menu {_menu}:
    mode: phantom
    inventory type: chest inventory
    title: "Main Menu"
    layout: "AAA", "ABA", "AAA"
    id: "main_menu"
    click delay: 100
    hide player inventory: true
    edit:
        override slot 4 in page 1 to diamond named "Special Item" for {_menu}
```

`build a menu {_menu}:` 会把建好的菜单**存进那个变量**，这也是后面 `for {_menu}` 能用的原因。
`build a menu` 是段落，第一行末尾要有冒号。

## `create menu` 的各个部分

```
create [a] [phantom|static] menu
    with %inventorytype%
    titled <标题>
    with layout %strings%
    [with id %string%]
    [with page %number%]
    [with %number% ms click delay]
    [with|without hide player inventory]
```

顺序是固定的：`with id` 在 `with layout` **之后**，`with page` 又在 `with id` 之后。
`phantom` / `static` 不写就是 `phantom`。

| 部分 | 必填 | 说明 |
|---|---|---|
| `with %inventorytype%` | 是 | 容器类型，见下一节 |
| `titled …` | 是 | 标题，见[标题](titles.zh-CN.md) |
| `with layout %strings%` | 是 | 布局，见下一节 |
| `with id %string%` | 否 | 给菜单起名字；**重名的菜单会把旧的销毁** |
| `with page %number%` | 否 | 只决定 `open menu` 先显示第几页，见[页面](pages.zh-CN.md) |
| `with %number% ms click delay` | 否 | 两次点击之间的最小间隔，默认 50 毫秒 |
| `with hide player inventory` | 否 | 隐藏菜单下方的玩家背包区域 |

`without hide player inventory` 是默认值，写出来是为了可读性。

## 容器类型

`%inventorytype%` 用的是 Skript 自己的容器类型，所以名字就是 Skript 里的名字：

| 写法 | 格子数 | 备注 |
|---|---|---|
| `chest inventory` | 行数 × 9，见下 | 唯一一个**由布局决定大小**的类型 |
| `dropper inventory` | 9 | 3×3 |
| `dispenser inventory` | 9 | 3×3，和 dropper 不是同一个界面 |
| `workbench inventory` | 9 | 工作台 |
| `crafting table inventory` | 5 | 玩家自己的 2×2 合成格 |
| `crafter inventory` | 9 | 合成器方块。和 `workbench` 一样是 3×3，但客户端画的是另一个界面，所以两个都存在 |
| `anvil inventory` | 3 | |
| `beacon inventory` | 3 | |
| `blast furnace inventory` | 3 | |
| `furnace inventory` | 3 | |
| `smoker inventory` | 3 | |
| `grindstone inventory` | 3 | |
| `smithing inventory` | 3 | |
| `loom inventory` | 4 | |
| `cartography table inventory` | 4 | **不是** `cartography inventory` |
| `brewing stand inventory` | 5 | |
| `hopper inventory` | 5 | |
| `lectern inventory` | 2 | |
| `stonecutter inventory` | 2 | |
| `enchanting table inventory` | 3 | **不是** `enchanting inventory` |
| `merchant inventory` | 3 | 也可以写 `villager inventory` |
| `barrel inventory` | 27 | |
| `ender chest inventory` | 27 | |
| `shulker box inventory` | 27 | |

`chest inventory` 的行数就是**你给的布局字符串条数**（最多 6 条），所以 `"AAA", "ABA", "AAA"` 是
一个 3 行、27 格的箱子。其它类型的大小是固定的，布局字符串只决定哪些格子被用到。

除上面这些以外的类型会报 `Unsupported inventory type`。

## 布局字符串是什么

布局是**一行一个字符串**，每个字符对应一个格子，从左上角开始、从左到右、从上到下。

```skript
with layout "#########", "#  A  B #", "#########"
```

- 一个字符串 = 一行，长度不超过 9 个字符（箱子以外由容器宽度决定）。
- 每个字符占 1 个格子，**空格也是一个格子**，只是没有被映射到任何 key，所以留空。
  上面的例子里第 2 行是 9 个字符：`#`、空格、空格、`A`、空格、空格、`B`、空格、`#`。
- 字符只是名字，没有任何内置含义：`#` 不会自动变成玻璃，`A` 也不会自动变成钻石。真正决定外观的是
  `map key`（见[填充菜单](filling-menus.zh-CN.md)）。
- 同一个字符可以在布局里出现多次，那些格子会一起被映射、一起被改。

**格子编号怎么算**：第 1 行是 0–8，第 2 行是 9–17，第 3 行是 18–26，依此类推。所以
`"#########", "#  A  B #"` 里的 `A` 是第 12 格。

**想用长名字当 key**，就用反引号把名字括起来，整个 `` `名称` `` 占 1 个格子：

```skript
with layout "`上一页`AAA`下一页`"
map key "上一页" to icon paper named "&e上一页" for {_menu}
```

一个字符就是一个 key，所以布局里的字符和 `map key` 里的字符串必须**完全一致**（区分大小写）。
`map key` 用了一个布局里没有的字符不会报错，但那件物品不会出现在任何格子里 —— 这是最常见的
"我明明映射了却没有东西"的原因。

## 用 `edit menu` 改一个已经存在的菜单

```skript
edit menu with id "main_menu":
    override slot 0 in page 1 to diamond named "New Item" for menu with id "main_menu"
```

`(edit|change) [the] [(menu|gui)] %menu%` 本身不改变任何属性，它只是提供一个"当前菜单"的上下文，
让段落里的 `insert page …` 之类不写菜单也能工作。改属性请用对应的表达式，例如
`set the default title of {_menu} to "…"`。
