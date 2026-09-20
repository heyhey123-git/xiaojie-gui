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

`build a menu {_menu}:` 会把建好的菜单**存进那个变量**，这也是后面 `for {_menu}` 能用的原因。
`build a menu` 是段落，第一行末尾要有冒号。

## `create menu` 的各个部分

```
create [a] [phantom|static] menu
    with %inventorytype%
    titled <标题>
    with layout %strings%
    [with player layout %strings%]
    [with id %string%]
    [with page %number%]
    [with %number% ms click delay]
    [with|without hide player inventory]
    [with|without locked icons]
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

`without hide player inventory` 是默认值，写出来是为了可读性。它**只在 `phantom` 菜单里有意义**：`static`
菜单显示的就是玩家真实的背包，没有东西可以"隐藏"，所以在 `static` 菜单上写它会收到一句警告（而不是得到一个
看起来怎么都不对的菜单）。

`with player layout` 是容器下方那 4 行的布局，写它**会自己把隐藏开关打开**：那些格子只有玩家背包被隐藏时
才是菜单自己的空间，所以"要这份布局"就是"要这半场"。只想要下半场隐藏但空着，仍然只写
`with hide player inventory`；在 `static` 菜单上写 player layout 会收到一句警告，并且什么也不会变。

`with locked icons` 把**布局里有图标的格子**变成菜单自己的：点击、Shift 点击、数字键、副手交换、丢出、
双击收集全都拿不走也放不进，而碰到这些格子的**拖拽会被整次拒绝**（协议上也没法只应用一半）。布局里空着的
格子仍然完全属于玩家——所以"商店"（有图标）和"背包"（整页留空）用的是同一个机制。

哪些格子算"有图标"是刻意的：**布局里写了 key 的格子**，加上用 `override slot` 覆盖过的格子。运行时用
`set icon in slot N of {_session}` 放进某个窗口的物品**不算**——前者是菜单的家具，对所有人都一样；后者是
这一名玩家自己的内容，通常正是要让他拿走的。

**玩家自己那一半（`static` 菜单下半部分）不属于菜单，所以只有"会落到菜单格子上的操作"才被拒绝**，而不是
整个下半场都被封。举一个真正会这么设计的菜单：**收购商店**——玩家把背包里的东西放进容器的收购格，左下角
一个取消、右下角一个确认：

```skript
build a menu {_menu}:
    mode: static
    inventory type: chest inventory
    title: "&6Sell"
    layout: "         ", "         ", "C       ", "        S"
    locked icons: true
```

这里只有 `C` / `S` 两个按钮被锁住；中间那些空格是收购区，玩家照常点击放入、也照常 **Shift 点击**把背包里的
东西整叠送进去。具体的判断是：

- **Shift 点击**（从背包送进容器）：游戏会先并入同类堆叠、再找空格，所以只有当**菜单自己的某个格子里有一份
  同类的、还能继续叠的物品**时才拒绝——那正是它会去的地方；否则放行。"同类"指类型和组件都一样（名字不同就
  不算，游戏自己也不会合并它们）。
- **双击收集**：它会从所有格子里把同类物品收走，所以只有**菜单自己的格子确实持有该物品**时才拒绝。
- 其它操作（普通点击、右键、数字键、丢出……）不会碰到容器，全部放行。

幽灵菜单是另一回事：那个窗口完全是服务端自己画的，任何可能移动物品的点击都会在脚本看到这次交互之前被插件
自己撤销。所以 `locked icons` 在那里**不保护任何东西**——写它只改变 `if the icons of {_menu} are locked`
的返回值，那份状态脚本可以读，但幽灵菜单用不上。这个开关是给 `static` 菜单的，插件也会这么说：给幽灵菜单写
`with locked icons`、或者对它调 `lock the icons of %menu%`，都会收到一句"在那里不起作用"。

要在**运行时**开关这个开关，用 `lock the icons of %menu%` / `unlock the icons of %menu%`，读它的状态用
`if the icons of {_menu} are locked:`。它和创建时那个 `with locked icons` 是同一个开关，改完对**下一次**
交互生效（屏幕上的东西不会因此变化）：

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

格子数就是**客户端这个界面真正有的格子数**：玩家背包的 27 格和快捷栏的 9 格紧跟在后面。数错了不会报错，
但玩家背包里的东西和点击会整体错位，所以这张表和单元测试里的同一张表是钉在一起的，改错了测试会立刻失败。

`merchant inventory` 的窗口由服务端自己画（`phantom`），所以能用；`static` 不行，因为 Bukkit 不给商人
建库存（交易内容来自商人 API），写 `create a static menu with merchant inventory ...` 会直接报错。

除上面这些以外的类型会报 `Unsupported inventory type`。其中 `crafting table inventory`（玩家自己的 2×2
合成格）是**故意**不支持的：客户端没有画这个窗口的界面，硬开只会画出工作台；要合成台界面请用
`workbench inventory`。

## 布局字符串是什么

布局是**一行一个字符串**，每个字符对应一个格子，从左上角开始、从左到右、从上到下。

行与行之间用 `and` 连接。写成逗号分隔含义完全相同，但 Skript 会在加载脚本时打印
`List is missing 'and' or 'or', defaulting to 'and'`，所以本文示例统一用 `and`；若想保留逗号，可以在
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
