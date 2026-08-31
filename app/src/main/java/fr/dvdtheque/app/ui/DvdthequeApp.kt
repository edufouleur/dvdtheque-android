package fr.dvdtheque.app.ui

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import fr.dvdtheque.app.BuildConfig
import fr.dvdtheque.app.data.*
import fr.dvdtheque.app.network.TmdbMovieDetails
import fr.dvdtheque.app.network.TmdbMovieResult
import kotlinx.coroutines.delay
import java.time.LocalTime
import kotlin.random.Random

private const val LIBRARY = "library"
private const val WISHLIST = "wishlist"
private const val ADD = "add"
private const val WATCH = "watch"
private const val SETTINGS = "settings"
private const val DETAIL = "detail"
private val ReelioBrandPurple = Color(0xFF9D5CFF)

private enum class ReelioThemeMode(val label: String) { AUTO("Auto"), LIGHT("Clair"), DARK("Sombre") }

private enum class AccentChoice(val label: String, val color: Color, val onColor: Color) {
    CRIMSON("Carmin", Color(0xFFB71C1C), Color.White),
    RED("Rouge", Color(0xFFE53935), Color.White),
    CORAL("Corail", Color(0xFFFF6F61), Color.Black),
    DEEP_ORANGE("Orange vif", Color(0xFFFF5722), Color.White),
    ORANGE("Orange", Color(0xFFFF8C00), Color.Black),
    AMBER("Ambre", Color(0xFFFFB300), Color.Black),
    YELLOW("Jaune", Color(0xFFFFD54F), Color.Black),
    LIME("Citron vert", Color(0xFFCDDC39), Color.Black),
    LIGHT_GREEN("Vert clair", Color(0xFF7CB342), Color.Black),
    GREEN("Vert", Color(0xFF2E7D32), Color.White),
    EMERALD("Émeraude", Color(0xFF00A86B), Color.White),
    MINT("Menthe", Color(0xFF4DB6AC), Color.Black),
    TURQUOISE("Turquoise", Color(0xFF19C7B3), Color.Black),
    CYAN("Cyan", Color(0xFF00ACC1), Color.Black),
    SKY_BLUE("Bleu ciel", Color(0xFF29B6F6), Color.Black),
    BLUE("Bleu", Color(0xFF3687FF), Color.White),
    DEEP_BLUE("Bleu profond", Color(0xFF1565C0), Color.White),
    INDIGO("Indigo", Color(0xFF5B5FEF), Color.White),
    DEEP_PURPLE("Pourpre", Color(0xFF6A1B9A), Color.White),
    VIOLET("Violet", Color(0xFF9D5CFF), Color.White),
    LAVENDER("Lavande", Color(0xFFB388FF), Color.Black),
    MAGENTA("Magenta", Color(0xFFD81B60), Color.White),
    PINK("Rose", Color(0xFFFF5CA8), Color.White),
    ROSE("Rose poudré", Color(0xFFF48FB1), Color.Black),
    BROWN("Brun", Color(0xFF795548), Color.White),
    SILVER("Argent", Color(0xFF90A4AE), Color.Black)
}

@Composable
fun DvdthequeApp(vm: MovieViewModel = viewModel()) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("reelio_preferences", Context.MODE_PRIVATE) }
    var themeMode by remember {
        mutableStateOf(runCatching { ReelioThemeMode.valueOf(prefs.getString("theme", "AUTO") ?: "AUTO") }.getOrDefault(ReelioThemeMode.AUTO))
    }
    var accent by remember {
        mutableStateOf(runCatching { AccentChoice.valueOf(prefs.getString("accent", "VIOLET") ?: "VIOLET") }.getOrDefault(AccentChoice.VIOLET))
    }
    var hour by remember { mutableIntStateOf(LocalTime.now().hour) }

    LaunchedEffect(Unit) {
        while (true) {
            hour = LocalTime.now().hour
            delay(60_000)
        }
    }

    val dark = when (themeMode) {
        ReelioThemeMode.DARK -> true
        ReelioThemeMode.LIGHT -> false
        ReelioThemeMode.AUTO -> hour < 7 || hour >= 19
    }

    val colors = if (dark) {
        darkColorScheme(
            primary = accent.color,
            onPrimary = accent.onColor,
            secondary = accent.color,
            background = Color(0xFF07090D),
            surface = Color(0xFF0F1218),
            surfaceVariant = Color(0xFF171B23),
            outline = Color(0xFF2B313D)
        )
    } else {
        lightColorScheme(
            primary = accent.color,
            onPrimary = accent.onColor,
            secondary = accent.color,
            background = Color(0xFFF4F5F8),
            surface = Color(0xFFFFFFFF),
            surfaceVariant = Color(0xFFE9ECF2),
            outline = Color(0xFFD4D8E1)
        )
    }

    MaterialTheme(colorScheme = colors) {
        val nav = rememberNavController()
        NavHost(navController = nav, startDestination = LIBRARY) {
            composable(LIBRARY) {
                LibraryScreen(vm, onOpen = { nav.navigate("$DETAIL/$it") }, onAdd = { nav.navigate(ADD) }, onNavigate = nav::navigate)
            }
            composable(WISHLIST) {
                WishlistScreen(vm, onOpen = { nav.navigate("$DETAIL/$it") }, onAdd = { nav.navigate(ADD) }, onNavigate = nav::navigate)
            }
            composable(ADD) { AddMovieScreen(vm, onBack = { nav.popBackStack() }) }
            composable(WATCH) { WatchTonightScreen(vm, onOpen = { nav.navigate("$DETAIL/$it") }, onAdd = { nav.navigate(ADD) }, onNavigate = nav::navigate) }
            composable(SETTINGS) {
                SettingsScreen(
                    vm = vm,
                    themeMode = themeMode,
                    accent = accent,
                    onThemeChange = {
                        themeMode = it
                        prefs.edit().putString("theme", it.name).apply()
                    },
                    onAccentChange = {
                        accent = it
                        prefs.edit().putString("accent", it.name).apply()
                    },
                    onAdd = { nav.navigate(ADD) },
                    onNavigate = nav::navigate
                )
            }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReelioTopBar(
    screen: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.background
        ),
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "Retour")
                }
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = ReelioBrandPurple,
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.MovieFilter,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Column {
                    Text(
                        "Reelio",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    if (screen != "Bibliothèque") {
                        Text(
                            screen,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        actions = actions
    )
}

@Composable
private fun ScreenHeading(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        if (!subtitle.isNullOrBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MainBottomBar(current: String, onNavigate: (String) -> Unit, onAdd: () -> Unit) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        NavigationBarItem(
            selected = current == LIBRARY,
            onClick = { onNavigate(LIBRARY) },
            icon = { Icon(Icons.Default.VideoLibrary, null) },
            label = { Text("Bibliothèque") }
        )
        NavigationBarItem(
            selected = current == WISHLIST,
            onClick = { onNavigate(WISHLIST) },
            icon = { Icon(if (current == WISHLIST) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null) },
            label = { Text("Souhaits") }
        )
        NavigationBarItem(
            selected = false,
            onClick = onAdd,
            icon = {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 8.dp,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Add, "Ajouter", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            },
            label = { Text("") }
        )
        NavigationBarItem(
            selected = current == WATCH,
            onClick = { onNavigate(WATCH) },
            icon = { Icon(Icons.Default.Casino, null) },
            label = { Text("Ce soir") }
        )
        NavigationBarItem(
            selected = current == SETTINGS,
            onClick = { onNavigate(SETTINGS) },
            icon = { Icon(Icons.Default.Settings, null) },
            label = { Text("Paramètres") }
        )
    }
}

@Composable
private fun PremiumButton(
    text: String,
    icon: @Composable (() -> Unit)? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 52.dp)
            .shadow(8.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 13.dp)
    ) {
        if (icon != null) {
            icon()
            Spacer(Modifier.width(8.dp))
        }
        Text(text.uppercase(), fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun PremiumOutlineButton(
    text: String,
    icon: @Composable (() -> Unit)? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
    ) {
        if (icon != null) {
            icon()
            Spacer(Modifier.width(8.dp))
        }
        Text(text, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryScreen(
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
    var searchOpen by remember { mutableStateOf(false) }

    val owned = all.filter { it.status == MovieStatus.OWNED }
    val movies = owned.filter { watchedFilter == null || it.watched == watchedFilter }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ReelioTopBar("Bibliothèque", actions = {
                IconButton(onClick = { searchOpen = !searchOpen }) {
                    Icon(Icons.Default.Search, "Rechercher")
                }
                IconButton(onClick = { showSort = true }) {
                    Icon(Icons.Default.Tune, "Filtrer et trier")
                }
                DropdownMenu(expanded = showSort, onDismissRequest = { showSort = false }) {
                    MovieSort.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(sortLabel(option)) },
                            onClick = {
                                vm.setSort(option)
                                showSort = false
                            },
                            leadingIcon = {
                                if (sort == option) Icon(Icons.Default.Check, null)
                            }
                        )
                    }
                }
            })
        },
        bottomBar = { MainBottomBar(LIBRARY, onNavigate, onAdd) }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (searchOpen || query.isNotBlank()) {
                SearchField(query, vm::setQuery)
            }

            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = watchedFilter == null,
                    onClick = { watchedFilter = null },
                    label = { Text("Tous (${owned.size})") }
                )
                FilterChip(
                    selected = watchedFilter == true,
                    onClick = { watchedFilter = true },
                    label = { Text("Vus") }
                )
                FilterChip(
                    selected = watchedFilter == false,
                    onClick = { watchedFilter = false },
                    label = { Text("Pas vus") }
                )
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${movies.size} film${if (movies.size > 1) "s" else ""}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Tri : ${sortLabel(sort)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (movies.isEmpty()) {
                EmptyState(
                    "Aucun film dans la bibliothèque",
                    "Ajoute ton premier film pour commencer.",
                    onAdd
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(108.dp),
                    contentPadding = PaddingValues(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    gridItems(movies, key = { it.id }) { movie ->
                        MovieCard(movie, onOpen)
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        placeholder = { Text("Titre, acteur, réalisateur, genre…") },
        leadingIcon = { Icon(Icons.Default.Search, null) },
        trailingIcon = if (query.isNotBlank()) ({ IconButton(onClick = { onValueChange("") }) { Icon(Icons.Default.Close, null) } }) else null
    )
}

@Composable
private fun MovieCard(movie: Movie, onOpen: (Long) -> Unit) {
    Card(
        onClick = { onOpen(movie.id) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .65f))
    ) {
        Column {
            Box {
                Poster(
                    movie.posterUrl,
                    movie.title,
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(2f / 3f)
                )
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = .88f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(26.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (movie.status == MovieStatus.WANTED) Icons.Default.Favorite else if (movie.watched) Icons.Default.CheckCircle else Icons.Default.Circle,
                            contentDescription = null,
                            tint = if (movie.status == MovieStatus.WANTED || movie.watched) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            Column(Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                Text(
                    movie.title.uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    movie.year?.toString().orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    if (movie.rating != null) "★ ${movie.rating}/5" else if (movie.watched) "✓ Vu" else "○ À voir",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (movie.rating != null) Color(0xFFFFC928) else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun Poster(url: String, title: String, modifier: Modifier) {
    if (url.isNotBlank()) {
        AsyncImage(
            model = url,
            contentDescription = title,
            modifier = modifier.clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Movie,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun EmptyState(title: String, subtitle: String, onAdd: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.MovieCreation, null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
            PremiumButton("Ajouter un film", { Icon(Icons.Default.Add, null) }, onAdd)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WishlistScreen(vm: MovieViewModel, onOpen: (Long) -> Unit, onAdd: () -> Unit, onNavigate: (String) -> Unit) {
    val all by vm.movies.collectAsStateWithLifecycle()
    val discovery by vm.discovery.collectAsStateWithLifecycle()
    val wishes = all.filter { it.status == MovieStatus.WANTED }
    LaunchedEffect(Unit) { vm.loadDiscovery() }

    Scaffold(
        topBar = { ReelioTopBar("Souhaits") },
        bottomBar = { MainBottomBar(WISHLIST, onNavigate, onAdd) }
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            item {
                SectionHeader("Mes souhaits", if (wishes.isEmpty()) "Aucun film ajouté" else "${wishes.size} film(s)")
                if (wishes.isEmpty()) Text("Ajoute un film avec le cœur pour le retrouver ici.", modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                else LazyRow(contentPadding = PaddingValues(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(wishes, key = { it.id }) { movie -> WishlistMovieCard(movie, onOpen) }
                }
            }
            if (discovery.loading) item { Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            discovery.error?.let { message -> item { Text(message, modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.error) } }
            if (discovery.cinema.isNotEmpty()) item { DiscoverySection("🎬 Au cinéma", "Films actuellement ou récemment en salles", discovery.cinema, all, vm) }
            if (discovery.forYou.isNotEmpty()) item { DiscoverySection("✨ Pour vous", "Inspiré de votre bibliothèque", discovery.forYou, all, vm) }
            if (discovery.physical.isNotEmpty()) item { DiscoverySection("💿 DVD / Blu-ray", "Sorties physiques détectées en France", discovery.physical, all, vm) }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column(Modifier.padding(horizontal = 16.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun WishlistMovieCard(movie: Movie, onOpen: (Long) -> Unit) {
    Card(onClick = { onOpen(movie.id) }, modifier = Modifier.width(145.dp), shape = RoundedCornerShape(16.dp)) {
        Column {
            Poster(movie.posterUrl, movie.title, Modifier.fillMaxWidth().height(215.dp))
            Text(movie.title, Modifier.padding(10.dp), fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun DiscoverySection(title: String, subtitle: String, results: List<TmdbMovieResult>, all: List<Movie>, vm: MovieViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(title, subtitle)
        LazyRow(contentPadding = PaddingValues(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(results, key = { it.id }) { result ->
                val existing = all.firstOrNull { it.tmdbId == result.id }
                SuggestionCard(result, existing, onHeart = { if (existing == null) vm.addSuggestionToWishlist(result) }, onOpen = { vm.loadTmdbDetails(result.id) })
            }
        }
    }
}

@Composable
private fun SuggestionCard(result: TmdbMovieResult, existing: Movie?, onHeart: () -> Unit, onOpen: () -> Unit) {
    Card(onClick = onOpen, modifier = Modifier.width(150.dp), shape = RoundedCornerShape(16.dp)) {
        Box {
            Column {
                Poster(result.posterUrl, result.title, Modifier.fillMaxWidth().height(220.dp))
                Column(Modifier.padding(10.dp)) {
                    Text(result.title, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    result.year?.let { Text(it.toString(), style = MaterialTheme.typography.bodySmall) }
                    if (existing != null) Text(if (existing.status == MovieStatus.OWNED) "✓ Bibliothèque" else "♥ Souhait", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            if (existing == null) IconButton(onClick = onHeart, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).background(MaterialTheme.colorScheme.surface.copy(alpha = .78f), CircleShape)) {
                Icon(Icons.Default.FavoriteBorder, "Ajouter aux souhaits", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMovieScreen(vm: MovieViewModel, onBack: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    Scaffold(topBar = { ReelioTopBar("Ajouter un film", onBack) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Recherche") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Manuel") })
            }
            if (tab == 0) TmdbSearchTab(vm, onBack) else ManualAddTab(vm, onBack)
        }
    }
}

@Composable
private fun TmdbSearchTab(vm: MovieViewModel, onSaved: () -> Unit) {
    var text by remember { mutableStateOf("") }
    val state by vm.tmdbState.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = text, onValueChange = { text = it }, modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(16.dp), label = { Text("Titre du film") }, leadingIcon = { Icon(Icons.Default.Search, null) })
            PremiumButton("Chercher", onClick = { vm.searchTmdb(text) })
        }
        Spacer(Modifier.height(12.dp))
        when (val s = state) {
            TmdbUiState.Idle -> Text("Saisis un titre pour rechercher automatiquement les informations TMDB.")
            TmdbUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            is TmdbUiState.Error -> Text(s.message, color = MaterialTheme.colorScheme.error)
            is TmdbUiState.Results -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(s.movies, key = { it.id }) { result ->
                    Card(onClick = { vm.loadTmdbDetails(result.id) }, shape = RoundedCornerShape(14.dp)) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Poster(result.posterUrl, result.title, Modifier.width(70.dp).height(105.dp))
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
        if (details.posterUrl.isNotBlank()) item { Poster(details.posterUrl, details.title, Modifier.fillMaxWidth().height(330.dp)) }
        item { Text(details.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        item { Text(listOfNotNull(details.year?.toString(), details.runtime?.let { "$it min" }, details.genres.takeIf { it.isNotEmpty() }?.joinToString(", ") { it.name }).joinToString(" • ")) }
        if (director.isNotBlank()) item { Text("Réalisateur : $director") }
        if (actors.isNotBlank()) item { Text("Acteurs : $actors") }
        if (details.overview.isNotBlank()) item { Text(details.overview) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = status == MovieStatus.OWNED, onClick = { status = MovieStatus.OWNED }, label = { Text("Bibliothèque") })
                FilterChip(selected = status == MovieStatus.WANTED, onClick = { status = MovieStatus.WANTED }, label = { Text("Souhait") })
            }
        }
        item { PremiumButton("Ajouter", { Icon(Icons.Default.Add, null) }, { vm.addTmdbMovie(details, status, onSaved) }, Modifier.fillMaxWidth()) }
    }
}

@Composable
private fun ManualAddTab(vm: MovieViewModel, onSaved: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var director by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(MovieStatus.OWNED) }
    LazyColumn(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("Titre *") }, shape = RoundedCornerShape(14.dp)) }
        item { OutlinedTextField(year, { year = it.filter(Char::isDigit).take(4) }, Modifier.fillMaxWidth(), label = { Text("Année") }, shape = RoundedCornerShape(14.dp)) }
        item { OutlinedTextField(director, { director = it }, Modifier.fillMaxWidth(), label = { Text("Réalisateur") }, shape = RoundedCornerShape(14.dp)) }
        item { OutlinedTextField(genre, { genre = it }, Modifier.fillMaxWidth(), label = { Text("Genre") }, shape = RoundedCornerShape(14.dp)) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(status == MovieStatus.OWNED, { status = MovieStatus.OWNED }, label = { Text("Bibliothèque") })
            FilterChip(status == MovieStatus.WANTED, { status = MovieStatus.WANTED }, label = { Text("Souhait") })
        } }
        item { PremiumButton("Enregistrer", { Icon(Icons.Default.Save, null) }, {
            if (title.isNotBlank()) vm.save(Movie(title = title.trim(), year = year.toIntOrNull(), director = director.trim(), genre = genre.trim(), status = status), onSaved)
        }, Modifier.fillMaxWidth()) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailScreen(
    id: Long,
    vm: MovieViewModel,
    onBack: () -> Unit,
    onWatchGuide: () -> Unit
) {
    val movie by vm.movie(id).collectAsStateWithLifecycle(initialValue = null)
    val all by vm.movies.collectAsStateWithLifecycle()
    val current = movie

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ReelioTopBar(
                current?.title ?: "Fiche film",
                onBack,
                actions = {
                    IconButton(onClick = { }) { Icon(Icons.Default.Share, "Partager") }
                    IconButton(onClick = { }) { Icon(Icons.Default.MoreVert, "Plus") }
                }
            )
        }
    ) { padding ->
        if (current == null) {
            Box(
                Modifier.padding(padding).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        } else {
            LazyColumn(
                Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(245.dp)
                    ) {
                        if (current.posterUrl.isNotBlank()) {
                            AsyncImage(
                                model = current.posterUrl,
                                contentDescription = current.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                alpha = .34f
                            )
                        }
                        Box(
                            Modifier.fillMaxSize().background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.background.copy(alpha = .35f),
                                        MaterialTheme.colorScheme.background
                                    )
                                )
                            )
                        )
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Poster(
                                current.posterUrl,
                                current.title,
                                Modifier.width(116.dp).height(174.dp)
                            )
                            Column(
                                Modifier.weight(1f).padding(bottom = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    current.title,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    listOfNotNull(
                                        current.year?.toString(),
                                        current.durationMinutes?.let { "${it / 60}h ${it % 60}min" },
                                        current.genre.takeIf { it.isNotBlank() }
                                    ).joinToString(" • "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                RatingRow(current.rating ?: 0) { vm.setRating(current, it) }
                                AssistChip(
                                    onClick = { vm.setWatched(current, !current.watched) },
                                    label = { Text(if (current.watched) "✓ Déjà vu" else "○ Pas encore vu") }
                                )
                            }
                        }
                    }
                }

                item {
                    Row(
                        Modifier.padding(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PremiumOutlineButton(
                            "Modifier",
                            { Icon(Icons.Default.Edit, null) },
                            { },
                            Modifier.weight(1f)
                        )
                        PremiumOutlineButton(
                            if (current.status == MovieStatus.OWNED) "Souhait" else "Acheté",
                            { Icon(Icons.Default.Favorite, null) },
                            { vm.toggleStatus(current) },
                            Modifier.weight(1f)
                        )
                        PremiumOutlineButton(
                            "Ce soir",
                            { Icon(Icons.Default.PlayArrow, null) },
                            onWatchGuide,
                            Modifier.weight(1f)
                        )
                    }
                }

                if (current.synopsis.isNotBlank()) {
                    item {
                        PremiumInfoCard("Synopsis") {
                            Text(current.synopsis, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                if (current.director.isNotBlank() || current.actors.isNotBlank()) {
                    item {
                        PremiumInfoCard("Casting & équipe") {
                            if (current.director.isNotBlank()) InfoLine("Réalisation", current.director)
                            if (current.actors.isNotBlank()) InfoLine("Acteurs", current.actors)
                        }
                    }
                }
                if (current.edition.isNotBlank() || current.discCount != null || current.location.isNotBlank()) {
                    item {
                        PremiumInfoCard("Informations édition") {
                            if (current.edition.isNotBlank()) Text("Support / édition : ${current.edition}")
                            current.discCount?.let { Text("Disques : $it") }
                            if (current.location.isNotBlank()) Text("Emplacement : ${current.location}")
                        }
                    }
                }
                sagaFor(current)?.let { guide ->
                    item {
                        Column(Modifier.padding(horizontal = 14.dp)) {
                            SagaSection(guide, all)
                        }
                    }
                }
                item {
                    PremiumOutlineButton(
                        "Supprimer",
                        { Icon(Icons.Default.Delete, null) },
                        { vm.delete(current, onBack) },
                        Modifier.fillMaxWidth().padding(horizontal = 14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PremiumInfoCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.padding(horizontal = 14.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .55f))
    ) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
            content()
        }
    }
}

@Composable
private fun RatingRow(rating: Int, onRate: (Int) -> Unit) {
    Row { (1..5).forEach { n -> IconButton(onClick = { onRate(n) }, modifier = Modifier.size(34.dp)) { Icon(if (n <= rating) Icons.Default.Star else Icons.Default.StarBorder, "Note $n", tint = if (n <= rating) Color(0xFFFFC928) else MaterialTheme.colorScheme.onSurfaceVariant) } } }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Column { Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary); Text(value) }
}

private fun sagaFor(movie: Movie): UniverseGuide? = SagaCatalog.universes.firstOrNull { guide -> guide.chronological.any { SagaCatalog.findOwnedMatch(it, listOf(movie)) != null } }

@Composable
private fun SagaSection(guide: UniverseGuide, all: List<Movie>) {
    Card(shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.MovieFilter, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(8.dp)); Text("Saga ${guide.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            guide.chronological.forEachIndexed { index, entry ->
                val match = SagaCatalog.findOwnedMatch(entry, all)
                val status = when {
                    match == null -> "○ Absent"
                    match.status == MovieStatus.WANTED -> "♥ Souhait"
                    match.watched -> "✓ Vu"
                    else -> "▶ À voir"
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${index + 1}. ${entry.title}", modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(status, color = if (match != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WatchTonightScreen(
    vm: MovieViewModel,
    onOpen: (Long) -> Unit,
    onAdd: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val all by vm.movies.collectAsStateWithLifecycle()
    val owned = all.filter { it.status == MovieStatus.OWNED }
    var tab by remember { mutableIntStateOf(0) }
    var randomId by remember { mutableStateOf<Long?>(null) }
    var universeIndex by remember { mutableIntStateOf(0) }
    var chronological by remember { mutableStateOf(true) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { ReelioTopBar("Que regarder ce soir ?") },
        bottomBar = { MainBottomBar(WATCH, onNavigate, onAdd) }
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ScreenHeading(
                "Que regarder ce soir ?",
                "Trouvons le film parfait pour votre soirée",
                Modifier.padding(horizontal = 14.dp)
            )

            SingleChoiceSegmentedButtonRow(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp)
            ) {
                listOf("Aléatoire", "Continuer", "Ordre").forEachIndexed { index, label ->
                    SegmentedButton(
                        selected = tab == index,
                        onClick = { tab = index },
                        shape = SegmentedButtonDefaults.itemShape(index, 3),
                        label = { Text(label, maxLines = 1) }
                    )
                }
            }

            when (tab) {
                0 -> RandomWatchTab(
                    owned,
                    randomId,
                    { randomId = owned.filter { !it.watched }.ifEmpty { owned }.randomOrNull()?.id },
                    onOpen
                )
                1 -> ContinueSagaTab(owned, all)
                else -> WatchOrderTab(
                    all,
                    universeIndex,
                    { universeIndex = it },
                    chronological,
                    { chronological = it }
                )
            }
        }
    }
}

@Composable
private fun RandomWatchTab(
    owned: List<Movie>,
    randomId: Long?,
    onPick: () -> Unit,
    onOpen: (Long) -> Unit
) {
    val picked = owned.firstOrNull { it.id == randomId }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = .16f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .55f))
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Shuffle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
                    Text("ALÉATOIRE", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                    Text(
                        "Un film choisi dans votre bibliothèque",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    PremiumButton(
                        if (picked == null) "Choisir un film" else "Un autre film",
                        { Icon(Icons.Default.Casino, null) },
                        onPick,
                        Modifier.fillMaxWidth()
                    )
                }
            }
        }
        if (picked != null) {
            item {
                Poster(picked.posterUrl, picked.title, Modifier.width(190.dp).height(285.dp))
            }
            item {
                Text(picked.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
            }
            item {
                PremiumOutlineButton(
                    "Voir la fiche",
                    { Icon(Icons.Default.PlayArrow, null) },
                    { onOpen(picked.id) },
                    Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ContinueSagaTab(owned: List<Movie>, all: List<Movie>) {
    val suggestions = owned.filter { it.watched }.mapNotNull { watched -> SagaCatalog.nextInKnownSaga(watched, all)?.let { watched to it } }.distinctBy { it.second.title }
    LazyColumn(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("Reprendre là où vous en êtes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        if (suggestions.isEmpty()) item { Text("Marque des films comme vus pour que Reelio puisse proposer la suite d'une saga.") }
        items(suggestions) { (previous, next) ->
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Text(next.title, fontWeight = FontWeight.Bold)
                    Text("Après : ${previous.title}", style = MaterialTheme.typography.bodySmall)
                    Text(next.kind, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun WatchOrderTab(all: List<Movie>, universeIndex: Int, onUniverse: (Int) -> Unit, chronological: Boolean, onOrder: (Boolean) -> Unit) {
    val guide = SagaCatalog.universes[universeIndex]
    Column(Modifier.fillMaxSize()) {
        LazyRow(contentPadding = PaddingValues(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(SagaCatalog.universes.size) { i -> FilterChip(selected = i == universeIndex, onClick = { onUniverse(i) }, label = { Text(SagaCatalog.universes[i].name) }) }
        }
        Row(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = chronological, onClick = { onOrder(true) }, label = { Text("Chronologique") })
            FilterChip(selected = !chronological, onClick = { onOrder(false) }, label = { Text("Ordre de sortie") })
        }
        val entries = if (chronological) guide.chronological else guide.releaseOrder
        LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(entries.size) { index ->
                val entry = entries[index]
                val match = SagaCatalog.findOwnedMatch(entry, all)
                Card(shape = RoundedCornerShape(14.dp)) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${index + 1}", modifier = Modifier.width(34.dp), fontWeight = FontWeight.Bold)
                        Column(Modifier.weight(1f)) { Text(entry.title, fontWeight = FontWeight.SemiBold); Text(listOfNotNull(entry.year?.toString(), entry.kind).joinToString(" • "), style = MaterialTheme.typography.bodySmall) }
                        Text(when { match == null -> "Absent"; match.status == MovieStatus.WANTED -> "Souhait"; match.watched -> "Vu"; else -> "À voir" }, color = if (match != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    vm: MovieViewModel,
    themeMode: ReelioThemeMode,
    accent: AccentChoice,
    onThemeChange: (ReelioThemeMode) -> Unit,
    onAccentChange: (AccentChoice) -> Unit,
    onAdd: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val movies by vm.movies.collectAsStateWithLifecycle()
    val gson = remember { Gson() }

    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) runCatching {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use {
                it.write(gson.toJson(movies))
            }
        }.onSuccess {
            Toast.makeText(context, "Sauvegarde créée", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(context, "Échec de la sauvegarde", Toast.LENGTH_SHORT).show()
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) runCatching {
            val json = context.contentResolver.openInputStream(uri)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            val type = object : TypeToken<List<Movie>>() {}.type
            gson.fromJson<List<Movie>>(json, type) ?: emptyList()
        }.onSuccess { restored ->
            vm.restoreMovies(restored) {
                Toast.makeText(context, "Collection restaurée", Toast.LENGTH_SHORT).show()
            }
        }.onFailure {
            Toast.makeText(context, "Fichier de sauvegarde invalide", Toast.LENGTH_SHORT).show()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) runCatching {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                writer.appendLine("Titre;Année;Réalisateur;Genre;Statut;Vu;Note;Emplacement")
                movies.forEach { m ->
                    writer.appendLine(
                        listOf(
                            m.title,
                            m.year ?: "",
                            m.director,
                            m.genre,
                            if (m.status == MovieStatus.OWNED) "Bibliothèque" else "Souhait",
                            if (m.watched) "Oui" else "Non",
                            m.rating ?: "",
                            m.location
                        ).joinToString(";") { csvCell(it.toString()) }
                    )
                }
            }
        }.onSuccess {
            Toast.makeText(context, "Export CSV créé", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(context, "Échec de l'export", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ReelioTopBar(
                "Paramètres",
                actions = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.MoreVert, "Menu")
                    }
                }
            )
        },
        bottomBar = { MainBottomBar(SETTINGS, onNavigate, onAdd) }
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ScreenHeading(
                    "Paramètres",
                    "Personnalisez Reelio et gérez vos données"
                )
            }

            item {
                SettingsCard("APPARENCE") {
                    Text("Thème", fontWeight = FontWeight.Bold)
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ReelioThemeMode.entries.forEach { mode ->
                            val icon = when (mode) {
                                ReelioThemeMode.LIGHT -> Icons.Default.LightMode
                                ReelioThemeMode.DARK -> Icons.Default.DarkMode
                                ReelioThemeMode.AUTO -> Icons.Default.Schedule
                            }
                            Card(
                                onClick = { onThemeChange(mode) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (themeMode == mode)
                                        MaterialTheme.colorScheme.primary.copy(alpha = .20f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (themeMode == mode) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline.copy(alpha = .45f)
                                )
                            ) {
                                Column(
                                    Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        icon,
                                        null,
                                        tint = if (themeMode == mode) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(mode.label, fontWeight = FontWeight.Bold)
                                    if (mode == ReelioThemeMode.AUTO) {
                                        Text(
                                            "Selon l'heure",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Text("Couleur d’accentuation", fontWeight = FontWeight.Bold)
                    Text(
                        "Choisissez une couleur pour les boutons, icônes et sélections.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    AccentChoice.entries.chunked(7).forEach { rowChoices ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            rowChoices.forEach { choice ->
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(choice.color)
                                        .clickable { onAccentChange(choice) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (accent == choice) {
                                        Icon(
                                            Icons.Default.Check,
                                            null,
                                            tint = choice.onColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                            repeat(7 - rowChoices.size) { Spacer(Modifier.size(38.dp)) }
                        }
                    }
                }
            }

            item {
                SettingsCard("SAUVEGARDE & DONNÉES") {
                    SettingsActionRow(
                        icon = Icons.Default.Backup,
                        title = "Sauvegarder maintenant",
                        subtitle = "Crée une copie complète de votre collection"
                    ) { backupLauncher.launch("reelio-sauvegarde.json") }

                    SettingsActionRow(
                        icon = Icons.Default.Restore,
                        title = "Restaurer une sauvegarde",
                        subtitle = "Récupère une sauvegarde Reelio"
                    ) { restoreLauncher.launch("application/json") }

                    SettingsActionRow(
                        icon = Icons.Default.FileDownload,
                        title = "Exporter mes données",
                        subtitle = "Collection et souhaits au format CSV"
                    ) { exportLauncher.launch("reelio-collection.csv") }
                }
            }

            item {
                SettingsCard("À PROPOS") {
                    SettingsActionRow(
                        icon = Icons.Default.Info,
                        title = "À propos de Reelio",
                        subtitle = "Version ${BuildConfig.VERSION_NAME}"
                    ) { }

                    Text(
                        "Reelio — Votre collection, vos films, vos univers.",
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Recherche, affiches et métadonnées fournies par TMDB. Reelio utilise l'API TMDB mais n'est ni approuvé ni certifié par TMDB.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .62f)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .55f))
        ) {
            Column(
                Modifier.fillMaxWidth().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                content()
            }
        }
    }
}

private fun csvCell(value: String): String = if (value.contains(';') || value.contains('"') || value.contains('\n')) "\"${value.replace("\"", "\"\"")}\"" else value

private fun sortLabel(sort: MovieSort): String = when (sort) {
    MovieSort.TITLE_ASC -> "Titre A → Z"
    MovieSort.TITLE_DESC -> "Titre Z → A"
    MovieSort.YEAR_DESC -> "Année récente → ancienne"
    MovieSort.YEAR_ASC -> "Année ancienne → récente"
    MovieSort.RECENTLY_ADDED -> "Ajouts récents"
    MovieSort.RATING_DESC -> "Meilleures notes"
}
