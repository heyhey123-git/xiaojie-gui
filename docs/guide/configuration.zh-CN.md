# 配置

[English](configuration.md) | 简体中文

插件没有任何命令、也没有权限节点。唯一可以配置的是 `plugins/xiaojie-gui/config.yml`，
首次启动时自动生成，内容只有两个键：

```yaml
enable-async-check: true

# Print the banner in full colour. The colours are written as escape sequences by the plugin itself, so
# Paper's own idea of what the console supports does not decide it. Set this to false on a console that
# would print the escapes as text; the banner is then printed without colour.
force-truecolor: true
```

| 键 | 默认 | 作用 |
|---|---|---|
| `enable-async-check` | `true` | 检查菜单操作是否在主线程上执行，不是的话报一行可读的错误而不是让服务端崩 |
| `force-truecolor` | `true` | 启动横幅是否用真彩色转义序列打印 |

## `enable-async-check`

打开时，下面这些操作如果在**异步线程**里被调用，会往控制台写一行说明（并且**放弃这次操作**），
而不是带着不合法的状态继续：

- `open menu …` / `show menu …`
- `close the menu for …` / `close the menu session …`
- `turn to page …`
- `update title of the menu session …`
- `set icon in slot … of %menusession%`

报出来的文字长这样（会带上当前语句）：

```
Menu can only be opened from the main server thread, but got called from an asynchronous thread: ...
```

**建议保持 `true`**。Skript 里能造成异步调用的地方不多，但都很隐蔽（异步 `execute` 的
`run task later`、别的插件的异步回调、`on … async` 的自定义事件）。改成 `false` 只是把检查关掉，
并不会让异步调用变合法。

## `force-truecolor`

插件自己把 ANSI 真彩色转义序列写进横幅，而不是交给 Paper 决定 —— Paper 会对控制台能不能显示颜色
做判断，判定为"不能"时横幅就变成一片灰。

**如果控制台把转义序列原样打出来**（例如某些 Windows 老终端、把日志重定向到文件再打开），把它设为
`false`：横幅会以无颜色版本打印，其它一切不变。

## 这些键以前是坏的

`enable-async-check` 和 `force-truecolor` 过去读的是 Spigot 自己的配置文件，所以插件 `config.yml` 里
写的值**完全没用**。2.0.0 起读的是 `plugins/xiaojie-gui/config.yml`。**键名没有变**，所以如果你的
服务器上一直写着这两个键，升级后它们**才开始生效** —— 如果你的配置里写过 `force-truecolor: false`
却一直看到彩色横幅，这就是原因。

改完要重启服务端：这两个值在插件启用后第一次用到时读一次，之后不会再读。
