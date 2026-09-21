# 标题

[English](titles.md) | 简体中文

所有接受标题的语法都可以直接传入标题值，用法相同，包括：

- `create menu … titled …`
- `insert page … with title …`
- `update title of page N in {_menu} to …`
- `update title of the menu session … to …`
- `turn to page N for player with new title …`
- `build a menu` 中的 `title:`

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

`string:` 和 `component:` 是插件内部语法模式的分支标记，**不应写进脚本**。
旧文档曾使用这些前缀，导致前缀也成为标题的一部分。自 2.0.0 起，这种写法会**明确报错**，
不再渲染出带前缀的标题。

## `&` 颜色码

文本标题支持 `&` 颜色码，用法与 Skript 其他地方相同：

```skript
create a phantom menu with chest inventory titled "&6&l主菜单" with layout "#########" with id "main":
```

也可以使用 `§`，插件会在内部将 `&` 转换为它。支持的字符与 Spigot 相同：`0`-`9`、`a`-`f` 表示颜色，
`k`-`o` 表示格式，`r` 用于重置。

注意：`&` 后面如果紧跟颜色字符，就会被解析为颜色码。因此，直接写 `&B` 无法原样显示这两个字符，
这一点与 Spigot 的物品名相同。

## 十六进制颜色

使用 Adventure 的 `&x&r&r&g&g&b&b` 写法，**每一位十六进制数字前都要加 `&`**：

```skript
create a phantom menu with chest inventory titled "&x&f&f&5&5&0&0橙色标题" with layout "#########" with id "main":
```

这里不支持 `&#ff5500`，它会原样显示为 `&#ff5500`。

## 文本组件（需要 SkBee）

安装 SkBee 后，可以直接用文本组件作为标题：

```skript
set {_title} to a new text component from "主菜单"
create a phantom menu with chest inventory titled {_title} with layout "#########" with id "skbee_title":
```

使用文本组件时，有两点区别：

- 组件可携带 SkBee 提供的格式信息，如渐变、点击事件、字体等，不限于 `&` 颜色码。
- 通过 `the title of page 1 in {_menu}` 等表达式读取的标题也会是 SkBee 组件，
  输出时则使用旧式文本形式。

标题参数使用 `%-object%`，由插件在运行时判断传入的是字符串还是组件，而没有直接使用 `textcomponent` 类型。
这是因为语法模式中只要出现未注册的类型，整条模式就会编译失败；若直接使用 `textcomponent`，
未安装 SkBee 的服务器连纯文本标题也无法使用。

未安装 SkBee 时，只能传入字符串。`titled {_someItem}` 这类非字符串值会在运行时报错：
`create menu` 报 `Valid Menu title is required.`，其他语法报
`The given menu title is not a textcomponent, and cannot be converted to string.`。

## 页面标题与窗口标题

| 想改什么 | 写法 | 影响范围 |
|---|---|---|
| 某一页的标题（永久） | `update title of page 1 in {_menu} to "…"` | 该页；加 `and refresh` 会通知正在看**这一页**的人 |
| 某个窗口的标题（只对一个人） | `update title of the menu session of player to "…" and refresh` | 那一个玩家的窗口 |
| 直接改值 | `set the title of page 1 in {_menu} to "…"` | 只改模型，不主动推送 |

`update title of page N …` 只影响正在查看第 N 页的玩家，不会覆盖其他页面的标题。
要立即更新窗口标题，请加上 `and refresh`；否则要等下次翻页才会显示新标题。
单独执行 `refresh` 只会重新发送窗口内容，不会更新标题。

在 `on page turn` 中，`the title` 表示即将打开的页面标题。可以在事件中修改它，
但修改只对本次翻页生效：

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

`the default title of {_menu}` 是后续新增页面使用的默认标题，也就是 `titled …` 传入的值。

未安装 SkBee 时，这些表达式返回字符串，通过 `%…%` 输出的是带 `§` 颜色码的文本；安装 SkBee 后，
返回的是组件，通过 `%…%` 输出的是组件的旧式文本形式。两种输出都保留颜色码，比较标题时请留意。
