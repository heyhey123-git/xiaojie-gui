# 拖拽如何处理

[English](dragging.md) | 简体中文

玩家按住鼠标划过几个格子，看似只有一个动作，客户端却会发送一串数据包。
服务端根据实际涉及的格子数，可能将它视为点击、拖拽，也可能不触发任何事件。
理解这些差别，才能准确处理商品格保护、拖入付款等逻辑，也能解释为什么有时拖拽后脚本没有收到事件。

本页介绍拖拽的协议流程、脚本中的表现，以及两种菜单模式的处理差异。

## 你只需要记住的结论

1. **一次有效的多格拖拽只触发一次交互**。`on menu interact` 执行一次，涉及的格子通过 `the dragged slots` 读取。
2. **`the click type` 仍是 `left/right/middle mouse button`**，没有单独的拖拽点击类型。
3. **取消拖拽会取消整次操作**，不能通过 `cancel event` 只取消其中某一格。
4. **拖拽只能放入，不能取出物品**。记录取出操作时应处理点击；记录放入操作时，也要考虑拖拽。

## 拖拽阶段如何编码

客户端拖拽时发送的是一串容器点击包（`clickType = QUICK_CRAFT`），而非独立的拖拽包。
拖拽阶段编码在 **button** 字段中：

| 阶段 | 左键 | 右键 | 中键（创造） |
|---|---|---|---|
| 开始 | `0` | `4` | `8` |
| 继续 | `1` | `5` | `9` |
| 结束 | `2` | `6` | `10` |

低 2 位表示阶段（开始、继续、结束），高位表示使用的鼠标键。原版为三种操作使用了以下名称：

- 左键 = `CHARITABLE`：把光标上的那叠**均分**给扫过的格子；
- 右键 = `GREEDY`：每个格子**只放 1 个**；
- 中键 = `CLONE`：每个格子放**一整叠**（创造模式限定）。

**开始包带的槽位会被忽略**：服务端只在"继续"包里收集格子，一个继续包一个格子。

## 服务端的三个结局

收到"结束"包时，服务端按收集到的格子数分三种：

| 收集到的格子 | 服务端做什么 | 插件/脚本看到什么 |
|---|---|---|
| **0 格** | 什么都不做 | **没有事件** |
| **恰好 1 格** | 把它**改写成一次普通点击** | 一次普通点击：`the clicked slot` 是那一格，`the dragged slots` 没有值 |
| **≥2 格** | 一次性应用，并发**一次** `InventoryDragEvent` | 一次交互：`the dragged slots` 是被扫过的格子 |

另外两条规则决定了"什么算一格"：

- 光标是**空**的时候拖拽：直接清状态，什么都不发生（零事件）。
- 每个格子要被收进集合，必须**放得下**光标上的东西，而且光标里的数量**还够分**（`数量 <= 已收集格数`
  就跳过）。所以 **1 件物品最多只能扫到 1 格**，2 件最多 2 格；把一个石头拖过 3 个空石头的格子，
  也只有一格会被收进去。

### 为什么"拖拽只能放，不能拿"

收到结束包后，服务端按规则将光标上的物品分配到各格，并扣除光标上的相应数量。
整个流程没有从格子中取出物品的阶段。因此：

- 普通点击、Shift 点击、数字键、双击收集、Q 和副手交换等取出操作，都会作为点击传给脚本。
- 拖拽只能放入物品。为商店或背包记录取出操作时，只需处理点击。

## 脚本这一层

```skript
on menu interact:
    # 点击或有效的多格拖拽，每次只处理一个交互事件。
    set {_type} to the click type            # left / right / middle mouse button
    set {_slots::*} to the dragged slots     # 多格拖拽涉及的格子；普通点击时没有值
    set {_held} to the cursor item           # 光标上的物品，仅 static 模式可读取

    # 保护商品格（0、1）和预留格（5）：涉及其中一格，就拒绝整次操作
    if {_slots::*} is not set:
        set {_slots::*} to the clicked slot
    loop {_slots::*}:
        if loop-value is 0 or 1 or 5:
            cancel event
            stop

    # 对允许交互的格子逐一调用结算逻辑，拖拽也在此处理
    loop {_slots::*}:
        if loop-value is 11 or 12 or 13:
            settle(player, {_held}, loop-value)
```

几个要点：

- 拖拽时，**`the clicked slot` 是 `the dragged slots` 中的第一个格子**，具体顺序见下文。
  要判断拖拽是否涉及某一格，应检查完整的 `the dragged slots`。
- **`and when clicked` 的格子回调也会被拖拽触发**，每格一次。回调中的 `the clicked slot`
  指向当前回调对应的格子，因此编写点击回调时也要考虑拖拽。
- **`the cursor item` 只在 `static` 模式下有值**。幽灵模式没有可供读取的真实光标物品。
- **槽位号使用窗口槽位编号**：`0` 是容器左上角，容器之后依次是玩家背包的 27 格和快捷栏的 9 格，
  两种模式使用相同的编号规则。`static` 模式下，玩家背包区域不属于菜单，
  向该区域写入图标会被拒绝，并给出说明。

## 幽灵模式里的拖拽有什么意义

幽灵菜单由服务端绘制，插件会拦截每个点击包并重新发送窗口内容。玩家无法从中取出真实物品，
拖拽也不会改变真实库存。不过，插件仍需处理拖拽，原因如下：

1. **纠正客户端显示。** 客户端会先在本地预测容器操作，再由服务端同步结果。旧实现未处理拖拽的开始包和
   结束包，发生异常后既未取消操作，也未重新发送内容，导致客户端留下错误显示，也就是“幽灵物品”。
2. **处理可能收到的输入。** 修改过的客户端、其他插件或客户端预测期间，都可能产生拖拽输入。
   脚本可以读取涉及的格子，也可以直接拒绝，例如 `if the dragged slots is set: cancel event`。
   常规多选界面仍应使用点击，原因见下一节。
3. **避免重复执行交互逻辑。** 即使真实物品没有变化，消息、扣款、打开菜单和统计等操作仍会执行。
   一次拖拽必须只触发一次交互事件，避免重复处理。
4. **保持两种模式的接口一致。** 从 `static` 切换为 `phantom` 时，脚本仍可使用相同的交互表达式。

原版客户端很难在幽灵菜单中产生拖拽，因为拖拽需要光标上有物品，而每次点击后的同步会纠正客户端状态。
但玩家可能在点击取物后、同步到达前的短暂预测期间发起拖拽，修改过的客户端或其他插件也可能产生这类输入。
因此，插件不能假定它永远不会发生。

两种模式的格子顺序不同：`static` 接收 Bukkit 的 `InventoryDragEvent`，其 `rawSlots` 是不保留拖拽顺序的集合。
插件按槽位编号排序，因此 `the clicked slot` 是编号最小的一格。`phantom` 直接读取 QUICK_CRAFT 包，
可以保留玩家划过格子的顺序。若脚本需要统一的顺序，请自行排序。

`the dragged slots` 在两种模式下的范围也不同：

- `static` 返回游戏实际会填入物品的格子，放不下或物品数量不足的格子会被跳过。
  `the cursor item` 是本次用于分配的物品堆叠。
- `phantom` 返回玩家划过的所有格子，因为这里没有实际填入物品的操作。

两种模式下，普通点击的 `the dragged slots` 都没有值；单格拖拽也会按点击处理。

幽灵模式的 `the cursor item` **没有值**。从 1.21 起，协议中点击包的光标字段只包含物品哈希，
不包含物品本身，插件无法据此读取完整物品。

## 用点击实现多选

幽灵菜单适合制作点击式多选界面。玩家点击某格后，可以用 `set icon in slot N of {_session}`
只为该玩家显示选中状态，再由确认按钮读取选择集合。运行时设置的图标不属于 `locked icons` 保护的格子，
因此可用于显示玩家各自的内容。示例位于 `server-test/skript/13-phantom-picker.sk`，每次 `serverTest` 都会解析它。

但不能依赖拖拽来完成多选。原版客户端需要光标上有物品才能拖拽，而幽灵窗口无法维持这一状态，
所以“划过一行来选中多格”不能作为正常操作方式。
脚本仍可先读取 `the dragged slots`，将修改过的客户端或其他插件产生的拖拽作为一次交互处理；
没有拖拽列表时，再读取 `the clicked slot`：

```skript
on menu interact:
    set {_slots::*} to the dragged slots
    if {_slots::*} is not set:
        set {_slots::*} to the clicked slot      # 点一下：就是那一格
    loop {_slots::*}:
        if loop-value is not between 0 and 4:
            continue                             # 按钮之类的格子不参与选择
        if {picked::%uuid of player%::*} contains loop-value:
            remove loop-value from {picked::%uuid of player%::*}
        else:
            add loop-value to {picked::%uuid of player%::*}
```

## 客户端测试结果

以下结果来自服务端与客户端联测。由于 mineflayer 不会模拟拖拽，测试由客户端直接发送原始数据包：

| 手势 | 服务端收集 | 事件 | 结果 |
|---|---|---|---|
| 左键点一下 | —— | 一次 `InventoryClickEvent` | 物品移动 |
| 拿 **1 件**物品扫过 2 格 | 1 格 | 一次 `InventoryClickEvent` | 和点一下**完全一样**（落在第一格） |
| 拿 **2 件**物品扫过 2 格 | 2 格 | **只有**一次 `InventoryDragEvent` | 每格 1 件 |
| 空光标拖拽 / 开始包直接接结束包 | 0 格 | **没有任何事件** | 什么都没发生 |
| 拖过"放不下"的格子（例如往绿宝石上拖石头） | 跳过该格 | 看剩下几格 | 那个格子纹丝不动 |

请留意最后一行：即使鼠标划过了两格，服务端也可能只收集到一格，并将操作按普通点击处理。

## 这些是怎么查出来的

以上说明依据以下三个来源：

1. **协议**：`mineflayer` 自己的窗口同步用的就是 `mode: 5, mouseButton: 2`，注释写着
   *"end of a drag that never started"*；
2. **NMS 字节码**：`AbstractContainerMenu.doClick` 里 `getQuickcraftHeader` / `getQuickcraftType` 取的正是
   那两段位，`getQuickCraftPlaceCount` 就是上文的均分公式；整个服务端 jar 里**只有**这个类引用
   `InventoryDragEvent`；
3. **CraftBukkit 的事件处理**：点击包处理器在 `QUICK_CRAFT` 分支跳过构造 `InventoryClickEvent` 的代码，
   因此多格拖拽不触发点击事件；恰好涉及一格时，则会按一次普通点击处理。

对应的自动化：

- `src/test/.../gui/interact/QuickCraftTest.kt`：9 个按钮的相位/键/点击类型解析；
- `src/test/.../gui/receptacle/DragHandlingTest.kt`：一次拖拽 → 一次交互（含格子列表）、单格拖拽 → 点击、
  空拖拽 → 零事件、取消 → 取消整个拖拽事件；
- `server-test/skript/12-client-drag.sk` + `server-test/client/bot.mjs`：**真客户端发原始拖拽包**，验证
  "拒绝的拖拽一个格子都不动"和"接受的拖拽每格都填上"。
