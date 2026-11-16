package com.music.bitchord.ui.screens.home.templates

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
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
import com.music.bitchord.sharedui.resources.ic_player_play
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
 * Template 3: Editorial Hero (EDITORIAL_HERO).
 * Inspired by Mochy HeroCarousel & Prism Hero:
 * - Immersive full-width hero banner at the top with large artwork spotlight card,
 *   ambient blurred backdrop, station badge ("Para ti", "Destacado"), title, subtitle, and play button.
 * - Curated spotlight shelves below with hero/grid styling.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorialHeroHomeTemplate(
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
    val featuredPick = topPicks?.firstOrNull()
    val featuredShelfItem = (state as? UiState.Success)?.data?.firstOrNull()?.items?.firstOrNull()

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        // Editorial Brand Header
        item(key = "editorial_header") {
            EditorialBrandBar(
                listenerName = listenerName,
                festiveLogo = festiveLogo,
                onFestiveLogoClick = onFestiveLogoClick,
            )
        }

        // Immersive Full-Width Hero Spotlight Banner
        if (featuredPick != null || featuredShelfItem != null) {
            item(key = "editorial_hero_banner") {
                EditorialHeroBanner(
                    featuredPick = featuredPick,
                    featuredShelfItem = featuredShelfItem,
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
 * Editorial top bar with brand mark and personalized station text.
 */
@Composable
private fun EditorialBrandBar(
    listenerName: String?,
    festiveLogo: Boolean,
    onFestiveLogoClick: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
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
                    .size(28.dp)
                    .clickable(enabled = onFestiveLogoClick != null) {
                        onFestiveLogoClick?.invoke()
                    },
            )
            Column {
                Text(
                    text = "Editorial Spotlight",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = if (!listenerName.isNullOrBlank()) "Edición especial para $listenerName" else "Música seleccionada para ti",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Large spotlight hero banner inspired by Mochy HeroCarousel and Prism Hero card.
 */
@Composable
private fun EditorialHeroBanner(
    featuredPick: TopPick?,
    featuredShelfItem: ShelfItem?,
    onTopPickClick: ((TopPick, String) -> Unit)?,
    onItemClick: (ShelfItem, String) -> Unit,
) {
    val title = featuredPick?.title ?: featuredShelfItem?.title ?: "Descubrimiento de la semana"
    val subtitle = featuredPick?.artistName ?: featuredShelfItem?.subtitle ?: "Selección especial BeDivine"
    val imageUrl = featuredPick?.images?.firstOrNull() ?: featuredShelfItem?.thumbnailUrl

    val onClick: () -> Unit = {
        if (featuredPick != null) {
            onTopPickClick?.invoke(featuredPick, title)
        } else if (featuredShelfItem != null) {
            onItemClick(featuredShelfItem, "Destacado")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .height(280.dp)
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
    ) {
        // Spotlight Artwork
        AsyncImage(
            model = imageUrl?.artworkAt(600),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        // Gradient Backdrop Scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.25f),
                            Color.Black.copy(alpha = 0.45f),
                            Color.Black.copy(alpha = 0.90f),
                        ),
                    ),
                ),
        )

        // Top Badge
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.90f),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
        ) {
            Text(
                text = "★ DESTACADO HERO",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }

        // Bottom Details & Play CTA
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier.weight(1f).padding(end = 12.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Big circular Play button
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .size(52.dp)
                    .clickable(onClick = onClick),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_player_play),
                        contentDescription = "Reproducir",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }
}
