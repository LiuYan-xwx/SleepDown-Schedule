package com.xiaomanjun.sleepdownschedule.core.web

import android.webkit.WebView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.xiaomanjun.sleepdownschedule.feature.importing.shiguang.uninstallShiguangRuntime

internal fun WebView.releaseSleepDownWebView(clearResourceCache: Boolean = true) {
    runCatching { stopLoading() }
    runCatching { clearHistory() }
    // Resource cache can grow into tens of megabytes after repeated school-site imports.
    // Cookies and DOM storage are intentionally retained so login state is not lost.
    if (clearResourceCache) runCatching { clearCache(true) }
    runCatching { uninstallShiguangRuntime() }
    runCatching { destroy() }
}
