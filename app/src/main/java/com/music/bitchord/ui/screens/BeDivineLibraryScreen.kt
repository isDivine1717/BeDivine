package com.music.bitchord.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.music.bitchord.R
import com.music.bitchord.data.model.HomeShelf
import com.music.bitchord.data.model.LibraryPage
import com.music.bitchord.data.model.ShelfItem
import com.music.bitchord.data.model.UiState
import com.music.bitchord.data.spotify.LocalPlaylistStore
import com.music.bitchord.importer.AccountService
import com.music.bitchord.sharedui.resources.Res
import com.music.bitchord.sharedui.resources.library
import com.music.bitchord.sharedui.resources.library_sign_in_description
import com.music.bitchord.sharedui.resources.on_device
import com.music.bitchord.sharedui.resources.retry
import com.music.bitchord.sharedui.resources.sign_in
import com.music.bitchord.ui.AppUi
import com.music.bitchord.ui.components.LibraryCardGrid
import com.music.bitchord.ui.components.LibraryCategoryEntry
import com.music.bitchord.ui.components.LibraryImportBanner
import com.music.bitchord.ui.components.LibrarySearchBar
import com.music.bitchord.ui.components.LibraryTab
import com.music.bitchord.ui.components.LibraryTabChips
import com.music.bitchord.ui.components.MessageState
import com.music.bitchord.ui.components.PAGE_GUTTER
import com.music.bitchord.ui.components.ProviderSquircle
import com.music.bitchord.ui.components.PullToRefresh
import com.music.bitchord.ui.components.librarySkeleton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeDivineLibraryScreen(
    signedIn: Boolean,
    state: UiState<LibraryPage>,
    listState: LazyListState,
    onShelfItemClick: (ShelfItem) -> Unit,
    onShelfItemLongPress: (ShelfItem) -> Unit,
    onNewPlaylist: () -> Unit,
    onOpenImport: () -> Unit,
    onShowAll: (HomeShelf) -> Unit,
    replay: @Composable () -> Unit,
    onSignIn: () -> Unit,
    onRetry: () -> Unit,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    pullState: PullToRefreshState,
    contentPadding: PaddingValues,
    links: List<LibraryLink>,
    deviceItems: List<ShelfItem>,
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember { mutableStateOf(LibraryTab.All) }
    var searchQuery by remember { mutableStateOf("") }

    val onDevice = org.jetbrains.compose.resources.stringResource(Res.string.on_device)
    val libraryTitle = org.jetbrains.compose.resources.stringResource(Res.string.library)
    val signInDesc = org.jetbrains.compose.resources.stringResource(Res.string.library_sign_in_description)
    val signInAction = org.jetbrains.compose.resources.stringResource(Res.string.sign_in)
    val retryAction = org.jetbrains.compose.resources.stringResource(Res.string.retry)

    val pinnedPlaylists by AppUi.host.pinnedPlaylists.collectAsStateWithLifecycle()
    val localPlaylists by LocalPlaylistStore.playlists.collectAsStateWithLifecycle()

    val shelves = (state as? UiState.Success)?.data?.shelves.orEmpty()
    val playlistShelf = shelves.firstOrNull { it.title.equals("Playlists", ignoreCase = true) }
    val albumShelf = shelves.firstOrNull { it.title.equals("Albums", ignoreCase = true) }
    val artistShelf = shelves.firstOrNull { it.title.equals("Artists", ignoreCase = true) }
    val songsShelf = shelves.firstOrNull { it.title.equals("Songs", ignoreCase = true) }

    val totalPlaylistsCount = (playlistShelf?.items?.size ?: 0) + localPlaylists.size + deviceItems.size
    val totalAlbumsCount = albumShelf?.items?.size ?: 0
    val totalArtistsCount = artistShelf?.items?.size ?: 0
    val totalSongsCount = localPlaylists.sumOf { it.songs.size } + (songsShelf?.items?.size ?: 0)

    val playlistCovers = (localPlaylists.mapNotNull { it.coverUrl ?: it.songs.firstOrNull()?.thumbnailUrl } +
            (playlistShelf?.items?.mapNotNull { it.thumbnailUrl } ?: emptyList())).take(4)
    val albumCovers = albumShelf?.items?.mapNotNull { it.thumbnailUrl }?.take(4) ?: emptyList()
    val artistCovers = artistShelf?.items?.mapNotNull { it.thumbnailUrl }?.take(4) ?: emptyList()
    val songCovers = localPlaylists.flatMap { it.songs }.mapNotNull { it.thumbnailUrl }.take(4)

    val categoryGridEntries = listOf(
        LibraryCategoryEntry(
            title = "Songs",
            count = totalSongsCount,
            covers = songCovers,
            onClick = {
                val localAll = links.firstOrNull { it.item.browseId == "local:all" }?.item
                val localDownloads = links.firstOrNull { it.item.browseId == "local:downloads" }?.item
                val songsItem = songsShelf?.items?.firstOrNull()
                when {
                    songsShelf != null && songsShelf.items.isNotEmpty() -> onShowAll(songsShelf)
                    localAll != null -> onShelfItemClick(localAll)
                    localDownloads != null -> onShelfItemClick(localDownloads)
                    songsItem != null -> onShelfItemClick(songsItem)
                    localPlaylists.isNotEmpty() -> onShelfItemClick(
                        ShelfItem(
                            title = localPlaylists.first().title,
                            subtitle = "Local Playlist",
                            thumbnailUrl = localPlaylists.first().coverUrl,
                            videoId = null,
                            browseId = localPlaylists.first().browseId,
                        )
                    )
                }
            },
        ),
        LibraryCategoryEntry(
            title = "Playlists",
            count = totalPlaylistsCount,
            covers = playlistCovers,
            onClick = {
                val localItems = localPlaylists.map {
                    ShelfItem(
                        title = it.title,
                        subtitle = "${it.songs.size} tracks",
                        thumbnailUrl = it.coverUrl ?: it.songs.firstOrNull()?.thumbnailUrl,
                        videoId = null,
                        browseId = it.browseId,
                    )
                }
                val allPlaylistItems = localItems + (playlistShelf?.items ?: emptyList())
                onShowAll(HomeShelf(title = "Playlists", items = allPlaylistItems))
            },
        ),
        LibraryCategoryEntry(
            title = "Albums",
            count = totalAlbumsCount,
            covers = albumCovers,
            onClick = {
                onShowAll(albumShelf ?: HomeShelf(title = "Albums", items = emptyList()))
            },
        ),
        LibraryCategoryEntry(
            title = "Artists",
            count = totalArtistsCount,
            covers = artistCovers,
            roundCovers = true,
            onClick = {
                onShowAll(artistShelf ?: HomeShelf(title = "Artists", items = emptyList()))
            },
        ),
    )

    PullToRefresh(
        refreshing = refreshing,
        onRefresh = onRefresh,
        state = pullState,
        modifier = modifier,
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
        ) {
            // 1. Large Library Title
            item(key = "library_heading") {
                Text(
                    text = libraryTitle,
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = PAGE_GUTTER, vertical = 8.dp),
                )
            }

            // 2. Replay Hero Cards ("TU EXPERIENCIA MUSICAL" - 4 cards at the top)
            item(key = "replay") {
                replay()
                Spacer(modifier = Modifier.height(10.dp))
            }

            // 3. Search your library Bar
            item(key = "library_search_bar") {
                LibrarySearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    modifier = Modifier.padding(horizontal = PAGE_GUTTER, vertical = 6.dp),
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // 4. Tab Chips: All | Local | Cached | Imported
            item(key = "library_tab_chips") {
                LibraryTabChips(
                    selected = selectedTab,
                    onSelect = { selectedTab = it },
                    modifier = Modifier.padding(horizontal = PAGE_GUTTER),
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Search Filter Mode
            if (searchQuery.isNotBlank()) {
                val q = searchQuery.trim().lowercase()
                val matchedPlaylists = (localPlaylists.filter { it.title.lowercase().contains(q) }.map {
                    ShelfItem(
                        title = it.title,
                        subtitle = "${it.songs.size} tracks • ${it.provider?.replaceFirstChar { c -> c.uppercase() } ?: "Local"}",
                        thumbnailUrl = it.coverUrl ?: it.songs.firstOrNull()?.thumbnailUrl,
                        videoId = null,
                        browseId = it.browseId,
                    )
                } + (playlistShelf?.items?.filter { it.title.lowercase().contains(q) } ?: emptyList()))

                val matchedAlbums = albumShelf?.items?.filter { it.title.lowercase().contains(q) } ?: emptyList()
                val matchedArtists = artistShelf?.items?.filter { it.title.lowercase().contains(q) } ?: emptyList()

                if (matchedPlaylists.isEmpty() && matchedAlbums.isEmpty() && matchedArtists.isEmpty()) {
                    item(key = "search_empty") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "No library items matching \"$searchQuery\"",
                                color = Color(0xFF8E8E93),
                                fontSize = 15.sp,
                            )
                        }
                    }
                } else {
                    if (matchedPlaylists.isNotEmpty()) {
                        item(key = "matched_playlists_header") {
                            Text(
                                text = "Playlists",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = PAGE_GUTTER, vertical = 8.dp),
                            )
                        }
                        items(matchedPlaylists) { item ->
                            LibrarySearchResultRow(item = item, onClick = { onShelfItemClick(item) })
                        }
                    }
                    if (matchedAlbums.isNotEmpty()) {
                        item(key = "matched_albums_header") {
                            Text(
                                text = "Albums",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = PAGE_GUTTER, vertical = 8.dp),
                            )
                        }
                        items(matchedAlbums) { item ->
                            LibrarySearchResultRow(item = item, onClick = { onShelfItemClick(item) })
                        }
                    }
                    if (matchedArtists.isNotEmpty()) {
                        item(key = "matched_artists_header") {
                            Text(
                                text = "Artists",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = PAGE_GUTTER, vertical = 8.dp),
                            )
                        }
                        items(matchedArtists) { item ->
                            LibrarySearchResultRow(item = item, onClick = { onShelfItemClick(item) }, isArtist = true)
                        }
                    }
                }
                return@LazyColumn
            }

            // Normal Tab Content (when not searching)
            when (selectedTab) {
                LibraryTab.All -> {
                    // Import Marquee Banner
                    item(key = "import_banner") {
                        LibraryImportBanner(
                            onOpenImport = onOpenImport,
                            modifier = Modifier.padding(horizontal = PAGE_GUTTER),
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // 2x2 Grid of Category Cards (Songs, Playlists, Albums, Artists)
                    item(key = "category_grid") {
                        LibraryCardGrid(
                            entries = categoryGridEntries,
                            modifier = Modifier.padding(horizontal = PAGE_GUTTER),
                        )
                        Spacer(modifier = Modifier.height(28.dp))
                    }

                    if (!signedIn) {
                        item(key = "sign_in_prompt") {
                            MessageState(
                                message = signInDesc,
                                actionLabel = signInAction,
                                onAction = onSignIn,
                            )
                        }
                    }
                }

                LibraryTab.Local -> {
                    // Local folders (Downloads, Device Music)
                    if (links.isNotEmpty()) {
                        item(key = "local_links") {
                            LibraryLinkList(links = links, onClick = onShelfItemClick)
                        }
                    }
                    if (deviceItems.isNotEmpty()) {
                        item(key = "local_device_items") {
                            val onDeviceShelf = HomeShelf(title = "Local Releases", items = deviceItems)
                            LibraryGridShelf(
                                shelf = onDeviceShelf,
                                onItemClick = onShelfItemClick,
                                onItemLongPress = onShelfItemLongPress,
                                onShowAll = { onShowAll(onDeviceShelf) },
                            )
                        }
                    } else {
                        item(key = "local_empty") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "No local music found on device",
                                    color = Color(0xFF8E8E93),
                                    fontSize = 15.sp,
                                )
                            }
                        }
                    }
                }

                LibraryTab.Cached -> {
                    val cachedLink = links.firstOrNull { it.item.browseId == "local:cache" || it.item.browseId?.contains("cache") == true }
                    if (cachedLink != null) {
                        item(key = "cached_link") {
                            LibraryLinkList(links = listOf(cachedLink), onClick = onShelfItemClick)
                        }
                    } else {
                        item(key = "cached_info") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "Cached songs are stored automatically as you stream music.",
                                    color = Color(0xFF8E8E93),
                                    fontSize = 15.sp,
                                )
                            }
                        }
                    }
                }

                LibraryTab.Imported -> {
                    item(key = "imported_banner") {
                        LibraryImportBanner(
                            onOpenImport = onOpenImport,
                            modifier = Modifier.padding(horizontal = PAGE_GUTTER),
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                    }

                    if (localPlaylists.isEmpty()) {
                        item(key = "imported_empty") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = "No playlists imported yet",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Tap the banner above to sync and import playlists from Spotify, Apple Music, Tidal, or YouTube Music.",
                                    color = Color(0xFF8E8E93),
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                )
                            }
                        }
                    } else {
                        item(key = "imported_title") {
                            Text(
                                text = "Imported Playlists (${localPlaylists.size})",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = PAGE_GUTTER, vertical = 8.dp),
                            )
                        }
                        items(localPlaylists, key = { it.id }) { playlist ->
                            ImportedPlaylistCardRow(
                                playlist = playlist,
                                onClick = {
                                    onShelfItemClick(
                                        ShelfItem(
                                            title = playlist.title,
                                            subtitle = "${playlist.songs.size} tracks",
                                            thumbnailUrl = playlist.coverUrl ?: playlist.songs.firstOrNull()?.thumbnailUrl,
                                            videoId = null,
                                            browseId = playlist.browseId,
                                        )
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportedPlaylistCardRow(
    playlist: com.music.bitchord.data.spotify.LocalPlaylist,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PAGE_GUTTER, vertical = 5.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF1C1C1E))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val cover = playlist.coverUrl ?: playlist.songs.firstOrNull()?.thumbnailUrl
        if (cover != null) {
            AsyncImage(
                model = cover,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp)),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF28282D)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.LibraryMusic,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp),
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val service = AccountService.fromRoute(playlist.provider)
                if (service != null) {
                    ProviderSquircle(service = service, size = 16.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${service.label} • ",
                        color = Color(0xFF8E8E93),
                        fontSize = 12.sp,
                    )
                }
                Text(
                    text = "${playlist.songs.size} tracks",
                    color = Color(0xFF8E8E93),
                    fontSize = 12.sp,
                )
            }
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0xFF55555A),
        )
    }
}

@Composable
private fun LibrarySearchResultRow(
    item: ShelfItem,
    onClick: () -> Unit,
    isArtist: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PAGE_GUTTER, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val shape = if (isArtist) CircleShape else RoundedCornerShape(8.dp)
        AsyncImage(
            model = item.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(46.dp)
                .clip(shape),
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!item.subtitle.isNullOrBlank()) {
                Text(
                    text = item.subtitle.orEmpty(),
                    color = Color(0xFF8E8E93),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun LibraryLinkList(links: List<LibraryLink>, onClick: (ShelfItem) -> Unit) {
    Column(Modifier.padding(horizontal = PAGE_GUTTER, vertical = 6.dp)) {
        links.forEach { link ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onClick(link.item) }
                    .padding(horizontal = 8.dp),
            ) {
                val logo = link.logo
                if (logo != null) {
                    androidx.compose.foundation.Image(
                        painter = org.jetbrains.compose.resources.painterResource(logo),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                } else {
                    Icon(
                        imageVector = link.icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = link.item.title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFF55555A),
                )
            }
        }
    }
}
