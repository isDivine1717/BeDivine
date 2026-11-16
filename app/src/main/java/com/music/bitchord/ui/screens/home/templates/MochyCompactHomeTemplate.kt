package com.music.bitchord.ui.screens.home.templates

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.music.bitchord.data.model.HomeShelf
import com.music.bitchord.data.model.ShelfItem
import com.music.bitchord.data.model.ShelfLayout
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.TopPick
import com.music.bitchord.data.model.TopPickMood
import com.music.bitchord.data.model.UiState
import com.music.bitchord.data.model.artworkAt
import com.music.bitchord.data.settings.HomeSectionConfig
import com.music.bitchord.data.settings.HomeSectionType
import com.music.bitchord.sharedui.resources.Res
import com.music.bitchord.sharedui.resources.ic_logo
import com.music.bitchord.sharedui.resources.retry
import com.music.bitchord.ui.components.MessageState
import com.music.bitchord.ui.components.SignInBanner
import com.music.bitchord.ui.components.feedMoreSkeleton
import com.music.bitchord.ui.components.feedSkeleton
import com.music.bitchord.ui.components.recentlyPlayedSkeleton
import com.music.bitchord.ui.screens.FindYourMoodShelf
import com.music.bitchord.ui.screens.LocalArtworkMotion
import com.music.bitchord.ui.screens.RecentShelf
import com.music.bitchord.ui.screens.Shelf
import com.music.bitchord.ui.screens.TopPicksShelf
import com.music.bitchord.ui.screens.home.HomeLayoutResolver
import com.music.bitchord.ui.screens.topPicksSkeleton
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Template 2: Mochy Compacto (MOCHY_COMPACT).
 * Inspired by Mochy-Music:
 * - Compact ambient header with soft top glow.
 * - Quick-access horizontal pill cards grid (2x2 or 2x3 squircle cards) for quick 1-tap playback into Top Picks / Recents / Playlists.
 * - High-density shelf presentation and compact track rows.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MochyCompactHomeTemplate(
    state: UiState<List<HomeShelf>>,
    listState: LazyListState,
    visibleSections: List<HomeSectionConfig>,
    contentPadding: PaddingValues,
    title: String?,
    festiveLogo: Boolean,
    onFestiveLogoClick: (() -> Unit)?,
    signedIn: Boolean,
    onSignIn: (() -> Unit)?,
    topPicks: List<TopPick>?,
    topPicksLoading: Boolean,
    listenerName: String?,
    recentlyPlayedLoading: Boolean,
    currentSong: Song?,
    isPlaying: Boolean,
    loadingMore: Boolean,
    onItemClick: (ShelfItem, String) -> Unit,
    onItemLongPress: ((ShelfItem) -> Unit)?,
    onTopPickClick: ((TopPick, String) -> Unit)?,
    onTopPickLongPress: ((TopPick) -> Unit)?,
    onMoodClick: ((TopPickMood, String) -> Unit)?,
    onMoodsShowAll: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        // Compact Ambient Header with soft top glow
        item(key = "mochy_compact_header") {
            MochyCompactHeader(
                listenerName = listenerName,
                festiveLogo = festiveLogo,
                onFestiveLogoClick = onFestiveLogoClick,
            )
        }

        // Quick-access 2x2 / 2x3 squircle pill cards
        if (!topPicks.isNullOrEmpty() || (state as? UiState.Success)?.data?.isNotEmpty() == true) {
            item(key = "mochy_quick_access_grid") {
                val recentItems = (state as? UiState.Success)?.data?.firstOrNull {
                    it.layout == ShelfLayout.LIST || it.title.equals("recents", ignoreCase = true)
                }?.items.orEmpty()

                MochyQuickAccessGrid(
                    picks = topPicks.orEmpty(),
                    recentItems = recentItems,
                    onTopPickClick = onTopPickClick,
                    onItemClick = onItemClick,
                )
            }
        }

        // Account sign-in banner
        if (!signedIn && onSignIn != null) {
            item(key = "header_sign_in") {
                SignInBanner(
                    onSignIn = onSignIn,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    motion = LocalArtworkMotion.current && !listState.isScrollInProgress,
                )
            }
        }

        if (onTopPickClick != null && topPicks == null && topPicksLoading) {
            topPicksSkeleton()
        }

        when (state) {
            is UiState.Loading -> {
                if (recentlyPlayedLoading) {
                    recentlyPlayedSkeleton()
                }
                feedSkeleton()
            }
            is UiState.Error -> {
                item(key = "feed_error") {
                    MessageState(
                        message = state.message,
                        actionLabel = stringResource(Res.string.retry),
                        onAction = onRetry,
                    )
                }
            }
            is UiState.Success -> {
                if (recentlyPlayedLoading) {
                    recentlyPlayedSkeleton()
                }

                val shelves = state.data
                val groupedShelves = HomeLayoutResolver.groupShelvesBySectionType(shelves)
                val recentShelf = shelves.firstOrNull {
                    it.layout == ShelfLayout.LIST ||
                        it.title.equals("recents", ignoreCase = true) ||
                        it.title.equals("recently played", ignoreCase = true) ||
                        it.title.equals("gần đây", ignoreCase = true)
                }

                for (section in visibleSections) {
                    when (section.type) {
                        HomeSectionType.TOP_PICKS -> {
                            if (onTopPickClick != null && !topPicks.isNullOrEmpty()) {
                                item(key = section.type.id) {
                                    TopPicksShelf(
                                        picks = topPicks,
                                        listenerName = listenerName,
                                        onClick = onTopPickClick,
                                        onLongPress = onTopPickLongPress,
                                    )
                                }
                            }
                        }
                        HomeSectionType.RECENTS -> {
                            if (recentShelf != null) {
                                item(key = section.type.id) {
                                    RecentShelf(
                                        shelf = recentShelf,
                                        onItemClick = { item -> onItemClick(item, recentShelf.title) },
                                        onItemLongPress = onItemLongPress,
                                        currentSong = currentSong,
                                        isPlaying = isPlaying,
                                    )
                                }
                            }
                        }
                        HomeSectionType.MOODS -> {
                            if (onMoodClick != null) {
                                item(key = section.type.id) {
                                    FindYourMoodShelf(
                                        onClick = onMoodClick,
                                        onShowAll = onMoodsShowAll,
                                    )
                                }
                            }
                        }
                        else -> {
                            val matchedShelves = groupedShelves[section.type].orEmpty()
                            if (matchedShelves.isNotEmpty()) {
                                item(key = section.type.id) {
                                    Column {
                                        for (shelf in matchedShelves) {
                                            Shelf(
                                                shelf = shelf,
                                                onItemClick = { item -> onItemClick(item, shelf.title) },
                                                onItemLongPress = onItemLongPress,
                                                currentSong = currentSong,
                                                isPlaying = isPlaying,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (loadingMore) {
                    feedMoreSkeleton()
                }
            }
        }
    }
}

/**
 * Compact ambient header with subtle radial/linear glow.
 */
@Composable
private fun MochyCompactHeader(
    listenerName: String?,
    festiveLogo: Boolean,
    onFestiveLogoClick: (() -> Unit)?,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                        Color.Transparent,
                    ),
                ),
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_logo),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(32.dp)
                        .clickable(enabled = onFestiveLogoClick != null) {
                            onFestiveLogoClick?.invoke()
                        },
                )
                Column {
                    Text(
                        text = "BeDivine",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = if (!listenerName.isNullOrBlank()) "Para $listenerName" else "Mochy Compacto",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            ) {
                Text(
                    text = "Compacto",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
    }
}

/**
 * 2x2 or 2x3 quick-access squircle pill cards for 1-tap playback (Mochy Hop In style).
 */
@Composable
private fun MochyQuickAccessGrid(
    picks: List<TopPick>,
    recentItems: List<ShelfItem>,
    onTopPickClick: ((TopPick, String) -> Unit)?,
    onItemClick: (ShelfItem, String) -> Unit,
) {
    // Take up to 4 quick items from top picks and recent items
    val quickPicks = picks.take(2)
    val quickRecent = recentItems.take(2)

    if (quickPicks.isEmpty() && quickRecent.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Row 1
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            quickPicks.getOrNull(0)?.let { pick ->
                MochyPillCard(
                    title = pick.title,
                    subtitle = pick.artistName ?: "Estación",
                    imageUrl = pick.images.firstOrNull(),
                    onClick = { onTopPickClick?.invoke(pick, pick.title) },
                    modifier = Modifier.weight(1f),
                )
            }
            quickPicks.getOrNull(1)?.let { pick ->
                MochyPillCard(
                    title = pick.title,
                    subtitle = pick.artistName ?: "Estación",
                    imageUrl = pick.images.firstOrNull(),
                    onClick = { onTopPickClick?.invoke(pick, pick.title) },
                    modifier = Modifier.weight(1f),
                )
            } ?: quickRecent.getOrNull(0)?.let { item ->
                MochyPillCard(
                    title = item.title,
                    subtitle = item.subtitle,
                    imageUrl = item.thumbnailUrl,
                    onClick = { onItemClick(item, "Recientes") },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // Row 2 (if enough items)
        if (quickRecent.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                quickRecent.getOrNull(0)?.let { item ->
                    MochyPillCard(
                        title = item.title,
                        subtitle = item.subtitle,
                        imageUrl = item.thumbnailUrl,
                        onClick = { onItemClick(item, "Recientes") },
                        modifier = Modifier.weight(1f),
                    )
                }
                quickRecent.getOrNull(1)?.let { item ->
                    MochyPillCard(
                        title = item.title,
                        subtitle = item.subtitle,
                        imageUrl = item.thumbnailUrl,
                        onClick = { onItemClick(item, "Recientes") },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun MochyPillCard(
    title: String,
    subtitle: String,
    imageUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        modifier = modifier.height(54.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                AsyncImage(
                    model = imageUrl?.artworkAt(108),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
