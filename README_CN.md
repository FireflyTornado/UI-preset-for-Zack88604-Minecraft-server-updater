# FireflyTornado JavaFX UI Preset

[English](README.md) | 简体中文

当前版本：**1.2.4**。

这是一个供 Minecraft 自动更新 agent 加载的 V2 `java-helper` GUI preset。
JavaFX 窗口运行在独立 helper JVM 中，不会让 Minecraft JVM 加载 `javafx.*`。

本项目基于 Zack88604 的
[minecraft-server-updater-for656](https://github.com/Zack88604/minecraft-server-updater-for656)
项目打造，需要依赖这个项目加载与运行。

## 功能

- 独立 JavaFX helper JVM，避免污染 Minecraft 进程。
- 保留进度、速度、日志、状态插图、动画、错误帮助和关闭确认界面。
- 退出确认、失败恢复选择、维护详情和错误帮助弹窗统一相对主窗口居中；靠近屏幕边缘时调整位置，在所在显示器可用区域内保留 24px 边距。弹窗可超出主窗口范围，各自保留适合内容的尺寸。
- 上述弹窗右上角的“×”只关闭当前弹窗，不选择退出、重试或可信版本启动。关闭退出确认时解除确认期间的暂停，让原更新流程继续。
- 配合新版核心在上方显示网络等待、重试和验证状态；收到新字节后恢复普通文件计数文案，续传和重下方式保留在日志中。速度栏只显示速度，连续 2 秒没有新字节时显示 `0 KB/s`，即使没有收到新的状态回调也会归零。失败时保留当前文件进度并显示零速度。
- 失败选择框提供“回退到可信版本”“重试更新”和“退出”。重试需要支持 `requestRetryUpdate()` 的新版核心，会重新获取清单并保留已完成的更新及原始回滚备份。
- 自 1.2.3 起，配合新版 me-main 核心支持维护模式：全部更新源维护时在主界面显示简短操作提示，不自动弹窗；状态区右上角显示“查看详情”，点击后显示服务端完整维护消息、“可信版本启动”和“退出”。前者由核心回滚并校验可信缓存后启动；退出或直接关闭主窗口保留已完成的修改，不启动 Minecraft。普通失败时该位置仍显示“获取帮助”。已开始的整轮更新及自动重试继续执行，手动重试重新检查维护。
- 维护主界面采用固定两行提示；维护弹窗按当前屏幕可用高度限制尺寸，长维护消息可滚动查看全部内容，插画和操作按钮保留在滚动区域之外。
- 可信版本按钮在默认且聚焦状态下也保留交互反馈：悬停时蓝色变亮，按下时蓝色变深。维护弹窗和普通失败弹窗均生效。
- 内嵌固定版本并带 SHA-256 校验的 OpenJFX runtime。
- helper 无法启动或异常退出时，由上游 main 的 V2 adapter 接管 Swing 回退。

## 构建

要求 JDK 17 或更高版本。Windows 下默认一次构建四个平台包：

```bat
build.bat
```

也可沿用 `Classifier` 参数，只构建指定平台：

```bat
build.bat -Classifier win
build.bat -Classifier linux
build.bat -Classifier mac
build.bat -Classifier mac-aarch64
```

PowerShell 用户也可执行 `./build.ps1`（全部四个平台）或
`./build.ps1 -Classifier mac-aarch64`。四种包可在同一台构建机器上生成；窗口启动
需要在对应操作系统和 JVM 架构下验证。每次构建只替换同名产物，保留 `dist/` 中
其他平台及版本的 JAR。

构建所需的 updater API 契约保存在本项目的 `provided-api/`，仅用于编译，不会打入
preset JAR。

OpenJFX 依赖缓存在本项目的 `lib/javafx/`；缺失时会自动从 Maven Central 下载。
四个平台的固定 OpenJFX 21.0.4 依赖均会与脚本内置的 SHA-256 比较，校验通过后才
参与编译和打包。每个平台只使用自己的三个运行时 JAR，不会混用缓存中的其他平台依赖。
依赖缓存 `lib/`、临时目录 `build/` 和发布产物 `dist/` 均已被 Git 忽略，无需提交依赖。

最终产物沿用现有的版本号与 classifier 命名规则：

```text
dist/fireflytornado-javafx-preset-1.2.4-win.jar
dist/fireflytornado-javafx-preset-1.2.4-linux.jar
dist/fireflytornado-javafx-preset-1.2.4-mac.jar
dist/fireflytornado-javafx-preset-1.2.4-mac-aarch64.jar
```

## 安装

根据启动 Minecraft 所用 Java 的操作系统和架构，选择对应的一个产物复制到游戏目录：

```text
<game-dir>/.mc-update/gui-presets/
```

删除 `<game-dir>/.mc-update/gui-selection.properties` 可让上游 main 在下次启动时重新显示
GUI 选择器。首次选择外部 preset 时，main 会显示外部代码风险确认。

## 平台与运行时

| 操作系统 | JVM 架构 | OpenJFX classifier / 文件名后缀 |
|----------|----------|-------------------------------|
| Windows | x64（`amd64` / `x86_64`） | `win` |
| Linux | x64（`amd64` / `x86_64`） | `linux` |
| macOS | x64（`amd64` / `x86_64`） | `mac` |
| macOS | ARM64（`aarch64` / `arm64`） | `mac-aarch64` |

- 每个包仅内嵌对应平台的运行时。
- JavaFX 版本为 21.0.4，helper 最低需要 Java 17。
- 内嵌 `javafx-base`、`javafx-graphics` 和 `javafx-controls`。
- updater API 是 provided 依赖，不会重复打入 preset。
- 不包含 Linux ARM64、原生 Windows ARM64 和 32 位运行时。
- 完成分平台打包不代表已在全部平台实测；需在各目标系统及 JVM 上验证 helper，
  并满足 JavaFX 的系统依赖要求。
- `me-main` 的平台识别与自动下载属于后续改动；目前服务端预设描述仍发布单个产物。

## 许可证与署名

本项目代码以及从上游项目派生的内容按照 MIT License 发布，详见 [LICENSE](LICENSE)
和 [NOTICE](NOTICE)。上游版权声明会随源代码和构建产物保留。

最终 preset JAR 内嵌 OpenJFX 二进制。OpenJFX 使用 GPLv2 with Classpath Exception；
相关许可文本会打包到 `META-INF/licenses/openjfx/`。依赖、源码获取地址和商标声明见
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。
