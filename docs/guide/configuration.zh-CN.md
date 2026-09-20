# 配置

[English](configuration.md) | 简体中文

插件没有任何命令、没有权限节点，也没有任何选项，因此**不生成配置文件**：`plugins/xiaojie-gui/` 里
只有脚本自己写的东西。

## 主线程检查

菜单操作会改动真实的物品栏，所以必须在服务端主线程上执行。下面这些操作如果在**异步线程**里被调用，
会往控制台写一行说明（并且**放弃这次操作**），而不是带着不合法的状态继续：

- `open menu …` / `show menu …`
- `close the menu for …` / `close the menu session …`
- `turn to page …`
- `update title of the menu session …`
- `set icon in slot … of %menusession%`

报出来的文字长这样（会带上当前语句）：

```
Menu can only be opened from the main server thread, but got called from an asynchronous thread: ...
```

这个检查关不掉：异步调用在这里永远不合法，而它又总是很隐蔽（异步 `execute` 的
`run task later`、别的插件的异步回调、`on … async` 的自定义事件）。

## 横幅

横幅自己带着真彩色转义序列，除非 Paper 用的那套控制台判断 —— `net.kyori.ansi.colorLevel` 系统属性
—— 说这个控制台会把序列原样打出来；那时横幅写成纯文本。日志文件里永远是纯文本：Paper 的文件
appender 会在写进 `logs/latest.log` 时丢掉这些转义序列。
