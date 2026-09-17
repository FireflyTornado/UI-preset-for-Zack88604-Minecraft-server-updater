# FireflyTornado JavaFX UI Preset

[English](README.md) | 简体中文

这是一个供 Minecraft 自动更新 agent 加载的 V2 `java-helper` GUI preset。
JavaFX 窗口运行在独立 helper JVM 中，不会让 Minecraft JVM 加载 `javafx.*`。

本项目基于 Zack88604 的
[minecraft-server-updater-for656](https://github.com/Zack88604/minecraft-server-updater-for656)
项目打造，需要依赖这个项目加载与运行。

## 功能

- 独立 JavaFX helper JVM，避免污染 Minecraft 进程。
- 保留进度、速度、日志、状态插图、动画、错误帮助和关闭确认界面。
- 支持关闭、跳过更新、使用最后可信版本等上游 GUI 协议动作。
- 内嵌固定版本并带 SHA-256 校验的 OpenJFX runtime。
- helper 无法启动或异常退出时，由上游 main 的 V2 adapter 接管 Swing 回退。

## 构建

要求 JDK 17 或更高版本。Windows：

```bat
build.bat
```

构建所需的 updater API 契约保存在本项目的 `provided-api/`，仅用于编译，不会打入
preset JAR。

OpenJFX 依赖缓存在本项目的 `lib/javafx/`；缺失时会自动从 Maven Central 下载。
固定的 OpenJFX 21.0.4 Windows 依赖还会与脚本内置的 SHA-256 比较，校验通过后才
参与编译和打包。

最终发布产物保留在：

```text
dist/fireflytornado-javafx-preset-1.1.1-win.jar
```

## 安装

把产物复制到游戏目录：

```text
<game-dir>/.mc-update/gui-presets/
```

删除 `<game-dir>/.mc-update/gui-selection.properties` 可让上游 main 在下次启动时重新显示
GUI 选择器。首次选择外部 preset 时，main 会显示外部代码风险确认。

## 平台与运行时

- 当前发布目标为 Windows，对应 OpenJFX `win` classifier。
- JavaFX 版本为 21.0.4，helper 最低需要 Java 17。
- 内嵌 `javafx-base`、`javafx-graphics` 和 `javafx-controls`。
- updater API 是 provided 依赖，不会重复打入 preset。
- Linux 和 macOS runtime 尚未包含在当前产物中。

## 许可证与署名

本项目代码以及从上游项目派生的内容按照 MIT License 发布，详见 [LICENSE](LICENSE)
和 [NOTICE](NOTICE)。上游版权声明会随源代码和构建产物保留。

最终 preset JAR 内嵌 OpenJFX 二进制。OpenJFX 使用 GPLv2 with Classpath Exception；
相关许可文本会打包到 `META-INF/licenses/openjfx/`。依赖、源码获取地址和商标声明见
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。
