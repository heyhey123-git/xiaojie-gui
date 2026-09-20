# 尚未支持

[English](not-supported-yet.md) | 简体中文

这一页列的是**有意不在 2.0.0 里**的东西，以及已知的边界。碰到"为什么这行不解析"时先看这里。

## 没有声明式的页面段落

页面只能这样产生：

- 创建菜单时给的 `layout`（它永远是第 1 页）；
- `insert page …`（效果或段落形式）。

**没有** `page:` 段落，也没有"给菜单声明一组页面"的写法。一页 = 一个布局 + 一个标题，
这是有意为之：页面不是一个需要长期维护的对象，而是"布局 + 标题"的简写。

## 没有删除页面的语法

没有 `delete page` / `remove page`。要减少页面，只能销毁整个菜单再按需要重新建：

```skript
on load:
    # 重建之前先销毁，页数就重新从"创建时的那个布局"算起。
    destroy the menu with id "main"
    create a phantom menu with chest inventory titled "重建" with layout "#########" with id "main":
        insert page with layout "#########" with title "第二页"
```

`insert page N` 能改页序（插到第 N 位，原来的顺延），但**移走**一页做不到。

### 这对"物品浏览器"意味着什么

1000 个物品、每页 45 格 = 23 页，**能做**（见[填充菜单](filling-menus.zh-CN.md)里的列表页）。但有两件事要提前知道：

- **页数是创建时定下的，删不掉**。所以"搜索结果只有 3 条"时，后面那些页只能是空的（或者你干脆把"位置"编码进内容，
  而不是编码进页码）。想真正变少，只能销毁重建整个菜单。
- **每一页的内容要按玩家写**（`set the menu list of {_session} to …`），但**页的份数**是全菜单共享的：A 在第 3 页、
  B 在第 5 页各自看到自己的结果没问题，可页的总数对所有人一样。

## 没有文字输入

窗口里放不了输入框：原版容器没有这个控件，玩家的输入从来不经过点击包，而本插件的整套模型都建立在点击包上。

**铁砧能做，但我们不做。** 铁砧（还有锻造台）确实有一个客户端输入框，服务端能通过**改名包**（`ServerboundRenameItemPacket`）
拿到那串文字，所以"在菜单里打字搜索"在技术上是可行的。不做的理由：

- 它需要**另一条协议通路**：改名包不是点击包，得单独注册一个包监听器、单独管一个"这个玩家现在在给哪一格输入"的
  状态，还要在客户端与服务端之间同步那个文字（`ClientboundContainerSetDataPacket`）。这已经不是"菜单"了，是另一个子系统。
- 铁砧界面**画的是铁砧**：三格、锤子、经验等级。想让它看起来像搜索框，就得接受它长得像附魔台旁边的铁砧；想让它长得像别的，
  就又回到了 phantom 那一套。
- 它天然是**一次性的**：铁砧窗口只有一次改名机会，输完要么关窗要么重开，做"边打边筛"的体验很差。

**推荐的做法**：输入走聊天（`on chat` 里判断"这个玩家正在搜索菜单 X"），拿到的关键词重算列表再 `set the menu list`
刷新那一页。这样输入框的事情交给聊天栏，菜单只负责显示结果，两边都简单。

## 没有共享库存

两个玩家看同一个菜单时，**看不到同一份箱子**：`static` 菜单的库存是每个窗口一份的（每个人自己的真实库存）。
窗口结束时它会被**清空**，下一个窗口是新建的一份——所以玩家放进格子的东西不会漏给下一个打开的人，同一个玩家
再打开也一样从空开始。要留住玩家放的东西，得在 `on menu close` 里自己读出来（[菜谱](cookbook.zh-CN.md)里的
背包示例就是这么做的）。

"共享货架"要用**图标**来表达：货架上的东西是页面的布局和 `override slot` 放的，而它们改的是**页面**——所有人
看到的那一份，所以商店 = 有图标的共享货架 + 空格子的各人存储（想让改动立刻到屏幕上，写 `and refresh`）。
真的让多人操作同一个真实库存，会和"每个窗口一份、每窗口一页"的模型正面冲突，我们不打算做。

## 没有 `set slot N of {_menu}` 这类写法

改格子内容只有两条语法：

```skript
override slot 10 in page 1 to diamond for menu with id "main_menu"
map key "A" to icon stone for {_menu}
```

`set slot 4 in page 1 of menu with id "main" to diamond` **不解析**（它既不是效果也不是条件）。
旧文档里的 `set slot …` 从来不属于这个插件。

## 旧的 `gui` 语法已删除

`create a gui …`、`%players% (has|have) a gui [open]`、`the player's gui` 来自更早的 skript-gui
插件，2.0.0 把它们**删掉**而不是修好。对照表：

| 已删除 | 现在这样写 |
|---|---|
| `create a gui with {_inv} with id "main":` | `create a phantom menu with chest inventory titled "…" with layout "…" with id "main":`，或 `build a menu:` 段落 |
| `the player's gui` | `the menu of the menu session of player` |
| `if player has a gui open:` | `if the menu session of player is set:` |
| `create a gui … with removable items` | `create a static menu …`，或在 `build a menu` 里写 `mode: static` |

旧的 `create gui` 页码从 0 开始，只要传进去的库存里有物品就会崩；它写死的那个模式（`removable items`
即 `static`）现在有了正式写法。

## 有些容器类型没有窗口

`composter`、`chiseled bookshelf`、`decorated pot`、`shelf`、`jukebox` 不是"还没做"，而是**做不了**：
Minecraft 没有为它们提供菜单，客户端也就没有这个窗口可画（Bukkit 里这些库存类型没有对应菜单、也不能被
创建），所以 `create menu` 只能明确报 `Unsupported inventory type`。

玩家自己的 2×2 合成格（`crafting table inventory`）也是同理：客户端画这个窗口时画出来的是工作台，格数
对不上，所以不支持，要合成台界面用 `workbench inventory`。

## 没有命令，没有权限，也没有配置项

插件不注册任何命令、不注册任何权限节点，也没有任何选项，所以它不写配置文件：`plugins/xiaojie-gui/` 里只有
脚本自己放的东西。所有入口都是 Skript 语法，菜单的打开/关闭/按钮都由脚本自己决定。

主线程检查和横幅颜色同样是写死的，没有开关可试——两件事都写在
[窗口到底是怎么工作的](how-it-works.zh-CN.md#所有菜单操作都必须在主线程上)里。

## 依赖是硬性的

- 服务端必须是 **Paper 26.2** 或与之兼容的分支：`plugin.yml` 声明了 `api-version: 26.2`，
  更旧的服务端会直接拒绝加载。
- **Skript 2.16.2** 和 **PacketEvents 2.13.0** 是 `depend`，缺了插件不会启用。
- **SkBee** 是 `softdepend`：只有"标题用文本组件"这一条路需要它，见[标题](titles.zh-CN.md)。

## 点击和物品在真实客户端上验证到什么程度

服务端的点击一直都有验证；真实客户端那一层（`clientTest`）现在验证的比"窗口标题变了"更多：

- **物品**：第一页四个键的图标类型（`stone` / `diamond` / `emerald` / `clock`）、第二页的 `apple`，
  以及每个图标身上的 `custom_name`，都由客户端自己读出来断言。类型是更强的那一条 —— 它说的是
  "菜单给这个键映射了哪个物品"，而 `custom_name` 只说明"这个物品被起了什么名字"，一个映射错了物品
  但名字照抄的实现能过后者、过不了前者。名字仍然留着，因为它是唯一与版本注册表无关的身份信息。
- **槽位对齐、页码与标题**：客户端看到的槽位号和 `11-client.sk` 报的点击槽位必须一致，翻页后的标题
  与物品也在客户端一侧断言。
- **拖拽与 `locked icons`**：`12-client-drag.sk` 发的是真正的拖拽包（拖拽是一串"把阶段放在 button 里"
  的点击，mineflayer 造不出来，所以由 bot 直接发原始包），bot 再读回自己的窗口，确认被拒绝的拖拽哪个
  格子都没动、被接受的拖拽把扫过的格子都填上了，以及 `locked icons` 自己就能挡住点击、Shift 点击和
  数字键。
- **容器下方那几行**（两个方向都查）：`11-client.sk` 专门有一个隐藏了玩家背包、又写了 `player layout` 的
  菜单。玩家自己的主背包里放了一个钻石，而窗口同一格（3 行箱子下方的第一格，第 27 格）客户端读到的是页面
  自己的面包——这一条只有客户端能看见，服务端两边都是"菜单的一格里有东西"。点那一格也会带着 27 这个槽位号
  上报。同一条规则的另一面是第一页那个**显示**玩家背包的菜单：它也给第 27 格摆了东西，而 bot 读回来必须是
  它自己（空）的那一格——页面的图标一旦跑到玩家的格子里，这一条就会失败。

**但这不等于"真实的 26.2 客户端看到了这些包"。** 客户端的 mineflayer/minecraft-data 最新只有
26.1（协议 775），服务端是 26.2（协议 776），所以 `clientTest` 在这一层装了 ViaVersion + ViaBackwards：
客户端如实声明自己是 26.1，服务端把 26.2 翻译成 26.1 再发出去，客户端读到的是**翻译后的视图**。
因此这一层证明的是：

- 插件真的发出了这些包 —— Via 只翻译它拿到的东西，插件没发的包不可能出现在这里；
- 26.2 才有的包头/字段经过翻译后仍然能被真实客户端解析，物品注册表也被翻译回 26.1，所以类型可断言。

它**不能**证明的是：真实 26.2 客户端直接看到的字节就是对的。Via 改写过的形状和插件原本的 26.2 形状
在这一层是同一件事，所以"插件发对了 26.2 的字节"和"插件发的字节 Via 能翻译"在这里分不开；反过来，
Via 自己的翻译问题在这一层看起来也会像插件的问题。要区分只能真有一个 26.2 客户端。

还没有覆盖的客户端行为：

- **只在客户端存在的"幽灵物品"**（`static` 模式下服务端没换、客户端自己动了格子的情况）。
- **26.2 客户端本身**，见上。
- **同时多个客户端**：两个玩家看同一个菜单时各自看到什么。

发现对不上时请报告，不要说"大概能用"。

## 一些边界行为

这些不是 bug，但第一次遇到会意外：

- **点击冷却会静默丢弃点击。** 两次点击间隔小于 `click delay` 时，事件不触发、格子回调也不跑，
  没有任何提示。默认 `create menu` 是 50 毫秒，`build a menu` 是 5 毫秒。
- **`override slot` 不写 `in page` 时用的是"默认页"**（`with page N` / `default page:`），
  不是硬编码的 1。默认页大于实际页数时会在运行时报 `Page N does not exist in this menu.`。
  只建了一页却设了 `with page 2` 时，请显式写 `in page 1`。
- **`map key` 不写 `on page` 时作用于所有页**，包括之后 `insert page` 加进来的页。想只作用于某一页
  就写 `on page 1`。
- **`the pressed number key` 只在 `number key` 点击时有值**，其它点击是"没有值"。
- **`the future page` 只在 `on page turn` 里有值**，在 `on menu open` / `on menu interact` 里什么都不是。
- **没有 id 的菜单不出现在 `all menus` / `all menu ids` 里。**
- **Skript 重载脚本会销毁全部菜单**，玩家会被弹出窗口；脚本在 `on load` 里重建即可。
- **`the viewers of …` 在 2.16 上不再指向本插件。** Skript 自己注册了一个声明在任何对象上的
  `viewer[s]` 属性，并且注册在插件之前，所以这个写法会被 Skript 接走、返回空。菜单的观众要写
  `the menu viewers of {_menu}`（或 `all players viewing {_menu}`）。
- **`the id of {_menu}` 装了 SkBee 时可能被 SkBee 的 `id of %bounds%` 抢走**（未定类型的变量
  也匹配得上）。想要菜单 id 又装了 SkBee，就用**按键查菜单**那一侧，或者把菜单存进变量、
  记下 id 自己保存。
