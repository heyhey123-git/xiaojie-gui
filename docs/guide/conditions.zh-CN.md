# 条件

[English](conditions.md) | 简体中文

## 玩家背包是否隐藏

```
player inventory of %menu% is hidden
player inventory of %menu% (isn't|is not) hidden
```

```skript
if player inventory of {_menu} is hidden:
    send "这个菜单隐藏了玩家背包。" to player
else:
    send "这个菜单显示了玩家背包。" to player
```

该条件读取菜单的 `hide player inventory` 标志。`create menu … with hide player inventory`、
`build a menu` 中的 `hide player inventory: true`，以及 `hide player inventory of {_menu}` 都会设置它。
定义 **`player layout` 也会启用隐藏**，因为此时容器下方的四行已划为菜单区域，
详见[页面](pages.zh-CN.md#player-layout)。

可以使用以下效果切换显示状态：

```skript
hide player inventory of {_menu}
show player inventory of {_menu}
```

只要菜单中有页面定义了 `player layout`，`show player inventory` 就会被拒绝，并在控制台说明原因。
该区域不能同时用于页面图标和玩家物品。要恢复玩家背包区域，请销毁菜单，再以不含 `player layout` 的定义重建。

否定形式只支持 `isn't` 和 `is not`，不支持 `isnt` 或 `isn't not`。
要判断背包是否显示，可以写 `if player inventory of {_menu} is not hidden:`。

如果 `{_menu}` 未设置，`if player inventory of {_menu} is hidden:` 的结果为 **false**，
不会报错，也不会执行该条件下的代码。

## 菜单是否已销毁

```
[the] [menu] %menu% (is|was) destroyed
[the] [menu] %menu% (isn't|is not|wasn't|was not) destroyed
```

```skript
if the menu with id "main_menu" is destroyed:
    send "主菜单已经被销毁，不能再用。" to player

if {_menu} is not destroyed:
    open menu {_menu} for player
```

以下情况会销毁菜单：

- 使用同一个 id 再次执行 `create menu`，旧菜单会被销毁；
- 执行 `destroy the menu {_menu}` 或 `destroy the menu with id "main_menu"`；
- 禁用插件或重载 Skript 脚本，此时所有菜单都会被销毁。

已销毁的菜单不能再打开、翻页或修改内容，尝试这些操作会收到错误说明。
可以先检查菜单状态，避免执行无效操作：

```skript
if {_menu} is not destroyed:
    open menu {_menu} for player
else:
    send "菜单还没建好，稍等。" to player
```

`destroy` 可以重复调用：对已销毁的菜单再次执行，不会产生额外操作。

## 判断"玩家有没有打开菜单"

无需专门的条件，检查玩家是否有菜单窗口即可：

```skript
if the menu session of player is not set:
    send "你还没有打开任何菜单。" to player
```

这是 2.0.0 里取代旧 `if player has a gui open:` 的写法（旧的 `gui` 语法已删除）。
