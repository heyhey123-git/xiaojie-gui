# 尚未支持

[English](../../README.md) | 简体中文

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

## 没有命令，也没有权限

插件不注册任何命令、不注册任何权限节点。所有入口都是 Skript 语法，菜单的打开/关闭/按钮都由脚本
自己决定。

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
- **拖拽**：`clientTest` 只做单击、Shift 点击、数字键和翻页，没有跨格拖动。
- **26.2 客户端本身**，见上。

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
