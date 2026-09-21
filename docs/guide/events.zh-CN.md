# 事件与事件值

[English](events.md) | 简体中文

## 四个事件

| 事件 | 什么时候触发 | 能取消吗 |
|---|---|---|
| `on menu open` | 向玩家打开菜单之前 | 是 |
| `on menu interact` | 玩家点击或拖拽菜单格子时 | 是 |
| `on page turn` | 玩家翻页时 | 是 |
| `on menu close` | 菜单关闭时，包括玩家关闭、代码关闭或菜单被销毁 | 否 |

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

在 `on menu open` 和 `on page turn` 中使用 `cancel event`，分别可以阻止菜单打开和翻页。
取消翻页后，玩家会留在原来的页面。

## 事件值的两种写法

下表前八行是插件提供的表达式，通常不带 `event-` 前缀。最后两行则是通过 Skript 的
`EventValueRegistry` 注册的事件值，**必须带 `event-` 前缀**：`the event-menu` 和
`the event-menu window`。`the menu`、`the session` 等写法无法解析，并不是返回空值。

| 值 | 意思 | 在哪些事件里 |
|---|---|---|
| `the clicked slot` | 被点击的格子编号，从 0 开始；拖拽时为 `the dragged slots` 中的第一个格子，顺序见拖拽页 | `on menu interact` |
| `the clicked icon` | 被点击格子中的物品 | `on menu interact` |
| `the dragged slots` | 拖拽涉及的所有格子；普通点击时无值 | `on menu interact` |
| `the cursor item` | 鼠标光标上的物品，仅在 `static` 模式下有值 | `on menu interact` |
| `the pressed number key` | 按下的数字键，范围为 1–9；其他点击时无值 | `on menu interact` |
| `the page` | 事件所在页；翻页事件中指原来的页面 | `on menu open`、`on menu interact`、`on page turn` |
| `the future page` | 即将打开的页码 | `on page turn` |
| `the title` | 即将打开的页面标题，可设置 | `on page turn` |
| `the event-menu` | 事件对应的菜单 | 四个事件均可用 |
| `the event-menu window` | 事件对应的窗口 | 四个事件均可用 |

为兼容已有脚本，部分表达式保留了 `the event-` 形式的别名，如 `the event-page`、`the event-title`、
`the event-icon`，返回值与上表相同。`the event-slot` 不在此列，因为它与 Skript 自带的 `event-slot` 冲突。

`the clicked icon` 还保留了较短的旧别名 `the icon`。
`the pressed number key` 没有 `event-` 形式的别名。

一次有效的多格拖拽只触发一次 `on menu interact`，`the dragged slots` 包含本次涉及的格子，
`cancel event` 会取消整次操作。单格拖拽则按普通点击处理，不提供拖拽列表。
具体判定规则，以及拖拽只能放入而不能取出物品的原因，详见[拖拽如何处理](dragging.zh-CN.md)。

### `the future page` 的边界

`the future page` **只在 `on page turn` 中有值**。在 `on menu open` 和 `on menu interact` 中，
它没有值，也不会退回当前页码。在打开事件中读取页码，请使用 `the page`。

`the title` 同样只在 `on page turn` 中可读可写。

## 点击类型

`on menu interact` 使用 Skript 自带的点击类型，与 `on inventory click` 相同，
可以直接使用 Skript 的表达式和字面量：

```skript
on menu interact:
    if the click type is left mouse button:
        send "Left click!" to player
    if the event-clicktype is left mouse button with shift:
        send "Shift + 左键。" to player
```

`the click type` 和 `the event-clicktype` 在 `on menu interact` 中返回相同的值，两种写法都可用。

- `the click type` 原本属于 Skript 的 `on inventory click`，不能随意用于其他事件。
  插件为自己的事件注册了该属性，因此这里也可以使用。
- `the event-clicktype` 是 Skript 通用的事件值写法，通过插件注册的转换器，
  将包含按键和点击模式信息的内部点击类型转换为 Bukkit 点击类型。

可用的字面量包括 `left mouse button`、`right mouse button`、
`left mouse button with shift`、`right mouse button with shift`、`middle mouse button`、`number key`、
`double click`、`drop key`、`swap offhand key` 等。不要使用插件旧有的 `LEFT` 或 `LEFT_CLICK`。
旧的 `menu click type` / `menu click mode` 类型已在 2.0.0 中移除，它们曾输出 `LEFT` 这类
无法直接用作脚本字面量的名称。

### 数字键：`the pressed number key`

Skript 的点击类型无法区分具体按下了哪个数字键，九个键都表示为 `number key`。
要读取具体的按键，请使用 `the pressed number key`：

```skript
on menu interact:
    if the pressed number key is 1:
        send "You pressed 1!" to player
```

数字键点击时，它的值为 1 到 9；其他点击时没有值。配合 `when slot … is clicked` 可以设置快捷键：
在布局中留一格不映射物品，用 `when slot 9 in page 1 of {_menu} is clicked:` 为该格绑定回调，
再判断 `the pressed number key`，就能做成一个隐形按钮。

## 事件中的玩家

`player` 是触发事件的玩家，与窗口的 `viewer` 相同：

```skript
on menu interact:
    send "你点了 %the clicked slot%。" to player
    loop the menu viewers of the event-menu:
        send "有人点了菜单。" to loop-player
```

同一个菜单可以被多名玩家同时打开，各自停留的页面也可以不同。`on menu interact` 只针对执行点击的玩家触发。
要更新其他玩家看到的内容，可以通过 `the menu viewers of {_menu}` 获取查看者，或使用带 `and refresh`
的更新语法；具体影响范围见[窗口](windows.zh-CN.md)。在 Skript 2.16 中，`the viewers of …`
会优先匹配 Skript 自带的属性，相关说明也见该页。

## 取消

```skript
on menu open:
    if player doesn't have permission "shop.open":
        cancel event
        send "你没有权限打开这个菜单。" to player
```

格子回调会先执行，之后才判定 `on menu interact` 的取消状态。因此，取消事件不能撤回已经执行的回调；
如果需要在回调中拦截操作，应自行判断条件并使用 `stop`。
