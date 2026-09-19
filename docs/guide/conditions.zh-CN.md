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

它读的就是 `hide player inventory` 那个标志：`create menu … with hide player inventory`、
`build a menu` 的 `hide player inventory: true`、`hide player inventory of {_menu}` 都会改它。

要改就用效果，两种写法：

```skript
hide player inventory of {_menu}
show player inventory of {_menu}
```

否定只有两种写法：`isn't` 和 `is not`（pattern 就是 `(isn't|is not)`），别的写法（`isnt`、
`isn't not`）都不认。要判断"显示了"，写 `if player inventory of {_menu} is not hidden:`。

菜单是"没有值"时这个条件是 **false**（不是报错）：`if player inventory of {_menu} is hidden:` 在
`{_menu}` 没设置时不会成立，脚本继续往下跑。

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

什么时候会被销毁：

- 有人对同一个 id 再执行一次 `create menu`（旧的那份被销毁）；
- 你执行了 `destroy the menu {_menu}` / `destroy the menu with id "main_menu"`；
- 插件禁用，或者 Skript 重载脚本（**所有**菜单都被销毁）。

一个已销毁的菜单**不能**被打开、翻页、改内容 —— 这些操作会给出可读的错误。这个条件就是用来在脚本里
提前挡住它们的：

```skript
if {_menu} is not destroyed:
    open menu {_menu} for player
else:
    send "菜单还没建好，稍等。" to player
```

`destroy` 本身是幂等的：已经销毁的菜单再销毁一次什么也不做。

## 判断"玩家有没有打开菜单"

没有专门的语法，用会话：

```skript
if the menu session of player is not set:
    send "你还没有打开任何菜单。" to player
```

这是 2.0.0 里取代旧 `if player has a gui open:` 的写法（旧的 `gui` 语法已删除）。
