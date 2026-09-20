# 标题

[English](titles.md) | 简体中文

凡是能写标题的地方 —— `create menu … titled …`、`insert page … with title …`、
`update title of page N in {_menu} to …`、`update title of the menu session … to …`、
`turn to page N for player with new title …`、`build a menu` 的 `title:` —— 用法**完全一样**：
直接给那个值就行。

## 纯文本

```skript
create a phantom menu with chest inventory titled "Main Menu" with layout "#########" with id "main":
update title of page 1 in {_menu} to "Page One"
```

## 不要写 `string:` 和 `component:`

```skript
update title of page 1 in {_menu} to "第一页"          # 对
update title of page 1 in {_menu} to string:"第一页"   # 错
```

`string:` 和 `component:` 是**插件自己 pattern 里的分支标记**，不是脚本写法。输入里只有冒号后面的
内容才是标题；自己手写上前缀，整段文字（含前缀）就变成了标题。旧文档里有这种写法，2.0.0 起会**明确
报错**而不是渲染出一个以 `string:` 开头的标题。

## `&` 颜色码

标题是文本，所以里面的 `&` 颜色码就是格式，**和 Skript 其它地方一样可用**：

```skript
create a phantom menu with chest inventory titled "&6&l主菜单" with layout "#########" with id "main":
```

`§` 同样可用（`&` 会在内部被翻译成它）。接受的字符是 Spigot 的那一套：`0`-`9`、`a`-`f` 是颜色，
`k`-`o` 是格式，`r` 重置。

**副作用**：一个后面跟着颜色字符的 `&` 会被当成颜色码。所以标题里要显示**字面的** `&B`，`&` 做不到 ——
这一点和 Spigot 的物品名完全相同。

## 十六进制颜色

用 Adventure 的 `&x&r&r&g&g&b&b` 写法，**每一位数字都带自己的 `&`**：

```skript
create a phantom menu with chest inventory titled "&x&f&f&5&5&0&0橙色标题" with layout "#########" with id "main":
```

`&#ff5500` 这种写法**不属于**这套语法，写了就是字面上的 `&#ff5500`。

## 文本组件（需要 SkBee）

装了 SkBee 时，标题可以直接给一个文本组件：

```skript
set {_title} to a new text component from "主菜单"
create a phantom menu with chest inventory titled {_title} with layout "#########" with id "skbee_title":
```

这条路径带来两个区别：

- 组件可以带 SkBee 提供的完整格式能力（渐变、点击事件、字体等），不受 `&` 那套限制。
- **读回来**的标题也会是 SkBee 的组件（`the title of page 1 in {_menu}` 之类），
  打印出来是它的旧式文本形式。

`textcomponent` 这个类型**没有**被写进 pattern：一个 pattern 里出现了未注册的类型，整条 pattern 都会
编译失败，那样没有 SkBee 的服务器连 `"纯文本"` 这种标题都用不了了。所以标题槽的类型是 `%-object%`，
到底来的是字符串还是组件，由插件在运行时判断。

没装 SkBee 时，字符串以外的写法不是标题：`titled {_someItem}` 会在运行时报
`Valid Menu title is required.`（`create menu`）或
`The given menu title is not a textcomponent, and cannot be converted to string.`（其它语法）。

## 每页的标题 vs 每个窗口的标题

| 想改什么 | 写法 | 影响范围 |
|---|---|---|
| 某一页的标题（永久） | `update title of page 1 in {_menu} to "…"` | 该页；加 `and refresh` 会通知正在看**这一页**的人 |
| 某个窗口的标题（只对一个人） | `update title of the menu session of player to "…" and refresh` | 那一个玩家的窗口 |
| 直接改值 | `set the title of page 1 in {_menu} to "…"` | 只改模型，不主动推送 |

`update title of page N …` **只会**给正在看第 N 页的玩家换标题；在第 2 页的人看到的一直是第 2 页的标题，
不会被别人的页标题覆盖。想立刻看到效果就加 `and refresh`，否则要等下一次刷新或翻页。

`on page turn` 里还有一个 `the title`：那是"即将翻到的那一页的标题"，在事件里**可以设置**，只对这一次
翻页生效：

```skript
on page turn:
    set the title to "第 %the future page% 章"
```

## 读标题

```skript
set {_title} to the default title of {_menu}
set {_pageTitle} to the title of page 1 in {_menu}
send "第 1 页标题是 %{_pageTitle}%" to player
```

`the default title of {_menu}` 是"之后新增的页默认叫什么"，它就是 `titled …` 给的那个值。

**没有 SkBee 时**这些表达式返回的是字符串，`%…%` 打印出来就是带 `§` 的文本；**有 SkBee 时**返回的是
组件，`%…%` 打印出来是它的旧式文本形式。两者都不是"纯颜色被剥掉"的样子，写比较时要注意。
