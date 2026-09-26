package com.kuvitta.movies

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class Movie(val title: String, val year: String, val genre: String, val poster: String)
data class VideoResult(val id: String, val title: String, val channel: String, val thumbnail: String)

private val movies = listOf(
    Movie("Night of the Living Dead", "1968", "Terror", "https://images.unsplash.com/photo-1509248961158-e54f6934749c?w=700"),
    Movie("Charade", "1963", "Intriga", "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=700"),
    Movie("Sherlock Jr.", "1924", "Comedia", "https://images.unsplash.com/photo-1485846234645-a62644f84728?w=700"),
    Movie("The Last Man on Earth", "1964", "Ciencia ficción", "https://images.unsplash.com/photo-1440404653325-ab127d49abc1?w=700"),
    Movie("The General", "1926", "Aventura", "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=700")
)

sealed interface Screen {
    data object Home : Screen
    data class Search(val initial: String = "") : Screen
    data object Favorites : Screen
    data class Player(val video: VideoResult) : Screen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { KuvittaMoviesApp() }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun KuvittaMoviesApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("kuvitta", Context.MODE_PRIVATE) }
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    var apiKey by remember { mutableStateOf(prefs.getString("youtube_key", "") ?: "") }
    var showKeyDialog by remember { mutableStateOf(false) }
    var favorites by remember { mutableStateOf(loadFavorites(prefs)) }

    MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFFE50914), background = Color(0xFF07080C), surface = Color(0xFF14161D))) {
        Scaffold(
            containerColor = Color(0xFF07080C),
            topBar = {
                if (screen !is Screen.Player) TopAppBar(
                    title = { Text("KUVITTA MOVIES", fontWeight = FontWeight.Black, color = Color.White) },
                    actions = { IconButton(onClick = { showKeyDialog = true }) { Icon(Icons.Default.Settings, "Configurar YouTube") } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0C0E14))
                )
            },
            bottomBar = {
                if (screen !is Screen.Player) NavigationBar(containerColor = Color(0xFF0C0E14)) {
                    NavigationBarItem(screen is Screen.Home, { screen = Screen.Home }, { Icon(Icons.Default.Home, null) }, label = { Text("Inicio") })
                    NavigationBarItem(screen is Screen.Search, { screen = Screen.Search() }, { Icon(Icons.Default.Search, null) }, label = { Text("Buscar") })
                    NavigationBarItem(screen is Screen.Favorites, { screen = Screen.Favorites }, { Icon(Icons.Default.Favorite, null) }, label = { Text("Favoritos") })
                }
            }
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                when (val current = screen) {
                    Screen.Home -> HomeScreen { screen = Screen.Search(it.title + " película completa") }
                    is Screen.Search -> SearchScreen(current.initial, apiKey, { showKeyDialog = true }) { screen = Screen.Player(it) }
                    Screen.Favorites -> FavoritesScreen(favorites) { screen = Screen.Player(it) }
                    is Screen.Player -> PlayerScreen(current.video, favorites.any { it.id == current.video.id }, { screen = Screen.Search() }) {
                        favorites = if (favorites.any { it.id == current.video.id }) {
                            favorites.filterNot { it.id == current.video.id }
                        } else {
                            favorites + current.video
                        }
                        saveFavorites(prefs, favorites)
                    }
                }
            }
        }
    }
    if (showKeyDialog) ApiKeyDialog(apiKey, { showKeyDialog = false }) {
        apiKey = it.trim()
        prefs.edit().putString("youtube_key", apiKey).apply()
        showKeyDialog = false
    }
}

@Composable
private fun HomeScreen(onMovie: (Movie) -> Unit) = LazyColumn(Modifier.fillMaxSize()) {
    item {
        Box(Modifier.fillMaxWidth().height(320.dp)) {
            AsyncImage(movies.first().poster, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF07080C)))))
            Column(Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                Text("CINE COMPLETO", color = Color(0xFFE50914), fontWeight = FontWeight.Black)
                Text("Sin salir de la app", color = Color.White, fontWeight = FontWeight.Black, fontSize = 32.sp)
                Text("Busca, elige y reproduce con el player integrado", color = Color.LightGray)
                Spacer(Modifier.height(12.dp))
                Button(onClick = { onMovie(movies.first()) }) { Icon(Icons.Default.PlayArrow, null); Text(" Buscar película") }
            }
        }
    }
    item { Text("Clásicos para descubrir", Modifier.padding(16.dp), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 21.sp) }
    item {
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(movies) { movie ->
                Column(Modifier.width(150.dp).clickable { onMovie(movie) }) {
                    AsyncImage(movie.poster, movie.title, Modifier.fillMaxWidth().height(210.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                    Text(movie.title, Modifier.padding(top = 8.dp), color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(movie.year + " · " + movie.genre, color = Color.Gray, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun SearchScreen(initial: String, apiKey: String, onNeedKey: () -> Unit, onPlay: (VideoResult) -> Unit) {
    var query by remember(initial) { mutableStateOf(initial) }
    var results by remember { mutableStateOf<List<VideoResult>>(emptyList()) }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    fun search(text: String = query) {
        if (apiKey.isBlank()) { onNeedKey(); return }
        val requestedQuery = text.trim()
        if (requestedQuery.isBlank()) return
        loading = true
        error = null
        query = ""
        suggestions = emptyList()
        keyboard?.hide()
        scope.launch {
            runCatching { youtubeSearch(apiKey, requestedQuery) }
                .onSuccess { results = it; if (it.isEmpty()) error = "No se encontraron vídeos reproducibles." }
                .onFailure { error = it.message ?: "No se pudo conectar con YouTube." }
            loading = false
        }
    }
    LaunchedEffect(query) {
        val text = query.trim()
        if (text.length < 2) {
            suggestions = emptyList()
        } else {
            delay(350)
            suggestions = runCatching { youtubeSuggestions(text) }.getOrDefault(emptyList())
        }
    }
    LaunchedEffect(initial, apiKey) { if (initial.isNotBlank() && apiKey.isNotBlank()) search() }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text("Buscar en YouTube", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
        Text("Resultados reales y reproducción dentro de KuvittaMovies", color = Color.Gray)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Película, documental…") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (query.isNotEmpty()) IconButton(onClick = { query = "" }) {
                        Icon(Icons.Default.Close, "Borrar texto")
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { search() })
            )
            Spacer(Modifier.width(8.dp))
            FilledIconButton(onClick = { search() }, enabled = !loading) { Icon(Icons.Default.ArrowForward, "Buscar") }
        }
        if (suggestions.isNotEmpty()) {
            Card(
                Modifier.fillMaxWidth().padding(top = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1F28))
            ) {
                Column {
                    suggestions.take(6).forEach { suggestion ->
                        Row(
                            Modifier.fillMaxWidth().clickable { search(suggestion) }.padding(horizontal = 14.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Search, null, tint = Color.Gray, modifier = Modifier.size(19.dp))
                            Text(suggestion, Modifier.padding(start = 12.dp), color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
        if (apiKey.isBlank()) Card(Modifier.fillMaxWidth().padding(top = 16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1F28))) {
            Column(Modifier.padding(16.dp)) {
                Text("Activa la búsqueda oficial", color = Color.White, fontWeight = FontWeight.Bold)
                Text("Añade tu clave gratuita de YouTube Data API. Se guarda solo en este teléfono.", color = Color.LightGray)
                TextButton(onClick = onNeedKey) { Text("CONFIGURAR CLAVE") }
            }
        }
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 14.dp))
        error?.let { Text(it, color = Color(0xFFFF7777), modifier = Modifier.padding(vertical = 12.dp)) }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(results, key = { it.id }) { video -> VideoCard(video) { onPlay(video) } }
        }
    }
}

@Composable
private fun VideoCard(video: VideoResult, onPlay: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onPlay), colors = CardDefaults.cardColors(containerColor = Color(0xFF14161D))) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box {
                AsyncImage(video.thumbnail, video.title, Modifier.size(150.dp, 90.dp), contentScale = ContentScale.Crop)
                Icon(Icons.Default.PlayCircle, null, Modifier.align(Alignment.Center).size(42.dp), tint = Color(0xFFE50914))
            }
            Column(Modifier.padding(12.dp)) {
                Text(video.title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(video.channel, color = Color.Gray, maxLines = 1)
            }
        }
    }
}

@Composable
private fun PlayerScreen(video: VideoResult, favorite: Boolean, onBack: () -> Unit, onFavorite: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val playerView = remember(video.id) { YouTubePlayerView(context) }
    BackHandler(onBack = onBack)
    DisposableEffect(playerView) {
        lifecycleOwner.lifecycle.addObserver(playerView)
        onDispose {
            (context as? Activity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            playerView.release()
        }
    }
    Column(Modifier.fillMaxSize().background(Color.Black)) {
        if (!isLandscape) {
            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Volver", tint = Color.White) }
                Text("KUVITTA PLAYER", Modifier.weight(1f), color = Color.White, fontWeight = FontWeight.Black)
                IconButton(onClick = { (context as? Activity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE }) {
                    Icon(Icons.Default.Fullscreen, "Pantalla completa", tint = Color.White)
                }
            }
        }
        AndroidView(
            factory = {
                playerView.apply {
                    addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
                        override fun onReady(youTubePlayer: YouTubePlayer) { youTubePlayer.loadVideo(video.id, 0f) }
                    })
                }
            },
            modifier = if (isLandscape) Modifier.fillMaxSize() else Modifier.fillMaxWidth().aspectRatio(16f / 9f)
        )
        if (!isLandscape) {
            Column(Modifier.padding(18.dp)) {
                Text(video.title, color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                Text(video.channel, color = Color.Gray)
                Spacer(Modifier.height(14.dp))
                OutlinedButton(onClick = onFavorite) {
                    Icon(if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null)
                    Text(if (favorite) " Quitar de favoritos" else " Guardar en favoritos")
                }
                Text("Reproducción mediante el reproductor oficial IFrame de YouTube.", color = Color.DarkGray, fontSize = 12.sp, modifier = Modifier.padding(top = 16.dp))
            }
        }
    }
}

@Composable
private fun FavoritesScreen(videos: List<VideoResult>, onPlay: (VideoResult) -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text("Mis favoritos", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
        if (videos.isEmpty()) {
            Text("Aún no has guardado vídeos.", color = Color.Gray, modifier = Modifier.padding(top = 16.dp))
        } else {
            Text("${videos.size} vídeos guardados", color = Color.Gray, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(videos, key = { it.id }) { video ->
                    VideoCard(video) { onPlay(video) }
                }
            }
        }
    }
}

private fun loadFavorites(prefs: android.content.SharedPreferences): List<VideoResult> {
    val saved = runCatching {
        val array = JSONArray(prefs.getString("favorite_videos", "[]") ?: "[]")
        buildList {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                add(VideoResult(item.getString("id"), item.getString("title"), item.getString("channel"), item.getString("thumbnail")))
            }
        }
    }.getOrDefault(emptyList())
    if (saved.isNotEmpty()) return saved

    // Conserva los favoritos creados por las versiones anteriores.
    return (prefs.getStringSet("favorites", emptySet()) ?: emptySet()).map { id ->
        VideoResult(id, "Vídeo favorito", "YouTube", "https://i.ytimg.com/vi/$id/mqdefault.jpg")
    }
}

private fun saveFavorites(prefs: android.content.SharedPreferences, videos: List<VideoResult>) {
    val array = JSONArray()
    videos.forEach { video ->
        array.put(JSONObject().apply {
            put("id", video.id)
            put("title", video.title)
            put("channel", video.channel)
            put("thumbnail", video.thumbnail)
        })
    }
    prefs.edit()
        .putString("favorite_videos", array.toString())
        .putStringSet("favorites", videos.mapTo(mutableSetOf()) { it.id })
        .apply()
}

@Composable
private fun ApiKeyDialog(current: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var value by remember(current) { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("YouTube Data API") },
        text = { Column { Text("Introduce tu clave de Google Cloud. Se guarda únicamente en este dispositivo."); Spacer(Modifier.height(12.dp)); OutlinedTextField(value, { value = it }, label = { Text("API key") }, singleLine = true) } },
        confirmButton = { Button(onClick = { onSave(value) }, enabled = value.isNotBlank()) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

private suspend fun youtubeSearch(apiKey: String, query: String): List<VideoResult> = withContext(Dispatchers.IO) {
    val endpoint = "https://www.googleapis.com/youtube/v3/search?part=snippet&type=video&videoEmbeddable=true&safeSearch=moderate&maxResults=20&q=" +
        URLEncoder.encode(query, "UTF-8") + "&key=" + URLEncoder.encode(apiKey, "UTF-8")
    val connection = URL(endpoint).openConnection() as HttpURLConnection
    connection.connectTimeout = 12_000
    connection.readTimeout = 12_000
    val code = connection.responseCode
    val body = (if (code in 200..299) connection.inputStream else connection.errorStream).bufferedReader().use { it.readText() }
    if (code !in 200..299) {
        val message = runCatching { JSONObject(body).getJSONObject("error").getString("message") }.getOrDefault("Error de YouTube (" + code + ")")
        throw IllegalStateException(message)
    }
    val jsonItems = JSONObject(body).getJSONArray("items")
    buildList {
        for (i in 0 until jsonItems.length()) {
            val item = jsonItems.getJSONObject(i)
            val id = item.getJSONObject("id").optString("videoId")
            if (id.isBlank()) continue
            val snippet = item.getJSONObject("snippet")
            add(VideoResult(id, snippet.getString("title").htmlDecode(), snippet.getString("channelTitle").htmlDecode(), snippet.getJSONObject("thumbnails").getJSONObject("medium").getString("url")))
        }
    }
}

private suspend fun youtubeSuggestions(query: String): List<String> = withContext(Dispatchers.IO) {
    val endpoint = "https://suggestqueries.google.com/complete/search?client=firefox&ds=yt&q=" +
        URLEncoder.encode(query, "UTF-8")
    val connection = URL(endpoint).openConnection() as HttpURLConnection
    connection.connectTimeout = 6_000
    connection.readTimeout = 6_000
    connection.setRequestProperty("User-Agent", "KuvittaMovies/1.3")
    if (connection.responseCode !in 200..299) return@withContext emptyList()
    val body = connection.inputStream.bufferedReader().use { it.readText() }
    val array = JSONArray(body).optJSONArray(1) ?: return@withContext emptyList()
    buildList {
        for (i in 0 until minOf(array.length(), 8)) {
            array.optString(i).takeIf { it.isNotBlank() }?.let(::add)
        }
    }
}

private fun String.htmlDecode(): String = replace("&amp;", "&").replace("&quot;", "\"").replace("&#39;", "'").replace("&lt;", "<").replace("&gt;", ">")
