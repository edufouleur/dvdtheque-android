package fr.dvdtheque.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import fr.dvdtheque.app.data.*
import fr.dvdtheque.app.network.TmdbMovieDetails
import kotlin.random.Random

private const val LIBRARY = "library"
private const val WISHLIST = "wishlist"
private const val ADD = "add"
private const val WATCH = "watch"
private const val STATS = "stats"
private const val SETTINGS = "settings"
private const val DETAIL = "detail"

private val DarkColors = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF9D5CFF),
    secondary = androidx.compose.ui.graphics.Color(0xFFC89BFF),
    background = androidx.compose.ui.graphics.Color(0xFF090A0E),
    surface = androidx.compose.ui.graphics.Color(0xFF12141A),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF1A1D24)
)

@Composable
fun DvdthequeApp(vm: MovieViewModel = viewModel()) {
    MaterialTheme(colorScheme = DarkColors) {
        val nav = rememberNavController()
        NavHost(navController = nav, startDestination = LIBRARY) {
            composable(LIBRARY) {
                CollectionScreen(MovieStatus.OWNED, vm, onOpen = { nav.navigate("$DETAIL/$it") }, onAdd = { nav.navigate(ADD) }, onNavigate = nav::navigate)
            }
            composable(WISHLIST) {
                CollectionScreen(MovieStatus.WANTED, vm, onOpen = { nav.navigate("$DETAIL/$it") }, onAdd = { nav.navigate(ADD) }, onNavigate = nav::navigate)
            }
            composable(ADD) { AddMovieScreen(vm, onBack = { nav.popBackStack() }) }
            composable(WATCH) { WatchTonightScreen(vm, onOpen = { nav.navigate("$DETAIL/$it") }, onNavigate = nav::navigate) }
            composable(STATS) { StatsScreen(vm, onNavigate = nav::navigate) }
            composable(SETTINGS) { SettingsScreen(onNavigate = nav::navigate) }
            composable("$DETAIL/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                DetailScreen(
                    id = entry.arguments?.getLong("id") ?: 0L,
                    vm = vm,
                    onBack = { nav.popBackStack() },
                    onWatchGuide = { nav.navigate(WATCH) }
                )
            }
        }
    }
}

@Composable
private fun MainBottomBar(current: String, onNavigate: (String) -> Unit, onAdd: () -> Unit = { onNavigate(ADD) }) {
    NavigationBar {
        NavigationBarItem(selected = current == LIBRARY, onClick = { onNavigate(LIBRARY) }, icon = { Icon(Icons.Default.VideoLibrary, null) }, label = { Text("Bibliothèque") })
        NavigationBarItem(selected = current == WISHLIST, onClick = { onNavigate(WISHLIST) }, icon = { Icon(Icons.Default.FavoriteBorder, null) }, label = { Text("Souhaits") })
        NavigationBarItem(selected = false, onClick = onAdd, icon = { Icon(Icons.Default.AddCircle, null) }, label = { Text("Ajouter") })
        NavigationBarItem(selected = current == WATCH, onClick = { onNavigate(WATCH) }, icon = { Icon(Icons.Default.Shuffle, null) }, label = { Text("Que regarder ?") })
        NavigationBarItem(selected = current == STATS || current == SETTINGS, onClick = { onNavigate(STATS) }, icon = { Icon(Icons.Default.BarChart, null) }, label = { Text("Plus") })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CollectionScreen(
    status: MovieStatus,
    vm: MovieViewModel,
    onOpen: (Long) -> Unit,
    onAdd: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val all by vm.movies.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val sort by vm.sort.collectAsStateWithLifecycle()
    var watchedFilter by remember { mutableStateOf<Boolean?>(null) }
    var showSort by remember { mutableStateOf(false) }

    val movies = all.filter { movie ->
        movie.status == status && (watchedFilter == null || movie.watched == watchedFilter)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (status == MovieStatus.OWNED) "Bibliothèque" else "Souhaits") },
                actions = {
                    IconButton(onClick = { showSort = true }) { Icon(Icons.Default.Sort, "Trier") }
                    DropdownMenu(expanded = showSort, onDismissRequest = { showSort = false }) {
                        MovieSort.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(sortLabel(option)) },
                                onClick = { vm.setSort(option); showSort = false },
                                leadingIcon = { if (sort == option) Icon(Icons.Default.Check, null) }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = { MainBottomBar(if (status == MovieStatus.OWNED) LIBRARY else WISHLIST, onNavigate, onAdd) }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = vm::setQuery,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                singleLine = true,
                placeholder = { Text("Titre, acteur, réalisateur, genre…") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = if (query.isNotBlank()) ({ IconButton(onClick = { vm.setQuery("") }) { Icon(Icons.Default.Close, null) } }) else null
            )
            if (status == MovieStatus.OWNED) {
                Row(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = watchedFilter == null, onClick = { watchedFilter = null }, label = { Text("Tous (${moviesCount(all, null)})") })
                    FilterChip(selected = watchedFilter == true, onClick = { watchedFilter = true }, label = { Text("Vus") })
                    FilterChip(selected = watchedFilter == false, onClick = { watchedFilter = false }, label = { Text("Pas vus") })
                }
            }
            if (movies.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(if (status == MovieStatus.OWNED) "Aucun film dans la bibliothèque" else "Aucun film dans les souhaits")
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(145.dp),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    gridItems(movies, key = { it.id }) { movie -> MovieCard(movie, onOpen) }
                }
            }
        }
    }
}

private fun moviesCount(all: List<Movie>, watched: Boolean?): Int = all.count { it.status == MovieStatus.OWNED && (watched == null || it.watched == watched) }

@Composable
private fun MovieCard(movie: Movie, onOpen: (Long) -> Unit) {
    Card(onClick = { onOpen(movie.id) }, modifier = Modifier.fillMaxWidth()) {
        Column {
            if (movie.posterUrl.isNotBlank()) {
                AsyncImage(
                    model = movie.posterUrl,
                    contentDescription = movie.title,
                    modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(Modifier.fillMaxWidth().aspectRatio(2f / 3f), contentAlignment = Alignment.Center) { Icon(Icons.Default.Movie, null, modifier = Modifier.size(52.dp)) }
            }
            Column(Modifier.padding(10.dp)) {
                Text(movie.title, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(listOfNotNull(movie.year?.toString(), movie.rating?.let { "★ $it/5" }).joinToString(" • "), style = MaterialTheme.typography.bodySmall)
                if (movie.status == MovieStatus.OWNED) {
                    Text(if (movie.watched) "✓ Vu" else "○ Pas vu", style = MaterialTheme.typography.labelSmall, color = if (movie.watched) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMovieScreen(vm: MovieViewModel, onBack: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    Scaffold(topBar = { TopAppBar(title = { Text("Ajouter un film") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Recherche") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Manuel") })
            }
            if (tab == 0) TmdbSearchContent(vm, onBack) else ManualAddContent(vm, onBack)
        }
    }
}

@Composable
private fun TmdbSearchContent(vm: MovieViewModel, onSaved: () -> Unit) {
    var text by remember { mutableStateOf("") }
    val state by vm.tmdbState.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = text, onValueChange = { text = it }, modifier = Modifier.weight(1f), singleLine = true, label = { Text("Rechercher sur TMDB") })
            Button(onClick = { vm.searchTmdb(text) }) { Icon(Icons.Default.Search, null) }
        }
        Spacer(Modifier.height(12.dp))
        when (val s = state) {
            TmdbUiState.Idle -> Text("Saisis le titre du film à ajouter.")
            TmdbUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            is TmdbUiState.Error -> Text(s.message, color = MaterialTheme.colorScheme.error)
            is TmdbUiState.Results -> {
                if (s.movies.isEmpty()) Text("Aucun résultat.")
                else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(s.movies, key = { it.id }) { result ->
                        Card(onClick = { vm.loadTmdbDetails(result.id) }) {
                            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (result.posterUrl.isNotBlank()) AsyncImage(result.posterUrl, null, Modifier.width(70.dp).height(105.dp), contentScale = ContentScale.Crop)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(result.title, fontWeight = FontWeight.Bold)
                                    result.year?.let { Text(it.toString()) }
                                    if (result.originalTitle.isNotBlank() && result.originalTitle != result.title) Text(result.originalTitle, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
            is TmdbUiState.Preview -> TmdbPreview(s.details, vm, onSaved)
        }
    }
}

@Composable
private fun TmdbPreview(details: TmdbMovieDetails, vm: MovieViewModel, onSaved: () -> Unit) {
    var status by remember { mutableStateOf(MovieStatus.OWNED) }
    val director = details.credits?.crew?.firstOrNull { it.job.equals("Director", true) }?.name.orEmpty()
    val actors = details.credits?.cast?.sortedBy { it.order }?.take(6)?.joinToString(", ") { it.name }.orEmpty()
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { TextButton(onClick = vm::resetTmdb) { Text("← Retour aux résultats") } }
        if (details.posterUrl.isNotBlank()) item { AsyncImage(details.posterUrl, details.title, Modifier.fillMaxWidth().height(320.dp), contentScale = ContentScale.Fit) }
        item { Text(details.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        item { Text(listOfNotNull(details.year?.toString(), details.runtime?.let { "$it min" }, details.genres.takeIf { it.isNotEmpty() }?.joinToString(", ") { it.name }).joinToString(" • ")) }
        if (director.isNotBlank()) item { Text("Réalisateur : $director") }
        if (actors.isNotBlank()) item { Text("Acteurs : $actors") }
        if (details.overview.isNotBlank()) item { Text(details.overview) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = status == MovieStatus.OWNED, onClick = { status = MovieStatus.OWNED }, label = { Text("Possédé") })
                FilterChip(selected = status == MovieStatus.WANTED, onClick = { status = MovieStatus.WANTED }, label = { Text("Souhait") })
            }
        }
        item { Button(onClick = { vm.addTmdbMovie(details, status, onSaved) }, modifier = Modifier.fillMaxWidth()) { Text("Ajouter") } }
    }
}

@Composable
private fun ManualAddContent(vm: MovieViewModel, onSaved: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var director by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(MovieStatus.OWNED) }
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("Titre") })
        OutlinedTextField(year, { year = it.filter(Char::isDigit).take(4) }, Modifier.fillMaxWidth(), label = { Text("Année") })
        OutlinedTextField(director, { director = it }, Modifier.fillMaxWidth(), label = { Text("Réalisateur") })
        OutlinedTextField(genre, { genre = it }, Modifier.fillMaxWidth(), label = { Text("Genre") })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(status == MovieStatus.OWNED, { status = MovieStatus.OWNED }, label = { Text("Possédé") })
            FilterChip(status == MovieStatus.WANTED, { status = MovieStatus.WANTED }, label = { Text("Souhait") })
        }
        Button(onClick = { if (title.isNotBlank()) vm.save(Movie(title = title.trim(), year = year.toIntOrNull(), director = director, genre = genre, status = status), onSaved) }, modifier = Modifier.fillMaxWidth(), enabled = title.isNotBlank()) { Text("Enregistrer") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailScreen(id: Long, vm: MovieViewModel, onBack: () -> Unit, onWatchGuide: () -> Unit) {
    val movie by vm.movie(id).collectAsStateWithLifecycle(initialValue = null)
    val all by vm.movies.collectAsStateWithLifecycle()
    val current = movie
    Scaffold(topBar = { TopAppBar(title = { Text(current?.title ?: "Film") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } }) }) { padding ->
        if (current == null) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            val next = SagaCatalog.nextInKnownSaga(current, all)
            LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (current.posterUrl.isNotBlank()) item { AsyncImage(current.posterUrl, current.title, Modifier.fillMaxWidth().height(360.dp), contentScale = ContentScale.Fit) }
                item { Text(current.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
                item { Text(listOfNotNull(current.year?.toString(), current.durationMinutes?.let { "$it min" }, current.genre.takeIf { it.isNotBlank() }).joinToString(" • ")) }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        (1..5).forEach { value ->
                            IconButton(onClick = { vm.setRating(current, value) }) { Icon(if ((current.rating ?: 0) >= value) Icons.Default.Star else Icons.Default.StarBorder, "Note $value") }
                        }
                        Text(current.rating?.let { "$it/5" } ?: "Non noté")
                    }
                }
                item {
                    FilledTonalButton(onClick = { vm.setWatched(current, !current.watched) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(if (current.watched) Icons.Default.Visibility else Icons.Default.VisibilityOff, null); Spacer(Modifier.width(8.dp)); Text(if (current.watched) "Déjà vu" else "Marquer comme vu")
                    }
                }
                if (next != null) item {
                    Button(onClick = onWatchGuide, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(8.dp)); Text("Continuer cette saga : ${next.title}")
                    }
                }
                if (current.director.isNotBlank()) item { InfoLine("Réalisation", current.director) }
                if (current.actors.isNotBlank()) item { InfoLine("Acteurs", current.actors) }
                if (current.synopsis.isNotBlank()) item { Column { Text("Synopsis", fontWeight = FontWeight.Bold); Text(current.synopsis) } }
                item {
                    OutlinedButton(onClick = { vm.toggleStatus(current); onBack() }, modifier = Modifier.fillMaxWidth()) {
                        Icon(if (current.status == MovieStatus.OWNED) Icons.Default.FavoriteBorder else Icons.Default.CheckCircle, null); Spacer(Modifier.width(8.dp)); Text(if (current.status == MovieStatus.OWNED) "Ajouter aux souhaits" else "Je l'ai acheté")
                    }
                }
                item { OutlinedButton(onClick = { vm.delete(current, onBack) }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Delete, null); Spacer(Modifier.width(8.dp)); Text("Supprimer") } }
            }
        }
    }
}

@Composable private fun InfoLine(label: String, value: String) { Column { Text(label, fontWeight = FontWeight.Bold); Text(value) } }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WatchTonightScreen(vm: MovieViewModel, onOpen: (Long) -> Unit, onNavigate: (String) -> Unit) {
    val all by vm.movies.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }
    Scaffold(topBar = { TopAppBar(title = { Text("Que regarder ce soir ?") }) }, bottomBar = { MainBottomBar(WATCH, onNavigate) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = tab) {
                Tab(tab == 0, { tab = 0 }, text = { Text("Au hasard") })
                Tab(tab == 1, { tab = 1 }, text = { Text("Continuer une saga") })
                Tab(tab == 2, { tab = 2 }, text = { Text("Ordre de visionnage") })
            }
            when (tab) {
                0 -> RandomMovieContent(all, onOpen)
                1 -> ContinueSagaContent(all)
                else -> ViewingOrderContent(all)
            }
        }
    }
}

@Composable
private fun RandomMovieContent(all: List<Movie>, onOpen: (Long) -> Unit) {
    val candidates = all.filter { it.status == MovieStatus.OWNED && !it.watched }
    var seed by remember { mutableIntStateOf(0) }
    val chosen = remember(candidates, seed) { if (candidates.isEmpty()) null else candidates[Random.nextInt(candidates.size)] }
    Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        if (chosen == null) Text("Aucun film non vu disponible.") else {
            if (chosen.posterUrl.isNotBlank()) AsyncImage(chosen.posterUrl, chosen.title, Modifier.height(330.dp).fillMaxWidth(), contentScale = ContentScale.Fit)
            Text(chosen.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(listOfNotNull(chosen.year?.toString(), chosen.genre.takeIf { it.isNotBlank() }).joinToString(" • "))
            Spacer(Modifier.height(16.dp))
            Button(onClick = { onOpen(chosen.id) }, modifier = Modifier.fillMaxWidth()) { Text("Voir la fiche") }
            OutlinedButton(onClick = { seed++ }, modifier = Modifier.fillMaxWidth()) { Text("Un autre film au hasard") }
        }
    }
}

@Composable
private fun ContinueSagaContent(all: List<Movie>) {
    val owned = all.filter { it.status == MovieStatus.OWNED && it.watched }
    val suggestions = owned.mapNotNull { movie -> SagaCatalog.nextInKnownSaga(movie, all)?.let { movie to it } }.distinctBy { it.second.title }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("Reprendre là où vous en êtes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        if (suggestions.isEmpty()) item { Text("Marquez des films comme vus pour obtenir des suggestions de saga.") }
        items(suggestions) { (seen, next) ->
            Card {
                Column(Modifier.padding(14.dp)) {
                    Text(seen.title, fontWeight = FontWeight.Bold)
                    Text("Vu ✓")
                    Spacer(Modifier.height(6.dp))
                    Text("À regarder ensuite : ${next.title}", color = MaterialTheme.colorScheme.primary)
                    next.year?.let { Text("${next.kind} • $it", style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
    }
}

@Composable
private fun ViewingOrderContent(all: List<Movie>) {
    var universeIndex by remember { mutableIntStateOf(0) }
    var chronological by remember { mutableStateOf(true) }
    val universe = SagaCatalog.universes[universeIndex]
    val guide = if (chronological) universe.chronological else universe.releaseOrder
    Column(Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Text("Univers", fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SagaCatalog.universes.forEachIndexed { index, item ->
                        FilterChip(selected = index == universeIndex, onClick = { universeIndex = index }, label = { Text(item.name, maxLines = 1) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = chronological, onClick = { chronological = true }, label = { Text("Chronologique") })
                    FilterChip(selected = !chronological, onClick = { chronological = false }, label = { Text("Sortie") })
                }
            }
            items(guide) { entry ->
                val match = SagaCatalog.findOwnedMatch(entry, all)
                val stateText = when {
                    match?.watched == true -> "✓ Vu"
                    match?.status == MovieStatus.OWNED -> "○ Dans la collection"
                    match?.status == MovieStatus.WANTED -> "♡ Souhait"
                    else -> "— Manquant"
                }
                Card {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(entry.title, fontWeight = FontWeight.SemiBold)
                            Text(listOfNotNull(entry.year?.toString(), entry.kind).joinToString(" • "), style = MaterialTheme.typography.bodySmall)
                        }
                        Text(stateText, color = if (match?.watched == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatsScreen(vm: MovieViewModel, onNavigate: (String) -> Unit) {
    val all by vm.movies.collectAsStateWithLifecycle()
    val owned = all.filter { it.status == MovieStatus.OWNED }
    val watched = owned.count { it.watched }
    val duration = owned.sumOf { it.durationMinutes ?: 0 }
    val favoriteGenres = owned.flatMap { it.genre.split(",") }.map { it.trim() }.filter { it.isNotBlank() }.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.take(5)
    Scaffold(
        topBar = { TopAppBar(title = { Text("Statistiques") }, actions = { IconButton(onClick = { onNavigate(SETTINGS) }) { Icon(Icons.Default.Settings, "Paramètres") } }) },
        bottomBar = { MainBottomBar(STATS, onNavigate) }
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { StatCard("${owned.size}", "Films", Modifier.weight(1f)); StatCard("$watched", "Vus", Modifier.weight(1f)); StatCard("${owned.size - watched}", "Pas vus", Modifier.weight(1f)) } }
            item { StatCard("${duration / 60} h ${duration % 60}", "Durée totale", Modifier.fillMaxWidth()) }
            item { Text("Genres les plus présents", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            items(favoriteGenres) { entry -> ListItem(headlineContent = { Text(entry.key) }, trailingContent = { Text(entry.value.toString()) }) }
        }
    }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier) {
    Card(modifier) { Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text(label, style = MaterialTheme.typography.bodySmall) } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(onNavigate: (String) -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Paramètres") }) }, bottomBar = { MainBottomBar(SETTINGS, onNavigate) }) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Text("Apparence", fontWeight = FontWeight.Bold) }
            item { ListItem(headlineContent = { Text("Thème") }, supportingContent = { Text("Sombre") }, leadingContent = { Icon(Icons.Default.DarkMode, null) }) }
            item { Text("Collection", fontWeight = FontWeight.Bold) }
            item { ListItem(headlineContent = { Text("Recherche automatique") }, supportingContent = { Text("TMDB en français") }, leadingContent = { Icon(Icons.Default.Search, null) }) }
            item { Text("À propos", fontWeight = FontWeight.Bold) }
            item { ListItem(headlineContent = { Text("Reelio v1.3") }, supportingContent = { Text("Bibliothèque, souhaits, notes, films vus, statistiques et assistant de visionnage.") }) }
            item { Text("Ce produit utilise l'API TMDB mais n'est ni approuvé ni certifié par TMDB.", style = MaterialTheme.typography.bodySmall) }
        }
    }
}

private fun sortLabel(sort: MovieSort) = when (sort) {
    MovieSort.TITLE_ASC -> "Titre A → Z"
    MovieSort.TITLE_DESC -> "Titre Z → A"
    MovieSort.YEAR_DESC -> "Année récente d'abord"
    MovieSort.YEAR_ASC -> "Année ancienne d'abord"
    MovieSort.RECENTLY_ADDED -> "Ajoutés récemment"
    MovieSort.RATING_DESC -> "Meilleures notes"
}
