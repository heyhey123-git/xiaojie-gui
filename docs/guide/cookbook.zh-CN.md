# 菜谱

[English](cookbook.md) | 简体中文

整段可抄的写法，只用文档其它页面里出现过的语法。每条菜谱都是一个问题、一段脚本，再几条说明为什么这么写。

这里的脚本在 [`server-test/skript/`](../../server-test/skript/) 下各有一份副本，`serverTest` 每次运行都会
在真实 Paper 服务端上解析它们：一段停止解析、或者不再做自己声称的事，构建就会失败，所以菜谱不会悄悄过期。
副本和页面上的写法有两处故意的不同：商店和浏览器那两份用 `assert` 断言自己声称的格子、模式和算术（那一层
没有玩家，点不了任何东西）；背包那一份就是服务端自己跑的那份。这一页只放每条菜谱需要的部分，完整的脚本在
副本里。

## 一间商店

货摆在一行，每个货下面一格是价格按钮。菜单是 `phantom`，这本身就保证玩家拿不走商品：幽灵窗口里的东西动
不了，任何试图移动的点击都会被插件撤销。所以这间商店**不写** `with locked icons`——那个开关保护的是
`static` 菜单的格子，那里的物品才是真的。价格行用一个 key 映射一个列表，点中哪一格由
`the menu list index` 说出商品号。

```skript
on load:
    # 每件货一个价格按钮。五个物品共用一个 key，这一行才是"列表行"：
    # `set the menu list` 填的就是它，`the menu list index` 说出点到了第几个。
    set {shop::prices::1} to stone named "&eBuy 1"
    set {shop::prices::2} to stone named "&eBuy 2"
    set {shop::prices::3} to stone named "&eBuy 3"
    set {shop::prices::4} to stone named "&eBuy 4"
    set {shop::prices::5} to stone named "&eBuy 5"

    build a menu {_menu}:
        inventory type: chest inventory
        title: "&6Shop"
        layout: "12345    ", "ppppp    ", "         "
        id: "shop"
        edit:
            # 五件货，一个 key 一件，所以每件货独占一格；只有单个物品的 key 不是列表行，
            # 于是下面那一行价格是这一页唯一的列表。
            map key "1" to icon diamond named "&bDiamond" for {_menu}
            map key "2" to icon emerald named "&aEmerald" for {_menu}
            map key "3" to icon gold ingot named "&6Gold ingot" for {_menu}
            map key "4" to icon iron ingot named "&7Iron ingot" for {_menu}
            map key "5" to icon redstone named "&cRedstone" for {_menu}
            # 列表行：五个按钮，按布局里五个 `p` 格子的顺序。
            map key "p" to icon {shop::prices::*} for {_menu}

command /shop:
    trigger:
        open menu (the menu with id "shop") for player

# 价格回调写在事件里：点任何一格都会走到这里，只有价格格能读出列表序号。
on menu interact:
    if the event-menu is not the menu with id "shop":
        stop

    set {_buy} to {shop::goods::%the menu list index%}
    set {_price} to {shop::cost::%the menu list index%}
    # 点到商品格时 `the menu list index` 没有值，这两个变量也就没有值：那不是一笔交易。
    if {_buy} is not set:
        stop

    if {coins::%uuid of player%} is less than {_price}:
        send "&cYou need %{_price}% coins for that." to player
        stop

    remove {_price} from {coins::%uuid of player%}
    give {_buy} to player
    send "&aYou bought %{_buy}% for %{_price}% coins." to player
```

- 商品格是布局里**写了 key 的格子**。在 `static` 菜单里，这正是 `with locked icons` 保护的东西：点击、
  Shift 点击、数字键、副手交换、丢出、双击收集都拿不走，碰到它们的拖拽整次拒绝；`phantom` 菜单不需要这个
  开关，因为它的窗口本来就什么都动不了，见[菜单](menus.zh-CN.md)与[工作原理](how-it-works.zh-CN.md)。
  布局里空着的格子仍然属于玩家——商店和背包用的是同一个机制。
- 一次点击的判定顺序是：先跑格子回调，再触发事件，最后（在 `static` 菜单里）才应用 `locked icons`。所以
  "购买"跑完之后商品还在原处，不需要额外把商品放回去，见
  [工作原理](how-it-works.zh-CN.md#一次点击是怎么被判定的)。
- 价格行只用一个 key，是因为一页只能有一个"列表 key"：`set the menu list` 和 `the menu list index` 认的
  都是**第一个**映射了多个物品的 key。给每件货一个 key、给整行价格一个 key，序号就正好是商品号。
- 回调读的是 `the event-menu`：`the menu`、`the window`、`the session` 这些短写法不是事件值，根本解析
  不了，事件值的两种形式见[事件](events.zh-CN.md)。`{shop::goods::*}` 和 `{shop::cost::*}` 是脚本
  自己那份商品数据，这里不展开。

### 卖完了：给大家看，还是只给这个人看

一件货卖光时，把它的价格按钮换掉。两种作用范围只差一行，但含义完全不同：

```skript
# 所有人：改的是这一页，正在看这一页的人都会看到，重开菜单也还在。
override slot 9 in page 1 to barrier named "&cSold out" for menu with id "shop" and refresh

# 只给这一个玩家：改的是他的窗口，别人看到的还是原样，翻页会被那一页重新画掉。
set icon in slot 9 of the menu window of player to barrier named "&cSold out"
```

- `override slot … for menu …` 写的是**页面**：它推给所有正在看这一页的玩家（`and refresh`），并且重开
  菜单时还在。商店要的是这种——货架是共享的，一个人买走了别人也该看到，见
  [填充菜单](filling-menus.zh-CN.md)。
- `set icon in slot N of the menu window of player` 写的是**这个玩家的窗口**：只改他一个人看到的副本，
  不动菜单本身。翻页会把那一页重新画进窗口，所以落在那一页格子里的临时图标会被盖回去，见
  [窗口](windows.zh-CN.md)。
- 对**列表**来说，卖光的格子仍然是列表格：`override slot` 只是把这一页自己的图标盖在那一格上，格子在列表里
  的位置没变，所以 `the menu list index` 照样给它编号，之后一次 `set the menu list` 还会把按钮写回去。真正
  拦住这一单的是回调里自己的库存判断——换图只是好看，判断才是规矩。

## 一千件物品的浏览器

一页显示 45 件，一格一个结果，另外两格是上一页/下一页。整页内容**一次调用**填进去，而且必须在窗口已经打开、
或者页面已经翻过去**之后**填；点中的是结果还是箭头，由 `the menu list index` 是否为空区分。

```skript
# 浏览器的一页：把 45 件物品放进窗口的列表格。`set the menu list` 多出来的物品会丢掉、
# 没填满的格子会清空，所以"只剩 3 条结果"不需要额外处理。
# 屏幕上是哪 45 件要**按玩家**记：同一个菜单里两个玩家可以停在不同页，点下去不能读到对方那一页。
function fillBrowser(p: player, page: number):
    set {_window} to the menu window of {_p}
    set {_first} to ({_page} - 1) * 45
    delete {browser::shown::%uuid of {_p}%::*}
    loop 45 times:
        set {_one} to {browser::all::%{_first} + loop-number%}
        if {_one} is set:
            set {browser::shown::%uuid of {_p}%::%loop-number%} to {_one}
    set the menu list of {_window} to {browser::shown::%uuid of {_p}%::*}

on load:
    # 浏览器的每一页长得一样，所以布局写一次、所有页共用。
    set {_layout::*} to "LLLLLLLLL" and "LLLLLLLLL" and "LLLLLLLLL" and "LLLLLLLLL" and "LLLLLLLLL" and "P       N"
    build a menu {_menu}:
        inventory type: chest inventory
        title: "&6Item browser"
        layout: {_layout::*}
        id: "browser"
        edit:
            map key "L" to icon {browser::all::*} for {_menu}
            map key "P" to icon arrow named "&ePrevious" for {_menu} and when clicked:
                set {_page} to the page of the menu window of player
                if {_page} is greater than 1:
                    turn to page {_page} - 1 for player
                    # 翻过去之后再填：翻页事件跑在页面加载之前，而加载会把 key 自己的图标写进这些格子，
                    # 比它更早写的列表会被盖掉。
                    fillBrowser(player, {_page} - 1)
            map key "N" to icon arrow named "&eNext" for {_menu} and when clicked:
                set {_page} to the page of the menu window of player
                if {_page} is less than the page number of {_menu}:
                    turn to page {_page} + 1 for player
                    fillBrowser(player, {_page} + 1)
    # 上面那个布局就是第 1 页；其余的页要自己加，因为页数在菜单创建时就定下来了——
    # `turn to page` 翻不过最后一页。一千件物品是 23 页（每页 45 件），所以再加 22 页。
    loop 22 times:
        insert page to {_menu} with layout {_layout::*}

command /browse:
    trigger:
        # 真实脚本从自己的数据源填这个列表；这里用一千件物品做例子。
        delete {browser::all::*}
        loop 1000 times:
            set {browser::all::%loop-number%} to stone named "&7Item %loop-number%"
        open menu (the menu with id "browser") for player
        # 开窗之后再填，不要写在 `on menu open` 里：那个事件跑的时候还没有窗口可写。
        fillBrowser(player, 1)

on menu interact:
    set {_picked} to {browser::shown::%uuid of player%::%the menu list index%}
    if {_picked} is set:
        send "&aYou picked %{_picked}%" to player
    # 没有值：这一格不是结果，是箭头之类的按钮。
```

- 结果的格子是**一个 key**：`map key "L" to icon {browser::all::*}` 里那个列表让这一页有了列表格，格子
  顺序就是布局顺序。给了一页 45 个格子，第 `p` 页的第一件就是 `(p - 1) * 45 + 1`，一千件是 23 页——上面
  那个 `loop 22 times` 就是它们的来源，因为菜单的页数在创建时就固定了——最后一页 10 件，见
  [填充菜单](filling-menus.zh-CN.md)。
- `set the menu list` 是整页**一次调用、一次刷新**：45 个格子一次，而不是 45 次；多余的物品丢掉、没有物品
  的格子清空，所以短的一页不需要额外代码。
- `the menu list index` 是**1 基**的，点到的不是列表格时没有值。这就是"结果"和"上一页/下一页"共用一个
  事件的方式；屏幕上那一页是**按玩家**记的（`{browser::shown::%uuid of player%::*}`），因为同一个菜单里
  两个玩家可能停在不同页。见[填充菜单](filling-menus.zh-CN.md#列表页把一页填成一列物品)。
- 填的时机必须在**窗口或页面已经就位之后**，不要写在 `on menu open` 或 `on page turn` 里：前者跑的时候窗口
  还不存在，后者跑在页面加载之前——在它们里面写列表，不是被拒绝就是被覆盖。这个浏览器的翻页入口只有两个箭头
  和 `/browse`，所以填充也就写在这三处；如果别的脚本直接给这个菜单翻页，那里也得自己填，否则格子里一直是 key
  被映射时那个图标。页数在菜单创建时就固定了，搜索变短不会让页数变少，这一点写在
  [尚未支持](not-supported-yet.zh-CN.md)。

## 会记住东西的背包

一个窗口只在它打开期间存在，`static` 菜单里空着的格子是真实存在的物品，所以玩家放进去的东西必须在窗口
结束前读出来，下次打开再一格一格放回去。`the occupied slots of` 说出哪些格子有东西，`icon in slot N of`
说出是什么。

```skript
on load:
    # `build a menu` 是段落，第一行需要冒号；下面的属性就是它的内容，所以不存在"空段落"的警告。
    build a menu {_menu}:
        mode: static
        inventory type: chest inventory
        title: "&6Backpack"
        layout: "         ", "         ", "         "
        id: "backpack"
        click delay: 0

command /backpack:
    trigger:
        set {_menu} to the menu with id "backpack"
        open menu {_menu} for player
        set {_window} to the menu window of player
        # 放回上次留下的东西。存的是格子编号，所以物品回到它原来的格子。
        loop {backpack::%uuid of player%::*}:
            set {_slot} to loop-index parsed as number
            set icon in slot {_slot} of {_window} to {backpack::%uuid of player%::%loop-index%}

on menu close:
    if the event-menu is not the menu with id "backpack":
        stop
    set {_window} to the menu window of player
    # 先删掉：玩家拿走一件东西时，旧的那一份不清掉，下次就会白送他一件。
    delete {backpack::%uuid of player%::*}
    loop the occupied slots of {_window}:
        set {backpack::%uuid of player%::%loop-value%} to icon in slot loop-value of {_window}
```

- 窗口的容器**在窗口结束时就被清掉**，所以存东西只能在 `on menu close` 里做完。这个事件在**每一条**结束
  窗口的路径上都会触发 —— 玩家自己关掉、`close`、`destroy`、退出服务器、死亡、换世界 —— 而且触发时容器
  还读得到，因为事件跑在清空之前；没被存下的只是脚本没去读的东西。存下的东西放在脚本自己的存储里（这里
  是变量，真实服务端上通常是 skript-orm 之类的一份数据库），插件没有"保存窗口"的语法，见
  [窗口](windows.zh-CN.md)。
- `the occupied slots of {_window}` 只读**容器**——布局描述的那些格子。`static` 菜单下半场是玩家真实的
  背包，不属于菜单，所以会跟着玩家本人留下来，不需要存，见[窗口](windows.zh-CN.md#窗口身上有什么)。
- 只有 `static` 模式才值得这么写：`phantom` 菜单整页都是菜单自己的格子，玩家本来就放不进东西。两种模式
  的区别见[两种模式](modes.zh-CN.md)。
- 先在 `on menu close` 里 `delete` 再 `loop`：不清的话，玩家拿走的那件物品还留在旧数据里，下次打开会
  凭空回来。

## 只给一个玩家高亮

`set icon in slot N of the menu window of player` 只标记**那个玩家**看到的那一格，`override slot` 标记的是
所有人的那一格。两个词分开写，是因为一个写的是窗口，一个写的是页面。

```skript
on menu interact:
    # 只给点击的人看：别人窗口里还是原样。
    set icon in slot 5 of the menu window of player to red stained glass pane

    # 给所有正在看这一页的人看，重开菜单也还在。
    override slot 5 in page 1 to red stained glass pane for the event-menu and refresh
```

- 窗口上的图标只改"这个玩家看到的那一份"，**不动菜单本身**。变的是他自己，不是货架；页面的改动才推给
  所有正在看这一页的人，见[窗口](windows.zh-CN.md#改某个玩家看到的东西)。
- 翻页会把那一页重新画进窗口，所以落在那一页格子里的临时高亮会被盖掉；`refresh` 不会，它只是把窗口
  **当前**的内容再发一遍。想让高亮活过一次翻页，就在翻页后重新设一次。
- `set icon in slot N of {_window}` 放进窗口的物品**不属于** `locked icons` 保护的格子，这正是"每个玩家
  自己的内容"能存在的原因，见[工作原理](how-it-works.zh-CN.md#哪些格子属于谁)。
- 作用范围就这一条规则：窗口是**一个玩家的一份副本**，页面是**共享的定义**。哪些操作需要写出窗口、哪些
  不需要，[窗口](windows.zh-CN.md)开头有一张表。
