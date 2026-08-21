package fr.dvdtheque.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import fr.dvdtheque.app.data.*

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
private fun MovieListScreen(
    status: MovieStatus,
    vm: MovieViewModel,
    onOpen: (Long) -> Unit,
    onAdd: () -> Unit,
    onOtherList: () -> Unit
) {
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
                    IconButton(onClick = onOtherList) {
                        Icon(if (status == MovieStatus.OWNED) Icons.Default.FavoriteBorder else Icons.Default.Movie, null)
                    }
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
                label = { Text("Rechercher") },
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
                    items(movies, key = { it.id }) { movie -> MovieCard(movie) { onOpen(movie.id) } }
                }
            }
        }
    }
}

@Composable
private fun MovieCard(movie: Movie, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column {
            Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.LocalMovies, null, modifier = Modifier.size(72.dp))
            }
            Column(Modifier.padding(12.dp)) {
                Text(movie.title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(listOfNotNull(movie.year?.toString(), movie.genre.takeIf { it.isNotBlank() }).joinToString(" • "), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMovieScreen(vm: MovieViewModel, onBack: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var director by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("") }
    var synopsis by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(MovieStatus.OWNED) }

    Scaffold(topBar = { TopAppBar(title = { Text("Ajouter un film") }, navigationIcon = { IconButton(onBack) { Icon(Icons.Default.ArrowBack, null) } }) }) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(title, { title = it }, label = { Text("Titre *") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(year, { year = it.filter(Char::isDigit).take(4) }, label = { Text("Année") }, modifier = Modifier.weight(1f))
                OutlinedTextField(duration, { duration = it.filter(Char::isDigit).take(3) }, label = { Text("Durée (min)") }, modifier = Modifier.weight(1f))
            }
            OutlinedTextField(director, { director = it }, label = { Text("Réalisateur") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(genre, { genre = it }, label = { Text("Genre") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(synopsis, { synopsis = it }, label = { Text("Synopsis") }, minLines = 3, modifier = Modifier.fillMaxWidth())
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilterChip(status == MovieStatus.OWNED, { status = MovieStatus.OWNED }, label = { Text("Possédé") })
                Spacer(Modifier.width(8.dp))
                FilterChip(status == MovieStatus.WANTED, { status = MovieStatus.WANTED }, label = { Text("Souhait") })
            }
            Button(
                enabled = title.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    vm.save(Movie(title = title.trim(), year = year.toIntOrNull(), director = director.trim(), genre = genre.trim(), durationMinutes = duration.toIntOrNull(), synopsis = synopsis.trim(), status = status), onBack)
                }
            ) { Icon(Icons.Default.Save, null); Spacer(Modifier.width(8.dp)); Text("Enregistrer") }
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
            Column(Modifier.padding(padding).padding(20.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) { Icon(Icons.Default.LocalMovies, null, modifier = Modifier.size(96.dp)) }
                Text(current.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                val meta = listOfNotNull(current.year?.toString(), current.genre.takeIf { it.isNotBlank() }, current.durationMinutes?.let { "$it min" }).joinToString(" • ")
                if (meta.isNotBlank()) Text(meta)
                if (current.director.isNotBlank()) Text("Réalisateur : ${current.director}")
                if (current.actors.isNotBlank()) Text("Acteurs : ${current.actors}")
                if (current.synopsis.isNotBlank()) Text(current.synopsis)
                Spacer(Modifier.weight(1f))
                Button(onClick = { vm.toggleStatus(current); onBack() }, modifier = Modifier.fillMaxWidth()) {
                    Icon(if (current.status == MovieStatus.OWNED) Icons.Default.FavoriteBorder else Icons.Default.CheckCircle, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (current.status == MovieStatus.OWNED) "Déplacer vers les souhaits" else "Marquer comme acquis")
                }
                OutlinedButton(onClick = { vm.delete(current, onBack) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Delete, null); Spacer(Modifier.width(8.dp)); Text("Supprimer")
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
