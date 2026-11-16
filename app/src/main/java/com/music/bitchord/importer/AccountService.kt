package com.music.bitchord.importer

enum class AccountService(
    val route: String,
    val label: String,
    val loginUrl: String?,
    val webOrigins: List<String>,
    val playerOrigins: Set<String> = emptySet(),
    val desktopSite: Boolean = false,
) {
    SPOTIFY(
        route = "spotify",
        label = "Spotify",
        loginUrl = "https://accounts.spotify.com/login?continue=https%3A%2F%2Fopen.spotify.com%2F",
        webOrigins = listOf("https://accounts.spotify.com", "https://open.spotify.com", "https://www.spotify.com"),
        playerOrigins = setOf("https://open.spotify.com"),
    ),
    APPLE_MUSIC(
        route = "apple_music",
        label = "Apple Music",
        loginUrl = "https://music.apple.com/us/library/playlists",
        webOrigins = listOf("https://music.apple.com", "https://idmsa.apple.com", "https://authorize.music.apple.com", "https://www.apple.com"),
        playerOrigins = setOf("https://music.apple.com"),
    ),
    TIDAL(
        route = "tidal",
        label = "Tidal",
        loginUrl = "https://listen.tidal.com/login",
        webOrigins = listOf("https://listen.tidal.com", "https://login.tidal.com", "https://tidal.com"),
        playerOrigins = setOf("https://listen.tidal.com"),
        desktopSite = true,
    ),
    YOUTUBE_MUSIC(
        route = "youtube_music",
        label = "YouTube Music",
        loginUrl = null,
        webOrigins = emptyList(),
    );

    companion object {
        fun fromRoute(route: String?): AccountService? = entries.firstOrNull { it.route == route }
    }
}

data class RemotePlaylist(
    val id: String,
    val name: String,
    val trackCount: Int? = null,
    val subtitle: String? = null,
    val coverUrl: String? = null,
    val service: AccountService,
    val isLiked: Boolean = false,
)

data class ImportedTrack(
    val title: String,
    val artist: String,
    val album: String? = null,
    val durationMs: Long? = null,
    val videoId: String? = null,
    val thumbnailUrl: String? = null,
)

data class ImportProgressState(
    val isRunning: Boolean = false,
    val currentPlaylistTitle: String = "",
    val doneTracks: Int = 0,
    val totalTracks: Int = 0,
    val statusMessage: String = "",
    val isDone: Boolean = false,
    val service: AccountService? = null,
)
