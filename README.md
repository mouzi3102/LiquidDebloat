# 小卸载 · LiquidDebloat

一个用**液态白玻璃视觉**做的 Android 预装应用卸载器。要求 root（Magisk / KernelSU），纯本地运行，不联网、不上传任何数据。

> 默认是「安全卸载」：只对当前用户卸载，应用从桌面消失，系统分区里的文件仍保留，随时可恢复。
> 想要更彻底的行为，可以在设置里打开「彻底删除模式」——但那一步是不可逆的。

---

## 功能

- **一个总列表**：默认列出设备上全部应用（用户 + 系统），顶部标签可切到 用户 / 系统 / 已卸载
- **每行一个垃圾桶按钮**：点一下直接卸载，不需要先勾选、不需要进二级页面
- **已卸载页点一下就恢复**，且列表不会「自己消失」：本地保留卸载历史（`SharedPreferences`），即使 ROM 的 `pm -u` 表被重建，卸过的应用仍会列出
- **搜索**：按应用名或包名实时过滤
- **下拉刷新**：在列表任意位置下拉即可重新扫描包表
- **一键打开备份目录**：设置页点备份路径直接跳文件管理器
- **实时状态**：卸载完成当场刷新该行状态并给出结果提示，可下拉刷新重扫
- **卸载前自动备份**：把原 APK 复制到 `/sdcard/LiquidDebloat/backup/<包名>/base.apk`，并写一份 `meta.txt`
- **恢复**：`已卸载` 页点「恢复」走 `cmd package install-existing`，失败时回退到 `pm install -r` 备份
- **受保护白名单**：系统界面、设置、电话、权限控制器等关键包硬编码拦截，勾了也不会被卸
- **两种卸载模式**：`pm uninstall --user 0`（安全，默认）/ 彻底删除分区内 APK 目录（高级，需备份）
- **无 root 也能看列表**：没有 `su` 时退化为 `PackageManager` 只读列表，卸载时会提示需要 root
- **完整包可见性**：声明 `QUERY_ALL_PACKAGES`，Android 11+ 才能读到全部已安装应用的名称、图标和 APK 路径

## 界面

**液态白**：近白冷调渐变底 + Canvas 绘制的柔和径向光晕（天蓝 / 薄荷 / 蜜桃 / 白）+ 白色磨砂面板 + 白色高光描边 + 软阴影投影。文字用深墨灰保证可读性。背景不用 `blur`，靠 `Brush.radialGradient` 实现，所以在 Android 9–15 上也不会出现色块边界。

> 设计参考：[LiquidGlass-Cpp](https://github.com/SilentEaves/LiquidGlass-Cpp) 与它的上游 [AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass)（作者 Kyant）。

## 卸载语义（重要）

| 模式 | 执行命令 | 结果 | 可逆性 |
| --- | --- | --- | --- |
| 安全（默认） | `pm uninstall --user 0 <pkg>` | 当前用户不可见，分区文件保留 | 可恢复：`cmd package install-existing <pkg>` |
| 彻底删除（高级） | 在安全卸载基础上，`mount -o rw,remount` 后 `rm -rf` APK 目录 | 文件真正被删 | **不可逆**，只能靠备份 `pm install -r` 或线刷 |

彻底删除会破坏分区完整性，可能导致 **OTA 失败或无法开机**。开着备份再动。

## 手动恢复（应用卸完/卸崩了）

```bash
# 只是 user 0 卸载
adb shell su -c "cmd package install-existing com.example.app"

# 从备份装回
adb shell su -c "pm install -r /sdcard/LiquidDebloat/backup/com.example.app/base.apk"
```

## 构建

要求：JDK 21+、Android SDK（compileSdk 37、build-tools 36+）。

```bash
echo "sdk.dir=/path/to/android-sdk" > local.properties
./gradlew :app:assembleDebug      # 调试包
./gradlew :app:assembleRelease    # 需要签名配置
```

发布签名可选，在根目录放 `signing.properties`：

```properties
store.file=release.jks
store.password=****
key.alias=liquiddebloat
key.password=****
```

没有该文件时 `assembleRelease` 产出未签名包，CI 里只跑 `assembleDebug`。

## 目录结构

```
app/src/main/kotlin/com/liquiddebloat/
├── MainActivity.kt          # 状态机、布局、确认/结果对话框
├── model/AppEntry.kt        # 一行应用数据
├── data/
│   ├── RootShell.kt         # 唯一的 su 出口，所有系统操作都经这里
│   ├── DebloatRepository.kt # 列表加载、卸载、备份、恢复、彻底删除
│   └── SettingsStore.kt     # 备份开关、彻底删除开关
└── ui/
    ├── Glass.kt             # 玻璃色板、背景、面板、按钮、开关、标签
    ├── AppListScreen.kt     # 搜索框、分段标签、列表行
    └── SettingsScreen.kt    # 设置页
```

## 已知限制 / Roadmap

- 只支持 root，暂不支持 Shizuku（无 root 场景）
- 「彻底删除」依赖分区可 remount rw，部分新机型 system 分区是 EROFS，删不掉会输出「删除未生效」
- 备份目前只存 base.apk，不处理 split APK（App Bundle 应用恢复可能失败）
- 计划：按体积/安装时间排序、冻结（`pm disable`）模式、备份列表管理页

## 免责声明

卸载系统应用有风险。作者不对任何因使用本工具造成的设备损坏、数据丢失负责。请自行确认你有能力用 Recovery 或线刷恢复。

## License

Apache License 2.0，见 [LICENSE](LICENSE)。
