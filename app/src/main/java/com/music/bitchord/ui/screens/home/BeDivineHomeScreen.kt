package com.music.bitchord.ui.screens.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.bitchord.data.model.HomeShelf
import com.music.bitchord.data.model.ShelfItem
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.TopPick
import com.music.bitchord.data.model.TopPickMood
import com.music.bitchord.data.model.UiState
import com.music.bitchord.data.settings.HomeCustomizationStore
import com.music.bitchord.data.settings.HomeTemplate
import com.music.bitchord.ui.components.PullToRefresh
import com.music.bitchord.ui.screens.LocalArtworkImages
import com.music.bitchord.ui.screens.LocalArtworkMotion
import com.music.bitchord.ui.screens.PausedWhileScrolling
import com.music.bitchord.ui.screens.home.templates.EditorialHeroHomeTemplate
import com.music.bitchord.ui.screens.home.templates.MochyCompactHomeTemplate
import com.music.bitchord.ui.screens.home.templates.StandardHomeTemplate

/**
 * Unified customizable Home Screen for BeDivine.
 * Reactively observes [HomeCustomizationStore.state] to dynamically render
 * the configured template ([HomeTemplate.DYNAMIC_STANDARD], [HomeTemplate.MOCHY_COMPACT], [HomeTemplate.EDITORIAL_HERO])
 * and order/filter visible sections with stable item keys.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeDivineHomeScreen(
    state: UiState<List<HomeShelf>>,
    listState: LazyListState,
    onItemClick: (ShelfItem, String) -> Unit,
    onRetry: () -> Unit,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    pullState: PullToRefreshState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    title: String? = null,
    festiveLogo: Boolean = false,
    onFestiveLogoClick: (() -> Unit)? = null,
    signedIn: Boolean = true,
    onSignIn: (() -> Unit)? = null,
    onItemLongPress: ((ShelfItem) -> Unit)? = null,
    onLoadMore: (() -> Unit)? = null,
    loadingMore: Boolean = false,
    recentlyPlayedLoading: Boolean = false,
    currentSong: Song? = null,
    isPlaying: Boolean = false,
    topPicks: List<TopPick>? = null,
    topPicksLoading: Boolean = false,
    listenerName: String? = null,
    onTopPickClick: ((TopPick, String) -> Unit)? = null,
    onTopPickLongPress: ((TopPick) -> Unit)? = null,
    onMoodClick: ((TopPickMood, String) -> Unit)? = null,
    onMoodsShowAll: () -> Unit = {},
    artworkMotion: Boolean = true,
) {
    val customization by HomeCustomizationStore.state.collectAsStateWithLifecycle()
    val visibleSections = remember(customization.sections) {
        HomeLayoutResolver.resolveActiveSections(customization.sections)
    }

    val feedReady = state !is UiState.Loading
    CompositionLocalProvider(
        LocalArtworkMotion provides (artworkMotion && feedReady),
        LocalArtworkImages provides feedReady,
    ) {
        PausedWhileScrolling(listState) {
            PullToRefresh(
                refreshing = refreshing,
                onRefresh = onRefresh,
                state = pullState,
                modifier = modifier,
            ) {
                when (customization.template) {
                    HomeTemplate.DYNAMIC_STANDARD -> StandardHomeTemplate(
                        state = state,
                        listState = listState,
                        visibleSections = visibleSections,
                        contentPadding = contentPadding,
                        title = title,
                        festiveLogo = festiveLogo,
                        onFestiveLogoClick = onFestiveLogoClick,
                        signedIn = signedIn,
                        onSignIn = onSignIn,
                        topPicks = topPicks,
                        topPicksLoading = topPicksLoading,
                        listenerName = listenerName,
                        recentlyPlayedLoading = recentlyPlayedLoading,
                        currentSong = currentSong,
                        isPlaying = isPlaying,
                        loadingMore = loadingMore,
                        onItemClick = onItemClick,
                        onItemLongPress = onItemLongPress,
                        onTopPickClick = onTopPickClick,
                        onTopPickLongPress = onTopPickLongPress,
                        onMoodClick = onMoodClick,
                        onMoodsShowAll = onMoodsShowAll,
                        onRetry = onRetry,
                    )
                    HomeTemplate.MOCHY_COMPACT -> MochyCompactHomeTemplate(
                        state = state,
                        listState = listState,
                        visibleSections = visibleSections,
                        contentPadding = contentPadding,
                        title = title,
                        festiveLogo = festiveLogo,
                        onFestiveLogoClick = onFestiveLogoClick,
                        signedIn = signedIn,
                        onSignIn = onSignIn,
                        topPicks = topPicks,
                        topPicksLoading = topPicksLoading,
                        listenerName = listenerName,
                        recentlyPlayedLoading = recentlyPlayedLoading,
                        currentSong = currentSong,
                        isPlaying = isPlaying,
                        loadingMore = loadingMore,
                        onItemClick = onItemClick,
                        onItemLongPress = onItemLongPress,
                        onTopPickClick = onTopPickClick,
                        onTopPickLongPress = onTopPickLongPress,
                        onMoodClick = onMoodClick,
                        onMoodsShowAll = onMoodsShowAll,
                        onRetry = onRetry,
                    )
                    HomeTemplate.EDITORIAL_HERO -> EditorialHeroHomeTemplate(
                        state = state,
                        listState = listState,
                        visibleSections = visibleSections,
                        contentPadding = contentPadding,
                        title = title,
                        festiveLogo = festiveLogo,
                        onFestiveLogoClick = onFestiveLogoClick,
                        signedIn = signedIn,
                        onSignIn = onSignIn,
                        topPicks = topPicks,
                        topPicksLoading = topPicksLoading,
                        listenerName = listenerName,
                        recentlyPlayedLoading = recentlyPlayedLoading,
                        currentSong = currentSong,
                        isPlaying = isPlaying,
                        loadingMore = loadingMore,
                        onItemClick = onItemClick,
                        onItemLongPress = onItemLongPress,
                        onTopPickClick = onTopPickClick,
                        onTopPickLongPress = onTopPickLongPress,
                        onMoodClick = onMoodClick,
                        onMoodsShowAll = onMoodsShowAll,
                        onRetry = onRetry,
                    )
                }
            }
        }
    }

    if (onLoadMore != null && state is UiState.Success) {
        val loadMore by rememberUpdatedState(onLoadMore)
        LaunchedEffect(listState, loadingMore) {
            snapshotFlow {
                val layout = listState.layoutInfo
                (layout.visibleItemsInfo.lastOrNull()?.index ?: -1) to layout.totalItemsCount
            }.collect { (lastVisible, total) ->
                if (!loadingMore && total > 0 && lastVisible >= total - 3) loadMore()
            }
        }
    }
}
