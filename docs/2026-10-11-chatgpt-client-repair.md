# ChatGPT 登录与输出读取修复

分支：`codex/chatgpt-login-custom-tabs`，基线：`9662d103`。本次为本地测试候选，不发布新的 Beta 标签；两校适配仍在独立分支。

## 用户反馈与处理

- 已登录但模型或 AI 调用失败，错误为“ChatGPT 未返回要求的事件流”。原实现仅凭响应头拒绝正文。现在对实际响应内容分流：SSE 仍要求合法的 `response.completed`；完整 JSON 必须有 `status=completed` 和输出字段。HTML、错误响应、未完成及截断内容不能作为成功结果。
- AI 对话、助理与课表识别共用上述读取规则。请求仍使用官方 `/v1/responses`、`stream=true`、`store=false`；账号退出后拒绝迟到结果。
- 模型读取兼容账号目录的 `models` 和标准目录的 `data`，保留账号目录的可见性过滤与服务端顺序。选择结果立即写入共用配置。
- 登录使用支持 Custom Tab 的浏览器，优先复用 Activity 所在任务，显示授权、兑换、校验阶段。没有可用浏览器时给出明确提示。
- 页面整理为账号、模型和套餐用量三个区域。旧 OpenAI API Key 预设从选择器退役；已有配置与密钥密文一次性迁移为可删除的自定义接口，旧备份仍可恢复。

## 额度信息的边界

已查阅的公开 SIWC 文档未提供套餐剩余额度和重置时间的查询接口，而是要求链接到 ChatGPT 用量页。页面因此不再用“暂无法读取”模拟一个查询结果：展示最近调用状态和服务端返回的限制信息，并通过 Custom Tab 打开官方用量页。调用成功不代表剩余额度充足，HTTP 429 也不等于整个套餐耗尽。

参考官方文档（2026-10-11 核对）：

- [注册与登录](https://developers.openai.com/siwc/token-sharing-open-source/sign-in)
- [账号、会话与用量](https://developers.openai.com/siwc/token-sharing-open-source/profiles-and-sessions)
- [模型与推理](https://developers.openai.com/siwc/token-sharing-open-source/models-and-inference)
- [错误与恢复](https://developers.openai.com/siwc/token-sharing-open-source/errors-and-recovery)
- [预览限制](https://developers.openai.com/siwc/token-sharing-open-source/preview-limitations)

## 验证边界

定向测试覆盖完整 JSON、SSE 增量与完成事件、SSE 元数据、HTML 拦截页、截断流、结构化额度错误、模型目录、旧配置密文及备份迁移、Custom Tab Intent 和已有账号安全逻辑。

- 完整 `assembleGithubRelease` 已通过，保留 R8、资源压缩、lintVital 与正式签名。
- `testGithubDebugUnitTest` 定向运行 `*ChatGpt*`、`*AiProviderPresetsTest`、`*AgentToolsTest`，92 项通过。
- 2026-10-11 22:23 通过项目 5038 通道在 PLJ110 / Android 17 覆盖安装成功；未自动启动，未清除数据。
- 包名 `com.xiaomanjun.sleepdownschedule`，版本 `1.2.7_beta5` / `34`；APK 不含 `edu_adapter_test` 资产。
- 本地交付文件 `tmp/chatgpt-fix-20261011/SleepDown-1.2.7_beta5-chatgpt-fix-release.apk`，SHA-256：`70e800659358de5cd192d87ee84a991e630dc790e38dc093e9f0de08acac82b9`。

未持有测试账号授权，尚未完成真实 ChatGPT 登录、模型调用及官方用量页的账号内验收，不能据单元测试宣称真实服务已恢复。
