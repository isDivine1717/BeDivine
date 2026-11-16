package com.music.bitchord.importer

import android.content.Context
import android.util.Base64
import com.music.bitchord.data.Http
import com.music.bitchord.data.YtMusicRepository
import com.music.bitchord.data.model.SearchFilter
import com.music.bitchord.data.model.SearchResult
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.sources.TrackMatcher
import com.music.bitchord.data.spotify.LocalPlaylistStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI

class CapturedCredentials {
    private val bearers = LinkedHashMap<String, String>()
    private val headers = HashMap<String, String>()
    @Volatile var appleUserToken: String? = null
        private set

    fun addHeader(name: String, value: String, url: String) {
        val trimmed = value.trim()
        val lowerName = name.trim().lowercase()
        when (lowerName) {
            "authorization" -> {
                if (trimmed.startsWith("Bearer ", ignoreCase = true)) {
                    val token = trimmed.substring(7).trim()
                    if (token.length >= 15) {
                        val host = runCatching { URI(url).host?.lowercase() }.getOrNull().orEmpty()
                        synchronized(bearers) {
                            bearers[token] = host
                        }
                    }
                }
            }
            "media-user-token" -> {
                if (trimmed.length >= 15) {
                    appleUserToken = trimmed
                }
            }
            else -> {
                synchronized(headers) {
                    headers[lowerName] = trimmed
                }
            }
        }
    }

    fun getBearer(hostSuffix: String): String? {
        synchronized(bearers) {
            return bearers.entries.lastOrNull { it.value.contains(hostSuffix) }?.key
                ?: bearers.keys.lastOrNull()
        }
    }

    fun getAllBearers(): List<String> = synchronized(bearers) { bearers.keys.toList() }
}

object AccountImportManager {
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var importJob: Job? = null

    private val _progress = MutableStateFlow(ImportProgressState())
    val progress = _progress.asStateFlow()

    private val _currentService = MutableStateFlow<AccountService?>(null)
    val currentService = _currentService.asStateFlow()

    private val _capturedCredentials = MutableStateFlow(CapturedCredentials())
    val capturedCredentials = _capturedCredentials.asStateFlow()

    private val _remotePlaylists = MutableStateFlow<List<RemotePlaylist>>(emptyList())
    val remotePlaylists = _remotePlaylists.asStateFlow()

    private val _accountName = MutableStateFlow<String?>(null)
    val accountName = _accountName.asStateFlow()

    fun reset() {
        _capturedCredentials.value = CapturedCredentials()
        _remotePlaylists.value = emptyList()
        _accountName.value = null
        _currentService.value = null
    }

    fun prepareService(service: AccountService) {
        reset()
        _currentService.value = service
    }

    suspend fun resolveServiceAccount(
        service: AccountService,
        credentials: CapturedCredentials,
    ): Pair<String?, List<RemotePlaylist>> = withContext(Dispatchers.IO) {
        when (service) {
            AccountService.SPOTIFY -> fetchSpotifyPlaylists(credentials)
            AccountService.APPLE_MUSIC -> fetchAppleMusicPlaylists(credentials)
            AccountService.TIDAL -> fetchTidalPlaylists(credentials)
            AccountService.YOUTUBE_MUSIC -> fetchYouTubeMusicPlaylists()
        }
    }

    private fun fetchSpotifyPlaylists(credentials: CapturedCredentials): Pair<String?, List<RemotePlaylist>> {
        val token = credentials.getBearer("spotify.com")
            ?: error("No Spotify authorization token captured yet. Please log in.")

        var name: String? = null
        runCatching {
            val meReq = Request.Builder()
                .url("https://api.spotify.com/v1/me")
                .header("Authorization", "Bearer $token")
                .build()
            Http.client.newCall(meReq).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string().orEmpty()
                    val json = JSONObject(body)
                    name = json.optString("display_name", null) ?: json.optString("id", null)
                }
            }
        }

        val playlists = mutableListOf<RemotePlaylist>()
        val req = Request.Builder()
            .url("https://api.spotify.com/v1/me/playlists?limit=50")
            .header("Authorization", "Bearer $token")
            .build()

        Http.client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) error("Failed to fetch Spotify playlists (HTTP ${resp.code})")
            val body = resp.body?.string().orEmpty()
            val json = JSONObject(body)
            val items = json.optJSONArray("items") ?: JSONArray()
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                val id = item.optString("id")
                val pName = item.optString("name", "Untitled")
                val tracksObj = item.optJSONObject("tracks")
                val count = tracksObj?.optInt("total", 0)
                val images = item.optJSONArray("images")
                val cover = images?.optJSONObject(0)?.optString("url")
                if (id.isNotBlank()) {
                    playlists.add(
                        RemotePlaylist(
                            id = id,
                            name = pName,
                            trackCount = count,
                            coverUrl = cover,
                            service = AccountService.SPOTIFY,
                        )
                    )
                }
            }
        }
        return Pair(name, playlists)
    }

    private fun fetchAppleMusicPlaylists(credentials: CapturedCredentials): Pair<String?, List<RemotePlaylist>> {
        val userToken = credentials.appleUserToken
            ?: error("Apple Music session token missing. Please sign in.")
        val devToken = credentials.getBearer("apple.com")
            ?: error("Apple developer token missing.")

        val playlists = mutableListOf<RemotePlaylist>()
        val req = Request.Builder()
            .url("https://amp-api.music.apple.com/v1/me/library/playlists?limit=100")
            .header("Authorization", "Bearer $devToken")
            .header("Media-User-Token", userToken)
            .header("Origin", "https://music.apple.com")
            .build()

        Http.client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) error("Apple Music request failed (HTTP ${resp.code})")
            val body = resp.body?.string().orEmpty()
            val json = JSONObject(body)
            val data = json.optJSONArray("data") ?: JSONArray()
            for (i in 0 until data.length()) {
                val item = data.getJSONObject(i)
                val id = item.optString("id")
                val attr = item.optJSONObject("attributes")
                val pName = attr?.optString("name", "Untitled") ?: "Untitled"
                val artwork = attr?.optJSONObject("artwork")
                val urlTemplate = artwork?.optString("url")
                val cover = urlTemplate?.replace("{w}", "500")?.replace("{h}", "500")?.replace("{f}", "jpg")
                if (id.isNotBlank()) {
                    playlists.add(
                        RemotePlaylist(
                            id = id,
                            name = pName,
                            trackCount = null,
                            coverUrl = cover,
                            service = AccountService.APPLE_MUSIC,
                        )
                    )
                }
            }
        }
        return Pair("Apple Music", playlists)
    }

    private fun fetchTidalPlaylists(credentials: CapturedCredentials): Pair<String?, List<RemotePlaylist>> {
        val token = credentials.getBearer("tidal.com")
            ?: error("Tidal token missing. Please sign in.")

        var userId: String? = null
        var countryCode: String? = "US"

        val sessionReq = Request.Builder()
            .url("https://api.tidal.com/v1/sessions")
            .header("Authorization", "Bearer $token")
            .build()

        Http.client.newCall(sessionReq).execute().use { resp ->
            if (resp.isSuccessful) {
                val body = resp.body?.string().orEmpty()
                val json = JSONObject(body)
                userId = json.optString("userId", null)
                countryCode = json.optString("countryCode", "US")
            }
        }

        if (userId == null) {
            runCatching {
                val parts = token.split(".")
                if (parts.size >= 2) {
                    val decoded = String(Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP))
                    val json = JSONObject(decoded)
                    userId = json.optString("uid", null)
                    countryCode = json.optString("cc", "US")
                }
            }
        }

        if (userId == null) error("Could not retrieve Tidal user session.")

        val playlists = mutableListOf<RemotePlaylist>()
        val req = Request.Builder()
            .url("https://api.tidal.com/v1/users/$userId/playlists?countryCode=$countryCode&limit=50")
            .header("Authorization", "Bearer $token")
            .build()

        Http.client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) error("Failed to fetch Tidal playlists (HTTP ${resp.code})")
            val body = resp.body?.string().orEmpty()
            val json = JSONObject(body)
            val items = json.optJSONArray("items") ?: JSONArray()
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                val id = item.optString("uuid", item.optString("id"))
                val pName = item.optString("title", "Untitled")
                val count = item.optInt("numberOfTracks", 0)
                val imageUuid = item.optString("image", null)
                val cover = imageUuid?.takeIf { it.isNotBlank() }?.let {
                    "https://resources.tidal.com/images/${it.replace('-', '/')}/640x640.jpg"
                }
                if (id.isNotBlank()) {
                    playlists.add(
                        RemotePlaylist(
                            id = id,
                            name = pName,
                            trackCount = count,
                            coverUrl = cover,
                            service = AccountService.TIDAL,
                        )
                    )
                }
            }
        }
        return Pair("Tidal", playlists)
    }

    private suspend fun fetchYouTubeMusicPlaylists(): Pair<String?, List<RemotePlaylist>> {
        val shelves = YtMusicRepository.getLibrary().getOrNull()?.shelves.orEmpty()
        val playlistShelf = shelves.firstOrNull { it.title.equals("Playlists", ignoreCase = true) }
        val playlists = playlistShelf?.items?.mapNotNull { item ->
            val id = item.browseId ?: return@mapNotNull null
            RemotePlaylist(
                id = id,
                name = item.title,
                trackCount = null,
                coverUrl = item.thumbnailUrl,
                service = AccountService.YOUTUBE_MUSIC,
            )
        }.orEmpty()
        return Pair("YouTube Music", playlists)
    }

    suspend fun fetchPlaylistTracks(
        playlist: RemotePlaylist,
        credentials: CapturedCredentials,
    ): List<ImportedTrack> = withContext(Dispatchers.IO) {
        when (playlist.service) {
            AccountService.SPOTIFY -> fetchSpotifyTracks(playlist.id, credentials)
            AccountService.APPLE_MUSIC -> fetchAppleMusicTracks(playlist.id, credentials)
            AccountService.TIDAL -> fetchTidalTracks(playlist.id, credentials)
            AccountService.YOUTUBE_MUSIC -> fetchYouTubeTracks(playlist.id)
        }
    }

    private fun fetchSpotifyTracks(playlistId: String, credentials: CapturedCredentials): List<ImportedTrack> {
        val token = credentials.getBearer("spotify.com") ?: return emptyList()
        val tracks = mutableListOf<ImportedTrack>()
        var nextUrl: String? = "https://api.spotify.com/v1/playlists/$playlistId/tracks?limit=100"

        while (nextUrl != null && tracks.size < 500) {
            val req = Request.Builder()
                .url(nextUrl)
                .header("Authorization", "Bearer $token")
                .build()
            val (items, next) = Http.client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use Pair(null, null)
                val body = resp.body?.string().orEmpty()
                val json = JSONObject(body)
                val arr = json.optJSONArray("items")
                val n = json.optString("next", null).takeIf { !it.isNullOrBlank() && it != "null" }
                Pair(arr, n)
            }
            if (items == null) break
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                val trackObj = item.optJSONObject("track") ?: continue
                val title = trackObj.optString("name", "").trim()
                if (title.isBlank()) continue
                val artistsArr = trackObj.optJSONArray("artists")
                val artist = (0 until (artistsArr?.length() ?: 0))
                    .mapNotNull { artistsArr?.getJSONObject(it)?.optString("name") }
                    .joinToString(", ")
                val albumObj = trackObj.optJSONObject("album")
                val album = albumObj?.optString("name")
                val durationMs = trackObj.optLong("duration_ms", 0L)
                val thumb = albumObj?.optJSONArray("images")?.optJSONObject(0)?.optString("url")
                tracks.add(ImportedTrack(title = title, artist = artist, album = album, durationMs = durationMs, thumbnailUrl = thumb))
            }
            nextUrl = next
        }
        return tracks
    }

    private fun fetchAppleMusicTracks(playlistId: String, credentials: CapturedCredentials): List<ImportedTrack> {
        val userToken = credentials.appleUserToken ?: return emptyList()
        val devToken = credentials.getBearer("apple.com") ?: return emptyList()
        val tracks = mutableListOf<ImportedTrack>()

        val req = Request.Builder()
            .url("https://amp-api.music.apple.com/v1/me/library/playlists/$playlistId/tracks?limit=100")
            .header("Authorization", "Bearer $devToken")
            .header("Media-User-Token", userToken)
            .header("Origin", "https://music.apple.com")
            .build()

        Http.client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return emptyList()
            val body = resp.body?.string().orEmpty()
            val json = JSONObject(body)
            val data = json.optJSONArray("data") ?: JSONArray()
            for (i in 0 until data.length()) {
                val item = data.getJSONObject(i)
                val attr = item.optJSONObject("attributes") ?: continue
                val title = attr.optString("name", "").trim()
                val artist = attr.optString("artistName", "").trim()
                val album = attr.optString("albumName", null)
                val durationMs = attr.optLong("durationInMillis", 0L)
                if (title.isNotBlank()) {
                    tracks.add(ImportedTrack(title = title, artist = artist, album = album, durationMs = durationMs))
                }
            }
        }
        return tracks
    }

    private fun fetchTidalTracks(playlistId: String, credentials: CapturedCredentials): List<ImportedTrack> {
        val token = credentials.getBearer("tidal.com") ?: return emptyList()
        val tracks = mutableListOf<ImportedTrack>()

        val req = Request.Builder()
            .url("https://api.tidal.com/v1/playlists/$playlistId/items?limit=100")
            .header("Authorization", "Bearer $token")
            .build()

        Http.client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return emptyList()
            val body = resp.body?.string().orEmpty()
            val json = JSONObject(body)
            val items = json.optJSONArray("items") ?: JSONArray()
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                val itemObj = item.optJSONObject("item") ?: continue
                val title = itemObj.optString("title", "").trim()
                val artistObj = itemObj.optJSONObject("artist")
                val artist = artistObj?.optString("name", "") ?: ""
                val albumObj = itemObj.optJSONObject("album")
                val album = albumObj?.optString("title", null)
                val durationSec = itemObj.optLong("duration", 0L)
                if (title.isNotBlank()) {
                    tracks.add(ImportedTrack(title = title, artist = artist, album = album, durationMs = durationSec * 1000))
                }
            }
        }
        return tracks
    }

    private suspend fun fetchYouTubeTracks(playlistId: String): List<ImportedTrack> {
        val result = YtMusicRepository.getPlaylist(playlistId).getOrNull() ?: return emptyList()
        return result.songs.map { song ->
            ImportedTrack(
                title = song.title,
                artist = song.artist,
                album = song.albumName,
                videoId = song.videoId,
                thumbnailUrl = song.thumbnailUrl,
            )
        }
    }

    fun startImport(
        playlists: List<RemotePlaylist>,
        credentials: CapturedCredentials,
        onDone: (Int) -> Unit = {},
    ) {
        importJob?.cancel()
        importJob = scope.launch {
            val totalPlaylists = playlists.size
            var totalEstimatedSongs = playlists.sumOf { it.trackCount ?: 20 }
            if (totalEstimatedSongs <= 0) totalEstimatedSongs = playlists.size * 20

            _progress.value = ImportProgressState(
                isRunning = true,
                currentPlaylistTitle = playlists.firstOrNull()?.name.orEmpty(),
                doneTracks = 0,
                totalTracks = totalEstimatedSongs,
                statusMessage = "Starting import…",
                isDone = false,
                service = playlists.firstOrNull()?.service,
            )

            var matchedTotal = 0
            for ((index, playlist) in playlists.withIndex()) {
                _progress.update {
                    it.copy(
                        currentPlaylistTitle = playlist.name,
                        statusMessage = "Reading playlist (${index + 1}/$totalPlaylists): ${playlist.name}",
                    )
                }

                val remoteTracks = runCatching {
                    fetchPlaylistTracks(playlist, credentials)
                }.getOrNull().orEmpty()

                val resolvedSongs = mutableListOf<Song>()
                for (track in remoteTracks) {
                    _progress.update {
                        it.copy(
                            statusMessage = "Matching: ${track.title} - ${track.artist}",
                        )
                    }

                    val matchedSong = if (!track.videoId.isNullOrBlank()) {
                        Song(
                            videoId = track.videoId,
                            title = track.title,
                            artist = track.artist,
                            albumName = track.album,
                            thumbnailUrl = track.thumbnailUrl,
                        )
                    } else {
                        matchTrackToYouTube(track)
                    }

                    if (matchedSong != null) {
                        resolvedSongs.add(matchedSong)
                        matchedTotal++
                        _progress.update {
                            it.copy(doneTracks = matchedTotal)
                        }
                    }
                }

                if (resolvedSongs.isNotEmpty()) {
                    LocalPlaylistStore.savePlaylist(
                        title = playlist.name,
                        songs = resolvedSongs,
                        provider = playlist.service.route,
                        coverUrl = playlist.coverUrl,
                    )
                }
            }

            _progress.update {
                it.copy(
                    isRunning = false,
                    isDone = true,
                    statusMessage = "Successfully imported $totalPlaylists playlists ($matchedTotal tracks)!",
                )
            }
            onDone(matchedTotal)
        }
    }

    private suspend fun matchTrackToYouTube(track: ImportedTrack): Song? {
        val query = listOf(track.title, track.artist).filter { it.isNotBlank() }.joinToString(" ")
        if (query.isBlank()) return null
        val candidates = YtMusicRepository.search(query, SearchFilter.SONGS).getOrNull()
            ?.filterIsInstance<SearchResult.Track>()
            ?.map { it.song }
            .orEmpty()
        val durationSec = track.durationMs?.takeIf { it > 0 }?.div(1000)?.toInt()
        val best = TrackMatcher.best(
            candidates,
            TrackMatcher.Target(
                title = track.title,
                artist = track.artist,
                durationSec = durationSec,
                album = track.album,
            ),
        ) ?: candidates.firstOrNull()

        return best ?: Song(
            videoId = "",
            title = track.title,
            artist = track.artist,
            albumName = track.album,
            thumbnailUrl = track.thumbnailUrl,
        )
    }
}
