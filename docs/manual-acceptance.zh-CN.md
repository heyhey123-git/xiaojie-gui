# 人工验收清单

自动化有四层：单元测试、真实服务端的脚本与日志断言（`serverTest`，含日志形态门禁）、每个 `@Examples`
的解析、以及连真实客户端的 `clientTest`。这份清单**只写自动化触不到的东西**，每一条都注明"为什么必须
是人工"。发布前照着跑一遍，大约 20 分钟。

跑之前先确认 `./gradlew build serverTest clientTest` 全绿，`./gradlew gendocs` 能生成
`build/skripthub/xiaojie-gui.json`。

**能用断言表达的就不要留在纸面上。** 最近移走的几条：停服日志（`serverTest` 现在要求
`XiaojieGUI has been disabled!`）、`string:` 标题被拒后标题不变（`05-titles.sk` 的原版 `assert`）、
static 菜单上 `with hide player inventory` 的警告（`02-flags.sk` 建这个菜单，门禁要求那句警告文本）。
下一批最便宜的候选：`merchant inventory` 与 `crafting table inventory` 的报错文本。

## 0. 环境

| 项 | 要求 |
|---|---|
| 服务端 | Paper 26.2 |
| 插件 | Skript 2.16.2、PacketEvents 2.13.0；SkBee 可选，只影响文本组件标题 |
| 客户端 | **真实 26.2 客户端**。`clientTest` 用的是 26.1 客户端加 ViaVersion + ViaBackwards 翻译层，它证明的是"本插件发的包能穿过真实翻译层"，不是"26.2 客户端看到的就是这些字节" |
| 脚本 | 人工验收优先使用 README/指南里的示例。`server-test/skript/` 是自动测试输入，其中的原版 `assert` 需要专用 Skript 测试模式，不能整目录复制到普通服务器；**尤其别复制 `99-finish.sk`**，它会自动停服 |

### 启动与控制台（生产形态）

`serverTest` 永远带 `-Dskript.testing.enabled=true` 跑，Skript 的 debug 行因此只在测试里出现；普通
服务器的日志形态只能人看。

- [ ] 用**默认配置**启动：不加载测试模式的 JVM 参数，`plugins/Skript/config.sk` 保持
      `verbosity: normal`。控制台**不应**出现 `Missing entry 'types.*'`（那是 debug 行）。
- [ ] 随手写一句类型写错的脚本（例如 `open menu 5 for player`）触发一次 Skript 报错：类型名应显示成
      `menu`，而不是 `types.menu`。这一条验证的是本插件的 `lang/default.lang` 是否真的生效。
- [ ] 横幅彩色（`force-truecolor: false` 时为纯色），出现 `XiaojieGUI has been enabled!`，
      没有 `[Skript] Severe Error`，也没有 `can't understand`。
- [ ] 加载 README 里的示例脚本后，控制台不出现 `List is missing 'and' or 'or'`，也不出现
      `Empty configuration section`。
- [ ] 把 `plugins/Skript/config.sk` 的 `language` 换成 `simplifiedchinese` 重启一次：类型名词与运行期
      消息仍然可读。如果运行期报错显示成 `log.runtime.error` 这样的**未翻译键名**，那是 Skript 2.16.2
      只在 `english`/`catalan`/`spanish` 里提供了这几个键（`default.lang` 里没有），不是本插件的问题。

## 1. 幽灵模式（默认）

`clientTest` 的拖拽与 `locked icons` 场景都跑在 `static` 菜单上（它只有一个 bot），幽灵模式这套保护
只有真实客户端能看。

用 README 里的示例菜单（`create a phantom menu with chest inventory titled "&6Main Menu" ...`）：

- [ ] 窗口标题是金色的 `Main Menu`，3 行箱子，`A` / `B` 两个按钮在**布局字符串指定的格子**里。
      自动化断言的是标题文本与格子号，颜色是渲染层。
- [ ] 玩家背包显示在窗口下方，行序和真实背包一致（快捷栏在最下面一行）。
- [ ] 把窗口里的物品往外拖：松手后物品**不应该**被真的拿走。
- [ ] 把自己的背包物品往窗口里拖：同样不应该真的移走。
- [ ] Shift 点击窗口里的按钮：不改变窗口内容。
- [ ] 点击窗口下方**自己背包**的格子：`on menu interact` 不触发，控制台没有报错。
- [ ] 关掉菜单再打开：格子内容与脚本一致，没有客户端残留的"幽灵物品"。

## 2. 静态模式

用 `create a static menu ...` 建一个 27 格箱子：

- [ ] 界面与标题正常，放进去的图标出现在正确格子。
- [ ] 与幽灵模式的**区别**确实是设计里的那个：这里物品能被真的拿走、放进背包（库存由服务端托管）。
- [ ] 写 `with hide player inventory` 的 **static** 菜单：下半部分**仍然显示**玩家背包——这个关键字
      对 static 没有作用，`serverTest` 已经断言插件会就此警告一句，这里人眼看的是"窗口确实没变"。
- [ ] `create a static menu with merchant inventory ...` 给出的是一句**人话**（说明 static 不支持商人），
      不是 Bukkit 的异常堆栈。
- [ ] **拖拽**（只有 `static` 下才有意义，机制见 `docs/guide/dragging.zh-CN.md`）的手感：
      - [ ] 光标为空时拖拽：什么都不发生，控制台没有异常。`clientTest` 只覆盖了"手上有物品"的三种拖拽。
      - [ ] 拿一叠物品按住左键扫过几个空的自有格子，松手后**每一格**都应有它那一份。
      - [ ] 扫过一个被脚本 `cancel event` 拒绝的格子：整次拖拽都不生效，**所有**被扫过的格子都不动。
      - [ ] 只拿 **1 件**物品拖过 2 格：效果应该和点一下完全一样（游戏本身会把它变成一次点击）。
- [ ] **`with locked icons`**（商店式菜单）：建一个带图标的菜单并在创建时写 `with locked icons`，
      **脚本里不写任何 `cancel event`**。`clientTest` 已经断言了点击、Shift 点击与数字键交换三种，
      人要补的是剩下四种：**副手交换（F）、丢出（Q）、双击收集、把物品拖上去**——商品每次都应该原地不动，
      而它的格子回调（`and when clicked:`）照常触发。布局里空着的格子仍然可以正常存取。

## 3. 每种容器类型

自动化逐个创建过其中 10 种（`03-type-*.sk`）并断言"1 页"，单元测试钉着每种布局的格子数。这里人眼要
确认的是**界面身份**（是不是那个方块）与**玩家背包有没有被挤歪**——格子数写错时，唯一的症状就是整个
玩家背包和点击错位。这张表与 `docs/guide/menus.zh-CN.md` 一致。

| 类型 | 格子数 | 人眼要确认的 |
|---|---|---|
| `chest inventory` | 行数 × 9 | 行数 = 布局字符串条数 |
| `workbench inventory` | **10** | 是工作台界面：结果格在最左上，3×3 在它周围 |
| `beacon inventory` | **1** | 只有放宝石那一格 |
| `enchanting table inventory` | **2** | 物品格 + 青金石格 |
| `smithing inventory` | **4** | 模板、装备、材料、结果 |
| `cartography table inventory` | **3** | 地图、纸、结果 |
| `lectern inventory` | **1** | 只有放书那一格 |
| `stonecutter inventory` | 2 | |
| `anvil` / `furnace` / `blast furnace` / `smoker` / `grindstone` / `merchant` | 3 | |
| `loom inventory` | 4 | |
| `brewing stand` / `hopper inventory` | 5 | |
| `dropper` / `dispenser` / `crafter` | 9 | dropper 与 dispenser 是**两个不同界面** |
| `barrel` / `ender chest` / `shulker box` | 27 | |
| `crafting table inventory` | —— | **应该报 `Unsupported inventory type`**：客户端没有这个窗口 |

## 4. 标题

- [ ] `titled "&6金色"` 有颜色；`titled "&x&f&f&5&5&0&0橙色"` 是十六进制颜色；`titled "&#ff5500橙色"`
      里的 `&#...` 是**字面量**（Spigot 的行为，不是本插件的 bug）。颜色只有真客户端能看。
- [ ] 翻页后窗口标题变成新页的标题。自动化断言了翻页与标题值，人看的是视觉切换干不干净。
- [ ] `update title of page N ... and refresh` 只影响**正在看第 N 页**的人——这条需要**两个客户端**
      对照，自动化只有一个 bot。
- [ ] 装了 SkBee 时，文本组件标题能正常显示；`serverTest` 断言的是读回来的值，渲染要人看。
      见 `docs/guide/titles.zh-CN.md`。

## 5. 交互

- [ ] 把 `click delay` 设成 1000 再快速连点同一格：第二次点击被**静默丢弃**（事件不触发、格子回调不跑），
      控制台没有报错。`clientTest` 一直用 0ms。
- [ ] 翻页边界：第一页点"上一页"、最后一页点"下一页"，结果要么是不动、要么是一句人话，不是堆栈。
- [ ] 两个玩家同时看一个菜单时，`the menu viewers of {_menu}` 是两个人，不是空——同样需要两个客户端。

数字键、四种点击类型、事件值与 `docs/guide/events.zh-CN.md` 的一致性都已经由 `clientTest` 与
`serverTest` 的断言覆盖，不再列在这里。

## 6. 生命周期

- [ ] `/skript reload`：所有菜单被关闭（玩家窗口被收回），重载后的脚本重建自己的菜单；点旧窗口不会
      执行已经不在任何脚本里的代码。**这条自动化做不了**：重载会把 `serverTest` 依赖的脚本自己卸掉。
- [ ] 玩家退出再进：不会自动弹出菜单；重新 `open menu` 正常。
- [ ] 停服：没有残留任务报错（`XiaojieGUI has been disabled!` 本身已由 `serverTest` 断言）。

## 7. 配置

`plugins/xiaojie-gui/config.yml`，改完**重启服务端**（这两个值在插件启用后只读一次）；自动化一直跑
默认值，所以这两个开关只有人能试：

- [ ] `enable-async-check: false`：从异步代码里开菜单不再报错。默认 `true` 是刻意的：菜单要动库存，
      就得在主线程上。
- [ ] `force-truecolor: false`：控制台横幅变纯色；游戏内不受影响。

## 8. 发现问题时记什么

版本（Paper / Skript / PacketEvents / 本插件）、最小复现脚本、期望看到什么、实际看到什么，以及
`logs/latest.log` 里的相关行——**不要只贴截图**。能塞进 `serverTest` 或 `clientTest` 的，就顺手加一条
断言：这份清单上的每一条，只要能用自动化表达，就不该长期留在人手里。

## 附：自动化已经覆盖的部分

| 层 | 命令 | 覆盖 |
|---|---|---|
| 单元测试 | `./gradlew test` | 点击类型、点击冷却、页面计算、每个布局的格子数（对齐 Bukkit 自己的库存大小）、`locked icons` 的每种拒绝情形 |
| 真实服务端 | `./gradlew serverTest` | 元素能否注册、脚本能否解析、原版 `assert` 与 51 条行为完成记录、每个 `@Examples` 的解析、停服日志，以及日志形态门禁（运行期消息必须走运行期通道、禁止 `Severe Error` / `can't understand` / 已知解析错误形状） |
| 真实客户端 | `./gradlew clientTest` | 26.1 客户端穿过 Via 翻译层：窗口、标题、物品**类型**、四种点击、翻页、拖拽（含被拒绝的拖拽与空光标以外的三种）、`locked icons` 下点击/Shift/数字键都拿不走商品、隐藏玩家背包时下方那几行显示的是页面自己的图标（点它也算一次普通点击），而显示玩家背包时页面为那些格子摆的东西一个也不会出现 |
| 文档导出 | `./gradlew gendocs` | SkriptHub 用的 JSON（58 个元素；事件不在导出里） |
