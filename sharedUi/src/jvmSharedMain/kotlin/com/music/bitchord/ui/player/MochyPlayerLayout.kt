package com.music.bitchord.ui.player

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.music.bitchord.data.model.LikeStatus
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.NerdStats
import com.music.bitchord.playback.PlaybackPosition
import com.music.bitchord.data.canvas.CanvasArtwork
import com.music.bitchord.data.settings.PlayerCoverAnimation
import com.music.bitchord.data.settings.PlayerCoverSize
import com.music.bitchord.data.settings.PlayerCustomizationState
import com.music.bitchord.sharedui.resources.Res
import com.music.bitchord.sharedui.resources.ic_player_next
import com.music.bitchord.sharedui.resources.ic_player_pause
import com.music.bitchord.sharedui.resources.ic_player_play
import com.music.bitchord.sharedui.resources.ic_player_previous
import com.music.bitchord.ui.components.menuAnchor
import com.music.bitchord.ui.icons.BitChordIcons
import org.jetbrains.compose.resources.painterResource
import kotlin.math.abs

/**
 * Mochy-style player layout:
 * - 100% Edge-to-edge artwork with liquid bottom gradient fade into dark controls
 * - Zero dead air: perfectly compact, ergonomic vertical hierarchy
 * - Top dismiss chevron and overflow menu
 * - Track title, audio format badge (OPUS/FLAC), artist, heart button, and expand button
 * - Sleek progress slider with timestamps
 * - Transport controls (Previous, Play/Pause, Next)
 * - Volume slider
 * - Output device pill capsule on the bottom left and [ Lyrics | Queue ] pill on the bottom right
 */
@Composable
internal fun MochyPlayerLayout(
    song: Song,
    remoteArt: String?,
    art: PlayerArtwork,
    canvas: CanvasArtwork?,
    isPlaying: Boolean,
    isLoading: Boolean,
    position: PlaybackPosition,
    durationMs: Long,
    hasPrevious: Boolean,
    hasNext: Boolean,
    pastRestartPoint: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    shown: () -> Float,
    onScrub: (Float) -> Unit,
    onScrubFinished: () -> Unit,
    likeStatus: LikeStatus,
    onToggleLike: () -> Unit,
    volume: PlayerVolume,
    onVolumeChange: (Float) -> Unit,
    onVolumeChangeFinished: () -> Unit,
    accountName: String?,
    onOpenOutput: () -> Unit,
    onOpenMenu: () -> Unit,
    onDismiss: () -> Unit,
    onOpenArtist: (browseId: String?, name: String) -> Unit,
    onOpenAlbum: (String) -> Unit,
    lyricsOpen: Boolean,
    onToggleLyrics: () -> Unit,
    queueOpen: Boolean,
    onToggleQueue: () -> Unit,
    customization: PlayerCustomizationState,
    modifier: Modifier = Modifier,
) {
    val nerdStats by NerdStats.current.collectAsStateWithLifecycle()
    val codecBadge = remember(nerdStats?.mimeType, nerdStats?.container, nerdStats?.claimed) {
        val mime = nerdStats?.mimeType?.lowercase() ?: ""
        val container = nerdStats?.container?.uppercase() ?: ""
        val claimed = nerdStats?.claimed?.codec?.uppercase() ?: ""
        when {
            mime.contains("opus") || container.contains("OPUS") -> "OPUS"
            mime.contains("flac") || container.contains("FLAC") || claimed.contains("FLAC") -> "FLAC"
            mime.contains("mp4") || mime.contains("aac") || container.contains("M4A") -> "AAC"
            mime.contains("mp3") || mime.contains("mpeg") -> "MP3"
            claimed.isNotBlank() -> claimed
            container.isNotBlank() -> container
            else -> "OPUS"
        }
    }

    var dragAccumX by remember { mutableFloatStateOf(0f) }
    var dragAccumY by remember { mutableFloatStateOf(0f) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        dragAccumX = 0f
                        dragAccumY = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragAccumX += dragAmount.x
                        dragAccumY += dragAmount.y
                    },
                    onDragEnd = {
                        if (dragAccumY > 120f && abs(dragAccumY) > abs(dragAccumX) * 1.5f) {
                            onDismiss()
                        } else if (abs(dragAccumX) > 120f) {
                            if (dragAccumX < 0 && hasNext) {
                                onNext()
                            } else if (dragAccumX > 0 && (hasPrevious || pastRestartPoint)) {
                                onPrevious()
                            }
                        }
                    },
                )
            },
    ) {
        // Cover height ratio based on personalization setting
        val coverHeightFraction = when (customization.coverSize) {
            PlayerCoverSize.COMPACT -> 0.46f
            PlayerCoverSize.EXPANDED_DOWN -> 0.58f
            PlayerCoverSize.NORMAL -> 0.52f
        }

        // ---- 1. Full-bleed Edge-to-Edge Artwork ----
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(coverHeightFraction)
                .align(Alignment.TopCenter),
        ) {
            // Pulse & vinyl animations
            val coverPulseScale by animateFloatAsState(
                targetValue = if (isPlaying && customization.coverAnimation == PlayerCoverAnimation.SUBTLE_PULSE) 1.025f else 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2400, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "mochyCoverPulse",
            )
            val infiniteTransition = rememberInfiniteTransition(label = "mochyVinylRotation")
            val coverVinylRotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(18000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
                label = "mochyVinylAngle",
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        if (isPlaying && customization.coverAnimation == PlayerCoverAnimation.SUBTLE_PULSE) {
                            scaleX = coverPulseScale
                            scaleY = coverPulseScale
                        } else if (isPlaying && customization.coverAnimation == PlayerCoverAnimation.VINYL) {
                            rotationZ = coverVinylRotation
                        }
                    },
            ) {
                if (canvas != null) {
                    CanvasArtworkPlayer(
                        canvas = canvas,
                        isPlaying = isPlaying,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    AsyncImage(
                        model = art.request,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        onState = art::onState,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            // Liquid bottom fade: Seamlessly melts the artwork into the controls background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.50f)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.20f),
                                Color.Black.copy(alpha = 0.60f),
                                Color.Black.copy(alpha = 0.90f),
                                Color.Black,
                            ),
                        ),
                    ),
            )
        }

        // ---- 2. Top Header (Dismiss chevron and ⋯ options menu) ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(42.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Cerrar",
                    tint = Color.White,
                    modifier = Modifier.size(34.dp),
                )
            }

            IconButton(
                onClick = onOpenMenu,
                modifier = Modifier
                    .size(42.dp)
                    .menuAnchor(),
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreHoriz,
                    contentDescription = "Opciones",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp),
                )
            }
        }

        // ---- 3. Unified Bottom Controls Deck (Zero dead air) ----
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 12.dp)
                .offset(y = customization.controlsOffsetDp.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Track Info Row: Title, OPUS badge, Artist, Heart, Expand
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .clickable(enabled = song.albumId != null) {
                                    song.albumId?.let { onOpenAlbum(it) }
                                },
                        )
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color.White.copy(alpha = 0.18f),
                        ) {
                            Text(
                                text = codecBadge,
                                style = TextStyle(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.90f),
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }

                    Spacer(Modifier.height(3.dp))

                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                        ),
                        color = Color.White.copy(alpha = 0.70f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable {
                            onOpenArtist(song.artistId, song.artist)
                        },
                    )
                }

                Spacer(Modifier.width(12.dp))

                // Heart (Favorite) button
                val liked = likeStatus == LikeStatus.LIKE
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                        .clickable(onClick = onToggleLike),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (liked) BitChordIcons.HeartFilled else BitChordIcons.Heart,
                        contentDescription = if (liked) "Quitar de favoritos" else "Añadir a favoritos",
                        tint = if (liked) Color.White else Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(20.dp),
                    )
                }

                Spacer(Modifier.width(8.dp))

                // Expand/Options button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                        .clickable(onClick = onOpenMenu),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowUp,
                        contentDescription = "Opciones",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Scrubber & Timestamps
            if (customization.showProgressBar) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    ThinSlider(
                        value = shown(),
                        onValueChange = onScrub,
                        onValueChangeFinished = onScrubFinished,
                        idleHeight = 5.dp,
                        activeHeight = 9.dp,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (customization.showTimestamps) {
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            val currentMs = (shown() * durationMs).toLong().coerceIn(0L, durationMs)
                            Text(
                                text = formatTime(currentMs),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                ),
                                color = Color.White.copy(alpha = 0.70f),
                            )
                            Text(
                                text = formatTime(durationMs),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                ),
                                color = Color.White.copy(alpha = 0.70f),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Transport Controls (Previous, Play/Pause, Next)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onPrevious,
                    enabled = hasPrevious || pastRestartPoint,
                    modifier = Modifier.size(52.dp),
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_player_previous),
                        contentDescription = "Anterior",
                        tint = if (hasPrevious || pastRestartPoint) Color.White else Color.White.copy(alpha = 0.35f),
                        modifier = Modifier.size(34.dp),
                    )
                }

                Spacer(Modifier.width(36.dp))

                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.size(70.dp),
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(44.dp),
                            color = Color.White,
                            strokeWidth = 3.dp,
                        )
                    } else {
                        Icon(
                            painter = painterResource(if (isPlaying) Res.drawable.ic_player_pause else Res.drawable.ic_player_play),
                            contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                            tint = Color.White,
                            modifier = Modifier.size(54.dp),
                        )
                    }
                }

                Spacer(Modifier.width(36.dp))

                IconButton(
                    onClick = onNext,
                    enabled = hasNext,
                    modifier = Modifier.size(52.dp),
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_player_next),
                        contentDescription = "Siguiente",
                        tint = if (hasNext) Color.White else Color.White.copy(alpha = 0.35f),
                        modifier = Modifier.size(34.dp),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Volume Row
            if (customization.showVolumeBar) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.VolumeDown,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.55f),
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    ThinSlider(
                        value = volume.level.value,
                        onValueChange = onVolumeChange,
                        onValueChangeFinished = onVolumeChangeFinished,
                        idleHeight = 5.dp,
                        activeHeight = 9.dp,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(10.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.55f),
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.height(18.dp))
            }

            // Bottom Actions: Left Device Capsule + Right [ 💬 | ≡ ] Pill Capsule
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Left Capsule: Device output (e.g. Echo Dot-OR7)
                val outputName = rememberAudioOutputName(accountName)
                val outputs = rememberAudioOutputs()
                val activeDevice = outputs.firstOrNull { it.isActive }
                val outputIcon = activeDevice?.let { iconFor(it) } ?: Icons.Rounded.Speaker

                Surface(
                    onClick = onOpenOutput,
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = outputIcon,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(15.dp),
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            text = outputName,
                            style = TextStyle(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.90f),
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                // Right Capsule: [ Lyrics (💬) | Queue (≡) ]
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = onToggleLyrics,
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(
                                imageVector = BitChordIcons.LyricsQuote,
                                contentDescription = if (lyricsOpen) "Cerrar letras" else "Abrir letras",
                                tint = if (lyricsOpen) Color.White else Color.White.copy(alpha = 0.65f),
                                modifier = Modifier.size(18.dp),
                            )
                        }

                        Spacer(Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(14.dp)
                                .background(Color.White.copy(alpha = 0.22f)),
                        )
                        Spacer(Modifier.width(4.dp))

                        IconButton(
                            onClick = onToggleQueue,
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(
                                imageVector = BitChordIcons.Queue,
                                contentDescription = if (queueOpen) "Cerrar cola" else "Abrir cola",
                                tint = if (queueOpen) Color.White else Color.White.copy(alpha = 0.65f),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
