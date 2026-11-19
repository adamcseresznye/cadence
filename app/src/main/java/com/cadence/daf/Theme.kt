package com.cadence.daf

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.annotation.StringRes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Shapes
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/** Night Focus: one dark, low-glare scheme on every phone. Mist teal is the only accent. */
private val NightColors = darkColorScheme(
    primary = Color(0xFF7FD3C6), onPrimary = Color(0xFF0F1416),
    primaryContainer = Color(0xFF1F3F3B), onPrimaryContainer = Color(0xFFBFF1E8),
    secondary = Color(0xFFA9B8B4), onSecondary = Color(0xFF15201E),
    secondaryContainer = Color(0xFF2A3739), onSecondaryContainer = Color(0xFFD5E0DD),
    tertiary = Color(0xFFE8EEEC), onTertiary = Color(0xFF0F1416),
    tertiaryContainer = Color(0xFF3A4648), onTertiaryContainer = Color(0xFFE8EEEC),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0F1416), onBackground = Color(0xFFE8EEEC),
    surface = Color(0xFF0F1416), onSurface = Color(0xFFE8EEEC),
    surfaceVariant = Color(0xFF2A3739), onSurfaceVariant = Color(0xFF8DA19C),
    outline = Color(0xFF3E4D50), outlineVariant = Color(0xFF2A3739),
    inverseSurface = Color(0xFFE8EEEC), inverseOnSurface = Color(0xFF1C2629), inversePrimary = Color(0xFF1F5F66),
    scrim = Color(0xFF000000),
    surfaceContainerLowest = Color(0xFF0B1012), surfaceContainerLow = Color(0xFF161D1F),
    surfaceContainer = Color(0xFF1C2629), surfaceContainerHigh = Color(0xFF243033),
    surfaceContainerHighest = Color(0xFF2A3739)
)

private val CadenceTypography = Typography(
    displayLarge = TextStyle(fontSize = 60.sp, lineHeight = 66.sp, fontWeight = FontWeight.Light, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold),
    headlineSmall = TextStyle(fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 34.sp, fontWeight = FontWeight.Normal),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.15.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium)
)

private val CadenceShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

private val OutEasing = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)

/** True when the user turned system animations off (animator duration scale 0). */
val LocalReduceMotion = staticCompositionLocalOf { false }

/** Tween that becomes instant when system animations are off. */
@Composable
fun <T> motionTween(durationMs: Int, easing: Easing = OutEasing): TweenSpec<T> =
    tween(if (LocalReduceMotion.current) 0 else durationMs, easing = easing)

@Composable
fun CadenceTheme(content: @Composable () -> Unit) {
    val resolver = LocalContext.current.contentResolver
    val reduceMotion = remember {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    CompositionLocalProvider(LocalReduceMotion provides reduceMotion) {
        MaterialTheme(
            colorScheme = NightColors,
            typography = CadenceTypography,
            shapes = CadenceShapes,
            content = content
        )
    }
}

/** Spring that becomes instant when system animations are off. */
@Composable
fun <T> motionSpring(damping: Float = 0.8f, stiffness: Float = 380f): FiniteAnimationSpec<T> =
    if (LocalReduceMotion.current) snap() else spring(damping, stiffness)

/** The intro plays once per app launch, not every time Main comes back from Settings. */
internal var introPlayed = false

/** Fades and floats a block up on first show; later blocks start a little later. */
@Composable
fun Modifier.staggerIn(index: Int): Modifier {
    val reduce = LocalReduceMotion.current
    val skip = remember { reduce || introPlayed }
    val progress = remember { Animatable(if (skip) 1f else 0f) }
    SideEffect { introPlayed = true }
    LaunchedEffect(Unit) {
        if (!skip) {
            delay(index * 40L)
            progress.animateTo(1f, tween(300, easing = OutEasing))
        }
    }
    return graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 20.dp.toPx()
    }
}

/** Scrolling screen with an optional pinned bottom bar. */
@Composable
fun ScreenScaffold(
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = bottomBar,
        snackbarHost = snackbarHost
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

/** Slider in the Night Focus palette: mist teal active track and thumb, quiet inactive track. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CadenceSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accent: Color? = null
) {
    val colors = MaterialTheme.colorScheme
    val source = remember { MutableInteractionSource() }
    val dragged by source.collectIsDraggedAsState()
    val grow by animateFloatAsState(if (dragged) 1.2f else 1f, motionSpring(1f, 500f), label = "thumb")
    val active = if (enabled) accent ?: colors.primary else colors.outline
    val inactive = colors.outlineVariant
    val ring = colors.surfaceContainer
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        steps = steps,
        modifier = modifier,
        enabled = enabled,
        interactionSource = source,
        // Round thumb with a ring in the card colour, over a thin rounded track.
        thumb = {
            Box(
                Modifier
                    .size(SLIDER_THUMB)
                    .graphicsLayer { scaleX = grow; scaleY = grow }
                    .background(ring, CircleShape)
                    .padding(3.dp)
                    .background(active, CircleShape)
            )
        },
        track = { state ->
            val fraction = ((state.value - state.valueRange.start) / (state.valueRange.endInclusive - state.valueRange.start)).coerceIn(0f, 1f)
            Canvas(Modifier.fillMaxWidth().height(SLIDER_THUMB)) {
                val half = SLIDER_THUMB.toPx() / 2
                val thickness = 6.dp.toPx()
                val top = (size.height - thickness) / 2
                val corner = CornerRadius(thickness / 2)
                drawRoundRect(inactive, Offset(0f, top), Size(size.width, thickness), corner)
                // The thumb centre travels from half a thumb in to half a thumb short of the end.
                drawRoundRect(active, Offset(0f, top), Size(half + fraction * (size.width - 2 * half), thickness), corner)
            }
        }
    )
}

private val SLIDER_THUMB = 26.dp

/** Small explanatory line under a setting. */
@Composable
fun Caption(@StringRes id: Int) {
    Text(stringResource(id), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}
