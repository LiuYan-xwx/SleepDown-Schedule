# 凝光可选模式与架构整理

## 范围和起点

用户要求将独立 ColorOS 目录的凝光视效作为 ColorOS 17 下的可选模式，保留液态玻璃；梳理项目结构、厂商边界、大文件、无效代码和临时产物。纯结构改动不得改变既有功能与视效，每批大改动独立提交。

- 主线起点：`origin/main` 的 `9662d103`。
- 先完成已有工作：`1150d141` 保存普通版与独立 ColorOS 包的备份互认，36 项定向测试通过。此前的 ChatGPT 与备份导出修复保留在其祖先提交。
- 工作分支：`codex/coloros-visual-architecture`，从上述已提交基线开始。原备份分支保留，可独立回退。
- 凝光来源：`D:/Android studio/sleepdown for ColorOS`，分支 `codex/coloros-menu-rescue`，已提交基线 `af843d3`，另有待整合的工作树改动。迁入前核对实际文件；不改动来源工作树、不迁入其独立 applicationId、ADB 策略、分析材料或临时产物。
- `.codex-remote-attachments/`、`edu-adapters/` 以及其他工作树中的未完成工作需要保留。私有后端、签名和用户数据不在整理范围。

## Google 官方资料与本项目的适用方式

核对时间：2026-10-13。

| 官方资料 | 适用方式 |
| --- | --- |
| [Android 架构建议](https://developer.android.com/topic/architecture/recommendations) | UI 通过状态持有者和数据入口工作，使用单向数据流；按生命周期收集 Flow。保留既有 Manifest 入口和转场协议，不以本轮整理强行替换所有 Activity。 |
| [Compose 状态提升](https://developer.android.com/develop/ui/compose/state-hoisting) | 业务状态由 ViewModel/数据层负责；动画、滚动和临时界面状态放在使用者附近。不能把所有局部状态都塞进一个 ViewModel。 |
| [模块化模式](https://developer.android.com/topic/modularization/patterns) | 按职责和依赖划界，公共设计系统定义渲染协议，厂商实现由装配层接入。先消除反向依赖，再决定是否值得增加 Gradle 模块。 |
| [Compose 性能建议](https://developer.android.com/develop/ui/compose/performance/bestpractices) | 根据实际热点延后状态读取、避免重复计算，保留稳定列表标识。文件拆分本身不证明帧率或启动速度提升。 |

Google 没有适合所有应用的单一“最佳性能架构”。本项目的 CPU、GPU、原生 View 与 Compose 混合绘制、壁纸采样、Morph 和生命周期需要分别验证。代码可维护性结论与实机性能结论分开记录。

## 初始代码核对

- `ScheduleAppUi.kt` 10,328 行：混有首页装配、设置页面、多个 Activity host、个性化编辑、公共控件和 WebView 下载。
- `WeekScheduleUi.kt` 4,163 行，`DayAgentUi.kt` 3,482 行，`HomeAnchoredMorphOverlay.kt` 2,828 行，`HomeScheduleUi.kt` 2,405 行：按状态、纯计算、组件和页面职责逐项评估；不按固定行数切碎同一手势状态机。
- 数据层 `ScheduleRepository` 依赖 `feature.agent` 的操作计划；`core.identity` 依赖备份功能的图标偏好 DTO；公共设置控件从 `app.ui` 获取控件。这些依赖与文档约定方向不一致。
- 凝光来源已提供一组公共原生渲染接口，但首页、课表选择与学校搜索仍直接依赖 ColorOS 实现。可选模式需要统一门控和可卸载的资源归属。
- 来源修改了公共主题色和部分非原生后备路径，不能直接把目录覆盖到主线。默认液态玻璃的色值、几何、手势与保存语义需要逐项保留。

## 交付记录

后续每批记录实际提交、修改边界、检查结果及尚未完成的设备验收。未执行的实机测试不记为通过。

### 可选模式接入

- 默认模式保留液态玻璃；凝光选择仅在 GitHub 渠道、ColorOS 17 及以上并且系统 COUI 能力探测成功时开放。保存偏好与当前有效模式分离，未知偏好或缺失能力均回到液态玻璃。
- 统一提供按钮、滑块、开关、课程卡片、材质表面、菜单和弹窗渲染入口；切换会撤销原生 provider，使 Compose 释放其 AndroidView/窗口资源。
- 修正来源分支影响普通模式的主题色、弹窗操作顺序、快速设置密度/字体和后备按钮额外触觉反馈。原生 QuickSheet 保留退出内容及异步保存边界。
- `compileGithubReleaseKotlin` 通过；19 项模式、Picker/QuickSheet 生命周期、首页切换与转场目录测试通过。`assembleGithubRelease` 含 R8、资源压缩、lintVital 和签名通过（3m52s）；未安装，尚未进行两种模式的实机视觉验收。
- 干净依赖重放发现旧 Miuix 第五份增量补丁无法应用；改为相对官方 `v0.9.3` 的单一完整补丁。重新解包官方基线并应用后，20 个运行时源码文件与本地依赖一致（归一化 LF/CRLF）。不包含依赖目录的 IDE/CI 清理。
