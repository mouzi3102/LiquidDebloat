# 设计说明

## 为什么不用 LiquidGlass-Cpp 直接做界面

LiquidGlass-Cpp 是 AndroidLiquidGlass（Kotlin/Compose）的 C++ 移植，跑在 ImGui + OpenGL 的浮窗里。
它的折射/模糊效果最接近原版，但 ImGui 没有原生控件树，做滚动列表、文本输入、无障碍和 IME 都要自己写。
对一个要管理几百个应用、需要搜索和多选列表的工具来说，Compose 是更合适的承载层。

所以本项目取它的**视觉语言**，用 Compose 重新实现：

- 深色纵向渐变底 + 三个高斯模糊色块（`Modifier.blur`）作为玻璃背后的“内容”
- 面板 = 半透明白色纵向渐变 + 1dp 渐变描边（左上更亮）+ 左上角径向高光
- 交互元素统一 100dp 圆角，按压有 spring 缩放回弹
- 不用 RenderEffect，Android 9 也能出效果（低版本模糊退化为纯色叠加）

## 数据流

```
MainActivity（状态机）
  └── DebloatRepository ── RootShell.run("su -c …")
        ├── pm list packages -f -u / -s / (无参)
        ├── PackageManager（label / version / icon）
        ├── pm uninstall --user 0
        ├── cp → /sdcard/LiquidDebloat/backup
        └── cmd package install-existing
```

所有对系统的写操作都集中在 `DebloatRepository`，`RootShell` 是唯一出口，方便审计与后续替换成 libsu。

## 安全策略

1. 受保护包硬编码在白名单里，UI 层直接禁用按钮，仓库层再拦一次
2. 默认模式只做 `--user 0` 卸载（可恢复），彻底删除要显式开开关并二次确认
3. 备份默认开启，对话框里会明确显示当前是否备份
4. 每次操作结束后给出逐条结果，避免“批量操作后不知道哪条失败”
