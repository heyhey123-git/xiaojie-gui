# 在语法里指代菜单

[English](naming-menus.md) | 简体中文

脚本中有多个菜单时，需要明确每条语句操作的是哪一个。可以使用当前上下文、变量或 id 来引用菜单。

## 三种方式

**在菜单段落里：不写。**

```skript
create a phantom menu with chest inventory titled "菜单" with layout "AAAAA" with id "main":
    override slot 4 in page 1 to diamond named "特殊物品" for menu with id "main"
```

在 `create menu` / `build a menu` / `edit menu` 段落中，“当前菜单”就是正在创建或编辑的菜单。
支持上下文的语法（如 `insert page …`）可以省略菜单参数；上例则显式使用 id 引用。

**靠变量：`for {_menu}`。**

```skript
set {_menu} to the menu with id "main"
map key "A" to icon stone for {_menu}
override slot 2 in page 1 to diamond for {_menu}
set {_slots::*} to the slots of key "A" in page 1 of {_menu}
set {_key} to the key of slot 1 in page 1 of {_menu}
destroy the menu {_menu}
```

这里的 `menu` 可以省略，因此 `for {_menu}` 和 `for menu {_menu}` 都有效，选择习惯的写法即可。

**靠 id：`with id "main"`。**

```skript
set {_menu} to the menu with id "main"
map key "A" to icon stone for menu with id "main"
destroy the menu with id "main"
```

`menu with id "main"` 是一个返回菜单的**表达式**，可以用在需要菜单值的位置；找不到对应 id 时没有值。

`destroy` 提供两套匹配规则，分别支持变量与按 id 查找的写法，避免在 `destroy the menu with id "main"` 中重复写 `menu`。

## 一个容易踩的坑：属性表达式里不要再写 `menu`

```skript
set {_id} to the id of menu {_menu}     # 错：被读成"按 id 查找菜单"
set {_id} to the id of {_menu}          # 对
```

`menu {_menu}` 会被解析为“将 `{_menu}` 的值当作 id 查找菜单”，而不是直接引用变量中的菜单对象。
读取或修改属性时，通常直接写 `the … of {_menu}`，如[创建菜单](menus.zh-CN.md)中的用法：

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

`default layout` 的语法定义包含 `[(menu|gui)]`，因此 `the default layout of menu {_menu}` 和
`the default layout of {_menu}` 都可以。

## 没有 id 的菜单

`create menu` 的 `with id` 是可选的。没有 id 的菜单**照样能用**，只要你能拿到那个值：
`build a menu {_menu}:` 会把它存进变量，`create menu` 的段落体里也能直接引用。

不过，它有两项限制：

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
- 用同一个 id 再次执行 `create menu` 会**销毁旧菜单**，并关闭它的窗口。因此在 `on load` 中重建菜单时，不会留下旧窗口。

按钮（`define button` / `map key … to button "id"`）也是按 id 索引的，用的是另一套名字空间：
`all buttons` 列出所有按钮 id。按钮和菜单互不影响。
