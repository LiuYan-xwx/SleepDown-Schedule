# SleepDown 项目结构

本文档定义 Android 主工程的目录边界与渐进式重构规则。结构调整不得改变业务行为、持久化协议、视觉参数、动画时序或 Android 组件语义。

职责划分、拆分条件和验收步骤统一遵循 [代码组织约定](CODE_ORGANIZATION.md)。可选材质通过公共控件协议注入，详见 [ColorOS 材质适配](SLEEPDOWN_COLOROS_FRAMEWORK.md)。

## 仓库区域

- `app/`：Android 应用主模块。
- `benchmark/`：Macrobenchmark 与 Baseline Profile，不承载业务实现。
- `coloros-wakeup-proxy/`：厂商课程读取兼容组件，独立包名和版本。
- `third-party/kyant-backdrop/`：本地 Backdrop 源码模块。
- `sleepdown-site/`：独立静态官网，不参与 Android Gradle 构建。
- `docs/`：架构、迁移、性能与交接记录。
- `patches/`：第三方依赖补丁。
- `release-notes/`：面向用户的版本说明。
- `SleepDown-Server/`：本地私有后端，始终保持 Git 忽略，不属于公开 Android 工程。

## Android 包边界

`com.xiaomanjun.sleepdownschedule` 根包只保留稳定 Android 入口、Application 和必要兼容门面。实现按以下方向组织：

```text
app -> feature -> domain/model
 |       |          ^
 |       +-------> data
 +---------------> core
feature/ui ------> glass
app -------------> transition
```

- `app/`：应用装配、导航与顶层状态协调。
- `model/`：跨功能共享的当前业务/持久化模型；重构阶段不额外复制一套 domain model。
- `data/`：Room、数据库修复、迁移和 Repository。
- `domain/`：无 Android UI 副作用的课表规则与计算。
- `core/`：通用 UI、远程配置、应用身份、壁纸与性能基础设施。
- `platform/coloros/`：可选凝光材质的系统能力探测、COUI 适配与资源释放；不依赖 `app` 或 `feature`。页面通过 `core/ui` 的材质协议调用，由 `app` 装配。
- `feature/`：按用户功能纵向组织的页面、状态和工作流。
- `feature/experimental/`：当前普通版中按渠道和设备门控的厂商通知实验入口；专属 Shizuku/root 桥接分别在 `app/src/github/` 和 `app/src/store/`。
- `glass/`：液态玻璃统一框架，保持既有采样域和材质边界。
- `transition/`：跨 Activity 转场统一框架；Oplus 暂缓代码不得在结构重构中修改行为。
- `com.kyant.backdrop.catalog/`：SleepDown 修改过的第三方组件，保持原包名和许可说明。

## 文件规则

1. 一个 Manifest Activity、Service、Receiver 或 Widget Provider 独占一个文件。
2. 文件名对应主要声明；不再新增 `Data.kt`、`Logic.kt`、`Ui.kt` 这类跨职责聚合文件。
3. 页面根 Composable、可复用组件、状态持有者和纯计算分别存放。
4. 大文件按职责与当前修改需要评估拆分，不按固定行数机械切割，也不为局部修复强制拆分稳定文件。
5. `core`、`data` 和 `domain` 不依赖具体 `feature` 页面。
6. 功能之间通过共享模型、接口或 `app` 层协调，不直接读取另一功能的私有 UI 状态。
7. 测试包镜像生产包；迁移文件移动时同步移动对应测试。
8. Android 资源继续遵循系统资源目录，新增资源使用功能前缀，既有资源不做无意义批量重命名。

## 安全迁移规则

- 拆文件与改逻辑分批进行；机械移动批次不顺手重写实现。
- Manifest 组件 FQCN、`TransitionRouteCatalog` 和 Intent wire id 必须保持一致，或由稳定根包门面兼容。
- Room schema version、表名、字段、迁移顺序和 SQL 在结构批次中不得改变。
- `BackupFormatV1`、SleepDown 口令、ICS、Widget、通知和 AI 历史兼容格式不得改变。
- Home、周视图、Morph 与玻璃代码拆分时保持函数体、Modifier 顺序、几何和时间参数不变。
- 每批变更使用独立回退点，并执行与风险相称的最小充分验证。

## 当前落地结构（2026-10-13 核对）

```text
com/xiaomanjun/sleepdownschedule/
├── *.kt                              # 稳定 Android 入口、Application 与兼容门面
├── app/{config,startup,state,ui}/
├── model/
├── data/local/
├── data/repository/
├── domain/{agent,course,schedule}/
├── core/
│   ├── {analytics,identity,performance,remoteconfig,wallpaper,web}/
│   └── ui/{chrome,controls,designsystem,interaction,material,scaffold,settings,text}/
├── platform/coloros/
├── feature/
│   ├── home/{day,week,overlay,personalization}/
│   ├── course/{editor,management}/
│   ├── schedule/{manager,picker}/
│   ├── importing/{history,progress}/
│   ├── agent/background/
│   ├── experimental/ 与 coloros/
│   ├── backup/
│   ├── reminder/
│   ├── settings/
│   ├── update/
│   └── widget/providers/
├── glass/ui/
└── transition/legacy/
```

原跨职责聚合文件已完成以下实质拆分：

- `Data.kt` 拆为共享模型、Converters、DAO、Room migrations、Repository 和纯配置变换；`AppDatabase` 的稳定 FQCN 保留。
- `ScheduleLogic.kt` 拆为日历计算、导入编解码、提醒与 Android 入口组件。
- 课程管理、课表管理、首页日/周视图、Home overlay、设置通用组件、AI 历史/进度、Widget provider 和 Agent 后台服务均进入对应功能包。
- 二级页内容节奏、Dialog、Alert、Picker、输入胶囊、操作按钮和 QuickSheet 的稳定壳已收口到 `core/ui/designsystem/`；规范按设置型、内容型和沉浸型保留可组合差异，详见 `SLEEPDOWN_DESIGN_SYSTEM.md`。
- Day Agent 的稳定提示、工具决策和最终写入协议已分段，已读快照工具会在同一回合动态收窄；运行规则见 `DAY_AGENT_RUNTIME.md`。
- Manifest 中已有 Activity、Service、Receiver 和 Widget Provider 的 FQCN 由根包薄入口保持不变；数据库、备份和 Intent 协议不需要迁移。

## 稳定入口与实现映射

| 根包入口 | 实现位置 |
| --- | --- |
| `SettingsDetailActivity`、`EduSchoolSelectActivity`、`EduImportActivity` | `app.ui` 中对应 `*Host` |
| `ScheduleManagerActivity` | `feature.schedule.manager.ScheduleManagerActivityHost` |
| AI 历史、详情与进度 Activity | `feature.importing.history` / `feature.importing.progress` 中对应 `*Host` |
| 四个扩展 Widget Provider | `feature.widget.providers` 中对应 `*Host` |
| `DayAgentForegroundService` | `feature.agent.background.DayAgentForegroundServiceHost` |
| Oplus 课程管理入口 | 根包独立薄文件，继承既有课程管理 Activity |

根包中的 `ScheduleDataCompat.kt`、`ScheduleCalendarCompat.kt`、`PeriodSchemesCompat.kt` 和 `ScheduleLabelsCompat.kt` 是有意保留的源码兼容门面。它们让结构迁移保持机械、可回退，也避免一次性改写全部调用点。

## 有意保留的聚合文件

文件长度用于触发职责审查，不直接决定拆分。以下边界继续保持内聚：

| 文件组 | 保持聚合的原因 |
| --- | --- |
| `app/ui/ScheduleAppUi.kt` | 应用级 Compose 装配、首页导航及多个叠层之间的交接。Activity host、设置页面、个性化面板、公共控件与浏览器能力已拆出；后续调整需围绕具体会话所有权。 |
| `feature/home/week/WeekEditOverlayController.kt`、`feature/home/overlay` | 一个编辑或 Morph 会话共用进入、运动、保存和退出状态，保持这些状态及私有辅助函数在一起。周视图页面、卡片文字、卡片绘制与布局计算已分开。 |
| `feature/settings`、`feature/schedule/{manager,picker}` | 页面与共享设置控件已分开；课表编辑的保存交接与弹层会话保留在功能内。 |
| `feature/importing`、`feature/agent/DayAgentConversationDialog.kt` | 完整导入会话或对话窗口生命周期保持内聚；助手卡片、呈现状态、几何、输入栏、消息内容与共享执行规则分别归位。 |
| `feature/widget/providers/WeekScheduleWidgetRenderer.kt` | 同一 RemoteViews 输出的像素布局与位图渲染保持在一起；Provider 入口、日组件和周组件 renderer 已分开。 |
| `transition/legacy`、`transition/OplusSeamlessBackend.kt` | 保留既有 renderer 和暂缓的 Oplus 调查边界；结构整理不改动动画所有权或交接时序。 |
| `data/local/AppDatabaseMigrations.kt`、`data/repository/ScheduleRepository.kt` | 前者按 Room 版本形成连续迁移账本，后者维持单一数据入口；拆分前需先定义稳定接口。 |

在包依赖稳定且不存在反向引用后，再评估拆分 `:core:*` 和 `:feature:*` Gradle 模块。目录整理本身不以增加模块数量为目标。
