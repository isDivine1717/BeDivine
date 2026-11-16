package com.music.bitchord.importer

import android.annotation.SuppressLint
import android.net.Uri
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil3.compose.AsyncImage
import com.music.bitchord.data.spotify.SpotifyImporter
import com.music.bitchord.ui.components.ProviderSquircle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class SheetStage {
    SELECT_SERVICE,
    LOGIN_WEBVIEW,
    IMPORT_BY_LINK,
    CHOOSE_PLAYLISTS,
}

private const val BRIDGE_NAME = "MochyCapture"
private val HOOK_JS = """
(function() {
  if (window.__mochyHooked) return;
  window.__mochyHooked = true;
  var wanted = { 'authorization': 1, 'media-user-token': 1, 'client-token': 1 };
  function report(name, value, url) {
    try {
      name = String(name).toLowerCase();
      if (!wanted[name] || !value) return;
      window.$BRIDGE_NAME.onHeader(name, String(value), String(url || location.href));
    } catch (e) {}
  }
  function scan(headers, url) {
    try {
      if (!headers) return;
      if (typeof Headers !== 'undefined' && headers instanceof Headers) {
        headers.forEach(function(v, k) { report(k, v, url); });
      } else if (Array.isArray(headers)) {
        headers.forEach(function(pair) { report(pair[0], pair[1], url); });
      } else if (typeof headers === 'object') {
        Object.keys(headers).forEach(function(k) { report(k, headers[k], url); });
      }
    } catch (e) {}
  }
  try {
    var originalFetch = window.fetch;
    if (originalFetch) {
      window.fetch = function(input, init) {
        try {
          var url = (typeof input === 'string') ? input : (input && input.url);
          if (input && typeof input === 'object' && input.headers) scan(input.headers, url);
          if (init && init.headers) scan(init.headers, url);
        } catch (e) {}
        return originalFetch.apply(this, arguments);
      };
    }
  } catch (e) {}
  try {
    var originalOpen = XMLHttpRequest.prototype.open;
    var originalSet = XMLHttpRequest.prototype.setRequestHeader;
    XMLHttpRequest.prototype.open = function(method, url) {
      try { this.__mochyUrl = url; } catch (e) {}
      return originalOpen.apply(this, arguments);
    };
    XMLHttpRequest.prototype.setRequestHeader = function(name, value) {
      report(name, value, this.__mochyUrl);
      return originalSet.apply(this, arguments);
    };
  } catch (e) {}
})();
""".trimIndent()

private class CaptureBridge(private val onHeader: (String, String, String) -> Unit) {
    @JavascriptInterface
    fun onHeader(name: String?, value: String?, url: String?) {
        if (!name.isNullOrBlank() && !value.isNullOrBlank()) {
            onHeader(name, value, url.orEmpty())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeDivineImportSheet(
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var stage by remember { mutableStateOf(SheetStage.SELECT_SERVICE) }
    var selectedService by remember { mutableStateOf<AccountService?>(null) }
    var credentials by remember { mutableStateOf(CapturedCredentials()) }

    var accountName by remember { mutableStateOf<String?>(null) }
    var remotePlaylists by remember { mutableStateOf<List<RemotePlaylist>>(emptyList()) }
    val selectedPlaylists = remember { mutableStateListOf<String>() }

    var isResolvingSession by remember { mutableStateOf(false) }
    var resolveError by remember { mutableStateOf<String?>(null) }

    // Link import state
    var linkInput by remember { mutableStateOf("") }
    var linkLoading by remember { mutableStateOf(false) }
    var linkError by remember { mutableStateOf<String?>(null) }

    fun checkAndFetchPlaylists(service: AccountService, creds: CapturedCredentials) {
        if (isResolvingSession) return
        val hasSpotify = creds.getBearer("spotify.com") != null
        val hasApple = creds.appleUserToken != null
        val hasTidal = creds.getBearer("tidal.com") != null

        val ready = when (service) {
            AccountService.SPOTIFY -> hasSpotify
            AccountService.APPLE_MUSIC -> hasApple
            AccountService.TIDAL -> hasTidal
            AccountService.YOUTUBE_MUSIC -> true
        }

        if (ready) {
            isResolvingSession = true
            resolveError = null
            scope.launch {
                try {
                    val (name, playlists) = AccountImportManager.resolveServiceAccount(service, creds)
                    accountName = name
                    remotePlaylists = playlists
                    selectedPlaylists.clear()
                    selectedPlaylists.addAll(playlists.map { it.id })
                    stage = SheetStage.CHOOSE_PLAYLISTS
                } catch (e: Exception) {
                    resolveError = e.message ?: "Failed to connect to account"
                } finally {
                    isResolvingSession = false
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF141416),
        dragHandle = null,
        modifier = Modifier.fillMaxHeight(0.92f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
        ) {
            AnimatedContent(
                targetState = stage,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "importSheetStages",
                modifier = Modifier.fillMaxSize(),
            ) { currentStage ->
                when (currentStage) {
                    SheetStage.SELECT_SERVICE -> {
                        SelectServiceView(
                            onSelectService = { service ->
                                selectedService = service
                                credentials = CapturedCredentials()
                                resolveError = null
                                if (service == AccountService.YOUTUBE_MUSIC) {
                                    checkAndFetchPlaylists(service, credentials)
                                } else {
                                    stage = SheetStage.LOGIN_WEBVIEW
                                }
                            },
                            onSelectLink = {
                                stage = SheetStage.IMPORT_BY_LINK
                            },
                            onClose = onDismiss,
                        )
                    }

                    SheetStage.LOGIN_WEBVIEW -> {
                        val service = selectedService ?: return@AnimatedContent
                        WebViewLoginView(
                            service = service,
                            credentials = credentials,
                            isResolving = isResolvingSession,
                            error = resolveError,
                            onHeaderCaptured = { name, value, url ->
                                credentials.addHeader(name, value, url)
                                checkAndFetchPlaylists(service, credentials)
                            },
                            onBack = {
                                stage = SheetStage.SELECT_SERVICE
                            },
                            onRetry = {
                                checkAndFetchPlaylists(service, credentials)
                            },
                        )
                    }

                    SheetStage.IMPORT_BY_LINK -> {
                        ImportByLinkView(
                            url = linkInput,
                            onUrlChange = { linkInput = it },
                            isLoading = linkLoading,
                            error = linkError,
                            onBack = { stage = SheetStage.SELECT_SERVICE },
                            onImport = {
                                linkLoading = true
                                linkError = null
                                scope.launch {
                                    try {
                                        val playlistId = SpotifyImporter.extractPlaylistId(linkInput)
                                            ?: error("Please enter a valid Spotify playlist URL or link.")
                                        val (title, tracks) = SpotifyImporter.fetchPlaylistTracks(playlistId)
                                        val remote = RemotePlaylist(
                                            id = playlistId,
                                            name = title,
                                            trackCount = tracks.size,
                                            service = AccountService.SPOTIFY,
                                        )
                                        remotePlaylists = listOf(remote)
                                        selectedPlaylists.clear()
                                        selectedPlaylists.add(remote.id)
                                        stage = SheetStage.CHOOSE_PLAYLISTS
                                    } catch (e: Exception) {
                                        linkError = e.message ?: "Failed to read playlist link"
                                    } finally {
                                        linkLoading = false
                                    }
                                }
                            },
                        )
                    }

                    SheetStage.CHOOSE_PLAYLISTS -> {
                        val service = selectedService ?: AccountService.SPOTIFY
                        ChoosePlaylistsView(
                            service = service,
                            accountName = accountName,
                            playlists = remotePlaylists,
                            selectedIds = selectedPlaylists,
                            onToggle = { id ->
                                if (id in selectedPlaylists) selectedPlaylists.remove(id)
                                else selectedPlaylists.add(id)
                            },
                            onSelectAll = {
                                selectedPlaylists.clear()
                                selectedPlaylists.addAll(remotePlaylists.map { it.id })
                            },
                            onDeselectAll = {
                                selectedPlaylists.clear()
                            },
                            onConfirm = {
                                val toImport = remotePlaylists.filter { it.id in selectedPlaylists }
                                AccountImportManager.startImport(toImport, credentials)
                                scope.launch {
                                    sheetState.hide()
                                    onDismiss()
                                }
                            },
                            onBack = {
                                stage = SheetStage.SELECT_SERVICE
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectServiceView(
    onSelectService: (AccountService) -> Unit,
    onSelectLink: () -> Unit,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Sync & Import Music",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color(0xFF8E8E93),
                )
            }
        }

        Text(
            text = "Connect your music account to import your playlists and library directly into BeDivine.",
            color = Color(0xFF8E8E93),
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 22.dp),
        )

        val services = listOf(
            AccountService.SPOTIFY to "Import your playlists, liked songs & mixes",
            AccountService.APPLE_MUSIC to "Connect your Apple Music library & tracks",
            AccountService.TIDAL to "Transfer your Tidal playlists & favorites",
            AccountService.YOUTUBE_MUSIC to "Import your YouTube Music library",
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            services.forEach { (service, description) ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF1E1E22))
                        .clickable { onSelectService(service) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProviderSquircle(service = service, size = 44.dp)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = service.label,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = description,
                                color = Color(0xFF8E8E93),
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF1E1E22))
                    .clickable { onSelectLink() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF2C2C30)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Import from Playlist Link",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Paste any public Spotify playlist URL",
                            color = Color(0xFF8E8E93),
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun WebViewLoginView(
    service: AccountService,
    credentials: CapturedCredentials,
    isResolving: Boolean,
    error: String?,
    onHeaderCaptured: (String, String, String) -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit,
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var currentUrl by remember { mutableStateOf(service.loginUrl.orEmpty()) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF161618))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            ProviderSquircle(service = service, size = 30.dp)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Log in to ${service.label}",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Text(
                    text = if (isResolving) "Connecting account..." else "Complete login to proceed",
                    color = if (isResolving) Color(0xFF8A5DF6) else Color(0xFF8E8E93),
                    fontSize = 11.sp,
                    maxLines = 1,
                )
            }
            if (isResolving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = Color(0xFF8A5DF6),
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            IconButton(onClick = { webViewRef?.reload() }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reload",
                    tint = Color(0xFF8E8E93),
                )
            }
        }

        if (error != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF331616))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = error,
                    color = Color(0xFFFF8A80),
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onRetry) {
                    Text("Retry", color = Color.White, fontSize = 12.sp)
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.databaseEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false

                        if (service.desktopSite) {
                            settings.userAgentString =
                                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
                        }

                        addJavascriptInterface(
                            CaptureBridge { name, value, url ->
                                onHeaderCaptured(name, value, url)
                            },
                            BRIDGE_NAME,
                        )

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                currentUrl = url.orEmpty()
                                view?.evaluateJavascript(HOOK_JS, null)
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                currentUrl = url.orEmpty()
                                view?.evaluateJavascript(HOOK_JS, null)
                            }

                            override fun shouldInterceptRequest(
                                view: WebView?,
                                request: WebResourceRequest?,
                            ): WebResourceResponse? {
                                val headers = request?.requestHeaders.orEmpty()
                                val reqUrl = request?.url?.toString().orEmpty()
                                headers.forEach { (k, v) ->
                                    val lower = k.lowercase()
                                    if (lower == "authorization" || lower == "media-user-token") {
                                        onHeaderCaptured(k, v, reqUrl)
                                    }
                                }
                                return super.shouldInterceptRequest(view, request)
                            }
                        }

                        loadUrl(service.loginUrl.orEmpty())
                        webViewRef = this
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun ImportByLinkView(
    url: String,
    onUrlChange: (String) -> Unit,
    isLoading: Boolean,
    error: String?,
    onBack: () -> Unit,
    onImport: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                text = "Import by Link",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.size(48.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Paste a Spotify playlist link:",
            color = Color(0xFF8E8E93),
            fontSize = 14.sp,
        )
        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1E1E22))
                .padding(14.dp),
        ) {
            BasicTextField(
                value = url,
                onValueChange = onUrlChange,
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                cursorBrush = SolidColor(Color.White),
                modifier = Modifier.fillMaxWidth(),
            )
            if (url.isEmpty()) {
                Text(
                    text = "https://open.spotify.com/playlist/...",
                    color = Color(0xFF55555A),
                    fontSize = 14.sp,
                )
            }
        }

        if (error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = error, color = Color(0xFFFF8A80), fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onImport,
            enabled = url.isNotBlank() && !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(25.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black,
            ),
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black)
            } else {
                Text("Fetch & Continue", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ChoosePlaylistsView(
    service: AccountService,
    accountName: String?,
    playlists: List<RemotePlaylist>,
    selectedIds: List<String>,
    onToggle: (String) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "What to Import",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                if (accountName != null) {
                    Text(
                        text = "Signed in as $accountName",
                        color = Color(0xFF8E8E93),
                        fontSize = 12.sp,
                    )
                }
            }
            Spacer(modifier = Modifier.size(48.dp))
        }

        Spacer(modifier = Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${selectedIds.size} of ${playlists.size} selected",
                color = Color(0xFF8E8E93),
                fontSize = 13.sp,
            )
            Row {
                TextButton(onClick = onSelectAll) {
                    Text("Select All", color = Color(0xFF8A5DF6), fontSize = 13.sp)
                }
                TextButton(onClick = onDeselectAll) {
                    Text("None", color = Color(0xFF8E8E93), fontSize = 13.sp)
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(playlists, key = { it.id }) { playlist ->
                val isChecked = playlist.id in selectedIds
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isChecked) Color(0xFF1E1E24) else Color(0xFF17171A))
                        .clickable { onToggle(playlist.id) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (playlist.coverUrl != null) {
                        AsyncImage(
                            model = playlist.coverUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp)),
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF2C2C30)),
                            contentAlignment = Alignment.Center,
                        ) {
                            ProviderSquircle(service = playlist.service, size = 32.dp)
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = playlist.name,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = playlist.trackCount?.let { "$it tracks" } ?: playlist.service.label,
                            color = Color(0xFF8E8E93),
                            fontSize = 12.sp,
                        )
                    }

                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = { onToggle(playlist.id) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFF8A5DF6),
                            uncheckedColor = Color(0xFF48484A),
                            checkmarkColor = Color.White,
                        ),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Button(
            onClick = onConfirm,
            enabled = selectedIds.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black,
            ),
        ) {
            Text(
                text = if (selectedIds.isEmpty()) "Select playlists to import" else "Import ${selectedIds.size} Playlists",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
