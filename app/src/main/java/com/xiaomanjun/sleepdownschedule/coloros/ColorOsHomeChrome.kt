package com.xiaomanjun.sleepdownschedule.coloros

import android.content.Context
import android.graphics.BlendMode
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.xiaomanjun.sleepdownschedule.DockAlignment
import com.xiaomanjun.sleepdownschedule.R
import com.xiaomanjun.sleepdownschedule.app.ui.dockImeCompensationPx
import com.xiaomanjun.sleepdownschedule.core.performance.DockEntranceContainer
import com.xiaomanjun.sleepdownschedule.core.performance.StartupPhase
import com.xiaomanjun.sleepdownschedule.feature.agent.homeCountdownShockwave

/** Weather uses COUI's neutral/transparent button material, independent of its sky colors. */
@Immutable
internal data class ColorOsHomeChromeStyle(
    val dark: Boolean,
    val weatherMaterial: Boolean,
    val foreground: Int?
)

internal val LocalColorOsHomeChromeStyle = compositionLocalOf<ColorOsHomeChromeStyle?> { null }

/** The morph target and expanded menu share one neutral tint. The FAB keeps its accent. */
internal fun colorOsHomeNeutralTint(dark: Boolean) =
    androidx.compose.ui.graphics.Color(if (dark) 0xFF050505 else 0xFFFFFFFF)
        .copy(alpha = if (dark) 0.40f else 0.28f)

internal fun ColorOsNativeUi.Source.weatherButtonBlurParams(
    context: Context, radiusScale: Float = 1f, tintAlphaScale: Float = 1f
): Any = scaledBlurParams(context, "TYPE_CONTENT_TRANSPARENT_BUTTON", radiusScale, tintAlphaScale)

internal fun ColorOsNativeUi.Source.dockBlurParams(context: Context, neutral: Boolean): Any =
    scaledBlurParams(context, if (neutral) "TYPE_CONTENT_TRANSPARENT_BUTTON" else "TYPE_FRAMEWORK_BOTTOM_BAR", 1f)

/** Embed the alert's resource recipe in this window, without a second window-behind sampler. */
internal fun ColorOsNativeUi.Source.dialogBlurParams(context: Context): Any {
    val base = load("com.coui.appcompat.COUIMaterialBlurEffect\$BaseBlurEffectParams")
    val preset = weatherButtonBlurParams(context)
    val dark = context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
        android.content.res.Configuration.UI_MODE_NIGHT_YES
    val theme = if (dark) "dark" else "light"
    fun layer(name: String) = "#" + Integer.toHexString(
        context.getColor(resource("coui_dialog_list_${name}_blur_$theme", "color"))).padStart(8, '0')
    val radius = context.resources.getInteger(resource("coui_dialog_blur_radius", "integer")).toFloat()
    return load("com.coui.appcompat.COUIMaterialBlurEffect\$BlurEffectParams")
        .getConstructor(Float::class.javaPrimitiveType, String::class.java, BlendMode::class.java,
            String::class.java, BlendMode::class.java)
        .newInstance(radius, layer("blend"), base.getMethod("getBlendModeLayer1").invoke(preset),
            layer("mix"), base.getMethod("getBlendModeLayer2").invoke(preset))
}

/** Keep COUI's color layers and material delegate; only lighten its second blur pass. */
internal fun ColorOsNativeUi.Source.toolbarButtonBlurParams(
    context: Context, neutral: Boolean, backdropBlurred: Boolean
): Any? = when {
    backdropBlurred -> scaledBlurParams(context,
        if (neutral) "TYPE_CONTENT_TRANSPARENT_BUTTON" else "TYPE_FRAMEWORK_TOP_BAR_BLUR", 0.35f)
    neutral -> weatherButtonBlurParams(context)
    else -> null
}

/** Capsules use the same blur budget as the return button over the page's blurred surface. */
internal fun ColorOsNativeUi.Source.capsuleBlurParams(context: Context): Any =
    scaledBlurParams(context, "TYPE_FRAMEWORK_TOP_BAR_BLUR", 0.35f)

internal fun ColorOsNativeUi.Source.capsuleBlurRadius(context: Context): Float =
    load("com.coui.appcompat.COUIMaterialBlurEffect\$BaseBlurEffectParams")
        .getMethod("getBlurRadius", Context::class.java).invoke(capsuleBlurParams(context), context) as Float

private fun ColorOsNativeUi.Source.scaledBlurParams(
    context: Context, typeName: String, radiusScale: Float, tintAlphaScale: Float = 1f
): Any {
    val effect = load("com.coui.appcompat.COUIMaterialEffect")
    val type = load("com.coui.appcompat.COUIMaterialBlurEffect\$BlurEffectType")
    val base = load("com.coui.appcompat.COUIMaterialBlurEffect\$BaseBlurEffectParams")
    val preset = effect.getMethod("getBlurParamsByType", Context::class.java, type)
        .invoke(null, context, type.getField(typeName).get(null))
    if (radiusScale == 1f && tintAlphaScale == 1f) return preset
    fun color(name: String): String {
        val original = base.getMethod(name, Context::class.java).invoke(preset, context) as Int
        val alpha = (android.graphics.Color.alpha(original) * tintAlphaScale).toInt().coerceIn(0, 255)
        return "#" + Integer.toHexString((original and 0x00FFFFFF) or (alpha shl 24)).padStart(8, '0')
    }
    val radius = base.getMethod("getBlurRadius", Context::class.java).invoke(preset, context) as Float
    return load("com.coui.appcompat.COUIMaterialBlurEffect\$BlurEffectParams")
        .getConstructor(Float::class.javaPrimitiveType, String::class.java, BlendMode::class.java,
            String::class.java, BlendMode::class.java)
        .newInstance(radius * radiusScale, color("getColorLayer1"),
            base.getMethod("getBlendModeLayer1").invoke(preset), color("getColorLayer2"),
            base.getMethod("getBlendModeLayer2").invoke(preset))
}

/** Same color-layer recipe as COUIFloatingButton.getNonThemeFabColor, shared by colored buttons. */
internal fun ColorOsNativeUi.Source.accentButtonBlurParams(
    context: Context, accent: Int, enabled: Boolean = true, radiusScale: Float = 0.85f,
    radiusOverride: Float? = null
): Any {
    val theme = load("com.coui.appcompat.effect.COUIMaterialThemeBlurEffect")
    val companion = theme.getField("Companion").get(null)
    val type = load("${theme.name}\$ThemeBlurEffectType")
    val fabPreset = companion.javaClass.getMethod("getBlurEffectParamsByType", Context::class.java, type)
        .invoke(companion, context, type.getField(if (enabled) "TYPE_FRAMEWORK_FAB"
            else "TYPE_CONTENT_DISABLED_PRIMARY_BUTTON").get(null))
    // Disabled controls retain COUI's disabled treatment instead of an opaque active accent.
    if (!enabled) return fabPreset
    val base = load("com.coui.appcompat.COUIMaterialBlurEffect\$BaseBlurEffectParams")
    val radius = base.getMethod("getBlurRadius", Context::class.java).invoke(fabPreset, context) as Float
    val themeParams = load("com.coui.appcompat.effect.COUIMaterialThemeBlurEffect\$Companion\$ThemeBlurEffectParams")
    return themeParams.getConstructor(Float::class.javaPrimitiveType, Int::class.javaPrimitiveType,
        Boolean::class.javaPrimitiveType, BlendMode::class.java, Int::class.javaPrimitiveType,
        Boolean::class.javaPrimitiveType, BlendMode::class.java, Int::class.javaPrimitiveType)
        .newInstance(radiusOverride ?: (radius * radiusScale), accent or (0xFF shl 24),
            false, base.getMethod("getBlendModeLayer1").invoke(fabPreset),
            base.getMethod("getColorLayer2", Context::class.java).invoke(fabPreset, context), false,
            base.getMethod("getBlendModeLayer2").invoke(fabPreset), 255)
}

/** Native-only input scope: no schedule config, backdrop, overlay controller or animation value. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun NativeHomeDockChrome(
    visible: Boolean, parallel: Boolean, selectedIndex: Int, dark: Boolean,
    alignment: DockAlignment, buttonHidden: Boolean,
    onSelect: (Int) -> Unit, onHome: () -> Unit, onOpenMenu: (Rect, GraphicsLayer) -> Unit,
    fallback: State<@Composable () -> Unit>,
    phase: StartupPhase,
    modifier: Modifier = Modifier
) {
    if (!visible) return
    // Aggregate composition commits only; no user data or per-frame state is read here.
    SideEffect { Log.i("SleepDownCOUI", "Native dock commit: parallel=$parallel, selected=$selectedIndex, dark=$dark, hidden=$buttonHidden") }
    val restingModifier = modifier.homeCountdownShockwave(0.85f)
    DockEntranceContainer(phase, restingModifier) {
    if (parallel) {
        if (!ColorOsParallelHomeDock(selectedIndex, dark, buttonHidden, Modifier, onSelect, onOpenMenu)) NativeDockFallback(fallback)
    } else {
        val density = LocalDensity.current
        val systemBottom = WindowInsets.navigationBarsIgnoringVisibility.only(WindowInsetsSides.Bottom).getBottom(density)
        val bottom = with(density) { systemBottom.toDp() } + 8.dp
        val compensation = dockImeCompensationPx(WindowInsets.ime.getBottom(density), systemBottom)
        val position = when (alignment) {
            DockAlignment.LEFT -> Alignment.BottomStart
            DockAlignment.CENTER -> Alignment.BottomCenter
            DockAlignment.RIGHT -> Alignment.BottomEnd
        }
        val click = remember(onSelect, onHome) { { index: Int -> if (index == 0) onHome() else onSelect(2) } }
        val tabs = remember { listOf(R.drawable.ic_week_view to "课程", R.drawable.ic_settings to "设置") }
        Box(Modifier.fillMaxSize(), contentAlignment = position) {
            Box(Modifier.padding(start = 18.dp, end = 18.dp, bottom = bottom)
                .graphicsLayer { translationY = compensation.toFloat() }) {
                if (!ColorOsHomeDock(tabs, if (selectedIndex == 2) 1 else 0, dark,
                        Modifier.width(colorOsOrdinaryDockWidth()), click)) NativeDockFallback(fallback)
            }
        }
    }
    }
}

@Composable
private fun NativeDockFallback(content: State<@Composable () -> Unit>) { content.value() }
