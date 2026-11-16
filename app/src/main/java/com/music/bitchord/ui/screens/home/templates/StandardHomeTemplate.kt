package com.music.bitchord.ui.screens.home.templates

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.music.bitchord.R
import com.music.bitchord.data.model.HomeShelf
import com.music.bitchord.data.model.ShelfItem
import com.music.bitchord.data.model.ShelfLayout
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.TopPick
import com.music.bitchord.data.model.TopPickMood
import com.music.bitchord.data.model.UiState
import com.music.bitchord.data.settings.HomeSectionConfig
import com.music.bitchord.data.settings.HomeSectionType
import com.music.bitchord.sharedui.resources.Res
import com.music.bitchord.sharedui.resources.retry
import com.music.bitchord.ui.components.MessageState
import com.music.bitchord.ui.components.SignInBanner
import com.music.bitchord.ui.components.feedMoreSkeleton
import com.music.bitchord.ui.components.feedSkeleton
import com.music.bitchord.ui.components.recentlyPlayedSkeleton
import com.music.bitchord.ui.screens.FindYourMoodShelf
import com.music.bitchord.ui.screens.HOME_TITLE_LIFT
import com.music.bitchord.ui.screens.HomeTitle
import com.music.bitchord.ui.screens.LocalArtworkMotion
import com.music.bitchord.ui.screens.RecentShelf
import com.music.bitchord.ui.screens.Shelf
import com.music.bitchord.ui.screens.TopPicksShelf
import com.music.bitchord.ui.screens.home.HomeLayoutResolver
import com.music.bitchord.ui.screens.topPicksSkeleton
import org.jetbrains.compose.resources.stringResource

/**
 * Template 1: Estándar Dinámica (DYNAMIC_STANDARD).
 * Preserves the rich dynamic feed with brand wordmark header, Top Picks portrait carousel,
 * Recents compact track column pager, Moods carousel, and dynamic shelves with localized titles.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StandardHomeTemplate(
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
    val displayTitle = title ?: stringResource(R.string.listen_now)

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = if (title != null) {
            PaddingValues(
                top = (contentPadding.calculateTopPadding() - HOME_TITLE_LIFT).coerceAtLeast(0.dp),
                bottom = contentPadding.calculateBottomPadding(),
            )
        } else {
            contentPadding
        },
    ) {
        // Brand wordmark header
        item(key = "header_title") {
            HomeTitle(
                title = displayTitle,
                festiveLogo = festiveLogo,
                onFestiveLogoClick = onFestiveLogoClick,
            )
        }

        // Account sign-in banner
        if (!signedIn && onSignIn != null) {
            item(key = "header_sign_in") {
                SignInBanner(
                    onSignIn = onSignIn,
                    modifier = Modifier.padding(bottom = 8.dp),
                    motion = LocalArtworkMotion.current && !listState.isScrollInProgress,
                )
            }
        }

        // Top Picks skeleton if loading and no items yet
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
