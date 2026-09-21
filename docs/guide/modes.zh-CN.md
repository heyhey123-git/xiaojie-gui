# 两种模式：`phantom` 和 `static`

[English](modes.md) | 简体中文

菜单有两种模式：`phantom` 和 `static`。前者适合按钮式界面，后者适合需要存取物品的容器。

## 怎么写

**创建时指定**，省略则使用 `phantom`：

```skript
create a phantom menu with chest inventory titled "虚拟菜单" with layout "#########" with id "phantom_menu":
create a static menu with hopper inventory titled "真实菜单" with layout "AAAAA" with id "static_menu" with 250 ms click delay without hide player inventory:
```

**条目式写法**使用 `mode:`：

```skript
build a menu {_menu}:
    mode: phantom
    inventory type: chest inventory
    title: "Main Menu"
    layout: "AAA", "ABA", "AAA"
```

`mode:` 只接受 `phantom` 和 `static`，其他值会报错：
`Invalid menu mode: … Must be one of: static, phantom.`

**读取模式**时，返回值是普通文本，可以直接进行文本比较：

```skript
set {_mode} to the mode of {_menu}
if the mode of {_menu} is "phantom":
    send "这个菜单是虚拟模式。" to player
```

`the mode of {_menu}` 返回 `"phantom"` 或 `"static"`。

**比较时不要省略引号**，例如不要写 `if the mode of {_menu} is phantom:`。Skript 的枚举字面量来自插件语言文件，
本插件不提供对应定义，只会有自动生成的 `static receptacle mode` 一类写法。而不带引号的 `phantom` 指的是 Minecraft 的幻翼，
会被当作**实体类型**参与比较，结果始终不成立。模式改用文本返回，正是为了避免这种歧义。
旧的 `receptaclemode` 和 `menu click type` 类型也有类似问题，已在 2.0.0 移除。

## 两种模式对玩家意味着什么

| | `phantom`（虚拟） | `static`（真实） |
|---|---|---|
| 底层实现 | 插件通过数据包呈现的窗口 | 服务端创建的真实物品栏 |
| 物品操作 | **固定**，无法取出、拖动或放入 | **可以正常移动、交换和丢弃** |
| 点击行为 | 触发交互，物品仍留在原位 | 与普通容器中的操作相同 |
| 容器下方的背包区域 | 隐藏时由 `player layout` 定义；显示时映射玩家自己的物品 | 玩家的真实背包 |
| 单格刷新 | 支持 | 不支持，`refresh` 不执行操作 |
| 为每个玩家单独修改图标 | 支持 | 支持，修改各自的窗口图标 |

简单来说，**`phantom` 是按钮面板，`static` 是真实箱子**。

大多数功能菜单适合使用 `phantom`：点击按钮执行操作，装饰物也不会被玩家拿走。
如果需要让玩家整理或存取物品，例如自定义仓库、交易界面，再选择 `static`。

## 各自特有的行为

**`phantom`**

- 物品**无法被玩家移动**。客户端可能先显示拿起物品的预测结果，插件随后重发受影响的内容，将显示恢复原状，
  看起来就像物品“弹了回去”。这也使它比 `static` 多一些性能开销。
- 背包显示时，下方四行映射的是**玩家自己的物品**。窗口中的操作不会真正改变背包，此时菜单只使用上方容器区域。
- `hide player inventory` 只决定下方背包区域的归属，不影响上方容器。**隐藏后，这 36 格供菜单使用**，不再映射玩家物品。
  脚本可以用 `set icon in slot N of {_window}` 写入图标，也可以接收这些格子的点击。
  `player layout` 用来定义这四行的布局，设置它会同时隐藏玩家背包，因此两者不是互不相关的开关。
  背包显示时，该区域仍映射玩家物品，不能由 `player layout` 布置。详见[页面](pages.zh-CN.md#player-layout)。

**`static`**

- 物品操作直接作用于服务端的真实物品栏。没有额外保护时，`override slot` 放入的物品也能被玩家拿走。
- `refresh the menu session …` 不执行操作，内容由服务端物品栏自动同步。
- 点击仍受 `click delay` 冷却限制，也仍会触发 `on menu interact`。

## 冷却：两种模式都有

`with %number% ms click delay` / `click delay:` 设置两次点击之间的最小间隔。
`create menu` 默认 **50 毫秒**，`build a menu` 默认 **5 毫秒**。
冷却尚未结束时，点击会被**静默忽略**，既不触发事件，也不执行格子回调。遇到“点击没有反应”时，可以先检查这个值。

```skript
build a menu {_menu}:
    mode: static
    inventory type: hopper inventory
    title: "StaticSelftest"
    layout: "AAAAA"
    id: "static_selftest"
    click delay: 250
    hide player inventory: false
```

读取冷却值：`the minimum click delay of {_menu}`。
