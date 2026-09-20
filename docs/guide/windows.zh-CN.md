# 窗口

[English](windows.md) | 简体中文

**菜单**是定义（页面、布局、图标，所有人共享）；**窗口**是某个玩家打开它之后看到的**那一份**：他在第几页、
他的标题是什么、他每个格子里是什么。一个菜单可以同时被十个人打开，各自在不同的页、看到各自的内容——这就是
窗口存在的理由。

面向脚本的词就是**窗口**：`the menu window of player`。类型本身仍叫 `menusession`（它的名词是
`menu window`），所以语法里 `session` 也照样能写，两个词指的是同一个东西；这一页统一说窗口。

**大多数脚本根本不需要提到它。** 点名窗口只为一件事：改**某一个玩家看到的东西**。

| 想做的事 | 写法 | 要提窗口吗 |
|---|---|---|
| 点一下/拖一下之后做事 | `on menu interact:` 里的 `the clicked slot` / `the dragged slots` | 不用 |
| 给某个玩家开菜单、或关掉 | `open menu {_menu} for player` / `close the menu for player` | 不用 |
| 让某个玩家翻页 | `turn to page 2 for player` | 不用 |
| 改所有人看到的货架 | `override slot 5 in page 1 to X for {_menu}` | 不用（那是**菜单**的事） |
| 改**这个玩家**看到的格子、标题或列表 | `set icon in slot 5 of the menu window of player to X` 这一类 | 要 |

## 拿到窗口

```skript
set {_window} to the menu window of player
if {_window} is not set:
    send "你现在没有打开任何菜单。" to player
```

`the menu window of %player%`（也可以写 `the menu session of %player%`）在他没有窗口时返回**没有值**。
在 Skript 里判断它要用变量中转再 `is not set`（`is not null` 不是 Skript 的条件写法）。

在菜单事件里，这个事件讲的那个窗口是 **`the event-menu window`**。这里的 `event-` 前缀不能省：Skript 的
事件值就注册在这个写法上，`the session`、`the window`、`the menu window` 都不是表达式，写下去只会得到
一句"can't understand this expression"。想按玩家拿也一样：事件里 `player` 就是触发它的那个人，
`the menu window of player` 与 `the event-menu window` 指的是同一个窗口。

## 窗口身上有什么

下表的 `{_window}` 既可以来自 `the menu window of player`，也可以是事件里的 `the event-menu window`。

| 想要 | 写法 |
|---|---|
| 这个窗口属于哪个玩家 | `the viewer of {_window}` |
| 这个窗口对应哪个菜单 | `the menu of {_window}` |
| 这个窗口现在在第几页 | `the page of {_window}` |
| 这个窗口的标题 | `the title of {_window}` |
| 窗口里某个格子的物品 | `icon in slot 5 of {_window}` |
| 容器里**放着东西**的格子 | `the occupied slots of {_window}` |
| 容器里的全部物品（按格子顺序） | `the menu contents of {_window}` |
| 某个玩家此刻的窗口（一步拿到） | `the menu window of player` |

反过来，从玩家直接拿页码也可以：`player's current menu page`。

后两个是"把一个窗口存下来"用的：`the occupied slots of {_window}` 告诉你哪些格子有东西，
`the menu contents of {_window}` 就是那些东西本身（按格子顺序）。两个都只读**容器**——布局描述的那些
格子——因为 `static` 菜单下半部分属于玩家自己。把背包存起来的写法：

```skript
on menu close:
    loop the occupied slots of the menu window of player:
        set {backpack::%loop-value%} to icon in slot loop-value of the menu window of player
```

名字里的 `menu` 不是装饰：Skript 自己的 `contents of %inventory%` 比本插件先注册，短名字会被它接走，
这一点和 `menu viewers` 是一样的。

## 改某个玩家看到的东西

窗口上的标题和图标都是**只对那一个玩家**生效的，这是"每个玩家看到不同内容"的全部实现方式。

```skript
on menu interact:
    if the clicked slot is 4:
        # 只给点击的人换标题
        update title of the event-menu window to "&a你点了中间！"
        # 只给点击的人换一格的内容
        set icon in slot 5 of the event-menu window to red stained glass pane
```

- `update title of [the] [menu] [(session|window)] %menusession% to "…" [and refresh]` ——
  不写 `and refresh` 时只改模型，屏幕上不会立刻变。
- `set icon in slot %numbers% of %menusession% to <物品>` —— 立刻推给那个玩家
  （这里的 `set` 不带 `refresh` 也会刷新，因为它走的是窗口的 `setIcons(…, true)`）。
- `delete icon in slot %numbers% of %menusession%` —— 清掉那些格子。
- 也可以写成 `set the icon of %menusession% in slot %numbers% to …`。

窗口上的图标只改"这个玩家看到的那一份"，**不动菜单本身**：别人看到的还是原样。翻页会把那一页重新画进窗口，
所以落在那一页格子里的临时图标会被盖回去；`refresh` 不会——它只是把窗口**当前**的内容再发一遍，窗口里是什么
就发什么。要改所有人的，用 `override slot` / `map key`（见[填充菜单](filling-menus.zh-CN.md)）。

`set the title of {_window} to "…"` 和 `update title of the menu window … to "…"` 都能改标题，
前者直接改值，后者多一个 `and refresh` 开关。

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
| `close [the] [menu] [(session|window)] %menusession%` | 关掉这个窗口，它消失，并触发 `on menu close` |
| `refresh [the slot %-number% in] [the] [menu] [(session|window)] %menusession%` | 重发内容：给了格子就只发那一格，否则整窗 |
| `clear [the] [menu] [(session|window)] %menusession% [and refresh]` | 清空窗口里的内容但**不关闭**窗口 |

`refresh` 在 `static` 模式上什么也不做 —— 那里的内容由服务端真实库存托管，客户端自己就看得到最新状态。

也能从玩家那一侧操作菜单：`close the menu for player`（等价于关掉他的窗口；他没有窗口时会报一行可读
的错误）。

## 所有人

```skript
set {_viewers::*} to the menu viewers of {_menu}
loop the menu viewers of {_menu}:
    send "你现在在看一个 %the inventory type of {_menu}% 菜单。" to loop-player
broadcast "当前有 %size of the menu viewers of {_menu}% 人在看这个菜单。"
```

`the menu viewers of {_menu}` 返回**正在看**这个菜单的玩家列表。玩家自己关掉、切到另一个菜单、退出服务器，
都会从这里消失；`static` 模式下也是这样。

**为什么叫 `menu viewers` 而不是 `viewers`**：Skript 2.16 自己注册了一个 `viewer[s]` 属性，声明在
**任何对象**上，而且它的注册在插件之前，所以 `the viewers of {_menu}` 会被 Skript 接走、什么都不返回
（升级到 2.16 之后静默失效的写法之一）。`menu viewers` 这个名字 Skript 没有，因此稳定可用。

其它写法：`{_menu}'s menu viewers`、`all players viewing {_menu}`。

注意这是一次快照：拿到列表之后有人关了窗口，`loop` 还会把他走完一遍。要保证还开着，循环里再查一次
`the menu window of loop-player`。

## 窗口什么时候消失

| 情况 | 会发生什么 |
|---|---|
| 玩家关掉窗口 | `on menu close` 触发，窗口结束 |
| `close the menu …` / `destroy the menu …` | 同上；`destroy` 会关掉所有正在看这个菜单的人 |
| 玩家换到另一个菜单 | 旧菜单触发一次 `on menu close`，旧菜单的 `viewers` 里不再有他 |
| 玩家退出服务器 | 窗口被清掉 |
| 插件禁用 / Skript 重载脚本 | **所有菜单都被销毁**，玩家全员被弹出 |

最后一条很重要：菜单持有构建它的脚本里的回调，脚本重载之后那些回调就指向了不存在的触发器。
所以插件通过 Skript 生命周期事件注册表监听 `ScriptLoader.ScriptPreInitEvent`，在整批脚本解析之前
销毁全部菜单，脚本重新加载时自己再建一遍 —— 这就是为什么菜单几乎总是建在 `on load:` 里。

`on load` 里用固定 id 反复建同一个菜单是安全的：同 id 的新菜单会把旧的**销毁**，不会两份并存。
