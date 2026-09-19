# 在语法里指代菜单

[English](../../README.md) | 简体中文

一段脚本可能同时有十几个菜单，所以每条语法都要知道你在说哪一个。有三种方式，分别对应三种场合。

## 三种方式

**在菜单段落里：不写。**

```skript
create a phantom menu with chest inventory titled "菜单" with layout "AAAAA" with id "main":
    override slot 4 in page 1 to diamond named "特殊物品" for menu with id "main"
```

`create menu` / `build a menu` / `edit menu` 的段落体里，"当前菜单"就是刚建好或正在编辑的那个，所以
`insert page …` 之类可以完全省略菜单。

**靠变量：`for {_menu}`。**

```skript
set {_menu} to the menu with id "main"
map key "A" to icon stone for {_menu}
override slot 2 in page 1 to diamond for {_menu}
set {_slots::*} to the slots of key "A" in page 1 of {_menu}
set {_key} to the key of slot 1 in page 1 of {_menu}
destroy the menu {_menu}
```

`menu` 这个词在本来就可选的组里**现在是可选的**，所以 `for {_menu}` 和 `for menu {_menu}` 都能解析。
两条都对，选一条读起来顺的写就行。

**靠 id：`with id "main"`。**

```skript
set {_menu} to the menu with id "main"
map key "A" to icon stone for menu with id "main"
destroy the menu with id "main"
```

`menu with id "main"` 是一个**表达式**，求值得到那个菜单；它可以用在任何需要菜单的地方。没有这个
id 的菜单时它什么也不返回。

`destroy` 特意有两个 pattern，因为一个已经吃掉了 `menu` 这个词的 pattern 没法让菜单表达式从
`with id` 开始 —— 没有第二条的话，你就得把 `menu` 写两遍。

## 一个容易踩的坑：属性表达式里不要再写 `menu`

```skript
set {_id} to the id of menu {_menu}     # 错：被读成"按 id 查找菜单"
set {_id} to the id of {_menu}          # 对
```

`menu {_menu}` 会被解析成"用 `{_menu}` 当 id 去找菜单"，而不是"名为 `{_menu}` 的菜单"。上面的
[创建菜单](menus.zh-CN.md)里所有 `the … of {_menu}` 的写法都遵守这一点：

```skript
set {_title} to the default title of {_menu}
set the default title of {_menu} to "&a我的菜单"
set {_page} to the default page of {_menu}
set {_delay} to the minimum click delay of {_menu}
set {_mode} to the mode of {_menu}
set {_type} to the inventory type of {_menu}
set {_pages} to the page number of {_menu}
set {_layout::*} to the default layout of menu {_menu}   # 这条是例外，见下
```

`default layout` 是唯一一个把 `(menu|gui)` 写进自己 pattern 的属性，所以
`the default layout of menu {_menu}` 和 `the default layout of {_menu}` 都可以。

## 没有 id 的菜单

`create menu` 的 `with id` 是可选的。没有 id 的菜单**照样能用**，只要你能拿到那个值：
`build a menu {_menu}:` 会把它存进变量，`create menu` 的段落体里也能直接引用。

代价是两条：

- 它不会出现在 `all menus` / `all menu ids` 里（这两个只列有 id 的菜单）。
- 它不能用 `menu with id "…"` 找回来。

`destroy the menu {_menu}` 对两种情况都有效。

## 按 id 找菜单的细节

```skript
set {_menu} to the menu with id "main"
if {_menu} is not set:
    send "菜单 'main' 不存在。" to player
```

- `menu with id` 的 id 部分是一个**字符串表达式**，所以 `menu with id {_id}` 也行。
- 找不到时返回"没有值"，在 Skript 里判断一个没有值的表达式要用变量中转再 `is not set`
  （`is not null` 不是 Skript 的条件写法）。
- 用同一个 id 再 `create menu` 会把旧的菜单**销毁**，所以 `on load` 里的重建是安全的：
  旧的那份会被关掉，玩家不会卡在一个已经消失的窗口里。

按钮（`define button` / `map key … to button "id"`）也是按 id 索引的，用的是另一套名字空间：
`all buttons` 列出所有按钮 id。按钮和菜单互不影响。
