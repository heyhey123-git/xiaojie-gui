# 窗口

[English](windows.md) | 简体中文

**菜单**是所有玩家共享的定义，包括页面、布局和图标；**窗口**则保存某个玩家打开菜单后的状态，
例如当前页码、标题和各格内容。同一个菜单可以被多人同时打开，各自停留在不同页面，看到不同内容。

脚本中可以用 `the menu window of player` 获取窗口。其内部类型仍叫 `menusession`，对应名词为
`menu window`，语法中的 `session` 和 `window` 指的是同一对象。本页统一称为“窗口”。

大多数操作不必直接使用窗口。只有需要读取或修改某位玩家的独立状态时，才需要获取它。

| 想做的事 | 写法 | 要提窗口吗 |
|---|---|---|
| 处理点击或拖拽 | `on menu interact:` 中的 `the clicked slot` / `the dragged slots` | 不用 |
| 为玩家打开或关闭菜单 | `open menu {_menu} for player` / `close the menu for player` | 不用 |
| 让玩家翻页 | `turn to page 2 for player` | 不用 |
| 修改所有玩家看到的货架 | `override slot 5 in page 1 to X for {_menu}` | 不用，修改的是菜单定义 |
| 修改某位玩家看到的格子、标题或列表 | `set icon in slot 5 of the menu window of player to X` 等 | 需要 |

## 拿到窗口

```skript
set {_window} to the menu window of player
if {_window} is not set:
    send "你现在没有打开任何菜单。" to player
```

`the menu window of %player%` 也可以写作 `the menu session of %player%`。玩家未打开菜单时，表达式没有值。
判断时先将结果存入变量，再使用 `is not set`；`is not null` 不是 Skript 的条件写法。

在菜单事件中，使用 **`the event-menu window`** 获取对应窗口，`event-` 前缀不能省略。
`the session`、`the window`、`the menu window` 都无法解析。
也可以通过事件中的玩家获取：`the menu window of player` 与 `the event-menu window` 指向同一窗口。

<a id="窗口身上有什么"></a>

## 窗口的属性

下表的 `{_window}` 可以来自 `the menu window of player`，也可以来自事件中的 `the event-menu window`。

| 属性 | 写法 |
|---|---|
| 窗口所属玩家 | `the viewer of {_window}` |
| 窗口对应的菜单 | `the menu of {_window}` |
| 当前页码 | `the page of {_window}` |
| 窗口标题 | `the title of {_window}` |
| 指定格子的物品 | `icon in slot 5 of {_window}` |
| 容器中有物品的格子 | `the occupied slots of {_window}` |
| 容器中的全部物品，按格子顺序排列 | `the menu contents of {_window}` |
| 玩家当前的窗口 | `the menu window of player` |

也可以直接通过玩家读取页码：`player's current menu page`。

保存窗口内容时，可以用 `the occupied slots of {_window}` 获取有物品的格子，
或用 `the menu contents of {_window}` 按格子顺序读取物品。这两个表达式只读取布局对应的容器区域，
不包含玩家背包区域，因为在 `static` 模式下，后者属于玩家自己的库存。保存背包内容的示例：

```skript
on menu close:
    loop the occupied slots of the menu window of player:
        set {backpack::%loop-value%} to icon in slot loop-value of the menu window of player
```

`menu contents` 中的 `menu` 不能省略。Skript 自带的 `contents of %inventory%` 注册得更早，
省略后会优先匹配它，与下文 `menu viewers` 的情况相同。

## 改某个玩家看到的东西

修改窗口标题和图标，只对该窗口所属的玩家生效：

```skript
on menu interact:
    if the clicked slot is 4:
        # 只给点击的人换标题
        update title of the event-menu window to "&a你点了中间！"
        # 只给点击的人换一格的内容
        set icon in slot 5 of the event-menu window to red stained glass pane
```

- `update title of [the] [menu] [(session|window)] %menusession% to "…" [and refresh]`：
  不加 `and refresh` 时只修改模型，不会立即更新屏幕上的标题。
- `set icon in slot %numbers% of %menusession% to <物品>`：立即向该玩家发送更新，
  无需额外刷新，因为它调用的是窗口的 `setIcons(…, true)`。
- `delete icon in slot %numbers% of %menusession%`：清空指定格子。
- 设置图标也可以写成 `set the icon of %menusession% in slot %numbers% to …`。

窗口图标的修改不会改变菜单定义，也不影响其他玩家。翻页时，页面会重新绘制到窗口中，
覆盖该页格子的临时图标；`refresh` 则只重新发送窗口当前内容，不会将临时图标还原。
要修改共享内容，请使用 `override slot` / `map key`，详见[填充菜单](filling-menus.zh-CN.md)。

`set the title of {_window} to "…"` 和 `update title of the menu window … to "…"` 都能修改标题。
前者只改值，后者可以通过 `and refresh` 立即更新显示。

## 窗口上的操作

```skript
close the menu window of player
close the menu window {_window}

refresh the slot 5 in menu window {_window}
refresh the menu window {_window}

clear the menu window of player
clear the menu window of player and refresh
```

| 语法 | 做什么 |
|---|---|
| `close [the] [menu] [(session\|window)] %menusession%` | 关闭窗口，并触发 `on menu close` |
| `refresh [the slot %-number% in] [the] [menu] [(session\|window)] %menusession%` | 重新发送指定格子的内容；未指定格子时发送整个窗口 |
| `clear [the] [menu] [(session\|window)] %menusession% [and refresh]` | 清空窗口内容，但不关闭窗口 |

`refresh` 在 `static` 模式下不执行操作，因为该模式使用服务端真实库存，由服务端同步内容。

也可以使用 `close the menu for player`，效果等同于关闭该玩家的窗口。如果玩家没有打开窗口，会输出错误说明。

## 所有人

```skript
set {_viewers::*} to the menu viewers of {_menu}
loop the menu viewers of {_menu}:
    send "你现在在看一个 %the inventory type of {_menu}% 菜单。" to loop-player
broadcast "当前有 %size of the menu viewers of {_menu}% 人在看这个菜单。"
```

`the menu viewers of {_menu}` 返回当前正在查看菜单的玩家列表。玩家关闭窗口、切换到另一个菜单或退出服务器后，
都会从列表中移除，`static` 模式也相同。

**请使用 `menu viewers`，不要省略 `menu`。** Skript 2.16 自带的 `viewer[s]` 属性适用于任何对象，
且先于插件注册。因此，`the viewers of {_menu}` 会匹配到 Skript 的属性，却不返回结果。
使用 `menu viewers` 可以避开这一冲突。

其他可用写法包括 `{_menu}'s menu viewers` 和 `all players viewing {_menu}`。

这个列表是获取时的快照。即使之后有玩家关闭窗口，循环仍会遍历到他。
若需要确认窗口仍然打开，请在循环中再次检查 `the menu window of loop-player`。

## 窗口什么时候消失

| 情况 | 会发生什么 |
|---|---|
| 玩家关闭窗口 | 触发 `on menu close`，窗口结束 |
| `close the menu …` / `destroy the menu …` | 同上；`destroy` 会关闭该菜单的所有窗口 |
| 玩家切换到另一个菜单 | 旧菜单触发一次 `on menu close`，玩家从旧菜单的查看者列表中移除 |
| 玩家退出服务器 | 先触发 `on menu close`，再清理窗口 |
| 插件禁用 / Skript 重载脚本 | 所有菜单被销毁，所有菜单窗口关闭 |

脚本重载时需要销毁菜单，是因为菜单持有构建脚本中的回调；重载后，这些回调不再有效。
插件通过 Skript 生命周期事件注册表监听 `ScriptLoader.ScriptPreInitEvent`，在整批脚本解析前销毁全部菜单，
再由脚本加载逻辑重新创建。因此，通常应在 `on load:` 中构建菜单。

在 `on load` 中使用固定 id 重建菜单是安全的：新菜单会销毁同 id 的旧菜单，不会同时保留两份。
