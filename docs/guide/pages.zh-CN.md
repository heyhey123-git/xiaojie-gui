# 页面

[English](pages.md) | 简体中文

## 菜单默认只有一页

创建菜单时给的 `layout` **永远是第 1 页**，你在它上面映射的东西就都在这一页里。绝大多数菜单到
这里就结束了，不需要管页码。

分页是"需要时才加"的功能。如果你只想换页内容，就保留一页，用 `override slot` / `map key` 换掉格子
里的东西，把"页码"存在自己的脚本变量里 —— 这是 1.x 脚本常见的做法，2.0.0 依然支持。

## 页码从 1 开始

`page 0` **不存在**。旧文档里写 `page 0` 的地方是错的：`in page 1` 才是第一页，
`insert page 1` 指的是"插到第 1 位"。越界的页码会给出可读报错（`Page 0 does not exist` 之类），
而不是在 Skript 内部抛异常。

## `with page N` 只决定先显示哪一页

```skript
create a phantom menu with chest inventory titled "Paged" with layout "AAAAAAAAA" with id "paged" with page 2:
```

这行建出来的菜单**只有 1 页**（那个 layout），`default page` 是 2。它的意思是"打开时先显示第 2 页"，
不是"先建出 2 页"。所以上面这个菜单在 `open menu` 时会失败，因为第 2 页不存在。

`with page N` / `default page: N` **从来不会**改变菜单有几页。以前一旦写了它，插件就不创建任何页面，
而没有页面的菜单打不开 —— 也就是说 `create menu … with layout … with page 2` 过去会造出一个打不开的
菜单。这一版修好了：布局永远成为第 1 页。

要读这个值：`the default page of {_menu}`；要改它：`set the default page of {_menu} to 2`
（小于 1 的值会被夹到 1）。

## 加页：`insert page`

**效果写法**（一行写完）：

```
insert page [%-numbers%] [to %-menu%]
    [with layout %-strings%]
    [with player layout %-strings%]
    [with [new] title <标题>]
```

```skript
insert page 1 to {_menu} with layout "xxxxxxxxx", "xooooooxx", "xxxxxxxox" with player layout "ooooooooo", "oooooooox", "xxxxxxxxx" with title "New Page"
```

**段落写法**（`layout:` / `player layout:` / `title:` 三个条目，全是可选的）：

```skript
insert page 1 into menu {_menu}:
    layout: "xxxxxxxxx", "xooooooxx", "xxxxxxxox"
    player layout: "ooooooooo", "oooooooox", "xxxxxxxxx"
    title: "New Page"
```

插到哪里：

| 写法 | 结果 |
|---|---|
| `insert page …` | 追加到末尾 |
| `insert page 1 …` | 插到**第 1 位**：原来的布局页顺延成第 2 页 |
| `insert page 2 …` | 插在第 1 页之后 |
| 页码超出范围 | 夹到 `1..页数+1`，也就是等价于追加 |

翻页时**只会清掉这一页自己有的格子**（布局里写了 key 的格子，加上 `override slot` 覆盖过的格子），
其它格子原地不动。所以一个把东西存在空格子里的背包菜单，翻页回来东西还在——这也是"翻页背包"能成立的原因。

不写 `layout:` 的新页会继承菜单的**默认布局**（`the default layout of {_menu}`，可以用
`set the default layout of {_menu} to …` 改）；不写 `title:` 会继承默认标题
（`the default title of {_menu}`）。

**在菜单事件里可以省略菜单**，因为当前事件就带着它：

```skript
on menu interact:
    insert page with layout "BBBBBBBBB"
```

在事件之外**必须**写出菜单，否则这行根本不解析（`insert page … to %-menu%`）。

## 翻页

```skript
turn to page 2 for player
turn to page 2 for player with new title "第二章"
```

一次 `turn to page` 会触发 `on page turn`，事件里能拿到出发页和目的页。你也可以用
`open menu {_menu} for player and go to page 2` 达到同样效果；对一个已经打开该菜单的玩家再
`open menu` 不会报错，而是把他送到你指定的页。

想翻页但不想换标题，就不写 `with new title`。写了标题的话，那个标题只是**这一次显示**用的，不会写回
页面的标题 —— 要永久改标题用 `update title of page N in {_menu} to "…"`。

## 一个两页菜单

```skript
on load:
    create a phantom menu with chest inventory titled "&6商店" with layout "#########", "#AAAAAAA#", "#########" with id "shop":
        insert page 2 with layout "#########", "#BBBBBBB#", "#########" with title "&6第二页"
        map key "A" to icon diamond named "&b第一页的东西" for {_menu}
        map key "B" to icon emerald named "&a第二页的东西" for {_menu}
        map key ">" to icon arrow named "&e下一页" for {_menu} and when clicked:
            if the page is less than the page number of {_menu}:
                turn to page (the page + 1) for player
        map key "<" to icon arrow named "&e上一页" for {_menu} and when clicked:
            if the page is greater than 1:
                turn to page (the page - 1) for player

command /shop:
    trigger:
        set {_menu} to the menu with id "shop"
        open menu {_menu} for player
```

两个字符 `>` 和 `<` 在**两页**里都出现，`map key` 也就把它们映射到两页上：**不写 `on page` 时
作用范围是所有页**（包括之后 `insert page` 加进来的页）。所以翻页按钮在每一页都有效，而 `A` 只在
第 1 页有格子、`B` 只在第 2 页有格子，互不干扰。

## 读页码

| 想要 | 写法 |
|---|---|
| 菜单一共几页 | `the page number of {_menu}` |
| 某个会话当前在第几页 | `the page of the menu session of player` |
| 某个玩家当前在第几页 | `player's current menu page` |
| 某页的标题 | `the title of page 1 in {_menu}` |
| 布局里某个 key 在第几格 | `the slots of key "A" in page 1 of {_menu}` |
| 某个格子是哪个 key | `the key of slot 1 in page 1 of {_menu}` |

`the page number of {_menu}` 就是页数，不是"当前页"。菜单事件里的 `the page` 是事件发生的页。

## `player layout`

它描述**容器下方那 4 行**（主背包 3 行 + 快捷栏 1 行），也就是窗口里玩家那一半。不多于 4 条就被采用，
不够的补成空行。它和主布局用的是同一套规则：一个字符一个格子，空格留空。3 行箱子的下半场从第 27 格
开始，所以容器里写的 key 落在 0~26，下方写的 key 从 27 起。

**写了 `player layout` 就自动隐藏玩家背包**，这不是另一个要记的开关，而是同一句话：那 4 行只有"玩家背包
被隐藏"时才是菜单自己的空间——客户端无论哪种情况都是照最后一份内容包画这半场的。隐藏之后菜单可以把自己
的图标放在那里，点上去和点别处一样；没隐藏时那半场显示的就是玩家自己的物品。

```skript
create a phantom menu with chest inventory titled "&6背包" with layout "AAA      ", "AAA      ", "AAA      " with player layout "B        ", "         ", "         ", "         " with id "backpack":
    map key "B" to icon chest named "&e你的背包" for menu with id "backpack"
```

这半场是**菜单**的属性而不是某一页的——玩家的背包要么在要么不在——所以在**任意一页**写 `player layout`
都会隐藏整个菜单的这半场。自己没写 player layout 的页面，这 36 格就是空的；第 1 页保留自己 layout 给的
内容。

只想要"下半场隐藏但空着"，就只写 `with hide player inventory`：那 36 格属于菜单，运行时可以用
`set icon in slot N of {_window}` 填一格。

事后又用 `show player inventory of {_menu}` 把它显示出来，就等于取消它：容器下方那几行又回到玩家手里，
而这一页为它们摆的图标会压在玩家自己的物品上——每次这么调用都会给一句警告。

`static` 模式没有玩家布局：那个窗口**就是**玩家真实的背包，所以在那里写 player layout 会在创建菜单时
给一句警告，并且什么也不会变。
