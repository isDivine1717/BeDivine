package com.music.bitchord.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.music.bitchord.R
import com.music.bitchord.importer.AccountImportManager
import com.music.bitchord.importer.AccountService
import kotlin.math.ceil

/** Continuous superellipse squircle shape used for platform logos */
val SquircleShape = GenericShape { size, _ ->
    val cx = size.width / 2f
    val cy = size.height / 2f
    val rx = size.width / 2f
    val ry = size.height / 2f
    val n = 5.0
    val steps = 72
    for (i in 0..steps) {
        val t = 2 * Math.PI * i / steps
        val cos = Math.cos(t)
        val sin = Math.sin(t)
        val x = cx + rx * Math.signum(cos) * Math.pow(Math.abs(cos), 2.0 / n)
        val y = cy + ry * Math.signum(sin) * Math.pow(Math.abs(sin), 2.0 / n)
        if (i == 0) moveTo(x.toFloat(), y.toFloat()) else lineTo(x.toFloat(), y.toFloat())
    }
    close()
}

@Composable
fun ProviderSquircle(
    service: AccountService,
    size: Dp = 40.dp,
    modifier: Modifier = Modifier,
) {
    val (drawable, background, circular, zoom) = when (service) {
        AccountService.SPOTIFY -> Quadruple(
            R.drawable.provider_spotify,
            SolidColor(Color(0xFF191414)),
            false,
            1.0f,
        )
        AccountService.TIDAL -> Quadruple(
            R.drawable.provider_tidal,
            SolidColor(Color.Black),
            true,
            1.06f,
        )
        AccountService.YOUTUBE_MUSIC -> Quadruple(
            R.drawable.provider_youtube_music,
            SolidColor(Color(0xFFFF0000)),
            false,
            1.06f,
        )
        AccountService.APPLE_MUSIC -> Quadruple(
            R.drawable.provider_apple_music,
            Brush.verticalGradient(listOf(Color(0xFFFA5C70), Color(0xFFFA2D48))),
            false,
            1.1f,
        )
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(SquircleShape)
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(drawable),
            contentDescription = service.label,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .then(if (circular) Modifier.clip(CircleShape) else Modifier)
                .scale(zoom),
        )
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

enum class LibraryTab(val label: String) {
    All("All"),
    Local("Local"),
    Cached("Cached"),
    Imported("Imported"),
}

private val ChipTrackColor = Color(0xFF161618)
private val ChipInactiveColor = Color(0xFF232326)
private val ChipActiveColor = Color(0xFF3D3D41)

@Composable
fun LibraryTabChips(
    selected: LibraryTab,
    onSelect: (LibraryTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(percent = 50))
            .background(ChipTrackColor)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        LibraryTab.entries.forEach { tab ->
            val isSelected = tab == selected
            val animatedBg by animateColorAsState(
                targetValue = if (isSelected) ChipActiveColor else ChipInactiveColor,
                animationSpec = tween(200),
                label = "chipBg",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(animatedBg)
                    .clickable { onSelect(tab) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = tab.label,
                    color = if (isSelected) Color.White else Color(0xFFB0B0B4),
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
fun LibrarySearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = "Search your library",
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(23.dp))
            .background(Color(0xFF1C1C1E))
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = Color(0xFF8E8E93),
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = Color(0xFF8E8E93),
                        fontSize = 15.sp,
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (query.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Clear search",
                    tint = Color(0xFF8E8E93),
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .clickable { onQueryChange("") },
                )
            }
        }
    }
}

private val BannerShape = RoundedCornerShape(24.dp)
private val BannerServices = listOf(
    AccountService.SPOTIFY,
    AccountService.APPLE_MUSIC,
    AccountService.TIDAL,
    AccountService.YOUTUBE_MUSIC,
)

@Composable
fun LibraryImportBanner(
    onOpenImport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress by AccountImportManager.progress.collectAsStateWithLifecycle()

    Crossfade(
        targetState = progress.isRunning,
        label = "libraryImportBanner",
        modifier = modifier.fillMaxWidth(),
    ) { running ->
        if (running) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(BannerShape)
                    .background(Color(0xFF18181A))
                    .padding(20.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Importing playlist...",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = progress.statusMessage.ifBlank { "Processing tracks..." },
                    color = Color(0xFFB0B0B4),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(12.dp))
                val fraction = if (progress.totalTracks > 0) {
                    (progress.doneTracks.toFloat() / progress.totalTracks.toFloat()).coerceIn(0f, 1f)
                } else 0f
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFF8A5DF6),
                    trackColor = Color(0xFF28282D),
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${progress.doneTracks} / ${progress.totalTracks} songs matched",
                    color = Color(0xFF8E8E93),
                    fontSize = 12.sp,
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(BannerShape)
                    .background(Color(0xFF18181A))
                    .clickable(role = Role.Button, onClick = onOpenImport)
                    .padding(vertical = 20.dp),
            ) {
                ProviderMarqueeStrip()
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "Import your music here!",
                    color = Color.White,
                    fontSize = 21.sp,
                    lineHeight = 25.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 22.dp),
                )
            }
        }
    }
}

@Composable
private fun ProviderMarqueeStrip(modifier: Modifier = Modifier) {
    val tile = 54.dp
    val gap = 14.dp
    val setWidth = (tile + gap) * BannerServices.size
    val setPx = with(LocalDensity.current) { setWidth.toPx() }

    val travel = rememberInfiniteTransition(label = "providerMarquee").animateFloat(
        initialValue = 0f,
        targetValue = setPx,
        animationSpec = infiniteRepeatable(tween(2600 * BannerServices.size, easing = LinearEasing)),
        label = "providerMarqueeTravel",
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(tile)
            .clipToBounds(),
    ) {
        val sets = ceil(maxWidth / setWidth).toInt() + 1
        Row(
            modifier = Modifier
                .wrapContentWidth(align = Alignment.Start, unbounded = true)
                .graphicsLayer { translationX = -travel.value },
            horizontalArrangement = Arrangement.spacedBy(gap),
        ) {
            repeat(sets) {
                BannerServices.forEach { service ->
                    ProviderSquircle(service = service, size = tile)
                }
            }
        }
    }
}

data class LibraryCategoryEntry(
    val title: String,
    val count: Int,
    val covers: List<String?>,
    val onClick: () -> Unit,
    val roundCovers: Boolean = false,
)

@Composable
fun LibraryCardGrid(
    entries: List<LibraryCategoryEntry>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        entries.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                pair.forEach { entry ->
                    LibraryCategoryCard(
                        title = entry.title,
                        count = entry.count,
                        covers = entry.covers,
                        onClick = entry.onClick,
                        roundCovers = entry.roundCovers,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun LibraryCategoryCard(
    title: String,
    count: Int,
    covers: List<String?>,
    onClick: () -> Unit,
    roundCovers: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(78.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1C1C1E))
            .clickable(onClick = onClick),
    ) {
        LibraryCoverStack(
            covers = covers,
            round = roundCovers,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 10.dp, y = 12.dp),
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 14.dp, top = 11.dp, end = 12.dp),
        ) {
            Text(
                text = title,
                color = Color(0xFF8E8E93),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = count.toString(),
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun LibraryCoverStack(
    covers: List<String?>,
    round: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val nonNull = covers.filterNotNull().take(3)
    if (nonNull.isEmpty()) return

    Box(modifier = modifier.size(60.dp)) {
        nonNull.forEachIndexed { index, url ->
            val offsetStep = (index * 6).dp
            val shape = if (round) CircleShape else RoundedCornerShape(8.dp)
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(44.dp)
                    .offset(x = -offsetStep, y = -offsetStep)
                    .clip(shape)
                    .border(1.5.dp, Color(0xFF1C1C1E), shape),
            )
        }
    }
}
