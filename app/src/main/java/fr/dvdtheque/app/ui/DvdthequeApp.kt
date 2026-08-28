package fr.dvdtheque.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
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

private const val LIBRARY = "library"
private const val WISHLIST = "wishlist"
private const val ADD = "add"
private const val DETAIL = "detail"

@Composable
fun DvdthequeApp(vm: MovieViewModel = viewModel()) {
    MaterialTheme {
        val nav = rememberNavController()
        NavHost(navController = nav, startDestination = LIBRARY) {
            composable(LIBRARY) { MovieListScreen(MovieStatus.OWNED, vm, { nav.navigate("$DETAIL/$it") }, { nav.navigate(ADD) }, { nav.navigate(WISHLIST) }) }
            composable(WISHLIST) { MovieListScreen(MovieStatus.WANTED, vm, { nav.navigate("$DETAIL/$it") }, { nav.navigate(ADD) }, { nav.popBackStack() }) }
            composable(ADD) { AddMovieScreen(vm) { nav.popBackStack() } }
            composable("$DETAIL/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { backStack ->
                DetailScreen(backStack.arguments?.getLong("id") ?: 0L, vm) { nav.popBackStack() }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MovieListScreen(status: MovieStatus, vm: MovieViewModel, onOpen: (Long) -> Unit, onAdd: () -> Unit, onOtherList: () -> Unit) {
    val all by vm.movies.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val sort by vm.sort.collectAsStateWithLifecycle()
    val movies = all.filter { it.status == status }
    var sortMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (status == MovieStatus.OWNED) "Ma DVDthèque" else "Mes souhaits") },
                actions = {
                    IconButton(onClick = onOtherList) { Icon(if (status == MovieStatus.OWNED) Icons.Default.FavoriteBorder else Icons.Default.Movie, null) }
                    Box {
                        IconButton(onClick = { sortMenu = true }) { Icon(Icons.Default.Sort, "Trier") }
                        DropdownMenu(sortMenu, { sortMenu = false }) {
                            MovieSort.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(sortLabel(option)) },
                                    onClick = { vm.setSort(option); sortMenu = false },
                                    leadingIcon = { if (option == sort) Icon(Icons.Default.Check, null) }
                                )
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = { FloatingActionButton(onClick = onAdd) { Icon(Icons.Default.Add, "Ajouter") } }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = vm::setQuery,
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                singleLine = true,
                label = { Text("Rechercher dans ma collection") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = if (query.isNotBlank()) ({ IconButton({ vm.setQuery("") }) { Icon(Icons.Default.Clear, null) } }) else null
            )
            if (movies.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(if (status == MovieStatus.OWNED) "Aucun DVD dans la bibliothèque" else "Aucun film dans les souhaits")
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(150.dp),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    gridItems(movies, key = { it.id }) { movie -> MovieCard(movie) { onOpen(movie.id) } }
                }
            }
        }
    }
}

@Composable
private fun MovieCard(movie: Movie, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column {
            if (movie.posterUrl.isNotBlank()) {
                AsyncImage(
                    model = movie.posterUrl,
                    contentDescription = "Affiche de ${movie.title}",
                    modifier = Modifier.fillMaxWidth().height(210.dp),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.LocalMovies, null, modifier = Modifier.size(72.dp))
                }
            }
            Column(Modifier.padding(12.dp)) {
                Text(movie.title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(listOfNotNull(movie.year?.toString(), movie.genre.takeIf { it.isNotBlank() }).joinToString(" • "), style = MaterialTheme.typography.bodySmall, maxLines = 2)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMovieScreen(vm: MovieViewModel, onBack: () -> Unit) {
    var search by remember { mutableStateOf("") }
    val state by vm.tmdbState.collectAsStateWithLifecycle()

    DisposableEffect(Unit) { onDispose { vm.resetTmdb() } }

    Scaffold(topBar = { TopAppBar(title = { Text("Rechercher un film") }, navigationIcon = { IconButton(onBack) { Icon(Icons.Default.ArrowBack, null) } }) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text("Titre du film") },
                    leadingIcon = { Icon(Icons.Default.Search, null) }
                )
                Button(onClick = { vm.searchTmdb(search) }, enabled = search.isNotBlank()) { Text("Chercher") }
            }

            when (val s = state) {
                TmdbUiState.Idle -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Saisis un titre, par exemple : Le Grand Bleu") }
                TmdbUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                is TmdbUiState.Error -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) { Text(s.message) }
                is TmdbUiState.Results -> {
                    if (s.movies.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Aucun film trouvé") }
                    } else {
                        LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(s.movies, key = { it.id }) { movie ->
                                Card(onClick = { vm.loadTmdbDetails(movie.id) }) {
                                    Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        AsyncImage(model = movie.posterUrl, contentDescription = null, modifier = Modifier.width(80.dp).height(120.dp), contentScale = ContentScale.Crop)
                                        Column(Modifier.weight(1f)) {
                                            Text(movie.title, fontWeight = FontWeight.Bold)
                                            movie.year?.let { Text(it.toString()) }
                                            if (movie.originalTitle.isNotBlank() && movie.originalTitle != movie.title) Text(movie.originalTitle, style = MaterialTheme.typography.bodySmall)
                                            if (movie.overview.isNotBlank()) Text(movie.overview, maxLines = 3, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                is TmdbUiState.Preview -> TmdbPreview(s.details, vm, onBack)
            }
        }
    }
}

@Composable
private fun TmdbPreview(details: TmdbMovieDetails, vm: MovieViewModel, onSaved: () -> Unit) {
    var status by remember { mutableStateOf(MovieStatus.OWNED) }
    val director = details.credits?.crew?.firstOrNull { it.job.equals("Director", true) }?.name.orEmpty()
    val actors = details.credits?.cast?.sortedBy { it.order }?.take(8)?.joinToString(", ") { it.name }.orEmpty()

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            AsyncImage(model = details.posterUrl, contentDescription = null, modifier = Modifier.fillMaxWidth().height(360.dp), contentScale = ContentScale.Fit)
        }
        item { Text(details.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        if (details.originalTitle.isNotBlank() && details.originalTitle != details.title) item { Text("Titre original : ${details.originalTitle}") }
        item { Text(listOfNotNull(details.year?.toString(), details.runtime?.let { "$it min" }, details.genres.takeIf { it.isNotEmpty() }?.joinToString(", ") { it.name }).joinToString(" • ")) }
        if (director.isNotBlank()) item { Text("Réalisateur : $director") }
        if (actors.isNotBlank()) item { Text("Acteurs : $actors") }
        if (details.overview.isNotBlank()) item { Text(details.overview) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(status == MovieStatus.OWNED, { status = MovieStatus.OWNED }, label = { Text("Possédé") })
                FilterChip(status == MovieStatus.WANTED, { status = MovieStatus.WANTED }, label = { Text("Souhait") })
            }
        }
        item {
            Button(onClick = { vm.addTmdbMovie(details, status, onSaved) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Save, null)
                Spacer(Modifier.width(8.dp))
                Text(if (status == MovieStatus.OWNED) "Ajouter à ma DVDthèque" else "Ajouter aux souhaits")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailScreen(id: Long, vm: MovieViewModel, onBack: () -> Unit) {
    val movie by vm.movie(id).collectAsStateWithLifecycle(initialValue = null)
    val current = movie
    Scaffold(topBar = { TopAppBar(title = { Text(current?.title ?: "Film") }, navigationIcon = { IconButton(onBack) { Icon(Icons.Default.ArrowBack, null) } }) }) { padding ->
        if (current == null) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    if (current.posterUrl.isNotBlank()) AsyncImage(model = current.posterUrl, contentDescription = null, modifier = Modifier.fillMaxWidth().height(360.dp), contentScale = ContentScale.Fit)
                }
                item { Text(current.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
                if (current.originalTitle.isNotBlank() && current.originalTitle != current.title) item { Text("Titre original : ${current.originalTitle}") }
                item {
                    val meta = listOfNotNull(current.year?.toString(), current.genre.takeIf { it.isNotBlank() }, current.durationMinutes?.let { "$it min" }).joinToString(" • ")
                    if (meta.isNotBlank()) Text(meta)
                }
                if (current.director.isNotBlank()) item { Text("Réalisateur : ${current.director}") }
                if (current.actors.isNotBlank()) item { Text("Acteurs : ${current.actors}") }
                if (current.synopsis.isNotBlank()) item { Text(current.synopsis) }
                item {
                    Button(onClick = { vm.toggleStatus(current); onBack() }, modifier = Modifier.fillMaxWidth()) {
                        Icon(if (current.status == MovieStatus.OWNED) Icons.Default.FavoriteBorder else Icons.Default.CheckCircle, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (current.status == MovieStatus.OWNED) "Déplacer vers les souhaits" else "Marquer comme acquis")
                    }
                }
                item {
                    OutlinedButton(onClick = { vm.delete(current, onBack) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Delete, null); Spacer(Modifier.width(8.dp)); Text("Supprimer")
                    }
                }
            }
        }
    }
}

private fun sortLabel(sort: MovieSort) = when (sort) {
    MovieSort.TITLE_ASC -> "Titre A → Z"
    MovieSort.TITLE_DESC -> "Titre Z → A"
    MovieSort.YEAR_DESC -> "Année récente d'abord"
    MovieSort.YEAR_ASC -> "Année ancienne d'abord"
    MovieSort.RECENTLY_ADDED -> "Ajout récent"
}
