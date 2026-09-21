# 实用示例

[English](cookbook.md) | 简体中文

语法学过了，不妨动手把它们串起来。本页从一间商店写起，再试试物品浏览器、背包和高亮效果。挑一个贴近需求的例子，结合说明改一改，往往就能举一反三。

对应的完整脚本位于 [`server-test/skript/`](../../server-test/skript/)，`serverTest` 会在 Paper 服务端上解析它们。
商店和浏览器的测试副本还使用 `assert` 检查格子、模式和计算结果；这部分测试没有玩家参与，
并不验证实际点击流程。背包示例使用服务端测试中的脚本。本页只展示各示例所需的部分，完整内容请参考测试副本。

## 一间商店

商品排成一行，下方分别放置价格按钮。菜单使用 `phantom` 模式，玩家无法取走展示物品，
因此无需设置 `with locked icons`；该选项用于保护 `static` 菜单中的真实物品。
价格行用同一个 key 映射一组按钮，再通过 `the menu list index` 读取所点击商品的序号。

```skript
on load:
    # 每件商品对应一个价格按钮，五个按钮共用一个 key，组成列表行。
    # `set the menu list` 填充这一行，`the menu list index` 返回点击的按钮序号。
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
            # 每件商品使用独立的 key，各占一格，不构成列表。
            # 下方的价格按钮是本页唯一的列表。
            map key "1" to icon diamond named "&bDiamond" for {_menu}
            map key "2" to icon emerald named "&aEmerald" for {_menu}
            map key "3" to icon gold ingot named "&6Gold ingot" for {_menu}
            map key "4" to icon iron ingot named "&7Iron ingot" for {_menu}
            map key "5" to icon redstone named "&cRedstone" for {_menu}
            # 五个按钮按布局中 `p` 格子的顺序排列。
            map key "p" to icon {shop::prices::*} for {_menu}

command /shop:
    trigger:
        open menu (the menu with id "shop") for player

# 在交互事件中处理购买；本菜单只有价格格能读到列表序号。
on menu interact:
    if the event-menu is not the menu with id "shop":
        stop

    set {_buy} to {shop::goods::%the menu list index%}
    set {_price} to {shop::cost::%the menu list index%}
    # 商品格没有列表序号，无法取得对应商品，因此不执行购买。
    if {_buy} is not set:
        stop

    if {coins::%uuid of player%} is less than {_price}:
        send "&cYou need %{_price}% coins for that." to player
        stop

    remove {_price} from {coins::%uuid of player%}
    give {_buy} to player
    send "&aYou bought %{_buy}% for %{_price}% coins." to player
```

- 商品格是布局中定义了 key 的格子。在 `static` 菜单中，`with locked icons` 会阻止通过普通点击、
  Shift 点击、数字键、副手交换、丢出和双击收集取走这些物品，并拒绝涉及这些格子的整次拖拽。
  `phantom` 菜单没有真实物品可移动，无需该选项，详见[菜单](menus.zh-CN.md)与[窗口如何工作](how-it-works.zh-CN.md)。
  在 `static` 模式下，布局中未定义 key 的格子仍可供玩家存取物品，背包示例也使用这一机制。
- 一次点击会先执行格子回调，再触发全局事件，最后在 `static` 模式下应用 `locked icons`。
  因此购买逻辑执行后，受保护的展示商品仍在原位，无需额外放回，详见
  [窗口如何工作](how-it-works.zh-CN.md#一次点击是怎么被判定的)。
- 一页只使用一个列表 key：`set the menu list` 和 `the menu list index` 都取第一个映射了多个物品的 key。
  每件商品各用一个 key，整行价格共用一个 key，就能让列表序号与商品序号对应。
- 事件中使用 `the event-menu`；`the menu`、`the window`、`the session` 不是有效的事件值写法，
  详见[事件](events.zh-CN.md)。`{shop::goods::*}` 和 `{shop::cost::*}` 是脚本自行维护的商品与价格数据，
  需要另行准备，本例未展开。

### 卖完了：给大家看，还是只给这个人看

一件货卖光时，把它的价格按钮换掉。两种作用范围只差一行，但含义完全不同：

```skript
# 修改共享页面并刷新：所有正在查看该页的玩家都会看到，重新打开也会保留。
override slot 9 in page 1 to barrier named "&cSold out" for menu with id "shop" and refresh

# 只修改该玩家的窗口，不影响他人；翻页重绘时会覆盖这个临时图标。
set icon in slot 9 of the menu window of player to barrier named "&cSold out"
```

- `override slot … for menu …` 修改页面定义。加上 `and refresh` 后，所有正在查看该页的玩家都会收到更新，
  重新打开菜单也会保留改动。共享库存的商店通常使用这种方式，详见[填充菜单](filling-menus.zh-CN.md)。
- `set icon in slot N of the menu window of player` 只修改该玩家的窗口，不改变菜单定义。
  翻页会重新绘制页面，覆盖对应格子的临时图标，详见[窗口](windows.zh-CN.md)。
- 售罄按钮仍然位于列表格中。`override slot` 只覆盖图标，不改变列表位置，
  `the menu list index` 仍会返回序号，后续 `set the menu list` 也可能重新写入按钮。
  **更换图标不等于阻止购买**，交易逻辑仍须检查库存。

## 一千件物品的浏览器

每页显示 45 件物品，每格一个结果，另设上一页和下一页按钮。整页列表只需一次调用即可填入，
但必须等窗口打开或翻页完成后再执行。通过 `the menu list index` 是否有值，可以区分结果格与翻页按钮。

```skript
# 每页显示 45 件物品。`set the menu list` 不显示超出容量的项目，并清空未填满的格子。
# 最后一页不足 45 件时，无需额外清理。
# 按玩家记录当前显示的结果，避免不同玩家的页面互相干扰。
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
    # 所有页面共用同一份布局。
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
                    # 翻页完成后再填充列表。翻页事件触发时，新页面尚未加载，
                    # 此时写入的列表会被随后加载的 key 图标覆盖。
                    fillBrowser(player, {_page} - 1)
            map key "N" to icon arrow named "&eNext" for {_menu} and when clicked:
                set {_page} to the page of the menu window of player
                if {_page} is less than the page number of {_menu}:
                    turn to page {_page} + 1 for player
                    fillBrowser(player, {_page} + 1)
    # 创建菜单时已有第 1 页，其余页面用 `insert page` 添加；`turn to page` 不会自动新增页面。
    # 一千件物品按每页 45 件分为 23 页，因此再添加 22 页。
    loop 22 times:
        insert page to {_menu} with layout {_layout::*}

command /browse:
    trigger:
        # 示例生成一千件物品，实际使用时可替换为自己的数据源。
        delete {browser::all::*}
        loop 1000 times:
            set {browser::all::%loop-number%} to stone named "&7Item %loop-number%"
        open menu (the menu with id "browser") for player
        # 打开窗口后再填充；`on menu open` 触发时，窗口尚未就绪。
        fillBrowser(player, 1)

on menu interact:
    set {_picked} to {browser::shown::%uuid of player%::%the menu list index%}
    if {_picked} is set:
        send "&aYou picked %{_picked}%" to player
    # 点击箭头等非列表格时，列表序号没有值，不会取得结果。
```

- 所有结果格共用 key `L`，通过 `map key "L" to icon {browser::all::*}` 定义为列表格，顺序与布局一致。
  每页 45 格时，第 `p` 页的首项序号为 `(p - 1) * 45 + 1`。一千件物品需要 23 页，最后一页显示 10 件；
  创建时已有第一页，因此再用 `loop 22 times` 添加其余页面，详见[填充菜单](filling-menus.zh-CN.md)。
- `set the menu list` 一次填充并刷新全部列表格，不必逐格调用。超出容量的物品会被忽略，
  未填满的格子会被清空，因此最后一页无需额外处理。
- `the menu list index` **从 1 开始编号**，点击非列表格时没有值，因此结果格与翻页按钮可以共用一个事件。
  当前显示的结果按玩家保存于 `{browser::shown::%uuid of player%::*}`，避免不同玩家的页面互相干扰，
  详见[填充菜单](filling-menus.zh-CN.md#列表页把一页填成一列物品)。
- 必须在窗口打开或翻页完成后填充列表，不要放在 `on menu open` 或 `on page turn` 中。
  前者触发时窗口尚未就绪，后者触发时页面尚未加载，过早写入会被拒绝或覆盖。
  本例在两个箭头回调和 `/browse` 中填充；其他脚本若为该菜单翻页，也需在翻页后填充，
  否则会显示 key 原先映射的图标。列表变短不会自动减少已有页数，详见[尚未支持](not-supported-yet.zh-CN.md)。

## 会记住东西的背包

要让背包“记住”物品，得由脚本替它留一份记录。窗口只在打开期间存在，`static` 菜单中的内容需要在窗口结束前保存，
下次打开时再按原槽位恢复。用 `the occupied slots of` 获取有物品的格子，
再用 `icon in slot N of` 读取各格物品。

```skript
on load:
    # `build a menu` 是段落语法，以冒号结尾，菜单属性写在段落内。
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
        # 按保存的格子编号，将物品放回原位。
        loop {backpack::%uuid of player%::*}:
            set {_slot} to loop-index parsed as number
            set icon in slot {_slot} of {_window} to {backpack::%uuid of player%::%loop-index%}

on menu close:
    if the event-menu is not the menu with id "backpack":
        stop
    set {_window} to the menu window of player
    # 保存前清除旧记录，避免下次打开时恢复已取走的物品。
    delete {backpack::%uuid of player%::*}
    loop the occupied slots of {_window}:
        set {backpack::%uuid of player%::%loop-value%} to icon in slot loop-value of {_window}
```

- 窗口结束时会清空容器，因此本例在 `on menu close` 中完成保存。玩家关闭窗口、执行 `close` 或 `destroy`、
  退出服务器、死亡或切换世界等结束窗口的路径，都会在清空前触发该事件，此时仍可读取容器。
  保存内容由脚本自行管理，本例使用变量，也可接入 skript-orm 等数据库方案。
  插件不提供“保存窗口”语法，详见[窗口](windows.zh-CN.md)。
- `the occupied slots of {_window}` 只读取布局对应的容器区域。`static` 菜单下方的玩家背包属于玩家自己的库存，
  不随菜单窗口清空，无需在这里保存，详见[窗口](windows.zh-CN.md#窗口身上有什么)。
- 这种做法适用于 `static` 模式。`phantom` 菜单只展示图标，玩家无法放入真实物品，
  两者的区别见[两种模式](modes.zh-CN.md)。
- 保存前先 `delete` 旧数据，再 `loop` 当前内容，否则玩家已经取走的物品仍会留在旧记录中，下次打开时被错误恢复。

## 只给一个玩家高亮

`set icon in slot N of the menu window of player` 修改某位玩家的窗口，
`override slot` 则修改共享页面。要为玩家单独显示选中状态，应使用前者。

```skript
on menu interact:
    # 只修改点击者的窗口，不影响其他玩家。
    set icon in slot 5 of the menu window of player to red stained glass pane

    # 修改第 1 页并刷新所有正在查看该页的窗口，重新打开也会保留。
    override slot 5 in page 1 to red stained glass pane for the event-menu and refresh
```

- 修改窗口图标不改变菜单定义，只影响对应玩家；页面更新则可以推送给所有正在查看该页的玩家，
  详见[窗口](windows.zh-CN.md#改某个玩家看到的东西)。
- 翻页会重新绘制页面，覆盖对应格子的临时高亮；`refresh` 只重新发送当前窗口内容，不会覆盖它。
  如果需要保留高亮，应在翻页后重新设置。
- 通过 `set icon in slot N of {_window}` 写入的内容本身不会新增 `locked icons` 保护。
  格子归属见[窗口如何工作](how-it-works.zh-CN.md#哪些格子属于谁)。
- 区分作用范围时，记住窗口属于单个玩家，页面是共享定义即可。
  [窗口](windows.zh-CN.md)开头的表格列出了哪些操作需要获取窗口。
