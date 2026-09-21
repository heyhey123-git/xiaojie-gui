# 尚未支持

[English](not-supported-yet.md) | 简体中文

本页列出 2.0.0 尚未提供的功能、设计限制和测试范围。遇到语法无法解析或行为与预期不同时，可以先核对这里的说明。

## 没有声明式的页面段落

页面只能这样产生：

- 创建菜单时给的 `layout`（它永远是第 1 页）；
- `insert page …`（效果或段落形式）。

目前没有 `page:` 段落，也没有一次声明整组页面的写法。脚本通过布局和标题定义每一页，
再用 `insert page` 添加后续页面。

## 没有删除页面的语法

没有 `delete page` / `remove page`。要减少页面，只能销毁整个菜单再按需要重新建：

```skript
on load:
    # 重建之前先销毁，页数就重新从"创建时的那个布局"算起。
    destroy the menu with id "main"
    create a phantom menu with chest inventory titled "重建" with layout "#########" with id "main":
        insert page with layout "#########" with title "第二页"
```

`insert page N` 可以在第 N 位插入新页，原有页面依次后移，但不能用它删除页面。

### 这对"物品浏览器"意味着什么

1000 件物品每页显示 45 件，共需 23 页，可以通过[填充菜单](filling-menus.zh-CN.md)中的列表页实现。
需要留意两点：

- **列表变短不会自动减少页面**。搜索结果只剩 3 条时，后续页面仍然存在。可以改为在固定页面中切换列表内容；
  若要真正减少菜单页数，只能销毁并重建菜单。
- **列表内容可以按玩家设置，页数却是共享的**。使用 `set the menu list of {_session} to …`，
  可以让两名玩家在不同页看到各自的结果，但菜单总页数对所有玩家都相同。

## 没有文字输入

插件目前不提供文字输入语法。铁砧改名虽然能接收文字，但需要额外监听数据包并管理输入状态，不在当前功能范围内。

需要搜索功能时，建议通过聊天接收关键词：在 `on chat` 中检查玩家是否正在搜索，重新计算结果后，
回到主线程使用 `set the menu list` 更新列表。聊天栏负责输入，菜单负责展示结果。

## 没有共享库存

`static` 菜单为每个窗口创建独立的真实库存。即使两名玩家打开同一菜单，也不会操作同一份库存。
窗口结束后，该库存会被清空；无论下一次是谁打开，都不会继承上次放入的物品。
要保留这些内容，需要在 `on menu close` 中自行读取和保存，做法见[实用示例](cookbook.zh-CN.md)中的背包示例。

共享货架可以通过页面图标实现：布局和 `override slot` 修改的是共享页面，配合 `and refresh` 可立即通知查看者。
而供玩家存取物品的空格仍属于各自窗口。多人共同操作一份真实库存不符合当前的独立窗口模型，不在计划范围内。

## 没有 `set slot N of {_menu}` 这类写法

修改菜单定义中的格子内容，请使用以下语法：

```skript
override slot 10 in page 1 to diamond for menu with id "main_menu"
map key "A" to icon stone for {_menu}
```

`set slot 4 in page 1 of menu with id "main" to diamond` 无法解析。
旧文档中出现过的 `set slot …` 并不是本插件提供的语法。

## 旧的 `gui` 语法已删除

`create a gui …`、`%players% (has|have) a gui [open]` 和 `the player's gui` 来自早期的 skript-gui 插件，
已在 2.0.0 中移除。迁移时请参考下表：

| 已删除 | 现在这样写 |
|---|---|
| `create a gui with {_inv} with id "main":` | `create a phantom menu with chest inventory titled "…" with layout "…" with id "main":`，或 `build a menu:` 段落 |
| `the player's gui` | `the menu of the menu session of player` |
| `if player has a gui open:` | `if the menu session of player is set:` |
| `create a gui … with removable items` | `create a static menu …`，或在 `build a menu` 里写 `mode: static` |

旧的 `create gui` 从 0 开始计算页码，传入非空库存时还会报错，不应继续使用。
原先的 `removable items` 模式已有正式对应写法 `static`。

## 有些容器类型没有窗口

`composter`、`chiseled bookshelf`、`decorated pot`、`shelf` 和 `jukebox` 没有对应的 Minecraft 菜单窗口，
也不能通过 Bukkit 创建为可打开的菜单。因此，`create menu` 会报 `Unsupported inventory type`。

玩家背包中的 2×2 合成区域（`crafting table inventory`）同样不支持作为独立菜单打开。
需要工作台界面时，请使用 `workbench inventory`。

## 没有命令，没有权限，也没有配置项

插件不注册命令或权限节点，也不生成配置文件；`plugins/xiaojie-gui/` 中只会有脚本自行写入的内容。
所有功能都通过 Skript 语法调用，菜单的打开、关闭和按钮行为由脚本决定。

主线程检查和横幅颜色也没有配置开关，相关说明见
[窗口如何工作](how-it-works.zh-CN.md#所有菜单操作都必须在主线程上)。

## 依赖是硬性的

- 服务端必须是 **Paper 26.2** 或与之兼容的分支：`plugin.yml` 声明了 `api-version: 26.2`，
  更旧的服务端会直接拒绝加载。
- **Skript 2.16.2** 和 **PacketEvents 2.13.0** 是 `depend`，缺了插件不会启用。
- **SkBee** 是 `softdepend`：只有"标题用文本组件"这一条路需要它，见[标题](titles.zh-CN.md)。

## 点击和物品在真实客户端上验证到什么程度

除服务端点击测试外，`clientTest` 还从客户端检查以下内容：

- **物品**：客户端读取并断言第一页的 `stone` / `diamond` / `emerald` / `clock`、第二页的 `apple`，
  同时检查各图标的 `custom_name`。类型检查可以发现“名称正确但物品映射错误”的问题；
  名称检查则提供不依赖版本物品注册表的识别信息。
- **槽位对齐、页码与标题**：客户端看到的槽位号和 `11-client.sk` 报的点击槽位必须一致，翻页后的标题
  与物品也在客户端一侧断言。
- **拖拽与 `locked icons`**：配合 `12-client-drag.sk`，测试客户端直接发送原始 QUICK_CRAFT 数据包，
  再读取窗口，确认被拒绝的拖拽没有改变格子、被接受的拖拽填入了目标格子。
  测试也检查 `locked icons` 能否阻止普通点击、Shift 点击和数字键操作。
- **玩家背包区域**：`11-client.sk` 分别检查隐藏和显示背包的菜单。
  隐藏背包并定义 `player layout` 时，玩家背包中的钻石不应显示在窗口对应位置；
  客户端应在槽位 27（三行箱子下方的第一格）读到页面设置的面包，点击时也应上报槽位 27。
  显示背包时，即使页面为槽位 27 设置了图标，客户端仍应看到玩家自身的空槽位，防止页面图标覆盖玩家背包。

**这些测试不等于原生 26.2 客户端验证。** 当前测试使用的 mineflayer/minecraft-data 客户端为
26.1（协议 775），服务端为 26.2（协议 776），两者通过 ViaVersion + ViaBackwards 转换协议。
客户端声明使用 26.1，读取的是从 26.2 转换后的数据，物品注册表也转换为 26.1。

因此，这部分测试验证的是：菜单输出经过协议转换后，测试客户端能否解析并得到预期结果。
它不能直接证明原生 26.2 客户端收到的字节完全正确，也不能仅凭失败结果区分插件问题与 Via 转换问题。
要确认原生协议兼容性，仍需使用 26.2 客户端测试。

还没有覆盖的客户端行为：

- **只在客户端存在的"幽灵物品"**（`static` 模式下服务端没换、客户端自己动了格子的情况）。
- **26.2 客户端本身**，见上。
- **同时多个客户端**：两个玩家看同一个菜单时各自看到什么。

如果实际表现与文档不符，请报告复现步骤和客户端版本，便于区分功能问题与协议兼容问题。

## 一些边界行为

以下行为容易与预期不同，编写脚本时请留意：

- **点击冷却会静默丢弃点击。** 两次点击间隔小于 `click delay` 时，事件不触发、格子回调也不跑，
  没有任何提示。默认 `create menu` 是 50 毫秒，`build a menu` 是 5 毫秒。
- **`override slot` 不写 `in page` 时用的是"默认页"**（`with page N` / `default page:`），
  不是硬编码的 1。默认页大于实际页数时会在运行时报 `Page N does not exist in this menu.`。
  只建了一页却设了 `with page 2` 时，请显式写 `in page 1`。
- **`map key` 不写 `on page` 时作用于所有页**，包括之后 `insert page` 加进来的页。想只作用于某一页
  就写 `on page 1`。
- **`the pressed number key` 只在数字键点击时有值**，其他点击时没有值。
- **`the future page` 只在 `on page turn` 中有值**，在 `on menu open` / `on menu interact` 中没有值。
- **没有 id 的菜单不出现在 `all menus` / `all menu ids` 中。**
- **Skript 重载脚本会销毁全部菜单**并关闭玩家的菜单窗口，可在 `on load` 中重建。
- **`the viewers of …` 在 Skript 2.16 中会匹配自带属性。** Skript 的 `viewer[s]` 属性适用于任何对象，
  且先于插件注册，因此该写法不会返回菜单查看者。请使用 `the menu viewers of {_menu}`
  或 `all players viewing {_menu}`。
- **安装 SkBee 后，`the id of {_menu}` 可能匹配到 SkBee 的 `id of %bounds%`**，因为未确定类型的变量也能匹配该语法。
  可以自行保存菜单 id，再用 id 查找菜单，或将菜单对象与 id 分别存入变量。
