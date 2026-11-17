package com.music.bitchord.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.data.settings.PlayerBackdropStyle
import com.music.bitchord.data.settings.PlayerCoverAnimation
import com.music.bitchord.data.settings.PlayerCoverSize
import com.music.bitchord.data.settings.PlayerCustomizationState
import com.music.bitchord.data.settings.PlayerCustomizationStore
import com.music.bitchord.data.settings.PlayerPreset
import com.music.bitchord.data.settings.PlayerStyle
import com.music.bitchord.ui.components.IosSwitch

/**
 * Settings UI for customizing the Now Playing player.
 */
@Composable
fun PlayerCustomizationContent(
    modifier: Modifier = Modifier,
) {
    val state by PlayerCustomizationStore.state.collectAsStateWithLifecycle()
    var showResetDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        // 1. Live Interactive Mini Mockup Preview
        PlayerLivePreviewCard(state = state)

        Spacer(Modifier.height(24.dp))

        // 2. Player Design Style (Mochy Inmersivo vs BitChord Clásico)
        PlayerStyleSection(
            activeStyle = state.playerStyle,
            onSelectStyle = { style -> PlayerCustomizationStore.setPlayerStyle(style) },
        )

        Spacer(Modifier.height(24.dp))

        // 3. Quick Presets Section
        PlayerPresetsSection(
            activePreset = state.preset,
            onSelectPreset = { preset -> PlayerCustomizationStore.applyPreset(preset) },
        )

        Spacer(Modifier.height(24.dp))

        // 3. Ergonomics: Controls Vertical Offset Slider
        PlayerControlsOffsetSection(
            offsetDp = state.controlsOffsetDp,
            onOffsetChange = { offset -> PlayerCustomizationStore.setControlsOffset(offset) },
        )

        Spacer(Modifier.height(24.dp))

        // 4. Control Toggles (Visibility)
        PlayerControlsVisibilitySection(
            state = state,
        )

        Spacer(Modifier.height(24.dp))

        // 5. Cover Size & Animation
        PlayerCoverCustomizationSection(
            coverSize = state.coverSize,
            coverAnimation = state.coverAnimation,
            onSizeSelect = { PlayerCustomizationStore.setCoverSize(it) },
            onAnimationSelect = { PlayerCustomizationStore.setCoverAnimation(it) },
        )

        Spacer(Modifier.height(24.dp))

        // 6. Blur and Backdrop Intensity
        PlayerBackdropSection(
            blurIntensity = state.blurIntensity,
            backdropStyle = state.backdropStyle,
            onBlurChange = { PlayerCustomizationStore.setBlurIntensity(it) },
            onStyleSelect = { PlayerCustomizationStore.setBackdropStyle(it) },
        )

        Spacer(Modifier.height(28.dp))

        // 7. Reset to Defaults
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable { showResetDialog = true },
            color = Color(0xFF1C1C1E),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "Restablecer reproductor por defecto",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                )
            }
        }

        Spacer(Modifier.height(32.dp))
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Text(text = "Restablecer reproductor", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(text = "¿Deseas restaurar la apariencia y controles originales del reproductor Now Playing?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        PlayerCustomizationStore.resetToDefaults()
                        showResetDialog = false
                    },
                ) {
                    Text(
                        text = "Restablecer",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancelar")
                }
            },
        )
    }
}

/**
 * Interactive Live Preview mockup of the player reflecting user preferences.
 */
@Composable
private fun PlayerLivePreviewCard(
    state: PlayerCustomizationState,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "previewPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (state.coverAnimation == PlayerCoverAnimation.SUBTLE_PULSE) 1.04f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )
    val vinylRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (state.coverAnimation == PlayerCoverAnimation.VINYL) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000),
            repeatMode = RepeatMode.Restart,
        ),
        label = "vinyl",
    )

    val bgColor = when {
        state.backdropStyle == PlayerBackdropStyle.OLED_BLACK || state.blurIntensity == 0f -> Color(0xFF000000)
        state.backdropStyle == PlayerBackdropStyle.GRADIENT -> Color(0xFF1A1A24)
        else -> Color(0xFF1E1428)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp),
        ) {
            Text(
                text = "VISTA PREVIA EN VIVO",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF8E8E93),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(bgColor)
                .border(1.dp, Color(0xFF2C2C2E), RoundedCornerShape(20.dp))
                .padding(14.dp),
        ) {
            // Simulated gradient overlay based on blur intensity
            if (state.blurIntensity > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF381E48).copy(alpha = state.blurIntensity * 0.7f),
                                    Color(0xFF140D1E).copy(alpha = state.blurIntensity * 0.9f),
                                    Color.Black.copy(alpha = 0.9f),
                                )
                            )
                        )
                )
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // Top bar mockup
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.size(16.dp).background(Color(0xFF48484A), CircleShape))
                    Text(
                        text = "BeDivine Player",
                        color = Color(0xFFAAAAAA),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Box(modifier = Modifier.size(16.dp).background(Color(0xFF48484A), CircleShape))
                }

                if (state.playerStyle == PlayerStyle.MOCHY) {
                    // Mochy-style Edge-to-Edge Preview Mockup
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .scale(pulseScale)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF1E3A8A), Color(0xFF3B82F6), Color(0xFF0F172A))
                                )
                            ),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        // Top icons mockup
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(modifier = Modifier.size(12.dp).background(Color.White.copy(alpha = 0.7f), CircleShape))
                            Box(modifier = Modifier.size(12.dp).background(Color.White.copy(alpha = 0.7f), CircleShape))
                        }

                        // Liquid bottom gradient fade
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f), Color.Black)
                                    )
                                )
                        )
                    }
                } else {
                    // Album Artwork Mockup (Classic)
                    val coverHeight = when (state.coverSize) {
                        PlayerCoverSize.NORMAL -> 80.dp
                        PlayerCoverSize.EXPANDED_DOWN -> 105.dp
                        PlayerCoverSize.COMPACT -> 65.dp
                    }
                    val coverShape = if (state.coverAnimation == PlayerCoverAnimation.VINYL) CircleShape else RoundedCornerShape(12.dp)

                    Box(
                        modifier = Modifier
                            .size(coverHeight)
                            .scale(pulseScale)
                            .rotate(vinylRotation)
                            .clip(coverShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF6200EA), Color(0xFFFF4081), Color(0xFF00E5FF))
                                )
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.2f), coverShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (state.coverAnimation == PlayerCoverAnimation.VINYL) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF111111))
                                    .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                            )
                        }
                    }
                }

                // Controls Block with dynamic vertical offset
                val previewOffset = (state.controlsOffsetDp * 0.25f).dp
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = previewOffset),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Song title & artist (with OPUS badge in Mochy)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = "Canción de Ejemplo",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        if (state.playerStyle == PlayerStyle.MOCHY) {
                            Spacer(Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "OPUS",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                )
                            }
                        }
                    }
                    Text(
                        text = "Artista BeDivine",
                        color = Color(0xFF8E8E93),
                        fontSize = 10.sp,
                    )

                    Spacer(Modifier.height(6.dp))

                    // Progress bar
                    if (state.showProgressBar) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .height(3.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF3A3A3C))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.45f)
                                    .height(3.dp)
                                    .background(Color.White)
                            )
                        }
                        if (state.showTimestamps) {
                            Row(
                                modifier = Modifier.fillMaxWidth(0.9f).padding(top = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text("01:24", color = Color(0xFF8E8E93), fontSize = 8.sp)
                                Text("03:15", color = Color(0xFF8E8E93), fontSize = 8.sp)
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    // Transport Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(0.85f),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (state.playerStyle == PlayerStyle.CLASSIC && state.showShuffle) {
                            Icon(Icons.Default.Shuffle, contentDescription = null, tint = Color(0xFF8E8E93), modifier = Modifier.size(14.dp))
                        }
                        Icon(Icons.Default.SkipPrevious, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color.White, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        }
                        Icon(Icons.Default.SkipNext, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        if (state.playerStyle == PlayerStyle.CLASSIC && state.showRepeat) {
                            Icon(Icons.Default.Repeat, contentDescription = null, tint = Color(0xFF8E8E93), modifier = Modifier.size(14.dp))
                        }
                    }

                    // Bottom capsules
                    if (state.playerStyle == PlayerStyle.MOCHY) {
                        Row(
                            modifier = Modifier.fillMaxWidth(0.92f).padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.White.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text("Echo Dot-OR7", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Medium)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.White.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text("💬  |  ≡", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        // Bottom mini row (Classic)
                        Row(
                            modifier = Modifier.fillMaxWidth(0.7f).padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            if (state.showLyricsButton) {
                                Text("Letras", color = Color(0xFF00E5FF), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                            if (state.showQueueButton) {
                                Icon(Icons.Default.QueueMusic, contentDescription = null, tint = Color(0xFF8E8E93), modifier = Modifier.size(12.dp))
                            }
                            if (state.showVolumeBar) {
                                Icon(Icons.Default.Tune, contentDescription = null, tint = Color(0xFF8E8E93), modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Selector for overall Player Visual Design Style (Mochy Inmersivo vs BitChord Clásico).
 */
@Composable
private fun PlayerStyleSection(
    activeStyle: PlayerStyle,
    onSelectStyle: (PlayerStyle) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "DISEÑO DEL REPRODUCTOR",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF8E8E93),
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PlayerStyle.entries.forEach { style ->
                val isSelected = style == activeStyle
                val borderColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF2C2C2E),
                    label = "styleBorder",
                )
                val bgCardColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color(0xFF1C1C1E),
                    label = "styleBg",
                )

                Surface(
                    onClick = { onSelectStyle(style) },
                    shape = RoundedCornerShape(16.dp),
                    color = bgCardColor,
                    border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
                    modifier = Modifier.weight(1f),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = style.titleEs,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                            )
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = style.descEs,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = Color(0xFF8E8E93),
                            lineHeight = 15.sp,
                        )
                    }
                }
            }
        }
    }
}

/**
 * 4 Quick Presets
 */
@Composable
private fun PlayerPresetsSection(
    activePreset: PlayerPreset,
    onSelectPreset: (PlayerPreset) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "PLANTILLAS PREDEFINIDAS DEL REPRODUCTOR",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF8E8E93),
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 10.dp),
        )

        val presets = listOf(
            PlayerPreset.MINIMAL_OLED,
            PlayerPreset.IMMERSIVE,
            PlayerPreset.ERGONOMIC,
            PlayerPreset.POWER_USER,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            presets.forEach { preset ->
                val isSelected = activePreset == preset
                val borderColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White else Color.Transparent,
                    label = "presetBorder",
                )
                val bg = if (isSelected) Color(0xFF2C2C2E) else Color(0xFF1C1C1E)

                Box(
                    modifier = Modifier
                        .width(155.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(bg)
                        .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
                        .clickable { onSelectPreset(preset) }
                        .padding(14.dp),
                ) {
                    Column {
                        Text(
                            text = preset.titleEs,
                            color = if (isSelected) Color.White else Color(0xFFD1D1D6),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = preset.descEs,
                            color = Color(0xFF8E8E93),
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Height / Vertical Offset Slider for Controls Ergonomics
 */
@Composable
private fun PlayerControlsOffsetSection(
    offsetDp: Int,
    onOffsetChange: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1C1C1E))
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Altura y Posición de Controles",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
            )
            val label = when {
                offsetDp < -10 -> "Más arriba (${offsetDp}dp)"
                offsetDp in -10..10 -> "Equilibrado (${offsetDp}dp)"
                else -> "Cerca del pulgar (+${offsetDp}dp)"
            }
            Text(
                text = label,
                color = Color(0xFF00E5FF),
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
            )
        }

        Spacer(Modifier.height(6.dp))
        Text(
            text = "Desplaza el bloque de botones hacia abajo para alcanzar Play y la barra fácilmente con una mano.",
            color = Color(0xFF8E8E93),
            fontSize = 12.sp,
            lineHeight = 16.sp,
        )

        Spacer(Modifier.height(10.dp))

        Slider(
            value = offsetDp.toFloat(),
            onValueChange = { onOffsetChange(it.toInt()) },
            valueRange = -40f..60f,
            steps = 20,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color(0xFF00E5FF),
                inactiveTrackColor = Color(0xFF3A3A3C),
            ),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("-40dp (Arriba)", color = Color(0xFF636366), fontSize = 10.sp)
            Text("0dp (Centro)", color = Color(0xFF636366), fontSize = 10.sp)
            Text("+60dp (Abajo)", color = Color(0xFF636366), fontSize = 10.sp)
        }
    }
}

/**
 * Control Visibility Toggles
 */
@Composable
private fun PlayerControlsVisibilitySection(
    state: PlayerCustomizationState,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1C1C1E))
            .padding(16.dp),
    ) {
        Text(
            text = "Visibilidad de Controles",
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        Text(
            text = "Oculta los botones que no utilices para un reproductor más limpio.",
            color = Color(0xFF8E8E93),
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        PlayerToggleRow(
            title = "Botón de reproducción aleatoria (Shuffle)",
            checked = state.showShuffle,
            onCheckedChange = { PlayerCustomizationStore.toggleShuffle() },
        )
        PlayerToggleRow(
            title = "Botón de repetición (Repeat)",
            checked = state.showRepeat,
            onCheckedChange = { PlayerCustomizationStore.toggleRepeat() },
        )
        PlayerToggleRow(
            title = "Barra de progreso de canción",
            checked = state.showProgressBar,
            onCheckedChange = { PlayerCustomizationStore.toggleProgressBar() },
        )
        PlayerToggleRow(
            title = "Tiempos numéricos (00:00 / 03:45)",
            checked = state.showTimestamps,
            onCheckedChange = { PlayerCustomizationStore.toggleTimestamps() },
        )
        PlayerToggleRow(
            title = "Acceso directo a Letras",
            checked = state.showLyricsButton,
            onCheckedChange = { PlayerCustomizationStore.toggleLyricsButton() },
        )
        PlayerToggleRow(
            title = "Acceso directo a Cola / Fila",
            checked = state.showQueueButton,
            onCheckedChange = { PlayerCustomizationStore.toggleQueueButton() },
        )
        PlayerToggleRow(
            title = "Barra de volumen en pantalla",
            checked = state.showVolumeBar,
            onCheckedChange = { PlayerCustomizationStore.toggleVolumeBar() },
        )
    }
}

@Composable
private fun PlayerToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            color = Color(0xFFD1D1D6),
            fontSize = 14.sp,
            modifier = Modifier.weight(1f).padding(end = 12.dp),
        )
        IosSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

/**
 * Cover Size & Animation
 */
@Composable
private fun PlayerCoverCustomizationSection(
    coverSize: PlayerCoverSize,
    coverAnimation: PlayerCoverAnimation,
    onSizeSelect: (PlayerCoverSize) -> Unit,
    onAnimationSelect: (PlayerCoverAnimation) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1C1C1E))
            .padding(16.dp),
    ) {
        Text(
            text = "Tamaño y Animación de Portada",
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        Text(
            text = "Tamaño de carátula:",
            color = Color(0xFF8E8E93),
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PlayerCoverSize.entries.forEach { size ->
                val isSelected = coverSize == size
                val bg = if (isSelected) Color(0xFF3A3A3C) else Color(0xFF2C2C2E)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(bg)
                        .clickable { onSizeSelect(size) }
                        .padding(vertical = 10.dp, horizontal = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = size.titleEs,
                        color = if (isSelected) Color.White else Color(0xFF8E8E93),
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Efecto de movimiento:",
            color = Color(0xFF8E8E93),
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PlayerCoverAnimation.entries.forEach { anim ->
                val isSelected = coverAnimation == anim
                val bg = if (isSelected) Color(0xFF3A3A3C) else Color(0xFF2C2C2E)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(bg)
                        .clickable { onAnimationSelect(anim) }
                        .padding(vertical = 10.dp, horizontal = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = anim.titleEs,
                        color = if (isSelected) Color.White else Color(0xFF8E8E93),
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

/**
 * Blur & Backdrop Style Section
 */
@Composable
private fun PlayerBackdropSection(
    blurIntensity: Float,
    backdropStyle: PlayerBackdropStyle,
    onBlurChange: (Float) -> Unit,
    onStyleSelect: (PlayerBackdropStyle) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1C1C1E))
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Desenfoque y Fondo (Blur)",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
            )
            val pct = (blurIntensity * 100).toInt()
            val label = if (pct == 0) "0% (OLED Negro Puro)" else "$pct%"
            Text(
                text = label,
                color = if (pct == 0) Color(0xFF30D158) else Color(0xFF00E5FF),
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
            )
        }

        Spacer(Modifier.height(6.dp))
        Text(
            text = "Ajusta la intensidad del vidrio esmerilado de fondo. A 0% se activa el modo OLED para máximo contraste.",
            color = Color(0xFF8E8E93),
            fontSize = 12.sp,
            lineHeight = 16.sp,
        )

        Spacer(Modifier.height(10.dp))

        Slider(
            value = blurIntensity,
            onValueChange = onBlurChange,
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color(0xFF00E5FF),
                inactiveTrackColor = Color(0xFF3A3A3C),
            ),
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Estilo de fondo:",
            color = Color(0xFF8E8E93),
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PlayerBackdropStyle.entries.forEach { style ->
                val isSelected = backdropStyle == style
                val bg = if (isSelected) Color(0xFF3A3A3C) else Color(0xFF2C2C2E)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(bg)
                        .clickable { onStyleSelect(style) }
                        .padding(vertical = 10.dp, horizontal = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = style.titleEs,
                        color = if (isSelected) Color.White else Color(0xFF8E8E93),
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
