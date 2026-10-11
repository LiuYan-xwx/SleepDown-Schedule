package com.xiaomanjun.sleepdownschedule.feature.settings

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kyant.backdrop.Backdrop
import com.xiaomanjun.sleepdownschedule.R
import com.xiaomanjun.sleepdownschedule.ScheduleConfigEntity
import com.xiaomanjun.sleepdownschedule.core.ui.designsystem.DialogLiquidButton
import com.xiaomanjun.sleepdownschedule.core.ui.settings.SleepDownLiquidDropdownPreference
import com.xiaomanjun.sleepdownschedule.feature.importing.ChatGptUsageStatus
import com.xiaomanjun.sleepdownschedule.feature.importing.chatgpt.ChatGptAuthException
import com.xiaomanjun.sleepdownschedule.feature.importing.chatgpt.ChatGptAuthManager
import com.xiaomanjun.sleepdownschedule.feature.importing.chatgpt.ChatGptBrowser
import com.xiaomanjun.sleepdownschedule.feature.importing.chatgpt.ChatGptModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.util.Date

@Composable
internal fun ChatGptSettingsPanel(
    config: ScheduleConfigEntity,
    backdrop: Backdrop?,
    selectedModel: String,
    onBeforeLogin: () -> Unit,
    onModelsChanged: (List<ChatGptModel>, ChatGptModel) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth by ChatGptAuthManager.state.collectAsStateWithLifecycle()
    val usage by ChatGptUsageStatus.state.collectAsStateWithLifecycle()
    val account = auth.account
    val models = auth.models
    val signedIn = account?.isSignedIn == true
    val planEnabled = signedIn && account?.planUsageEnabled == true
    var signingOut by remember { mutableStateOf(false) }
    var actionMessage by remember(account?.id) { mutableStateOf<String?>(null) }
    val latestSelectedModel by rememberUpdatedState(selectedModel)
    val latestModelsChanged by rememberUpdatedState(onModelsChanged)

    suspend fun refreshModels() {
        actionMessage = null
        try {
            val refreshed = ChatGptAuthManager.models(context)
            if (refreshed.isEmpty()) actionMessage = "账号尚未提供可用模型，请检查 ChatGPT 中的授权。"
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: ChatGptAuthException) {
            actionMessage = error.message
        } catch (_: Exception) {
            actionMessage = "模型读取失败，请检查网络后重试。"
        }
    }

    LaunchedEffect(Unit) { ChatGptAuthManager.initialize(context) }
    LaunchedEffect(account?.id, signedIn, planEnabled, auth.isSigningIn) {
        if (planEnabled && !auth.isSigningIn) refreshModels()
    }
    LaunchedEffect(account?.id, models, selectedModel) {
        if (models.isNotEmpty()) {
            val selected = models.firstOrNull { it.slug == latestSelectedModel } ?: models.first()
            latestModelsChanged(models, selected)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        GlassPreferenceSection("ChatGPT 账号") {
            SettingsGroup(backdrop = backdrop, config = config, modifier = Modifier.fillMaxWidth()) {
                SettingsInfoRow(
                    title = if (signedIn) account.email ?: account.displayName ?: "ChatGPT" else "使用你的 ChatGPT 套餐",
                    body = when {
                        auth.isSigningIn -> auth.loginStage.label
                        planEnabled -> "已授权用于 AI 对话、AI 助理和课表识别。"
                        signedIn -> "账号已登录，请授权使用 ChatGPT 套餐后继续。"
                        else -> "登录后使用套餐内的 AI 用量，无需填写 API Key。"
                    },
                    badgeText = when {
                        auth.isSigningIn -> "登录中"
                        planEnabled -> "已连接"
                        signedIn -> "待授权"
                        else -> null
                    }
                )
                Column(
                    Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (auth.isSigningIn) {
                        DialogLiquidButton(label = "取消登录", backdrop = backdrop,
                            onClick = { ChatGptAuthManager.cancelLogin() })
                    } else if (!planEnabled) {
                        DialogLiquidButton(
                            label = "使用 ChatGPT 继续", backdrop = backdrop,
                            modifier = Modifier.fillMaxWidth(), height = 48.dp,
                            highContrast = true, enabled = !signingOut,
                            onClick = {
                                actionMessage = null
                                onBeforeLogin()
                                ChatGptAuthManager.startLogin(context, requestConsent = signedIn)
                            }
                        )
                        Text("将在浏览器安全窗口中完成授权。登录信息仅加密保存在此设备。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    (actionMessage ?: auth.errorMessage)?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
                if (signedIn && !auth.isSigningIn) {
                    SettingsDivider()
                    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
                        DialogLiquidButton(
                            label = if (signingOut) "正在退出…" else "退出登录",
                            backdrop = backdrop, enabled = !signingOut,
                            onClick = {
                                signingOut = true
                                scope.launch {
                                    try {
                                        actionMessage = ChatGptAuthManager.signOut(context).message
                                    } catch (cancelled: CancellationException) { throw cancelled }
                                    catch (_: Exception) { actionMessage = "退出未完成，请重试。" }
                                    finally { signingOut = false }
                                }
                            }
                        )
                    }
                }
            }
        }
        if (planEnabled) {
            GlassPreferenceSection("模型") {
                SettingsGroup(backdrop = backdrop, config = config, modifier = Modifier.fillMaxWidth()) {
                    if (models.isNotEmpty()) {
                        SleepDownLiquidDropdownPreference(
                            items = models.map { it.displayName },
                            selectedIndex = models.indexOfFirst { it.slug == selectedModel }.coerceAtLeast(0),
                            title = "当前模型", backdrop = backdrop, config = config,
                            modifier = Modifier.fillMaxWidth(),
                            insideMargin = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                            maxHeight = 318.dp, onExpandedChange = {},
                            onSelectedIndexChange = { index -> models.getOrNull(index)?.let { onModelsChanged(models, it) } }
                        )
                    } else {
                        SettingsInfoRow("可用模型", if (auth.isLoadingModels) "正在读取…" else "暂未获取到模型，点击下方重试。")
                    }
                    SettingsDivider()
                    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
                        DialogLiquidButton(
                            label = if (auth.isLoadingModels) "正在刷新…" else "刷新模型",
                            backdrop = backdrop, iconRes = R.drawable.ic_refresh,
                            enabled = !auth.isLoadingModels && !auth.isSigningIn && !signingOut,
                            onClick = { scope.launch { refreshModels() } }
                        )
                    }
                }
            }
        }
        GlassPreferenceSection("套餐用量") {
            SettingsGroup(backdrop = backdrop, config = config, modifier = Modifier.fillMaxWidth()) {
                SettingsInfoRow(
                    title = if (planEnabled) usage.status else "用量由 ChatGPT 管理",
                    body = buildString {
                        if (planEnabled) {
                            usage.checkedAtMillis?.let {
                                append(DateFormat.getTimeFormat(context).format(Date(it))).append(" · ")
                            }
                            usage.diagnostic?.let { append(it).append("\n") }
                        }
                        append("套餐余额和使用限制请在 ChatGPT 官方用量页查看。此处显示最近一次调用状态，无法直接查询套餐剩余额度。")
                    },
                    badgeText = if (planEnabled && usage.limitReached) "用量受限" else null
                )
                SettingsDivider()
                SettingsActionRow(
                    title = "管理用量", subtitle = "打开 ChatGPT 用量设置", buttonText = "查看",
                    iconRes = R.drawable.ic_ai_import, backdrop = backdrop,
                    onClick = {
                        try { ChatGptBrowser.open(context, "https://chatgpt.com/settings/usage") }
                        catch (error: ChatGptAuthException) { actionMessage = error.message }
                    }
                )
            }
        }
    }
}
