package com.kuvitta.movies

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

data class Movie(
    val id: String,
    val title: String,
    val year: String,
    val duration: String,
    val genre: String,
    val description: String,
    val poster: String,
    val youtubeQuery: String
)

private val movies = listOf(
    Movie("night", "Night of the Living Dead", "1968", "1 h 36 min", "Terror", "Un grupo de personas queda atrapado en una granja mientras el exterior es invadido por muertos vivientes.", "https://images.unsplash.com/photo-1509248961158-e54f6934749c?w=700", "Night of the Living Dead 1968 full movie official"),
    Movie("charade", "Charade", "1963", "1 h 53 min", "Intriga", "Una mujer perseguida en París intenta descubrir quién puede ayudarla y quién quiere engañarla.", "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=700", "Charade 1963 full movie official"),
    Movie("sherlock", "Sherlock Jr.", "1924", "45 min", "Comedia", "Un proyeccionista sueña con convertirse en detective y entra literalmente en la película que está proyectando.", "https://images.unsplash.com/photo-1485846234645-a62644f84728?w=700", "Sherlock Jr Buster Keaton full movie official"),
    Movie("lastman", "The Last Man on Earth", "1964", "1 h 26 min", "Ciencia ficción", "El único superviviente de una epidemia mundial lucha cada noche contra seres infectados.", "https://images.unsplash.com/photo-1440404653325-ab127d49abc1?w=700", "The Last Man on Earth 1964 full movie official"),
    Movie("general", "The General", "1926", "1 h 18 min", "Aventura", "Un maquinista persigue una locomotora robada atravesando territorio enemigo.", "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=700", "The General Buster Keaton full movie official"),
    Movie("hisgirl", "His Girl Friday", "1940", "1 h 32 min", "Comedia", "Un editor intenta impedir que su mejor reportera y exmujer abandone el periódico.", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=700", "His Girl Friday full movie official")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { KuvittaMoviesApp() }
    }
}

@Composable
fun KuvittaMoviesApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var tab by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<Movie?>(null) }
    val prefs = remember { context.getSharedPreferences("favorites", Context.MODE_PRIVATE) }
    var favorites by remember { mutableStateOf(prefs.getStringSet("ids", emptySet()) ?: emptySet()) }

    MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFFE50914), background = Color(0xFF080A0F), surface = Color(0xFF12151C))) {
        Scaffold(
            containerColor = Color(0xFF080A0F),
            bottomBar = {
                NavigationBar(containerColor = Color(0xFF10131A)) {
                    listOf(Icons.Default.Home to "Inicio", Icons.Default.Search to "Buscar", Icons.Default.Favorite to "Favoritos").forEachIndexed { index, item ->
                        NavigationBarItem(selected = tab == index, onClick = { tab = index }, icon = { Icon(item.first, item.second) }, label = { Text(item.second) })
                    }
                }
            }
        ) { padding ->
            Box(Modifier.padding(padding)) {
                when (tab) {
                    0 -> HomeScreen(onMovie = { selected = it })
                    1 -> SearchScreen(query, { query = it }, movies.filter { it.title.contains(query, true) || it.genre.contains(query, true) }, { selected = it })
                    else -> MovieGrid(movies.filter { it.id in favorites }, "Mis favoritos", { selected = it })
                }
            }
        }

        selected?.let { movie ->
            MovieDialog(movie, movie.id in favorites, onDismiss = { selected = null }, onFavorite = {
                favorites = if (movie.id in favorites) favorites - movie.id else favorites + movie.id
                prefs.edit().putStringSet("ids", favorites).apply()
            }, onPlay = {
                val url = "https://www.youtube.com/results?search_query=" + Uri.encode(movie.youtubeQuery)
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            })
        }
    }
}

@Composable
private fun HomeScreen(onMovie: (Movie) -> Unit) = LazyColumn(Modifier.fillMaxSize()) {
    item {
        Box(Modifier.fillMaxWidth().height(330.dp)) {
            AsyncImage(movies.first().poster, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF080A0F)))))
            Column(Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                Text("KUVITTA", color = Color(0xFFE50914), fontWeight = FontWeight.Black, fontSize = 16.sp)
                Text("MOVIES", color = Color.White, fontWeight = FontWeight.Black, fontSize = 36.sp)
                Text("Cine completo y legal en YouTube", color = Color.LightGray)
                Spacer(Modifier.height(12.dp))
                Button(onClick = { onMovie(movies.first()) }) { Icon(Icons.Default.PlayArrow, null); Text(" Ver película") }
            }
        }
    }
    item { MovieRow("Clásicos imprescindibles", movies, onMovie) }
    item { MovieRow("Terror y ciencia ficción", movies.filter { it.genre in listOf("Terror", "Ciencia ficción") }, onMovie) }
    item { MovieRow("Comedia y aventura", movies.filter { it.genre in listOf("Comedia", "Aventura") }, onMovie) }
    item { Spacer(Modifier.height(24.dp)) }
}

@Composable
private fun MovieRow(title: String, list: List<Movie>, onMovie: (Movie) -> Unit) {
    Column(Modifier.padding(vertical = 12.dp)) {
        Text(title, Modifier.padding(horizontal = 16.dp), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(Modifier.height(10.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(list) { MovieCard(it, onMovie) }
        }
    }
}

@Composable
private fun MovieCard(movie: Movie, onMovie: (Movie) -> Unit) {
    Column(Modifier.width(145.dp).clickable { onMovie(movie) }) {
        AsyncImage(movie.poster, movie.title, Modifier.fillMaxWidth().height(205.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
        Text(movie.title, Modifier.padding(top = 7.dp), color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
        Text("${movie.year} · ${movie.genre}", color = Color.Gray, fontSize = 12.sp)
    }
}

@Composable
private fun SearchScreen(query: String, onQuery: (String) -> Unit, results: List<Movie>, onMovie: (Movie) -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Buscar", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp))
        OutlinedTextField(query, onQuery, Modifier.fillMaxWidth(), placeholder = { Text("Título o género") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true)
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(results) { movie ->
                Row(Modifier.fillMaxWidth().clickable { onMovie(movie) }, verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(movie.poster, null, Modifier.size(90.dp, 120.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
                    Column(Modifier.padding(start = 14.dp)) {
                        Text(movie.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("${movie.year} · ${movie.duration}", color = Color.Gray)
                        Text(movie.genre, color = Color(0xFFE50914))
                    }
                }
            }
        }
    }
}

@Composable
private fun MovieGrid(list: List<Movie>, title: String, onMovie: (Movie) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text(title, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold) }
        if (list.isEmpty()) item { Text("Todavía no has añadido películas.", color = Color.Gray) }
        items(list) { movie ->
            Row(Modifier.fillMaxWidth().clickable { onMovie(movie) }) {
                AsyncImage(movie.poster, null, Modifier.size(100.dp, 140.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
                Column(Modifier.padding(14.dp)) { Text(movie.title, color = Color.White, fontWeight = FontWeight.Bold); Text(movie.description, color = Color.Gray, maxLines = 4) }
            }
        }
    }
}

@Composable
private fun MovieDialog(movie: Movie, favorite: Boolean, onDismiss: () -> Unit, onFavorite: () -> Unit, onPlay: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, confirmButton = {}, containerColor = Color(0xFF151820), text = {
        Column {
            AsyncImage(movie.poster, movie.title, Modifier.fillMaxWidth().height(250.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
            Spacer(Modifier.height(14.dp)); Text(movie.title, color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            Text("${movie.year} · ${movie.duration} · ${movie.genre}", color = Color.Gray)
            Spacer(Modifier.height(10.dp)); Text(movie.description, color = Color.LightGray)
            Spacer(Modifier.height(18.dp))
            Button(onClick = onPlay, Modifier.fillMaxWidth()) { Icon(Icons.Default.PlayArrow, null); Text(" Ver en YouTube") }
            OutlinedButton(onClick = onFavorite, Modifier.fillMaxWidth()) { Icon(if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null); Text(if (favorite) " Quitar de favoritos" else " Añadir a favoritos") }
            TextButton(onClick = onDismiss, Modifier.align(Alignment.End)) { Text("Cerrar") }
        }
    })
}
