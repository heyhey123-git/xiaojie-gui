# 两种模式：`phantom` 和 `static`

[English](modes.md) | 简体中文

每个菜单都有模式，只有两个值：`phantom` 和 `static`。

## 怎么写

**创建时**（不写就是 `phantom`）：

```skript
create a phantom menu with chest inventory titled "虚拟菜单" with layout "#########" with id "phantom_menu":
create a static menu with hopper inventory titled "真实菜单" with layout "AAAAA" with id "static_menu" with 250 ms click delay without hide player inventory:
```

**条目式**用 `mode:`：

```skript
build a menu {_menu}:
    mode: phantom
    inventory type: chest inventory
    title: "Main Menu"
    layout: "AAA", "ABA", "AAA"
```

`mode:` 只接受 `phantom` 和 `static` 两个词，写别的会报
`Invalid menu mode: … Must be one of: static, phantom.`

**读回来**是一段**普通文本**，用文本比较就行：

```skript
set {_mode} to the mode of {_menu}
if the mode of {_menu} is "phantom":
    send "这个菜单是虚拟模式。" to player
```

`the mode of {_menu}` 返回 `"phantom"` 或 `"static"`。

**注意不要**写 `if the mode of {_menu} is phantom:`（不带引号）。Skript 里枚举常量的字面量来自插件的
语言文件，本插件不提供，所以能解析的只有自动生成的 `static receptacle mode` 这种东西；而裸的
`phantom` 属于 Minecraft 的幻翼实体，于是 `is phantom` 会悄悄地和**实体类型**比较，永远不成立。
文本没有这套机制，所以模式是文本。同样的坑也适用于旧的 `receptaclemode` / `menu click type` 类型 ——
它们在 2.0.0 都取消了。

## 两种模式对玩家意味着什么

| | `phantom`（虚拟） | `static`（真实） |
|---|---|---|
| 底层是什么 | 插件用数据包自己画出来的窗口 | 服务端真实创建的一个物品栏 |
| 里面的物品 | **固定**，拿不起来、拖不动、放不进去 | **可以正常移动、交换、丢弃** |
| 玩家的鼠标 | 只是"点一下"，物品永远留在原地 | 就是普通箱子里的行为 |
| 玩家背包区域 | 由 `player inventory layout` 描述，是**只读的镜像** | 玩家看到的就是他自己的真实背包 |
| 每格单独刷新 | 支持 | 不支持（`refresh` 什么也不做） |
| 每玩家单独改图标 | 支持 | 支持（会话图标是窗口层的） |

一句话：**`phantom` 是"按钮板"，`static` 是"真箱子"。**

绝大多数 UI 菜单用 `phantom` —— 它就是为"一排按钮，点一下做一件事"设计的，玩家不可能把里面的
装饰方块拖走。要做一个玩家能整理、存取物品的容器（比如自定义仓库、交易界面）才用 `static`。

## 各自特有的行为

**`phantom`**

- 物品**不能被玩家搬动**。玩家真的把物品拿起来（拖到光标上、拿去合成）时，插件会把受影响的那一格
  重发一遍，看起来就像"刚拿起来又弹回去了" —— 这就是它"按不动"的实现方式，也是它比 `static`
  多花一点性能的地方。
- 下方玩家背包是用 `player inventory layout` 摆出来的一份**显示副本**，玩家在里面的操作不会真的改到
  背包。所以 `phantom` 菜单不适合做需要搬运物品的交互。
- `hide player inventory` 只影响"下方那 4 行还在不在"，对上面的容器部分没影响。

**`static`**

- 玩家移动物品是**真的**在服务端物品栏上发生的，所以 `override slot` 改过的格子也会被玩家搬走。
- `refresh the menu session …` 是空操作（内容本来就由客户端自己同步）。
- 仍然会按菜单的冷却（`click delay`）判定点击，也仍然触发 `on menu interact`。

## 冷却：两种模式都有

`with %number% ms click delay` / `click delay:` 是两次点击之间的最小间隔，默认 `create menu` 是
**50 毫秒**，`build a menu` 是 **5 毫秒**。没到时间的那次点击会被**静默丢弃**（事件不触发，格子回调
也不跑）—— 排查"点了没反应"时先看这个值。

```skript
create a static menu with hopper inventory titled "StaticSelftest" with layout "AAAAA" with id "static_selftest" with 250 ms click delay without hide player inventory:
```

读回来：`the minimum click delay of {_menu}`。
