# 创建菜单

[English](menus.md) | 简体中文

## 两种写法

**段落式**（`create menu`）一次性把所有属性写在第一行，段落里放内容：

```skript
create a static menu with chest inventory titled "Main Menu" with id "main_menu" with layout "AAA" and "ABA" and "AAA" with 100 ms click delay:
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

`build a menu {_menu}:` 会把菜单**存入指定变量**，之后便可通过 `for {_menu}` 引用它。
这是段落语法，首行末尾需要冒号。

## `create menu` 的各个部分

```
create [a] [phantom|static] menu
    with %inventorytype%
    titled <标题>
    with layout %strings%
    [with player layout %-strings%]
    [with id %-string%]
    [with page %-number%]
    [with %-number% ms click delay]
    [(with|without) hide player inventory]
    [(with|without) locked icons]
```

顺序是固定的：`with player layout` 在 `with layout` **之后**，`with id` 又在它之后，`with page` 再往后。
`phantom` / `static` 不写就是 `phantom`。

| 部分 | 必填 | 说明 |
|---|---|---|
| `with %inventorytype%` | 是 | 容器类型，见下一节 |
| `titled …` | 是 | 标题，见[标题](titles.zh-CN.md) |
| `with layout %strings%` | 是 | 布局，见下一节 |
| `with player layout %strings%` | 否 | 容器下方那 4 行；写了它**就会隐藏玩家背包**，见[页面](pages.zh-CN.md#player-layout) |
| `with id %string%` | 否 | 给菜单起名字；**重名的菜单会把旧的销毁** |
| `with page %number%` | 否 | 只决定 `open menu` 先显示第几页，见[页面](pages.zh-CN.md) |
| `with %number% ms click delay` | 否 | 两次点击之间的最小间隔，默认 50 毫秒 |
| `with hide player inventory` | 否 | 隐藏菜单下方的玩家背包区域 |
| `with locked icons` | 否 | 布局里有图标的格子属于菜单：拿不走、放不进、换不掉、丢不出，碰到它们的拖拽整次拒绝 |

默认不隐藏玩家背包，显式写出 `without hide player inventory` 可以让意图更清楚。
隐藏背包**只适用于 `phantom`**；`static` 下方始终是玩家的真实背包，不支持隐藏，尝试开启时会收到警告。

`with player layout` 定义容器下方四行的布局，并**自动开启隐藏背包**，让菜单可以使用这片区域。
如果只想隐藏背包而不设置图标，写 `with hide player inventory` 即可。在 `static` 下设置 `player layout` 只会产生警告，不会改变内容。

`with locked icons` 保护**页面中有图标的格子**：点击、Shift 点击、数字键、副手交换、丢弃和双击收集都不能取出或放入物品。
拖拽只要经过受保护的格子，**整次操作就会被拒绝**，无法只执行其中一部分。布局中的空格子仍可自由存取物品，
因此同一个菜单既能保留商店按钮，也能提供储物空间。

受保护的范围包括**布局中已映射图标的格子**和 `override slot` 覆盖过的格子。
运行时通过 `set icon in slot N of {_session}` 写入某个玩家窗口的物品**不在此范围内**：
前者属于共享页面定义，后者只是该玩家窗口中的内容。

**`static` 下方的玩家背包不属于菜单；只有会影响受保护容器格子的操作才会被拒绝**，不会禁用整个背包区域。
例如一个**收购商店**可以在左下角放取消按钮、右下角放确认按钮，其余格子留给玩家放入待售物品：

```skript
build a menu {_menu}:
    mode: static
    inventory type: chest inventory
    title: "&6Sell"
    layout: "         ", "         ", "C       ", "        S"
    locked icons: true
```

为 `C` / `S` 映射图标后，这两个按钮会受到保护；中间的空格是收购区，玩家可以点击放入物品，也可以用 **Shift 点击**整叠转入。
对玩家背包区域的操作，判断如下：

- **Shift 点击送入容器**：游戏先合并同类堆叠，再寻找空格。只有受保护格子中存在**同类且尚未堆满的物品**时，操作才会被拒绝；否则放行。
  “同类”要求物品类型和组件相同，名称不同的物品不会合并。
- **双击收集**：可能收走窗口内所有同类物品，因此受保护格子中确实存在该物品时才会拒绝。
- **其他背包操作**：普通点击、右键、数字键和丢弃等不影响上方容器的操作会放行。

`phantom` 不同：窗口由插件通过数据包呈现，移动物品的预测结果会在交互交给脚本前恢复。
因此 `locked icons` 在这里**没有额外保护作用**，只改变 `if the icons of {_menu} are locked` 可读取的状态。
为 `phantom` 设置 `with locked icons` 或调用 `lock the icons of %menu%`，都会收到该功能不起作用的提示。

**运行时**可用 `lock the icons of %menu%` / `unlock the icons of %menu%` 切换锁定状态，
用 `if the icons of {_menu} are locked:` 读取状态。这与创建时的 `with locked icons` 是同一个设置，
修改后从**下一次交互**起生效，不会改变屏幕显示：

```skript
edit menu with id "main_menu":
    unlock the icons of the menu with id "main_menu"
```

## 容器类型

`%inventorytype%` 用的是 Skript 自己的容器类型，所以名字就是 Skript 里的名字：

| 写法 | 格子数 | 备注 |
|---|---|---|
| `chest inventory` | 行数 × 9，见下 | 唯一一个**由布局决定大小**的类型 |
| `dropper inventory` | 9 | 3×3 |
| `dispenser inventory` | 9 | 3×3，和 dropper 不是同一个界面 |
| `crafter inventory` | 9 | 合成器方块。和 `workbench` 一样是 3×3，但客户端画的是另一个界面，所以两个都存在 |
| `workbench inventory` | 10 | 工作台：1 个结果格 + 3×3 合成格 |
| `anvil inventory` | 3 | |
| `beacon inventory` | 1 | 只有放宝石的那一格 |
| `blast furnace inventory` | 3 | |
| `furnace inventory` | 3 | |
| `smoker inventory` | 3 | |
| `grindstone inventory` | 3 | |
| `smithing inventory` | 4 | 模板、装备、材料、结果 |
| `loom inventory` | 4 | |
| `cartography table inventory` | 3 | **不是** `cartography inventory` |
| `brewing stand inventory` | 5 | |
| `hopper inventory` | 5 | |
| `lectern inventory` | 1 | |
| `stonecutter inventory` | 2 | |
| `enchanting table inventory` | 2 | **不是** `enchanting inventory` |
| `merchant inventory` | 3 | 也可以写 `villager inventory`。**只支持 `phantom` 菜单**，见下 |
| `barrel inventory` | 27 | |
| `ender chest inventory` | 27 | |
| `shulker box inventory` | 27 | |

`chest inventory` 的行数就是**你给的布局字符串条数**（最多 6 条），所以 `"AAA", "ABA", "AAA"` 是
一个 3 行、27 格的箱子。其它类型的大小是固定的，布局字符串只决定哪些格子被用到。

表中列的是**容器本身的格子数**。除 lectern 没有背包区域外，后面通常还接着玩家的 27 格主背包和 9 格快捷栏。
容器格子数若计算错误，背包显示和点击位置就可能整体偏移。单元测试会检查这些数值，防止布局改动引入错位。

`merchant inventory` **仅支持 `phantom`**，由插件通过数据包呈现。Bukkit 不能直接创建商人物品栏，交易内容需通过商人 API 提供，
因此 `create a static menu with merchant inventory ...` 会直接报错。

未列出的类型会报 `Unsupported inventory type`。其中 `crafting table inventory` 指玩家自带的 2×2 合成格，
客户端没有可单独打开的对应界面，强行打开只会显示工作台，因此不予支持。需要工作台界面时，请用 `workbench inventory`。

## 布局字符串是什么

布局是**一行一个字符串**，每个字符对应一个格子，从左上角开始、从左到右、从上到下。

行与行之间建议用 `and` 连接。逗号分隔的含义相同，但 Skript 会在加载脚本时提示
`List is missing 'and' or 'or', defaulting to 'and'`。若想保留逗号，可以在
那一行之前写 `suppress missing conjunction warnings`，或在 `plugins/Skript/config.sk` 里设置
`disable variable missing and/or warnings: true`。

```skript
with layout "#########" and "#  A  B #" and "#########"
```

- 一个字符串 = 一行，长度不超过 9 个字符（箱子以外由容器宽度决定）。
- 每个字符占 1 个格子，**空格也是一个格子**，只是没有被映射到任何 key，所以留空。
  上面的例子里第 2 行是 9 个字符：`#`、空格、空格、`A`、空格、空格、`B`、空格、`#`。
- 字符只是名字，没有任何内置含义：`#` 不会自动变成玻璃，`A` 也不会自动变成钻石。真正决定外观的是
  `map key`（见[填充菜单](filling-menus.zh-CN.md)）。
- 同一个字符可以在布局里出现多次，那些格子会一起被映射、一起被改。

**格子编号怎么算**：第 1 行是 0–8，第 2 行是 9–17，第 3 行是 18–26，依此类推。所以
`"#########", "#  A  B #"` 里的 `A` 位于编号为 12 的格子。

**想用长名字当 key**，就用反引号把名字括起来，整个 `` `名称` `` 占 1 个格子：

```skript
with layout "`上一页`AAA`下一页`"
map key "上一页" to icon paper named "&e上一页" for {_menu}
```

普通字符或反引号中的名称都可作为 key，必须与 `map key` 中的字符串**完全一致**，且区分大小写。
映射布局中不存在的 key 不会报错，但图标也不会显示。遇到“已经映射却看不到物品”时，可以先检查名称是否一致。

## 用 `edit menu` 改一个已经存在的菜单

```skript
edit menu with id "main_menu":
    override slot 0 in page 1 to diamond named "New Item" for menu with id "main_menu"
```

`(edit|change) [the] [(menu|gui)] %menu%` 本身不改变任何属性，它只是提供一个"当前菜单"的上下文，
让段落里的 `insert page …` 之类不写菜单也能工作。改属性请用对应的表达式，例如
`set the default title of {_menu} to "…"`。
