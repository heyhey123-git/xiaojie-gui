# 页面

[English](pages.md) | 简体中文

## 菜单默认只有一页

菜单不必一开始就面面俱到。一页够用，就先做好这一页：创建时提供的 `layout` **始终作为第 1 页**，直接映射图标即可，不必处理页码。

需要时再添加分页。也可以始终保留一页，通过 `override slot` / `map key` 更换内容，并在脚本变量中自行记录页码。
这是 1.x 中常见的做法，2.0.0 仍然支持。

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

`with page N` / `default page: N` **只设置默认打开的页码，不改变页数**。旧版曾有设置此项后不创建初始页的问题，
现已修复：传入的布局始终创建为第 1 页，但其他页面仍需另行添加。

读取默认页码用 `the default page of {_menu}`，修改用 `set the default page of {_menu} to 2`。
小于 1 的值会自动调整为 1。

## 加页：`insert page`

**效果写法**（一行写完）：

```
insert page [%-numbers%] [to %-menu%]
    [with layout %-strings%]
    [with player layout %-strings%]
    [with [new] title <标题>]
```

```skript
insert page 1 to {_menu} with layout "xxxxxxxxx" and "xooooooxx" and "xxxxxxxox" with player layout "ooooooooo" and "oooooooox" and "xxxxxxxxx" with title "New Page"
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
| 页码超出范围 | 调整到 `1..页数+1` 范围内：小于 1 时插到开头，大于 `页数+1` 时追加到末尾 |

翻页时**只清除原页面管理的格子**，即布局中已映射图标的格子和 `override slot` 覆盖过的格子，其余内容保持不变。
这样，储物菜单就能在翻页后保留玩家放在空格子里的物品。不过，各页并非独立的储物空间；目标页的图标仍可能覆盖同一位置的内容。

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

省略 `with new title` 时，使用目标页自身的标题。指定的新标题**仅用于这一次显示**，不会写回页面。
要永久修改页面标题，请用 `update title of page N in {_menu} to "…"`。

翻页要求目标页与当前窗口使用**相同的容器布局**（包括箱子行数）。布局不一致时，会在改变页码或
现有物品之前拒绝翻页。关闭后再打开不同大小的页面会创建新窗口，**不是**安全迁移存储物品的方式；
关闭存储菜单前，请显式保存或返还玩家的物品。

## 一个两页菜单

```skript
on load:
    build a menu {_menu}:
        inventory type: chest inventory
        title: "&6商店"
        layout: "#########", "#AAAAAAA#", "#########"
        id: "shop"
        edit:
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

上例展示了翻页按钮的回调，但布局中尚未放入 `>` 和 `<`。要让按钮显示出来，还需在**两页布局中为这两个 key 留出格子**。

`map key` **不写 `on page` 时作用于所有页**，包括之后通过 `insert page` 添加的页面。
只要布局含有相应 key，就会显示映射图标。`A` 只出现在第 1 页，`B` 只出现在第 2 页，因此两者互不影响。

## 读页码

| 想要 | 写法 |
|---|---|
| 菜单一共几页 | `the page number of {_menu}` |
| 某个窗口当前在第几页 | `the page of the menu session of player` |
| 某个玩家当前在第几页 | `player's current menu page` |
| 某页的标题 | `the title of page 1 in {_menu}` |
| 布局里某个 key 在第几格 | `the slots of key "A" in page 1 of {_menu}` |
| 某个格子是哪个 key | `the key of slot 1 in page 1 of {_menu}` |

`the page number of {_menu}` 就是页数，不是"当前页"。菜单事件里的 `the page` 是事件发生的页。

## `player layout`

它描述**容器下方的四行**，即三行主背包和一行快捷栏。最多接受四条布局字符串，不足的补为空行。
规则与主布局相同：一个字符占一格，空格留空。对于三行箱子，容器槽位编号为 0–26，下方背包区域从编号 27 开始。

**设置 `player layout` 会自动隐藏玩家背包**，让这片区域交由菜单使用。菜单可在其中显示图标、接收点击，
与上方容器格子一样。未隐藏时，该区域显示玩家自己的物品；客户端始终按收到的内容包呈现窗口。

```skript
build a menu {_menu}:
    inventory type: chest inventory
    title: "&6背包"
    layout: "AAA      ", "AAA      ", "AAA      "
    player layout: "B        ", "         ", "         ", "         "
    id: "backpack"
    edit:
        map key "B" to icon chest named "&e你的背包" for {_menu}
```

背包的**隐藏状态属于整个菜单**，布局内容则各页独立。在任意一页设置 `player layout`，都会隐藏整个菜单的玩家背包。
未设置玩家布局的页面，这 36 格保持为空；第 1 页仍保留自身玩家布局定义的内容。

如果只想隐藏背包并留空，写 `with hide player inventory` 即可，运行时仍可用
`set icon in slot N of {_window}` 向其中写入图标。

菜单图标与玩家背包不能共用这片区域。存在玩家布局时，调用 `show player inventory of {_menu}` 会被**拒绝并给出说明**，
避免菜单图标与玩家物品重叠。要恢复背包显示，需要销毁菜单，再以不含 `player layout` 的定义重建。

`static` 下方始终是玩家的真实背包，不支持玩家布局。创建菜单时设置 `player layout` 只会产生警告，不会改变内容。
