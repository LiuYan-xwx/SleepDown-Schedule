# 本地备份导出零字节调查

## 已确认的证据

- 用户确认问题发生在普通包 `com.xiaomanjun.sleepdownschedule` 的 ChatGPT 修复版，页面提示“备份已保存”。不是并存的独立 ColorOS 包。
- 通过项目 ADB 通道读取文件元数据：两份近期导出的 `.sleepdown` 实际长度均为 0 字节；“0M”不是大小显示取整。未复制或记录备份原始内容。
- 设备现有 `SleepDownBackup` 日志没有保留失败堆栈，因此尚未确认文件提供器未落盘的具体原因。
- 旧流程先打开系统选择器创建目标文件，返回后才生成备份并直接写入 URI。写入和关闭没有抛异常就显示成功，没有回读文件内容；生成失败也可能留下已创建的空文件。

## 修复范围

- `BackupDocumentExport` 先在应用缓存目录生成完整的 v1 文件，再用既有恢复解码器校验；通过后才打开保存位置选择器。
- 使用同一份已校验文件写入目标，关闭输出流后重新打开目标 URI，核对实际字节数及 SHA-256。空文件、截断、内容变化和关闭时失败均不能进入成功提示。
- 生成、压缩、写入及回读在 IO 调度器运行。保存提示显示已验证的文件大小，失败提示包含具体原因。
- 选择器等待期间只保存临时文件名，以支持 Activity 重建。取消或结束后清理本次私有临时文件；失败时尝试删除本次 `CreateDocument` 新建的目标，不触及既有备份。
- 日志增加准备、保存和验证阶段，只增加提供器标识及文件字节数，不记录课程、备份内容或目标文件路径。
- 未更改 Room、BackupFormatV1、恢复确认行为或 WebDAV 传输。

Android 的 `ACTION_CREATE_DOCUMENT` 为新建文件授予读写访问，并不会覆盖现有文件；参见 [Android 文档](https://developer.android.com/training/data-storage/shared/documents-files)。回读使用返回的同一 URI，不依赖可缺失或滞后的大小元数据。

## 验证

- `BackupDocumentExportTest`：8 项通过，包括模拟“丢弃全部写入但不报错”、截断、等长损坏、文件关闭失败、读写打开失败、取消清理与恢复解码。
- `BackupCodecTest`、`BackupExportImportPlanTest`、`SharedPeriodSchemeBackupTest`：51 项通过，保留 v1 格式和共享作息兼容性。
- `compileGithubReleaseKotlin` 通过。
- 真实手机完整导出仍需使用修复包复测，不能把上述模拟测试当作该设备已恢复正常的证据。

本修复单独保留在 `codex/backup-export-empty-20261012`，基于 `main` 的 `9662d103`，不包含学校适配。临时安装候选需要额外保留已安装版本中的 ChatGPT 修复，避免覆盖安装时倒退其他功能。
