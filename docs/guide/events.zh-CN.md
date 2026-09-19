# 事件与事件值

[English](events.md) | 简体中文

## 四个事件

| 事件 | 什么时候触发 | 能取消吗 |
|---|---|---|
| `on menu open` | 菜单对一个玩家打开之前 | 是 |
| `on menu interact` | 玩家点了菜单里的一个格子 | 是 |
| `on page turn` | 某个玩家翻页时 | 是 |
| `on menu close` | 菜单关闭（玩家自己关、被代码关、被销毁） | 否 |

```skript
on menu open:
    send "你打开了菜单，现在是第 %the page% 页。" to player

on menu interact:
    if the event-clicktype is left mouse button:
        send "左键点了第 %the clicked slot% 格。" to player

on page turn:
    send "从第 %the page% 页翻到第 %the future page% 页。" to player

on menu close:
    send "菜单关掉了。" to player
```

`on menu open` 和 `on page turn` 里的 `cancel event` 是**真的生效**的：取消打开，菜单就不会弹出来；
取消翻页，玩家就停在原来那页。

## 事件值的两种写法

本插件自己的事件值都按脚本会说的说法写，**不带 `event-` 前缀**：

| 值 | 意思 | 在哪些事件里 |
|---|---|---|
| `the clicked slot` | 被点击的格子编号（0 起）；拖拽时是**第一个**被碰到的格子 | `on menu interact` |
| `the clicked icon` | 被点击的格子里那件物品 | `on menu interact` |
| `the dragged slots` | 拖拽扫过的所有格子；点一下时什么都不是 | `on menu interact` |
| `the cursor item` | 玩家手里拿着的东西（`static` 模式下才有值） | `on menu interact` |
| `the pressed number key` | 按下的数字键，1–9；其它点击什么都不是 | `on menu interact` |
| `the page` | 事件发生在第几页 | `on menu open`、`on menu interact`、`on page turn`（出发页） |
| `the future page` | 正在翻到第几页 | `on page turn` |
| `the title` | 即将翻到的那一页的标题（可设置） | `on page turn` |
| `the menu` | 事件的菜单 | 四个都有 |
| `the session` | 事件的菜单会话 | 四个都有 |

`the event-` 形式（`the event-slot` 除外，它和 Skript 自己的 `event-slot` 撞名）作为别名保留，照顾已经
这么写的脚本：`the event-page`、`the event-title`、`the event-icon` 都能用，值和上面完全一样。

`the clicked icon` 还有更短的历史别名 `the icon`。`the pressed number key` 没有 `event-` 形式 ——
`the event-pressed number key` 不是一句人话。

玩家拖拽时，一次拖拽仍然是**一次** `on menu interact`，`the dragged slots` 就是它扫过的所有格子，
`cancel event` 会拒绝整次拖拽。拖拽的完整机制（为什么只有多格拖拽才看得到、为什么它只能放不能拿）
写在[拖拽是怎么工作的](dragging.zh-CN.md)。

### `the future page` 的边界

`the future page` **只在 `on page turn` 里有值**。在 `on menu open` 和 `on menu interact` 里它什么都不是，
而不是"返回当前页" —— 后者会让脚本分不清"翻页事件"和"打开事件"。要在打开事件里拿页码，用 `the page`。

`the title` 同理，只在 `on page turn` 里可读可写。

## 点击类型

`on menu interact` 里的点击类型就是 **Skript 自己的点击类型**，也就是 `on inventory click` 给你的那个，
用 Skript 的词和它的字面量：

```skript
on menu interact:
    if the click type is left mouse button:
        send "Left click!" to player
    if the event-clicktype is left mouse button with shift:
        send "Shift + 左键。" to player
```

`the click type` 和 `the event-clicktype` 在 `on menu interact` 里**是同一个值**，都能用。

- `the click type` 本身属于 Skript 的 `on inventory click`，在别的事件里会拒绝解析；插件为自己的事件
  注册了这个属性，所以不带前缀的写法在这里也能得到答案。
- `the event-clicktype` 是 Skript 通用的事件值写法，走的是插件注册的转换器（把插件更细的点击类型
  ——知道按键和点击模式——转成 Bukkit 的）。

可用的字面量就是 Skript 那一套：`left mouse button`、`right mouse button`、
`left mouse button with shift`、`right mouse button with shift`、`middle mouse button`、`number key`、
`double click`、`drop key`、`swap offhand key` 等。**不要**再去写插件的 `LEFT` 或 `LEFT_CLICK`：旧的
`menu click type` / `menu click mode` 类型在 2.0.0 已删除，它们打印出来是 `LEFT` 这种没法在脚本里写
的名字。

### 数字键：`the pressed number key`

Skript 的点击类型唯一带不了的信息是**按的是哪个数字键** —— 九个键全都报成同一个 `number key`。
`the pressed number key` 补上这个：

```skript
on menu interact:
    if the pressed number key is 1:
        send "You pressed 1!" to player
```

它在 `number key` 点击时是 1 到 9，其它点击什么都不是。配合 `when slot … is clicked` 就能做快捷键：
在布局里留一格不映射物品，用 `when slot 9 in page 1 of {_menu} is clicked:` 挂一个隐形按钮，
再判断 `the pressed number key`。

## 谁在看这个事件

`player` 是事件的玩家，和 `the session` 的 `viewer` 是同一个：

```skript
on menu interact:
    send "你点了 %the clicked slot%。" to player
    loop the menu viewers of the event-menu:
        send "有人点了菜单。" to loop-player
```

要注意菜单是可以**同时被多个人打开**的，而且每个人可以在不同的页上。`on menu interact` 只给点击的
那个人触发。要改别人看到的东西，用 `the menu viewers of {_menu}` 或者 `and refresh` 那类会通知所有观众的
写法，见[会话](sessions.zh-CN.md)。（`the viewers of …` 在 2.16 上被 Skript 自己的属性接走，见那一页。）

## 取消

```skript
on menu open:
    if player doesn't have permission "shop.open":
        cancel event
        send "你没有权限打开这个菜单。" to player
```

被取消的 `on menu interact` 不会执行已经挂好的格子回调 —— 反过来说，回调**先**跑，事件**后**判定，
所以想在回调里"先拦后放"是做不到的，要拦就在回调里自己 `stop`。
