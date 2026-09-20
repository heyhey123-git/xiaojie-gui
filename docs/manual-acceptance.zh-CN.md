# 人工验收清单

自动化有四层：单元测试、真实服务端的脚本与日志断言（`serverTest`，含日志形态门禁）、每个 `@Examples`
的解析、以及连真实客户端的 `clientTest`。这份清单**只写自动化触不到的东西**，每一条都注明"为什么必须
是人工"。第一次搭环境大约 15 分钟（§0.1），之后照 §0.2 的三段式跑完所有检查大约 20 分钟。

跑之前先确认 `./gradlew build serverTest clientTest` 全绿，`./gradlew gendocs` 能生成
`build/skripthub/xiaojie-gui.json`。

**能用断言表达的就不要留在纸面上。** 最近移走的几条：停服日志（`serverTest` 现在要求
`XiaojieGUI has been disabled!`）、`string:` 标题被拒后标题不变（`05-titles.sk` 的原版 `assert`）、
static 菜单上 `with hide player inventory` 的警告（`02-flags.sk` 建这个菜单，门禁要求那句警告文本），
以及两条"不支持的容器类型"——`crafting table inventory` 与 static 商人现在各给一句可读消息并停下，
`02-flags.sk` 断言了这一点，控制台不再出现 `Severe Error` 堆栈。

## 0. 准备

### 0.1 服务端与客户端

| 项 | 要求 |
|---|---|
| 服务端 | Paper 26.2 |
| 插件 | Skript 2.16.2、PacketEvents 2.13.0、本插件（`./gradlew shadowJar` 产出的 `build/dist/xiaojiegui-<版本>.jar`）；SkBee 可选，只影响文本组件标题 |
| 客户端 | **真实 26.2 客户端**。`clientTest` 用的是 26.1 客户端加 ViaVersion + ViaBackwards 翻译层，它证明的是"本插件发的包能穿过真实翻译层"，不是"26.2 客户端看到的就是这些字节" |
| 双人对照 | `server.properties` 里 `online-mode=false`，同一个客户端开两个实例、用两个不同用户名进服；§4、§5 里有两条需要两个客户端同时在同一个菜单里 |
| 脚本 | 把 `docs/manual-acceptance.sk` 复制到 `plugins/Skript/scripts/`，然后 `/skript reload scripts`。**不要**复制 `server-test/skript/` 整个目录：那些脚本里的原版 `assert` 需要 Skript 测试模式，而且 `99-finish.sk` 会自动停服 |

`docs/manual-acceptance.sk` 只用普通语法，任何 Paper 26.2 + Skript 2.16.2 的服务器都能跑；它**只在命令里**
创建菜单、交互监听只对 `acc_*` 菜单说话，所以不会影响别的脚本。每次 `serverTest` 也会把它当普通脚本解析
一遍，写坏了会在 CI 里先失败，而不是在你游戏里；它的命令参数由 `/acc argcheck` 这条自检分支守着
（`serverTest` 会用控制台执行一次并要求那行日志出现），因为参数读错的表现是"每条命令都只打印帮助列表"。

### 0.2 怎么看结果

- 游戏里输入 `/acc` 会列出全部命令；每条命令都会把"要做什么"发到聊天栏。
- **聊天栏**是主要观察窗口：菜单格子上的一次交互会打印 `[菜单事件] 点击第 N 格…`，玩家自己背包那半不会。
- **控制台**指启动服务器的终端，或 `logs/latest.log`；下面说"看控制台"时都指这两处。
- 两个**原版行为**，验收时不要当成缺陷：容器开着的时候，客户端**不能打开聊天栏、也就不能执行任何指令**；
  暂停菜单同样要先关掉容器再按 ESC 才会出现。所以每条检查都先关掉上一个窗口再输入下一条命令。
- 关窗口：**ESC 和菜单里的关闭按钮都能关**（`/acc phantom` 的 B 格子就是关闭按钮），两条路径都验过。
  所以"想退出游戏"的正常流程是：先关掉菜单，再按 ESC 开暂停菜单。

### 0.3 常见节奏

每条检查都是三段：**输入命令 → 按聊天栏提示做动作 → 看聊天栏/控制台**。清单里每条前面都写了命令；
没有命令的（§0 的启动检查、§3 的手写一行、§7 的配置）都单独说明。

### 0.4 启动与控制台（生产形态）

`serverTest` 永远带 `-Dskript.testing.enabled=true` 跑，Skript 的 debug 行因此只在测试里出现；普通
服务器的日志形态只能人看。

- [ ] 用**默认配置**启动：不加载测试模式的 JVM 参数，`plugins/Skript/config.sk` 保持
      `verbosity: normal`。控制台**不应**出现 `Missing entry 'types.*'`（那是 debug 行）。
- [ ] 类型名那一半不需要人工：测试服开着 Skript 的 debug，`lang/default.lang` 一旦不再被读取，
      日志就会出现 `Missing entry 'types.menu'`，`serverTest` 会直接失败——门禁盯着这一条。
      你要确认的只是**生产形态**这一半：默认配置下不应该出现任何 `Missing entry`。
- [ ] 横幅按控制台自己判断颜色（控制台会把转义序列原样打出来时就是纯文本），出现
      `XiaojieGUI has been enabled!`，没有 `[Skript] Severe Error`，也没有 `can't understand`。
- [ ] 加载 README 里的示例脚本后，控制台不出现 `List is missing 'and' or 'or'`，也不出现
      `Empty configuration section`。
- [ ] 把 `plugins/Skript/config.sk` 的 `language` 换成 `simplifiedchinese` 重启一次：类型名词与运行期
      消息仍然可读。如果运行期报错显示成 `log.runtime.error` 这样的**未翻译键名**，那是 Skript 2.16.2
      只在 `english`/`catalan`/`spanish` 里提供了这几个键（`default.lang` 里没有），不是本插件的问题。

## 1. 幽灵模式（默认）

`clientTest` 的拖拽与 `locked icons` 场景都跑在 `static` 菜单上（它只有一个 bot），幽灵模式这套保护
只有真实客户端能看。

命令：`/acc phantom`（打开 README 里的那个菜单；它自己就是那三条检查用的菜单）。

- [ ] 窗口标题是金色的 `Main Menu`，3 行箱子，`A` / `B` 两个按钮在**布局字符串指定的格子**里。
      自动化断言的是标题文本与格子号，颜色是渲染层。
- [ ] 玩家背包显示在窗口下方，行序和真实背包一致（快捷栏在最下面一行）。
- [ ] 把窗口里的物品往外拖：松手后物品**不应该**被真的拿走。
- [ ] 把自己的背包物品往窗口里拖：同样不应该真的移走。
- [ ] Shift 点击窗口里的按钮：不改变窗口内容。
- [ ] 点击窗口下方**自己背包**的格子：聊天栏**不该**出现 `[菜单事件]` 那一行，控制台没有报错。
- [ ] 关掉菜单再打开：格子内容与脚本一致，没有客户端残留的"幽灵物品"（再执行一次 `/acc phantom` 即可）。

## 2. 静态模式

命令：`/acc static`（27 格箱子）、`/acc drag`（拖拽）、`/acc locked`（`locked icons`）、
`/acc statichide`（static + hide 关键字）。

- [ ] 界面与标题正常，放进去的图标出现在正确格子；把石头拿进背包**应该真的拿到**，再放回去。
- [ ] 与幽灵模式的**区别**确实是设计里的那个：这里物品能被真的拿走、放进背包（库存由服务端托管）。
- [ ] `/acc statichide`：下半部分**仍然显示**玩家背包——这个关键字对 static 没有作用，`serverTest`
      已经断言插件会就此警告一句（看控制台），这里人眼看的是"窗口确实没变"。
- [ ] `/acctype merchantstatic`：控制台给出的是一句**人话**（说明 static 不支持商人、用 phantom），
      窗口不应该打开。`02-flags.sk` 已经断言了这句消息与"没有开窗"，这里人眼确认它读起来确实像一句话，
      而不是 Java 堆栈。`/acctype merchant` 则是能正常打开的 phantom 商人窗口。
- [ ] `/acc drag` 的**拖拽**（只有 `static` 下才有意义，机制见 `docs/guide/dragging.zh-CN.md`）：
      - [ ] 光标为空时拖拽：什么都不发生，聊天栏与控制台都没有异常。`clientTest` 只覆盖了"手上有物品"的三种拖拽。
      - [ ] 拿一叠物品按住左键扫过几个空的自有格子，松手后**每一格**都应有它那一份。
      - [ ] 扫过 A / B（第 0、1 格）：聊天栏会说整次拖拽被拒绝，**所有**被扫过的格子都不动。
      - [ ] 只拿 **1 件**物品拖过 2 格：效果应该和点一下完全一样（游戏本身会把它变成一次点击）。
- [ ] `/acc locked` 的 **`with locked icons`**（脚本里不写任何 `cancel event`）。先往背包里准备一组方块，
      然后对商品依次尝试：**点击、Shift 点击、数字键 1-9 交换、副手交换（F）、丢出（Q）、双击收集、
      把物品拖到商品上**。`clientTest` 已经断言了前三种，后四种只有人能试；商品每次都应该原地不动，
      而它的格子回调照常触发（聊天栏会出现"商品格子的回调照常触发"）。布局里空着的格子仍然可以正常存取。

## 3. 每种容器类型

命令：`/acctype <类型>`，类型词就是下表第一列（去掉 `inventory`，见下面的对照）：
`chest`、`workbench`、`beacon`、`enchanting`、`smithing`、`cartography`、`lectern`、`stonecutter`、
`anvil`、`furnace`、`blastfurnace`、`smoker`、`grindstone`、`merchant`、`loom`、`brewing`、`hopper`、
`dropper`、`dispenser`、`crafter`、`barrel`、`enderchest`、`shulker`。每次都是"建一个 + 打开一个"。

自动化逐个创建过其中 10 种（`03-type-*.sk`）并断言"1 页"，单元测试钉着每种布局的格子数。这里人眼要
确认的是**界面身份**（是不是那个方块）与**玩家背包有没有被挤歪**——格子数写错时，唯一的症状就是整个
玩家背包和点击错位。`lectern` 是唯一的例外：原版这个窗口的客户端菜单**只有 1 格**，没有玩家背包那半
（`beacon` 的菜单仍有玩家那半的格子，只是原版界面画不画是另一回事）。这张表与
`docs/guide/menus.zh-CN.md` 一致。

| 类型 | 格子数 | 人眼要确认的 |
|---|---|---|
| `chest inventory` | 行数 × 9 | 行数 = 布局字符串条数 |
| `workbench inventory` | **10** | 是工作台界面：**左边 3×3 输入格、右边一个结果格** |
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
| `crafting table inventory` | —— | 用 `/acctype craftingtable`：**应该报 `Unsupported inventory type`**，窗口不开（`02-flags.sk` 已断言消息与"没有建出菜单"） |

## 4. 标题

命令：`/acc titles`（两页：金色 + 十六进制）、`/acc rename2`（只改第 2 页标题）、`/acc tagged`
（`string:` 形式会被拒绝）。

- [ ] 第 1 页标题是金色、第 2 页是 `&x&f&f&5&5&0&0` 十六进制橙色；点纸张来回翻页时标题跟着换。
      自动化断言了翻页与标题值，人看的是视觉切换干不干净。
- [ ] `&#ff5500` 是**字面量**（Spigot 的行为，不是本插件的 bug）。把 `&x` 那行改成 `&#ff5500橙色`
      再看一次即可。
- [ ] `/acc rename2`：两个客户端都先 `/acc titles`（脚本不会重建已存在的菜单），其中一个用纸张翻到第 2 页
      停住；再执行 `/acc rename2`（这条命令会先给执行者开第 1 页）后，**只有**停在第 2 页的那个客户端
      标题应该变。这条需要两个客户端对照，自动化只有一个 bot。
- [ ] `/acc tagged`：控制台应有一句可读错误（`string:` 是语法标签，不是给脚本写的），标题不变。
- [ ] 装 SkBee 时：把 `docs/manual-acceptance-skbee.sk` 也复制到 `plugins/Skript/scripts/`，
      `/skript reload scripts`，然后 `/accskbee`——窗口标题应显示为 `SkBee 组件标题`。
      这份单独一个文件是因为 `a new text component from` 是 SkBee 自己的语法，没装 SkBee 的服务器连
      解析都过不去。`serverTest` 断言的是读回来的值，渲染要人看。见 `docs/guide/titles.zh-CN.md`。

## 5. 交互

命令：`/acc delay`（1000ms 点击延迟）、`/acc pages`（翻页与边界）。

- [ ] `/acc delay`：快速连点左上角几下，聊天栏只应出现**一条**"第 N 次点击被接受"；第二次点击被
      **静默丢弃**（事件不触发、格子回调不跑），控制台没有报错。`clientTest` 一直用 0ms。
- [ ] `/acc pages`：从第 1 页点下一页翻到第 2 页；再点下一页应什么都不发生（聊天栏会给一句说明）；
      回第 1 页后点上一页同理。结果要么是不动、要么是一句人话，不是堆栈。
- [ ] **两个客户端对照**：两台客户端都执行 `/acc phantom`（脚本不会重建已经存在的菜单，所以不会把对方的
      窗口关掉），然后任意一台执行 `/acc viewers`：人数应该是 2，列表里是两个名字。这条自动化做不了
      （`clientTest` 只有一个 bot）。

数字键、四种点击类型、事件值与 `docs/guide/events.zh-CN.md` 的一致性都已经由 `clientTest` 与
`serverTest` 的断言覆盖，不再列在这里。

## 6. 生命周期

- [ ] `/skript reload scripts`：所有菜单被关闭（玩家窗口被收回），重载后的脚本重建自己的菜单；点旧窗口
      不会执行已经不在任何脚本里的代码。**这条自动化做不了**：重载会把 `serverTest` 依赖的脚本自己卸掉。
      做的时候注意：验收脚本自己也会被重载，重载后要重新 `/acc phantom`。
- [ ] 玩家退出再进：不会自动弹出菜单；重新 `/acc phantom` 正常。
- [ ] 停服：没有残留任务报错（`XiaojieGUI has been disabled!` 本身已由 `serverTest` 断言）。

## 7. 配置

插件没有选项，也不生成配置文件：主线程检查（§1–§6 里那些菜单操作）无条件执行，启动横幅按控制台自己
判断（§0.4）；两者都没有开关可试，这一节只是说明"没有配置项"这件事本身。

## 8. 收尾

- [ ] 把 `plugins/Skript/config.sk` 的 `language` 改回 `english`（§0 的非英语检查用过它）。
- [ ] 删掉 `plugins/Skript/scripts/manual-acceptance.sk`，`/skript reload scripts`。
- [ ] `server.properties` 的 `online-mode` 改回原值；如果为了双人对照建了第二个账号，把它删掉。

## 9. 发现问题时记什么

版本（Paper / Skript / PacketEvents / 本插件）、最小复现脚本、期望看到什么、实际看到什么，以及
`logs/latest.log` 里的相关行——**不要只贴截图**。能塞进 `serverTest` 或 `clientTest` 的，就顺手加一条
断言：这份清单上的每一条，只要能用自动化表达，就不该长期留在人手里。

## 附：自动化已经覆盖的部分

| 层 | 命令 | 覆盖 |
|---|---|---|
| 单元测试 | `./gradlew test` | 点击类型、点击冷却、页面计算、每个布局的格子数（对齐 Bukkit 自己的库存大小）、`locked icons` 的每种拒绝情形 |
| 真实服务端 | `./gradlew serverTest` | 元素能否注册、脚本能否解析、原版 `assert` 与 53 条行为完成记录、每个 `@Examples` 的解析、`docs/manual-acceptance.sk`（本次验收要用的脚本）的解析与参数自检、停服日志，以及日志形态门禁（运行期消息必须走运行期通道、禁止 `Severe Error` / `can't understand` / 已知解析错误形状） |
| 真实客户端 | `./gradlew clientTest` | 26.1 客户端穿过 Via 翻译层：窗口、标题、物品**类型**、四种点击、翻页、拖拽（含被拒绝的拖拽与空光标以外的三种）、`locked icons` 下点击/Shift/数字键都拿不走商品、隐藏玩家背包时下方那几行显示的是页面自己的图标（点它也算一次普通点击），而显示玩家背包时页面为那些格子摆的东西一个也不会出现 |
| 文档导出 | `./gradlew gendocs` | SkriptHub 用的 JSON（58 个元素；事件不在导出里） |
