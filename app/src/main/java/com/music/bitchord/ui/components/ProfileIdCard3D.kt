package com.music.bitchord.ui.components

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil3.compose.AsyncImage
import com.music.bitchord.data.settings.ProfileCardEffect
import com.music.bitchord.data.settings.ProfileCardTheme
import com.music.bitchord.ui.icons.SfIcons
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/** Width to height ratio of the Mochy-style 3D ID Card (332 x 176). */
const val IdCardAspect = 1.89f

private const val MarqueePeriodNanos = 5_200_000_000f

/**
 * Interactive 3D container that wraps [ProfileIdCard] with finger drag-to-tilt physics,
 * device gyroscope tilt, subtle floating motion, and optional dot-matrix stage backdrop.
 */
@Composable
fun InteractiveProfileIdCard3D(
    username: String,
    photo: Any?,
    cardNumber: String,
    joinedText: String,
    theme: ProfileCardTheme,
    effect: ProfileCardEffect,
    showDotBackdrop: Boolean = true,
    showBadgeText: Boolean = true,
    showEditHint: Boolean = true,
    onClick: (() -> Unit)? = null,
    onPhotoClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val viewConfig = LocalViewConfiguration.current
    val currentOnClick by rememberUpdatedState(onClick)
    val deviceTilt = rememberCardDeviceTilt()

    val tiltX = remember { Animatable(0f) }
    val tiltY = remember { Animatable(0f) }
    var tiltTarget by remember { mutableStateOf(Offset.Zero) }
    var isPressed by remember { mutableStateOf(false) }
    var swayTime by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        val start = System.nanoTime()
        while (true) {
            withInfiniteAnimationFrameNanos { now ->
                swayTime = ((now - start) / 1_000_000_000.0 % 3600.0).toFloat()
            }
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { tiltTarget }.collect { target ->
            val settle = target == Offset.Zero
            val spec = if (settle) {
                spring<Float>(dampingRatio = 0.42f, stiffness = Spring.StiffnessLow)
            } else {
                spring(stiffness = Spring.StiffnessMediumLow)
            }
            launch { tiltX.animateTo(target.y, spec) }
            launch { tiltY.animateTo(target.x, spec) }
        }
    }

    val containerShape = RoundedCornerShape(24.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (showDotBackdrop) {
                    Modifier
                        .clip(containerShape)
                        .background(Color(0xFF121214))
                        .cardDotBackdrop(color = { Color(0xFF252529) })
                        .border(0.5.dp, Color.White.copy(alpha = 0.08f), containerShape)
                        .padding(horizontal = 14.dp, vertical = 16.dp)
                } else {
                    Modifier
                },
            )
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    isPressed = true
                    var acc = Offset.Zero
                    var maxDistance = 0f
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        val delta = change.positionChange()
                        if (delta != Offset.Zero) {
                            acc += delta
                            maxDistance = maxOf(maxDistance, hypot(acc.x, acc.y))
                            if (maxDistance > viewConfig.touchSlop * 0.4f) {
                                change.consume()
                            }
                            tiltTarget = Offset(
                                (acc.x * 0.12f).coerceIn(-28f, 28f),
                                (-acc.y * 0.10f).coerceIn(-22f, 22f),
                            )
                        }
                    }
                    isPressed = false
                    tiltTarget = Offset.Zero
                    if (maxDistance < viewConfig.touchSlop) {
                        currentOnClick?.invoke()
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        ProfileIdCard(
            username = username,
            photo = photo,
            cardNumber = cardNumber,
            joinedText = joinedText,
            theme = theme,
            effect = effect,
            showIdCardOf = showBadgeText,
            showEditHint = showEditHint,
            onPhotoClick = onPhotoClick,
            pressed = { isPressed },
            sheen = {
                0.5f + (tiltY.value + deviceTilt.rotationY + cos(swayTime * 1.3f) * 6f) / 42f
            },
            tiltOffset = {
                Offset(
                    x = (tiltY.value + deviceTilt.rotationY) / 28f,
                    y = (tiltX.value + deviceTilt.rotationX) / 22f,
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 18.dp,
                    shape = RoundedCornerShape(22.dp),
                    ambientColor = theme.stripTextColor.copy(alpha = 0.35f),
                    spotColor = Color.Black.copy(alpha = 0.7f),
                )
                .graphicsLayer {
                    val liveY = tiltY.value + deviceTilt.rotationY + sin(swayTime * 0.9f) * 2.8f
                    val liveX = tiltX.value + deviceTilt.rotationX + cos(swayTime * 0.7f) * 1.9f
                    rotationY = liveY
                    rotationX = liveX
                    val pressScale = if (isPressed) 0.985f else 1f
                    scaleX = pressScale
                    scaleY = pressScale
                    cameraDistance = 14f * this.density
                },
        )
    }
}

/**
 * The 3D Profile ID Card surface, layout, typography, circular avatar, and vertical marquee strip.
 */
@Composable
fun ProfileIdCard(
    username: String,
    photo: Any?,
    cardNumber: String,
    joinedText: String,
    theme: ProfileCardTheme = ProfileCardTheme.ONYX_DARK,
    effect: ProfileCardEffect = ProfileCardEffect.HOLOGRAPHIC,
    modifier: Modifier = Modifier,
    showIdCardOf: Boolean = true,
    sheen: () -> Float = { 0.5f },
    tiltOffset: () -> Offset = { Offset.Zero },
    onPhotoClick: (() -> Unit)? = null,
    marqueeRunning: () -> Boolean = { true },
    showEditHint: Boolean = true,
    pressed: () -> Boolean = { false },
) {
    val name = username.trim().ifEmpty { "BeDivine" }
    val shape = RoundedCornerShape(22.dp)
    val bodyText = theme.bodyTextColor
    val mutedText = theme.mutedTextColor

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(IdCardAspect)
            .clip(shape)
            .cardSurface(
                theme = theme,
                effect = effect,
                sheen = sheen,
                tiltOffset = tiltOffset,
            )
            .drawWithContent {
                drawContent()
                if (pressed()) {
                    drawRect(
                        Color.White.copy(
                            alpha = if (theme.isMetallicLight) 0.22f else 0.10f,
                        ),
                    )
                }
            },
    ) {
        val unit = maxWidth / 332f
        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(
                        start = unit * 22f,
                        top = unit * 20f,
                        end = unit * 12f,
                        bottom = unit * 18f,
                    ),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "PROFILE USR#$cardNumber",
                        color = mutedText,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = (unit.value * 11f).sp,
                        letterSpacing = (-0.3).sp,
                        maxLines = 1,
                    )
                    if (showEditHint) {
                        EditPencilHint(tint = mutedText, size = unit * 10.5f)
                    }
                }
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(unit * 94f)
                            .clip(CircleShape)
                            .background(
                                if (theme.isMetallicLight) {
                                    Color(0xFF5A5A60)
                                } else {
                                    Color.White.copy(alpha = 0.14f)
                                },
                            )
                            .border(
                                width = 1.dp,
                                color = if (theme.isMetallicLight) {
                                    Color.Black.copy(alpha = 0.18f)
                                } else {
                                    Color.White.copy(alpha = 0.18f)
                                },
                                shape = CircleShape,
                            )
                            .then(
                                if (onPhotoClick != null) {
                                    Modifier.clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = onPhotoClick,
                                    )
                                } else {
                                    Modifier
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (photo != null) {
                            AsyncImage(
                                model = photo,
                                contentDescription = name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Icon(
                                imageVector = if (onPhotoClick != null) SfIcons.Add else SfIcons.Person,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.78f),
                                modifier = Modifier.size(unit * 34f),
                            )
                        }
                    }
                    Spacer(Modifier.width(unit * 18f))
                    Column(modifier = Modifier.weight(1f)) {
                        if (showIdCardOf) {
                            Text(
                                text = "ID CARD OF",
                                color = mutedText,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium,
                                fontSize = (unit.value * 10f).sp,
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            var nameCut by remember { mutableStateOf(false) }
                            Text(
                                text = name,
                                color = bodyText,
                                fontSize = (unit.value * 21f).sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                onTextLayout = { nameCut = it.hasVisualOverflow },
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            if (showEditHint && !nameCut) {
                                EditPencilHint(
                                    tint = bodyText.copy(alpha = 0.75f),
                                    size = unit * 12f,
                                )
                            }
                        }
                        Text(
                            text = joinedText,
                            color = bodyText.copy(alpha = 0.85f),
                            fontSize = (unit.value * 10.5f).sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
            }
            CardMarqueeStrip(
                text = "$name's Card",
                color = theme.stripTextColor,
                background = theme.stripBackground,
                modifier = Modifier
                    .width(unit * 40f)
                    .fillMaxHeight(),
                fontSizeSp = unit.value * 16.5f,
                running = marqueeRunning,
            )
        }
    }
}

@Composable
private fun EditPencilHint(tint: Color, size: Dp) {
    Icon(
        imageVector = SfIcons.Pencil,
        contentDescription = null,
        tint = tint,
        modifier = Modifier
            .padding(start = size / 3f)
            .size(size),
    )
}

@Composable
internal fun CardMarqueeStrip(
    text: String,
    color: Color,
    background: Color,
    fontSizeSp: Float,
    modifier: Modifier = Modifier,
    running: () -> Boolean = { true },
) {
    val measurer = rememberTextMeasurer()
    val style = remember(color, fontSizeSp) {
        TextStyle(
            color = color,
            fontSize = fontSizeSp.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
        )
    }
    val layout = remember(text, style) { measurer.measure("CARD   ·   $text   ·   ", style) }
    var progress by remember { mutableFloatStateOf(0f) }
    val isRunning by rememberUpdatedState(running)
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            if (!isRunning()) {
                snapshotFlow { isRunning() }.first { it }
                last = 0L
            }
            withInfiniteAnimationFrameNanos { now ->
                if (last != 0L) progress = (progress + (now - last) / MarqueePeriodNanos) % 1f
                last = now
            }
        }
    }
    Canvas(modifier = modifier.background(background)) {
        val step = layout.size.width.toFloat().coerceAtLeast(1f)
        rotate(90f, pivot = Offset.Zero) {
            translate(left = 0f, top = -size.width / 2f - layout.size.height / 2f) {
                var x = -step + progress * step
                while (x < size.height) {
                    drawText(layout, topLeft = Offset(x, 0f))
                    x += step
                }
            }
        }
    }
}

private fun Modifier.cardSurface(
    theme: ProfileCardTheme,
    effect: ProfileCardEffect,
    sheen: () -> Float,
    tiltOffset: () -> Offset,
): Modifier = drawWithCache {
    val baseBrush = if (theme.isMetallicLight) {
        Brush.linearGradient(
            0f to Color(0xFFBFC1C6),
            0.35f to Color(0xFFE9EAEE),
            0.55f to Color(0xFFA9ABB2),
            0.8f to Color(0xFFD7D9DE),
            1f to Color(0xFF9C9EA5),
            start = Offset.Zero,
            end = Offset(size.width, size.height),
        )
    } else {
        Brush.linearGradient(
            0f to theme.primaryColor,
            0.55f to theme.secondaryColor,
            1f to theme.primaryColor.copy(
                red = (theme.primaryColor.red * 0.75f).coerceIn(0f, 1f),
                green = (theme.primaryColor.green * 0.75f).coerceIn(0f, 1f),
                blue = (theme.primaryColor.blue * 0.75f).coerceIn(0f, 1f),
            ),
            start = Offset.Zero,
            end = Offset(size.width, size.height),
        )
    }

    val grainLines = (size.height / 3f).toInt()
    val cornerRadiusPx = 22.dp.toPx()

    // Precompute deterministic stardust particles
    val stars = run {
        val rng = Random(42)
        List(44) {
            Triple(
                rng.nextFloat() * size.width,
                rng.nextFloat() * size.height,
                (1.1f + rng.nextFloat() * 2.2f),
            )
        }
    }

    // Precompute dot matrix pattern shader
    val dotStep = 12.dp.toPx().roundToInt().coerceAtLeast(4)
    val dotTile = ImageBitmap(dotStep, dotStep)
    androidx.compose.ui.graphics.Canvas(dotTile).drawCircle(
        Offset(dotStep / 2f, dotStep / 2f),
        1.35.dp.toPx(),
        Paint().apply {
            color = Color.White
            isAntiAlias = true
        },
    )
    val dotShader = ShaderBrush(ImageShader(dotTile, TileMode.Repeated, TileMode.Repeated))

    onDrawBehind {
        drawRect(baseBrush)

        val currentSheen = sheen().coerceIn(-0.3f, 1.3f)
        val center = currentSheen * size.width
        val tilt = tiltOffset()

        when (effect) {
            ProfileCardEffect.METALLIC_BRUSHED -> {
                for (i in 0 until grainLines) {
                    val y = i * 3f
                    drawLine(
                        color = Color.White.copy(alpha = if (i % 2 == 0) 0.055f else 0.025f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f,
                    )
                }
                drawRect(
                    brush = Brush.linearGradient(
                        0f to Color.Transparent,
                        0.45f to Color.White.copy(alpha = if (theme.isMetallicLight) 0.55f else 0.24f),
                        0.55f to Color.White.copy(alpha = if (theme.isMetallicLight) 0.35f else 0.14f),
                        1f to Color.Transparent,
                        start = Offset(center - size.width * 0.35f, 0f),
                        end = Offset(center + size.width * 0.20f, size.height),
                    ),
                    blendMode = BlendMode.Screen,
                )
            }

            ProfileCardEffect.HOLOGRAPHIC -> {
                val holoAlpha = if (theme.isMetallicLight) 0.34f else 0.24f
                drawRect(
                    brush = Brush.linearGradient(
                        0f to Color.Transparent,
                        0.25f to Color(0xFF00F5D4).copy(alpha = holoAlpha * 0.7f),
                        0.45f to Color(0xFF9B5DE5).copy(alpha = holoAlpha),
                        0.60f to Color(0xFFF15BB5).copy(alpha = holoAlpha * 0.9f),
                        0.78f to Color(0xFFFEE440).copy(alpha = holoAlpha * 0.6f),
                        1f to Color.Transparent,
                        start = Offset(center - size.width * 0.45f, -size.height * 0.2f * tilt.y),
                        end = Offset(center + size.width * 0.35f, size.height * (1f + 0.2f * tilt.y)),
                    ),
                    blendMode = BlendMode.Screen,
                )
            }

            ProfileCardEffect.DOT_MATRIX -> {
                val dotAlpha = if (theme.isMetallicLight) 0.16f else 0.11f
                drawRect(
                    brush = dotShader,
                    alpha = dotAlpha,
                    colorFilter = ColorFilter.tint(
                        if (theme.isMetallicLight) Color.Black else theme.stripTextColor,
                        BlendMode.SrcIn,
                    ),
                )
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            theme.stripTextColor.copy(alpha = 0.22f),
                            Color.Transparent,
                        ),
                        center = Offset(
                            size.width * (0.5f + tilt.x * 0.35f),
                            size.height * (0.5f + tilt.y * 0.35f),
                        ),
                        radius = size.width * 0.65f,
                    ),
                    blendMode = BlendMode.Screen,
                )
            }

            ProfileCardEffect.NEON_GLOW -> {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            theme.stripTextColor.copy(alpha = 0.30f),
                            Color.Transparent,
                        ),
                        center = Offset(
                            size.width * (0.5f + tilt.x * 0.4f),
                            size.height * (0.5f + tilt.y * 0.4f),
                        ),
                        radius = size.width * 0.7f,
                    ),
                    blendMode = BlendMode.Screen,
                )
                drawRoundRect(
                    brush = Brush.linearGradient(
                        0f to theme.stripTextColor.copy(alpha = 0.85f),
                        0.5f to Color.White.copy(alpha = 0.9f),
                        1f to theme.stripTextColor.copy(alpha = 0.85f),
                        start = Offset(center - size.width * 0.4f, 0f),
                        end = Offset(center + size.width * 0.4f, size.height),
                    ),
                    cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                    style = Stroke(width = 2.2.dp.toPx()),
                )
            }

            ProfileCardEffect.STARDUST -> {
                stars.forEachIndexed { idx, (sx, sy, r) ->
                    val pulse = 0.35f + 0.65f * kotlin.math.abs(
                        sin(currentSheen * 4f + idx * 0.7f),
                    )
                    drawCircle(
                        color = (if (idx % 3 == 0) theme.stripTextColor else Color.White)
                            .copy(alpha = 0.45f * pulse),
                        radius = r,
                        center = Offset(
                            (sx + tilt.x * 8f * (idx % 3 + 1)).coerceIn(0f, size.width),
                            (sy + tilt.y * 6f * (idx % 2 + 1)).coerceIn(0f, size.height),
                        ),
                    )
                }
                drawRect(
                    brush = Brush.linearGradient(
                        0f to Color.Transparent,
                        0.5f to Color.White.copy(alpha = 0.16f),
                        1f to Color.Transparent,
                        start = Offset(center - size.width * 0.3f, 0f),
                        end = Offset(center + size.width * 0.2f, size.height),
                    ),
                    blendMode = BlendMode.Screen,
                )
            }

            ProfileCardEffect.MINIMAL_MATTE -> {
                // Subtle specular sheen only
                drawRect(
                    brush = Brush.linearGradient(
                        0f to Color.Transparent,
                        0.5f to Color.White.copy(alpha = 0.08f),
                        1f to Color.Transparent,
                        start = Offset(center - size.width * 0.35f, 0f),
                        end = Offset(center + size.width * 0.15f, size.height),
                    ),
                    blendMode = BlendMode.Screen,
                )
            }
        }

        // Subtle inner rim highlight
        if (effect != ProfileCardEffect.NEON_GLOW) {
            drawRoundRect(
                color = Color.White.copy(alpha = if (theme.isMetallicLight) 0.45f else 0.12f),
                cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                style = Stroke(width = 1.dp.toPx()),
            )
        }
    }
}

/** Tiled dot-matrix pattern behind the card container, matching Mochy's header backdrop. */
fun Modifier.cardDotBackdrop(
    color: () -> Color,
    alpha: () -> Float = { 1f },
    spacing: Dp = 16.dp,
    radius: Dp = 1.25.dp,
): Modifier = drawWithCache {
    val step = spacing.toPx().roundToInt().coerceAtLeast(4)
    val tile = ImageBitmap(step, step)
    androidx.compose.ui.graphics.Canvas(tile).drawCircle(
        Offset(step / 2f, step / 2f),
        radius.toPx(),
        Paint().apply {
            this.color = Color.White
            isAntiAlias = true
        },
    )
    val brush = ShaderBrush(ImageShader(tile, TileMode.Repeated, TileMode.Repeated))
    var lastColor = Color.Unspecified
    var filter: ColorFilter? = null
    onDrawBehind {
        val a = alpha()
        if (a <= 0.004f) return@onDrawBehind
        val c = color()
        if (c != lastColor) {
            lastColor = c
            filter = ColorFilter.tint(c, BlendMode.SrcIn)
        }
        drawRect(brush, alpha = a, colorFilter = filter)
    }
}

@Stable
class CardDeviceTilt internal constructor() {
    var rotationX by mutableFloatStateOf(0f)
        internal set
    var rotationY by mutableFloatStateOf(0f)
        internal set
}

@Composable
fun rememberCardDeviceTilt(maxDegrees: Float = 12f): CardDeviceTilt {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val tilt = remember { CardDeviceTilt() }

    DisposableEffect(context, lifecycleOwner) {
        val sensors = context.getSystemService(SensorManager::class.java)
        val sensor = sensors?.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            ?: sensors?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (sensors == null || sensor == null) return@DisposableEffect onDispose { }

        val matrix = FloatArray(9)
        val angles = FloatArray(3)
        var restPitch = Float.NaN
        var restRoll = Float.NaN

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(matrix, event.values)
                SensorManager.getOrientation(matrix, angles)
                val pitch = Math.toDegrees(angles[1].toDouble()).toFloat()
                val roll = Math.toDegrees(angles[2].toDouble()).toFloat()
                if (restPitch.isNaN()) {
                    restPitch = pitch
                    restRoll = roll
                }
                val dPitch = pitch - restPitch
                var dRoll = (roll - restRoll) % 360f
                if (dRoll > 180f) dRoll -= 360f
                if (dRoll < -180f) dRoll += 360f

                restPitch += dPitch * 0.012f
                restRoll += dRoll * 0.012f

                val targetX = (-dPitch * 0.85f).coerceIn(-maxDegrees, maxDegrees)
                val targetY = (dRoll * 0.85f).coerceIn(-maxDegrees, maxDegrees)
                tilt.rotationX += (targetX - tilt.rotationX) * 0.22f
                tilt.rotationY += (targetY - tilt.rotationY) * 0.22f
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        var listening = false
        fun listen(on: Boolean) {
            if (on == listening) return
            listening = on
            if (on) {
                restPitch = Float.NaN
                sensors.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
            } else {
                sensors.unregisterListener(listener)
                tilt.rotationX = 0f
                tilt.rotationY = 0f
            }
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> listen(true)
                Lifecycle.Event.ON_PAUSE -> listen(false)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) listen(true)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            listen(false)
        }
    }
    return tilt
}
