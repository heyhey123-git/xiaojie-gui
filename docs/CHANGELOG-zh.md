# 更新日志

## 2.0.0

这一版把插件搬到 Minecraft 26.2 / Skript 2.16，并修好了那些**从来没能真正用起来**的菜单语法。日常
用法基本没变，下面按"要改什么、为什么"来说。

### 运行环境

| | 之前 | 现在 |
|---|---|---|
| 服务端 | Paper 1.21.8 | **Paper 26.2** |
| Skript | 2.13.1 | **2.16.2** |
| PacketEvents | 2.9.5 | **2.13.0** |
| Java | 21 | **25** |

Skript 2.16.2 支持到 Paper 26.2；Paper 26.2 本身要求 Java 25。插件声明了 `api-version: 26.2`，所以更旧
的服务端会**直接拒绝加载**，而不是加载成功后在运行时报奇怪的错。

### 标题写法（以前一用就崩）

所有带标题的语法——`create menu … titled …`、`insert page … with title …`、`update title …`、
`update session title …`、`turn to page … with title …`——只要脚本执行到就会抛内部异常，**菜单根本
没被创建出来**。现在修好了。

可用写法就是给值本身：

```skript
create a phantom menu with chest inventory titled "主菜单" with layout "#########" with id "main":
update title of page 1 in {_menu} to "第一页"
```

**不要**写 `string:"主菜单"` 或 `component:"主菜单"`。这两个前缀来自旧文档，**它们不是语法的一部分**：
那是插件给自己 pattern 用的分支标记，输入里只有冒号**后面**的内容。写上前缀会让整段文字（含前缀）
成为标题 —— 2.0.0 起会明确报错，而不是渲染出一个以 `string:` 开头的标题。

标题是文本，所以里面的颜色码就是"格式"：`&a`、`&l` 这些**和 Skript 其它地方一样可用**，`§` 同样可用。
标题是以**文本组件**的形式发给客户端的（不是纯字符串），颜色和格式因此能保留下来。十六进制用
Adventure 的 `&x&f&f&5&5&0&0` 写法（每一位数字都带自己的 `&`）；`&#ff5500` 不属于这套语法。

有一个要和物品名一致的副作用：`&` 后面跟着颜色字符时会被当成颜色码。如果标题里真的要显示字面的
`&B`，`&` 做不到 —— 这一点和 Spigot 的物品名完全相同。

### 关于页面

- **页码从 1 开始。** 不存在 `page 0`；旧文档里写 `page 0` 的地方是错的，`in page 1` 才是第一页。
- **创建菜单时给的 `layout` 永远是第 1 页。** `with page N`（或 `default page`）只决定 `open menu`
  先显示第几页。以前一旦写了它，插件就**不创建任何页面**，而没有页面的菜单打不开 —— 也就是说
  `create menu … with layout … with page 2` 过去会造出一个打不开的菜单。
- `insert page 1 …` 会把新页插到第 1 位（原本的 layout 页顺延成第 2 页）；`insert page 2 …` 插在第 1 页
  之后；`insert page …`（不写页码）追加到末尾。
- 菜单默认只有一页。分页是"需要时才加"的功能：如果你只想换页内容，就保留一页、用 `override slot` /
  `map key` 改内容即可。

`update title of page N …` 的**本意**是只改"正在看第 N 页的玩家"看到的窗口标题。目前它会改掉该菜单
**所有**观众的窗口标题（无论他在第几页）—— 见"已知问题"。

### 在语法里指代菜单

`menu` 这个词在可读性需要的地方保留，但在本来就可选的组里现在是可选的，所以下面这些都能解析了：

```skript
map key "A" to icon stone for {_menu}
override slot 2 in page 1 to diamond for {_menu}
set {_slots::*} to the slots of key "A" in page 1 of {_menu}
set {_key} to the key of slot 1 in page 1 of {_menu}
```

`destroy` 现在也接受按 id 的写法（以前得把 `menu` 写两遍）：

```skript
destroy the menu {_menu}
destroy the menu with id "main"
```

### 旧的 `gui` 语法已删除

`create a gui …`、`%players% (has|have) a gui [open]`、`the player's gui` 都来自更早的 skript-gui 插件。
它们一直没人维护，也不遵守这个插件其余部分的约定：`create gui` 的页码从 0 开始，只要传进去的库存里有
物品就会崩；它写死的那个模式（`removable items` 即 static）现在也有了正式写法。所以它们是**删掉**而不
是修好，2.0.0 的脚本请用菜单语法：

| 已删除 | 现在这样写 |
|---|---|
| `create a gui with {_inv} with id "main":` | `create a phantom menu with chest inventory titled "…" with layout "…" with id "main":`，或 `build a menu:` 段落 |
| `the player's gui` | `the menu of the menu session of player` |
| `if player has a gui open:` | `if the menu session of player is set:` |
| `create a gui … with removable items` | `create a static menu …`，或在 `build a menu` 里写 `mode: static` |

### 菜单的模式是文本

`the mode of {_menu}` 返回 `phantom` 或 `static` —— 就是 `build a menu` 里 `mode:` 那一项接受的两个词 ——
所以比较起来就是普通的文本比较：

```skript
if the mode of {_menu} is "phantom":
```

原来的 `receptaclemode` 类型随之取消。顺带说清一个坑：**Skript 里枚举常量的字面量来自它的语言文件**，而
插件要自己提供带版本号的 `default.lang` 才有。没提供时，唯一能解析的字面量是自动生成的
`static receptacle mode`；而 `phantom` 这个词属于 Minecraft 的幻翼实体，所以 `is phantom` 会悄悄地和一
个实体类型比较，永远不成立。文本不需要这套机制。

同样的坑也在 `on menu interact` 里等着。插件以前注册了自己的 `menu click type` 和 `menu click mode`
类型，两个在脚本里都没法写，打印出来是 `LEFT`。现在都取消了 —— 脚本看到的点击类型就是 **Skript 自己的**
点击类型，也就是 `on inventory click` 给你的那个，用的是 Skript 的词和它的字面量：

```skript
on menu interact:
    if the event-clicktype is left mouse button:
        send "Left click!" to player
```

这条能成立，是因为插件注册了一个转换器：把自己更细的点击类型（知道按键和点击模式）转成 Bukkit 的，而
Skript 会通过已注册的转换器去解析事件值。把同一个值再注册成"我们自己的类型"，只会给它一个更差的名字。

在 `on menu interact` 里 `the click type` 和 `the event-clicktype` 都能用，而且是同一个值：Skript 自己的
`the click type` 属于 `on inventory click`，在别的事件里会拒绝解析，但插件为自己的事件注册了这个属性，所以
不带前缀的写法在这里也能得到答案。

插件自己的事件值都按脚本会说的说法写，不带 `event-` 前缀：`the clicked slot`、`the clicked icon`、
`the page`、`the pressed number key`；`event-` 形式作为别名保留，照顾已经这么写的脚本。

Skript 的点击类型唯一带不了的信息是**按的是哪个数字键**：九个键全都报成同一个 `number key`。而插件本来就
有九个 `NUMBER_KEY_*` 类型，所以这个数字单独可读：

```skript
on menu interact:
    if the pressed number key is 1:
        send "You pressed 1!" to player
```

`the pressed number key` 在 `number key` 点击时是 1 到 9，其它点击什么都不是 —— 想用数字键当快捷键就需要它。

### 配置终于生效了

`enable-async-check` 和 `force-truecolor` 以前读的是 Spigot 自己的配置文件，所以插件 `config.yml` 里
写的值**完全没用**。现在读的是 `plugins/xiaojie-gui/config.yml`（首次启动生成）。键名没变。

### 玩家能感觉到的行为变化

- `on menu open` 和 `on page turn` 里的 `cancel event` 现在真的生效了（这两者一个忽略参数恒取消、一个
  永远不取消）。
- `on menu interact` 里的 `the page` 现在第 1 页就是 1。它以前报的是"该页在菜单列表里的 0 基下标"，
  和脚本能看到的其它所有页码都对不上。
- `viewers of menu` 不再一直把"已经切到别的菜单的玩家"算进来。写法现在是 `the menu viewers of {_menu}`：
  Skript 2.16 自己注册了一个声明在**任何对象**上的 `viewer[s]` 属性，而且注册在插件之前，所以
  `the viewers of {_menu}` 会被 Skript 接走并返回空。`all players viewing {_menu}` 不变。
- 静态菜单翻页不再重复整包发送；玩家自己关掉菜单后，不会再被一个"迟到的标题更新"重新弹出窗口。
- 在菜单事件之外用 `expr`、页码越界、菜单为空这些情况，现在给的是可读报错，而不是在 Skript 里抛异常
  （旧日志里的 `Page 0 does not exist`、`index out of bounds` 之类）。
- `update title of page N` 现在只改**正在看第 N 页**的玩家，以前会改掉该菜单所有观众的窗口标题（在第 2 页
  的人会一直看到第 1 页的标题，直到他翻页）。
- 对一个已经打开该菜单的玩家再执行 `open menu {_menu} for player`，现在会跳到你指定的那一页，而不是报
  `already viewing this menu`。
- `barrel`、`ender chest`、`dispenser`、`enchanting`、`cartography`、`crafter` 现在都能创建菜单了：以前
  它们会报 `Unsupported inventory type`，因为没有对应的布局。其中两个在 Skript 里要写
  `enchanting table inventory` / `cartography table inventory`，不是 `enchanting inventory` /
  `cartography inventory`。
- 固定大小的布局现在用的就是**客户端真画出来的格子数**：`workbench` 10 格（不是 9）、`beacon` 1 格（不是 3）、
  `enchanting table` 2 格（不是 3）、`lectern` 1 格（不是 2）、`smithing` 4 格（不是 3）、
  `cartography table` 3 格（不是 4）。数错了不会报任何错——玩家背包里的东西和窗口里的每一次点击都会整体错位。
  现在有一个单元测试把每个布局钉在 Bukkit 自己的库存大小上，以后不会再漂。
- `crafting table inventory`（玩家自己的 2×2 合成格）不再是布局：客户端没有画这个窗口的界面，实际画出来的是
  10 格的工作台，而布局声明的是 5 格，Bukkit 也根本不为这种库存建对象。想要合成台界面请用 `workbench inventory`。
- `create a static menu with merchant inventory ...` 现在会直接说清楚是哪个类型不支持，而不是抛 Bukkit 自己的
  异常。商人窗口来自商人 API，只在 `phantom` 模式可用。
- 拖拽现在真的被处理了。以前只有"恰好扫过一格"的拖拽能被看见（游戏本身会把它变成一次点击），而真正的拖拽
  ——把一叠物品铺到好几格的那种——会在**没有任何 `on menu interact`** 的情况下改变窗口：商店拦不住，背包也
  看不到。现在一次拖拽就是一次交互，`the dragged slots` 是它扫过的所有格子，`the cursor item` 是玩家带进来的
  那一叠，`cancel event` 一次性拒绝整次拖拽（协议上也没法只应用一半）。完整机制（包括为什么拖拽只能放、不能拿）
  写在新的 wiki 页面《拖拽是怎么工作的》里。
- `static` 菜单里，玩家点自己背包那一半时，报的是客户端真正使用的窗口槽位。以前报的是相对于那一半的编号，
  于是它指向了一个玩家从未碰过的容器格子，还会触发那个格子的回调。
- 脚本读到的点击类型，就是玩家真正做出的那个点击。以前除了点击类型还会去看事件的 **action**，于是把格子上的
  `Q` 丢出报成了"在窗口外面点了一下"，又反过来把窗外点击报成了"丢出"；现在 `the click type` 和 Skript 对同一个
  手势的说法一致，窗内窗外都是。
- 插件叫不出名字的容器点击现在是**丢弃**，而不是在包监听里抛异常。幽灵菜单显示的是服务端自己画的窗口，所以
  没实现的点击就该什么都不做，而不是往日志里刷堆栈。
- 幽灵菜单在"可能把物品移到别的格子"的那类点击之后会重发整个窗口，而这个集合里不再包含丢出键：`Q` 丢出是
  把物品移出窗口，只有它点的那一格变了。
- `with locked icons`（`build a menu` 里写 `locked icons: true`）让**布局里有图标的格子**变成菜单自己的：点击、
  Shift 点击、数字键、副手交换、丢出、双击收集都拿不走也放不进，碰到这些格子的拖拽会被整次拒绝。空着的格子
  仍然属于玩家，所以"商店"和"背包"是同一个机制；锁定也**不会**阻止格子回调——商店的"购买"回调照常触发，而商品
  原地不动。`override slot` 覆盖的格子算菜单的家具，`set icon in slot N of {_session}` 放进去的算这名玩家自己的
  内容——这就是"商品"和"背包里的东西"的区别。玩家自己那一半只在"会碰到菜单"的地方设防：Shift 点击只有在菜单
  自己的某个格子里有一份同类、还装得下的物品时才拒绝，双击收集只有在菜单自己的格子里确实有那件物品时才拒绝，
  所以"从玩家背包里收购东西"的商店照样能让玩家把东西 Shift 进收购区。
- 翻页只清掉**正要离开的那一页自己有的格子**，而不是整个窗口。以前清空整个窗口会把玩家放进空格子里的东西一起
  丢掉，等于让"翻页背包"不可能做出来。
- 这个开关也能在运行时改：`lock the icons of %menu%` / `unlock the icons of %menu%`（写在 `edit menu …:`
  里或任何地方都行），读它用 `if the icons of {_menu} are locked:`。改完对下一次交互生效。
- `the occupied slots of %menusession%` 和 `the menu contents of %menusession%` 可以读回一个窗口：哪些格子里有
  东西，以及那些东西本身（按格子顺序）。配合 `icon in slot N of {_session}`，这就是"把东西存在自由格子里的菜单"
  存档与恢复的写法。
- 带着 `static` 菜单退出、死亡或换世界的玩家，不会再把一份库存留在内存里。以前那条记录（连同里面的物品）会一直
  留到服务端重启，每个玩家一份。
- 在 `static` 菜单上写 `hide player inventory` 现在会在创建时给一句警告。那种窗口就是玩家真实的背包，这个标志
  什么都隐藏不了，而一个"看起来怎么都不对"的菜单不如控制台里的一行字。
- `/skript reload` 会先关掉所有菜单再加载新脚本。菜单里存着"创建它的那批脚本"的回调，所以挺过一次重载的
  菜单，下一次点击执行的是已经不在任何脚本里的代码。重载后的脚本会像启动时一样重新建自己的菜单。

### 已知问题

- 点击、槽位对齐、标题、物品**类型**现在都由**真实客户端**覆盖（`./gradlew clientTest`）：一个 Minecraft
  客户端连进测试服务端，读取服务端为它打开的窗口、在里面点击，并检查每个槽位里物品的**类型**。客户端是
  26.1 客户端，测试服务端为它装了 ViaVersion + ViaBackwards，所以它证明的是"本插件发出的包能穿过真实翻译层
  到达真实客户端"，而不是"26.2 客户端看到的就是这些字节"。客户端侧的幽灵物品与拖拽仍未覆盖。
- `composter`、`chiseled bookshelf`、`decorated pot`、`shelf`、`jukebox` 不支持，而且**支持不了**：Minecraft
  没有为它们提供菜单（Bukkit 里这些库存类型没有对应菜单、也不能被创建），所以没有窗口可画。`create menu` 会
  明确报 `Unsupported inventory type`，而不是猜一个形状。
- 页面只能通过 `insert page` / 创建时的 `with page` 产生，没有声明式的 page 段落 —— 一页 = 一个布局
  + 一个标题，这是有意为之。

### 可靠性

插件现在有四层测试：单元测试；一个**真实启动的 Paper 26.2 服务端**（带 Skript 与 PacketEvents）跑一组
Skript 脚本并检查日志；同一个服务端还会解析每一个 `@Examples` 注解，让文档里读不了的例子直接构建失败；
以及 `./gradlew clientTest` 连接一个真实客户端，读服务端打开的窗口并在里面点击。

这些层抓出来的东西就是它们存在的理由："标题一用就崩"、页面契约、一批 pattern 问题、
`on menu interact` 里 `the page` 报 0 基下标、以及 `the viewers of` 被 Skript 自己的属性接走。
Skript 用散文报告、服务端照常启动的那类失败，正是构建必须发现的东西。

另外有两处控制台输出不再走 Skript / Adventure 的内部实现：

- 语法注册改用 Skript 的 addon API，不再用 Skript 2.16 已标记为"将要移除"的静态 `Skript.register…`。
  脚本写法没有任何变化，变的是注册不再依赖那批正在退场的调用。
- 横幅自己写颜色转义序列，不再用反射去摸 Adventure 的 ANSI serializer。那段反射**可能在插件启用时失败**
  —— 对一个横幅来说是最糟的时机；而且 Paper 判定控制台不支持颜色时，横幅会变成灰色。`force-truecolor:
  false` 仍然会打印无颜色的版本。
