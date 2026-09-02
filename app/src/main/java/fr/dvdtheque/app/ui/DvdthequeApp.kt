package fr.dvdtheque.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import com.google.gson.Gson
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import androidx.core.content.FileProvider
import java.io.File
import com.google.gson.reflect.TypeToken
import fr.dvdtheque.app.BuildConfig
import fr.dvdtheque.app.R
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
private const val CINEMA = "cinema"
private val ReelioBrandPurple = Color(0xFF9D5CFF)

private enum class ReelioThemeMode(val label: String) { AUTO("Auto"), LIGHT("Clair"), DARK("Sombre") }

private enum class AccentChoice(val label: String, val color: Color, val onColor: Color) {
    RED("Rouge", Color(0xFFE53935), Color.White),
    ORANGE("Orange", Color(0xFFFF8C00), Color.Black),
    AMBER("Ambre", Color(0xFFFFB300), Color.Black),
    GREEN("Vert", Color(0xFF2E7D32), Color.White),
    TURQUOISE("Turquoise", Color(0xFF19C7B3), Color.Black),
    CYAN("Cyan", Color(0xFF00ACC1), Color.Black),
    BLUE("Bleu", Color(0xFF3687FF), Color.White),
    INDIGO("Indigo", Color(0xFF5B5FEF), Color.White),
    VIOLET("Violet", Color(0xFF9D5CFF), Color.White),
    PINK("Rose", Color(0xFFFF5CA8), Color.White),
    SILVER("Gris", Color(0xFF90A4AE), Color.Black),
    WHITE("Blanc", Color(0xFFF5F5F5), Color.Black)
}

@Composable
fun DvdthequeApp(vm: MovieViewModel = viewModel()) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("reelio_preferences", Context.MODE_PRIVATE) }
    var themeMode by remember {
        mutableStateOf(runCatching { ReelioThemeMode.valueOf(prefs.getString("theme", "DARK") ?: "DARK") }.getOrDefault(ReelioThemeMode.DARK))
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
                val movieId = entry.arguments?.getLong("id") ?: 0L
                DetailScreen(
                    id = movieId,
                    vm = vm,
                    onBack = { nav.popBackStack() },
                    onWatchGuide = { nav.navigate(WATCH) }
                )
            }
            composable("$CINEMA/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                CinemaModeScreen(
                    id = entry.arguments?.getLong("id") ?: 0L,
                    vm = vm,
                    onBack = { nav.popBackStack() }
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
                ReelioReelLogo(Modifier.size(36.dp))
                Column {
                    Text(
                        "Reelio",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (screen !in setOf("Bibliothèque", "Souhaits", "Que regarder ce soir ?", "Paramètres")) {
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
private fun ReelioReelLogo(modifier: Modifier = Modifier) {
    // Même bobine que l'icône du lanceur, mais sans fond ni cadre.
    // Le masque transparent est teinté avec la couleur choisie dans Paramètres.
    Image(
        painter = painterResource(R.drawable.logo_reelio_dynamic),
        contentDescription = "Logo Reelio",
        modifier = modifier,
        contentScale = ContentScale.Fit,
        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
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
            label = { Text("Bibliothèque", maxLines = 1, softWrap = false, fontSize = 10.sp) }
        )
        NavigationBarItem(
            selected = current == WISHLIST,
            onClick = { onNavigate(WISHLIST) },
            icon = { Icon(if (current == WISHLIST) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null) },
            label = { Text("Souhaits", maxLines = 1, softWrap = false, fontSize = 10.sp) }
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
            icon = {
                Icon(
                    Icons.Default.Casino,
                    contentDescription = "Ce soir",
                    modifier = Modifier.size(25.dp)
                )
            },
            label = { Text("Ce soir", maxLines = 1, softWrap = false, fontSize = 10.sp) }
        )
        NavigationBarItem(
            selected = current == SETTINGS,
            onClick = { onNavigate(SETTINGS) },
            icon = { Icon(Icons.Default.Settings, null) },
            label = { Text("Paramètres", maxLines = 1, softWrap = false, fontSize = 10.sp) }
        )
    }
}

@Composable
private fun PremiumButton(
    text: String,
    icon: @Composable (() -> Unit)? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
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
            if (discovery.cinema.isNotEmpty()) item { DiscoverySection("Au cinéma", Icons.Default.MovieCreation, "Films actuellement ou récemment en salles", discovery.cinema, all, vm) }
            if (discovery.forYou.isNotEmpty()) item { DiscoverySection("Pour vous", Icons.Default.AutoAwesome, "Inspiré de votre bibliothèque", discovery.forYou, all, vm) }
            if (discovery.physical.isNotEmpty()) item { DiscoverySection("DVD / Blu-ray", Icons.Default.Album, "Sorties physiques détectées en France", discovery.physical, all, vm) }
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
private fun DiscoverySection(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, subtitle: String, results: List<TmdbMovieResult>, all: List<Movie>, vm: MovieViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
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
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Photo") })
                Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Manuel") })
            }
            when (tab) {
                0 -> TmdbSearchTab(vm, onBack)
                1 -> PhotoSearchTab(vm, onBack)
                else -> ManualAddTab(vm, onBack)
            }
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
private fun PhotoSearchTab(vm: MovieViewModel, onSaved: () -> Unit) {
    val context = LocalContext.current
    val state by vm.tmdbState.collectAsStateWithLifecycle()
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var detectedTitle by remember { mutableStateOf("") }
    var detectedText by remember { mutableStateOf("") }
    var titleCandidates by remember { mutableStateOf<List<String>>(emptyList()) }
    var isReading by remember { mutableStateOf(false) }

    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    DisposableEffect(recognizer) {
        onDispose { recognizer.close() }
    }

    fun analyzeImage(uri: Uri) {
        isReading = true
        detectedText = ""
        try {
            val image = InputImage.fromFilePath(context, uri)
            recognizer.process(image)
                .addOnSuccessListener { result ->
                    detectedText = result.text.trim()
                    titleCandidates = buildOcrTitleCandidates(result)
                    detectedTitle = titleCandidates.firstOrNull().orEmpty()
                    isReading = false
                    if (detectedTitle.isNotBlank()) {
                        vm.searchTmdb(detectedTitle)
                    } else {
                        Toast.makeText(
                            context,
                            "Aucun titre fiable détecté. Cadre le titre de plus près et évite les reflets.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                .addOnFailureListener { error ->
                    isReading = false
                    Toast.makeText(
                        context,
                        "Lecture de l'image impossible : ${error.message ?: "erreur inconnue"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
        } catch (error: Exception) {
            isReading = false
            Toast.makeText(
                context,
                "Impossible d'ouvrir cette image.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            photoUri = uri
            analyzeImage(uri)
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            cameraUri?.let { uri ->
                photoUri = uri
                analyzeImage(uri)
            }
        }
    }

    if (state is TmdbUiState.Preview) {
        TmdbPreview((state as TmdbUiState.Preview).details, vm, onSaved)
        return
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .55f)),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = .10f)
                )
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.PhotoCamera,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(30.dp)
                        )
                        Column {
                            Text(
                                "Recherche par image",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                "Photographie la jaquette ou choisis une image. Reelio analyse les zones de texte, élimine les mentions DVD/Blu-ray et propose plusieurs titres probables avant la recherche TMDB.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PremiumButton(
                            "Photo",
                            { Icon(Icons.Default.PhotoCamera, null) },
                            onClick = {
                                val uri = createCameraImageUri(context)
                                cameraUri = uri
                                cameraLauncher.launch(uri)
                            },
                            modifier = Modifier.weight(1f)
                        )
                        PremiumOutlineButton(
                            "Galerie",
                            { Icon(Icons.Default.PhotoLibrary, null) },
                            { galleryLauncher.launch("image/*") },
                            Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        photoUri?.let { uri ->
            item {
                AsyncImage(
                    model = uri,
                    contentDescription = "Image analysée",
                    modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(18.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }

        if (isReading) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(Modifier.size(24.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Lecture du titre sur la jaquette…")
                }
            }
        }

        if (detectedTitle.isNotBlank() || detectedText.isNotBlank()) {
            item {
                OutlinedTextField(
                    value = detectedTitle,
                    onValueChange = { detectedTitle = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Titre détecté") },
                    supportingText = {
                        Text("Tu peux corriger le titre avant de relancer la recherche.")
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    trailingIcon = {
                        IconButton(
                            enabled = detectedTitle.isNotBlank(),
                            onClick = { vm.searchTmdb(detectedTitle) }
                        ) {
                            Icon(Icons.Default.Search, "Rechercher sur TMDB")
                        }
                    }
                )
            }
            if (titleCandidates.size > 1) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Titres probables",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Si le premier choix n’est pas le bon, touche une autre proposition.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(titleCandidates.take(5)) { candidate ->
                                FilterChip(
                                    selected = candidate == detectedTitle,
                                    onClick = {
                                        detectedTitle = candidate
                                        vm.searchTmdb(candidate)
                                    },
                                    label = { Text(candidate, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                                )
                            }
                        }
                    }
                }
            }
            if (detectedText.isNotBlank()) {
                item {
                    Text(
                        "Texte reconnu : ${detectedText.replace("\n", " • ").take(240)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        when (val currentState = state) {
            TmdbUiState.Idle -> {
                if (!isReading && photoUri == null) {
                    item {
                        Text(
                            "Conseil : cadre surtout le titre du film et évite les reflets sur la jaquette.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            TmdbUiState.Loading -> item {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is TmdbUiState.Error -> item {
                Text(currentState.message, color = MaterialTheme.colorScheme.error)
            }
            is TmdbUiState.Results -> {
                if (currentState.movies.isEmpty()) {
                    item { Text("Aucun résultat TMDB. Corrige le titre détecté puis relance la recherche.") }
                } else {
                    items(currentState.movies, key = { it.id }) { result ->
                        Card(
                            onClick = { vm.loadTmdbDetails(result.id) },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Poster(result.posterUrl, result.title, Modifier.width(70.dp).height(105.dp))
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(result.title, fontWeight = FontWeight.Bold)
                                    result.year?.let { Text(it.toString()) }
                                    if (result.originalTitle.isNotBlank() && result.originalTitle != result.title) {
                                        Text(
                                            result.originalTitle,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            is TmdbUiState.Preview -> Unit
        }
    }
}

private fun createCameraImageUri(context: Context): Uri {
    val directory = File(context.cacheDir, "images").apply { mkdirs() }
    val file = File.createTempFile("reelio_cover_", ".jpg", directory)
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
}

private fun buildOcrTitleCandidates(result: com.google.mlkit.vision.text.Text): List<String> {
    val ignored = listOf(
        "dvd", "blu-ray", "blu ray", "bluray", "ultra hd", "4k", "collector", "edition",
        "édition", "disc", "disque", "video", "vidéo", "dolby", "digital", "copyright",
        "www.", ".com", "interdit", "tout public", "version française", "vf", "vost",
        "bonus", "nouveau", "film", "cinema", "cinéma"
    )

    data class Candidate(val text: String, val score: Double, val top: Int)

    fun clean(raw: String): String = raw
        .replace(Regex("[|•·]+"), " ")
        .replace(Regex("\\s+"), " ")
        .trim(' ', '-', '_', ':', ';', '.', ',')

    fun acceptable(text: String): Boolean {
        if (text.length !in 2..70) return false
        val letters = text.count(Char::isLetter)
        if (letters < 2) return false
        if (text.count(Char::isDigit) > text.length / 2) return false
        if (ignored.any { noise -> text.equals(noise, true) || text.contains(noise, true) }) return false
        return true
    }

    val imageHeight = result.textBlocks
        .flatMap { it.lines }
        .mapNotNull { it.boundingBox?.bottom }
        .maxOrNull()
        ?.coerceAtLeast(1) ?: 1

    val lineCandidates = result.textBlocks.flatMap { block ->
        block.lines.mapNotNull { line ->
            val text = clean(line.text)
            if (!acceptable(text)) return@mapNotNull null
            val box = line.boundingBox
            val height = box?.height()?.toDouble() ?: 1.0
            val width = box?.width()?.toDouble() ?: text.length.toDouble()
            val top = box?.top ?: imageHeight / 2
            val relativeTop = top.toDouble() / imageHeight
            val lettersOnly = text.filter(Char::isLetter)
            val upperRatio = if (lettersOnly.isEmpty()) 0.0 else lettersOnly.count(Char::isUpperCase).toDouble() / lettersOnly.length
            var score = height * 4.0 + width * 0.03 + text.count(Char::isLetter) * 1.5
            if (text.length in 4..35) score += 24.0
            if (text.split(" ").size in 1..6) score += 12.0
            if (upperRatio >= 0.65) score += 12.0
            if (relativeTop in 0.08..0.68) score += 10.0
            Candidate(text, score, top)
        }
    }

    val combined = lineCandidates
        .sortedBy { it.top }
        .windowed(2, 1, partialWindows = false)
        .mapNotNull { pair ->
            val a = pair[0]
            val b = pair[1]
            val joined = clean("${a.text} ${b.text}")
            if (!acceptable(joined) || joined.length > 55) null
            else Candidate(joined, (a.score + b.score) * 0.72 + 10.0, minOf(a.top, b.top))
        }

    val fallback = result.text
        .lineSequence()
        .map(::clean)
        .filter(::acceptable)
        .map { Candidate(it, it.length.toDouble(), imageHeight / 2) }
        .toList()

    return (lineCandidates + combined + fallback)
        .groupBy { it.text.lowercase() }
        .map { (_, same) -> same.maxBy { it.score } }
        .sortedByDescending { it.score }
        .map { it.text }
        .take(5)
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
                FilterChip(selected = status == MovieStatus.OWNED, onClick = { status = MovieStatus.OWNED }, label = { Text("Bibliothèque", maxLines = 1, softWrap = false, fontSize = 10.sp) })
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
            FilterChip(status == MovieStatus.OWNED, { status = MovieStatus.OWNED }, label = { Text("Bibliothèque", maxLines = 1, softWrap = false, fontSize = 10.sp) })
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
    val cinemaState by vm.cinemaState.collectAsStateWithLifecycle()
    val current = movie
    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var ratingDialogOpen by remember { mutableStateOf(false) }
    var backdropLoaded by remember(current?.tmdbId) { mutableStateOf(false) }
    val backdropFade = remember(current?.tmdbId) { Animatable(0f) }

    LaunchedEffect(current?.tmdbId) {
        backdropLoaded = false
        backdropFade.snapTo(0f)
        vm.resetCinema()
        vm.loadCinema(current?.tmdbId)
    }
    DisposableEffect(Unit) {
        onDispose { vm.resetCinema() }
    }

    val ready = cinemaState as? CinemaUiState.Ready
    val details = ready?.details
    val trailer = ready?.trailer
    // Le poster n'est jamais utilisé comme fond temporaire : cela évite le flash
    // de l'affiche avant l'arrivée du vrai backdrop TMDB.
    val backdrop = details?.backdropUrl.orEmpty()

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF05060A))
    ) {
        if (backdrop.isNotBlank()) {
            AsyncImage(
                model = backdrop,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(520.dp)
                    .graphicsLayer(alpha = backdropFade.value * .72f),
                contentScale = ContentScale.Crop,
                onSuccess = { backdropLoaded = true }
            )
            LaunchedEffect(backdropLoaded, backdrop) {
                if (backdropLoaded) {
                    backdropFade.animateTo(1f, animationSpec = tween(1100))
                }
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(560.dp)
                .background(
                    Brush.verticalGradient(
                        0f to Color(0x55000000),
                        .45f to Color(0x22000000),
                        .82f to Color(0xEE05060A),
                        1f to Color(0xFF05060A)
                    )
                )
        )

        if (current == null) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 82.dp, bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Spacer(Modifier.height(330.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Poster(
                            current.posterUrl,
                            current.title,
                            Modifier.width(104.dp).height(156.dp).clip(RoundedCornerShape(12.dp))
                        )
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val cleanTitle = current.title.trim()
                                val titleLength = cleanTitle.length
                                val singleWordTitle = cleanTitle.isNotEmpty() && cleanTitle.none { it.isWhitespace() }
                                val adaptiveTitleSize = if (singleWordTitle) {
                                    // Un seul mot : jamais de coupure ni de points de suspension.
                                    when {
                                        titleLength <= 10 -> 27.sp
                                        titleLength <= 14 -> 24.sp
                                        titleLength <= 18 -> 21.sp
                                        titleLength <= 22 -> 18.sp
                                        titleLength <= 26 -> 16.sp
                                        titleLength <= 32 -> 14.sp
                                        titleLength <= 38 -> 12.sp
                                        else -> 10.sp
                                    }
                                } else {
                                    when {
                                        titleLength <= 22 -> 26.sp
                                        titleLength <= 38 -> 22.sp
                                        titleLength <= 58 -> 19.sp
                                        else -> 17.sp
                                    }
                                }
                                val adaptiveSpacing = if (singleWordTitle) 0.2.sp else if (titleLength <= 30) 1.2.sp else 0.3.sp
                                Text(
                                    cleanTitle.uppercase(),
                                    fontSize = adaptiveTitleSize,
                                    lineHeight = adaptiveTitleSize * 1.08f,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = adaptiveSpacing,
                                    color = Color.White,
                                    maxLines = if (singleWordTitle) 1 else 2,
                                    softWrap = !singleWordTitle,
                                    overflow = if (singleWordTitle) TextOverflow.Clip else TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { vm.setWatched(current, !current.watched) },
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = .14f), CircleShape)
                                ) {
                                    Icon(
                                        if (current.watched) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (current.watched) "Vu" else "Pas vu",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                current.year?.let { CinemaMeta(Icons.Default.CalendarMonth, it.toString()) }
                                current.durationMinutes?.let { CinemaMeta(Icons.Default.Schedule, "${it / 60}h ${it % 60}min") }
                                Surface(
                                    onClick = { ratingDialogOpen = true },
                                    color = Color.Transparent,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            current.rating?.let { "$it/5" } ?: "-/5",
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 16.sp
                                        )
                                        Icon(
                                            Icons.Default.Star,
                                            contentDescription = "Noter le film",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    if (cinemaState is CinemaUiState.Loading) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                    }
                    PremiumButton(
                        if (trailer != null) "Regarder la bande-annonce" else "Bande-annonce indisponible",
                        { Icon(Icons.Default.PlayCircle, null) },
                        onClick = {
                            if (trailer != null) {
                                val uri = Uri.parse("https://www.youtube.com/watch?v=${trailer.key}")
                                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                                    .onFailure { Toast.makeText(context, "Impossible d'ouvrir la bande-annonce.", Toast.LENGTH_SHORT).show() }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = trailer != null
                    )
                }

                if (current.synopsis.isNotBlank()) {
                    item {
                        Text(
                            current.synopsis,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = .88f)
                        )
                    }
                }

                if (current.director.isNotBlank() || current.actors.isNotBlank() || current.genre.isNotBlank()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xD012141B)),
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .45f))
                        ) {
                            Column(
                                Modifier.fillMaxWidth().padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                if (current.director.isNotBlank()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("RÉALISATEUR", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                                        Text(current.director, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                                if (current.actors.isNotBlank()) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .3f))
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("ACTEURS PRINCIPAUX", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                                        Text(current.actors, color = Color.White, fontSize = 16.sp, lineHeight = 23.sp)
                                    }
                                }
                                if (current.genre.isNotBlank()) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .3f))
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("GENRE", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                                        Text(current.genre, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }
                }

                sagaFor(current)?.let { guide ->
                    item {
                        Column {
                            Text("SAGA", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(6.dp))
                            SagaSection(guide, all)
                        }
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PremiumOutlineButton(
                            if (current.status == MovieStatus.OWNED) "Souhait" else "Acheté",
                            { Icon(Icons.Default.Favorite, null) },
                            { vm.toggleStatus(current) },
                            Modifier.weight(1f)
                        )
                        PremiumOutlineButton(
                            "Ce soir",
                            { Icon(Icons.Default.Casino, null) },
                            onWatchGuide,
                            Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 4.dp, top = 4.dp)
                .background(Color(0x88000000), CircleShape)
        ) {
            Icon(Icons.Default.ArrowBack, "Retour", tint = Color.White)
        }

        Box(
            Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(end = 4.dp, top = 4.dp)
        ) {
            IconButton(
                onClick = { menuOpen = true },
                modifier = Modifier.background(Color(0x88000000), CircleShape)
            ) {
                Icon(Icons.Default.MoreVert, "Menu", tint = Color.White)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(if (current?.status == MovieStatus.OWNED) "Ajouter aux souhaits" else "Ajouter à la bibliothèque") },
                    leadingIcon = { Icon(Icons.Default.FavoriteBorder, null) },
                    onClick = {
                        menuOpen = false
                        current?.let(vm::toggleStatus)
                    }
                )
                DropdownMenuItem(
                    text = { Text(if (current?.watched == true) "Marquer comme non vu" else "Marquer comme vu") },
                    leadingIcon = { Icon(Icons.Default.Visibility, null) },
                    onClick = {
                        menuOpen = false
                        current?.let { vm.setWatched(it, !it.watched) }
                    }
                )
                if (current?.tmdbId != null) {
                    DropdownMenuItem(
                        text = { Text("Actualiser depuis TMDB") },
                        leadingIcon = { Icon(Icons.Default.Refresh, null) },
                        onClick = {
                            menuOpen = false
                            current?.let { film ->
                                vm.refreshMovieFromTmdb(
                                    film,
                                    onDone = { Toast.makeText(context, "Informations actualisées.", Toast.LENGTH_SHORT).show() },
                                    onError = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                                )
                            }
                        }
                    )
                }
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Supprimer") },
                    leadingIcon = { Icon(Icons.Default.Delete, null) },
                    onClick = {
                        menuOpen = false
                        confirmDelete = true
                    }
                )
            }
        }
    }

    if (ratingDialogOpen && current != null) {
        AlertDialog(
            onDismissRequest = { ratingDialogOpen = false },
            icon = { Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Noter ${current.title}") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    RatingRow(current.rating ?: 0) { value ->
                        vm.setRating(current, value)
                        ratingDialogOpen = false
                    }
                    if (current.rating != null) {
                        TextButton(onClick = {
                            vm.save(current.copy(rating = null))
                            ratingDialogOpen = false
                        }) { Text("Retirer la note") }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { ratingDialogOpen = false }) { Text("Annuler") } }
        )
    }

    if (confirmDelete && current != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            icon = { Icon(Icons.Default.Delete, null) },
            title = { Text("Supprimer ce film ?") },
            text = { Text("« ${current.title} » sera supprimé de Reelio.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    vm.delete(current, onBack)
                }) { Text("Supprimer") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Annuler") } }
        )
    }
}

@Composable
private fun CinemaModeScreen(
    id: Long,
    vm: MovieViewModel,
    onBack: () -> Unit
) {
    val movie by vm.movie(id).collectAsStateWithLifecycle(initialValue = null)
    val cinemaState by vm.cinemaState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val fade = remember { Animatable(0f) }

    LaunchedEffect(movie?.tmdbId) {
        fade.snapTo(0f)
        vm.resetCinema()
        vm.loadCinema(movie?.tmdbId)
        fade.animateTo(1f, animationSpec = tween(450))
    }

    DisposableEffect(Unit) {
        onDispose { vm.resetCinema() }
    }

    val current = movie
    val ready = cinemaState as? CinemaUiState.Ready
    val details = ready?.details
    val backdrop = details?.backdropUrl.orEmpty()
    val trailer = ready?.trailer

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF05060A))
            .graphicsLayer(alpha = fade.value)
    ) {
        if (backdrop.isNotBlank()) {
            AsyncImage(
                model = backdrop,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = .58f
            )
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color(0x8F000000),
                    .38f to Color(0x33000000),
                    .72f to Color(0xEE05060A),
                    1f to Color(0xFF05060A)
                )
            )
        )

        if (current == null) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 70.dp, bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                item {
                    Column(
                        Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            ReelioReelLogo(Modifier.size(30.dp))
                            Text(
                                "MODE CINÉMA",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.4.sp
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Poster(
                            current.posterUrl,
                            current.title,
                            Modifier.width(200.dp).height(300.dp).shadow(18.dp, RoundedCornerShape(18.dp))
                        )
                        Text(
                            current.title.uppercase(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            letterSpacing = 2.sp,
                            color = Color.White
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            current.year?.let { CinemaMeta(Icons.Default.CalendarMonth, it.toString()) }
                            current.durationMinutes?.let { CinemaMeta(Icons.Default.Schedule, "${it / 60}h ${it % 60}min") }
                            if (current.watched) CinemaMeta(Icons.Default.CheckCircle, "Vu")
                        }
                    }
                }

                item {
                    if (cinemaState is CinemaUiState.Loading) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                    }
                    PremiumButton(
                        if (trailer != null) "Regarder la bande-annonce" else "Bande-annonce indisponible",
                        { Icon(Icons.Default.PlayCircle, null) },
                        onClick = {
                            if (trailer != null) {
                                val uri = Uri.parse("https://www.youtube.com/watch?v=${trailer.key}")
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                }.onFailure {
                                    Toast.makeText(context, "Impossible d'ouvrir la bande-annonce.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = trailer != null
                    )
                }

                if (current.synopsis.isNotBlank()) {
                    item {
                        Text(
                            current.synopsis,
                            color = Color.White.copy(alpha = .88f),
                            style = MaterialTheme.typography.bodyLarge,
                            lineHeight = 24.sp
                        )
                    }
                }

                item {
                    HorizontalDivider(color = Color.White.copy(alpha = .18f))
                    Spacer(Modifier.height(14.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        if (current.director.isNotBlank()) {
                            CinemaCredit("RÉALISATEUR", current.director, Modifier.weight(1f))
                        }
                        if (current.actors.isNotBlank()) {
                            CinemaCredit("AVEC", current.actors, Modifier.weight(1f))
                        }
                    }
                }

                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CinemaStatusCard(
                            icon = if (current.status == MovieStatus.OWNED) Icons.Default.Bookmark else Icons.Default.Favorite,
                            text = if (current.status == MovieStatus.OWNED) "Dans ma bibliothèque" else "Dans mes souhaits",
                            Modifier.weight(1f)
                        )
                        CinemaStatusCard(
                            icon = if (current.watched) Icons.Default.CheckCircle else Icons.Default.Visibility,
                            text = if (current.watched) "Déjà vu" else "À voir",
                            Modifier.weight(1f)
                        )
                        CinemaStatusCard(
                            icon = Icons.Default.Star,
                            text = current.rating?.let { "$it / 5" } ?: "Non noté",
                            Modifier.weight(1f)
                        )
                    }
                }

                if (cinemaState is CinemaUiState.Error) {
                    item {
                        Text(
                            (cinemaState as CinemaUiState.Error).message,
                            color = Color.White.copy(alpha = .55f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(10.dp)
                .background(Color.Black.copy(alpha = .55f), CircleShape)
        ) {
            Icon(Icons.Default.Close, "Fermer le mode cinéma", tint = Color.White)
        }
    }
}

@Composable
private fun CinemaMeta(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
        Text(text, color = Color.White, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun CinemaCredit(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Text(value, color = Color.White, maxLines = 4, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun CinemaStatusCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = .48f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .45f))
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Text(
                text,
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 2
            )
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
    Row { (1..5).forEach { n -> IconButton(onClick = { onRate(n) }, modifier = Modifier.size(34.dp)) { Icon(if (n <= rating) Icons.Default.Star else Icons.Default.StarBorder, "Note $n", tint = if (n <= rating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) } } }
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
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ScreenHeading(
                    "Que regarder ce soir ?",
                    "Trouvons le film parfait pour votre soirée",
                    Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .45f))
                ) {
                    Box(
                        modifier = Modifier.size(58.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Casino,
                            contentDescription = "Dé",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

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
                    Button(
                        onClick = onPick,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 82.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Shuffle, null, modifier = Modifier.size(22.dp))
                                Text("Aléatoire", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Un film au hasard dans votre bibliothèque",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
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
    var showAbout by remember { mutableStateOf(false) }
    var resetStep by remember { mutableIntStateOf(0) }

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
            ReelioTopBar("Paramètres")
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.brush_theme),
                            contentDescription = "Thème",
                            modifier = Modifier.size(24.dp),
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
                        )
                        Text("Thème", fontWeight = FontWeight.Bold)
                    }
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
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            Icons.Default.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text("Couleurs", fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "Choisissez la couleur des boutons, icônes et éléments actifs.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    AccentChoice.entries.chunked(6).forEach { rowChoices ->
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
                            repeat(6 - rowChoices.size) { Spacer(Modifier.size(38.dp)) }
                        }
                    }
                }
            }

            item {
                SettingsCard("") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Storage,
                            contentDescription = "Données",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text("Données", fontWeight = FontWeight.Bold)
                    }
                    SettingsActionRow(
                        icon = Icons.Default.Backup,
                        title = "Sauvegarder",
                        subtitle = "Crée une copie complète de votre collection"
                    ) { backupLauncher.launch("reelio-sauvegarde.json") }

                    SettingsActionRow(
                        icon = Icons.Default.Restore,
                        title = "Restaurer",
                        subtitle = "Récupère une sauvegarde Reelio"
                    ) { restoreLauncher.launch("application/json") }

                    SettingsActionRow(
                        icon = Icons.Default.FileDownload,
                        title = "Exporter en CSV",
                        subtitle = "Collection et souhaits dans un fichier CSV"
                    ) { exportLauncher.launch("reelio-collection.csv") }

                    SettingsActionRow(
                        icon = Icons.Default.RestartAlt,
                        title = "Réinitialiser Reelio",
                        subtitle = "Efface toutes les données locales"
                    ) { resetStep = 1 }
                }
            }

            item {
                SettingsCard("À PROPOS") {
                    SettingsActionRow(
                        icon = Icons.Default.Info,
                        title = "À propos de Reelio",
                        subtitle = "Version ${BuildConfig.VERSION_NAME}"
                    ) { showAbout = true }

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

    if (resetStep == 1) {
        AlertDialog(
            onDismissRequest = { resetStep = 0 },
            icon = { Icon(Icons.Default.Warning, null) },
            title = { Text("Réinitialiser Reelio ?") },
            text = { Text("Cette action supprimera votre bibliothèque et vos souhaits enregistrés sur cet appareil. Vous pouvez créer une sauvegarde avant de continuer.") },
            confirmButton = {
                TextButton(onClick = { resetStep = 2 }) { Text("Continuer") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        resetStep = 0
                        backupLauncher.launch("reelio-sauvegarde-avant-reset.json")
                    }) { Text("Sauvegarder d'abord") }
                    TextButton(onClick = { resetStep = 0 }) { Text("Annuler") }
                }
            }
        )
    }

    if (resetStep == 2) {
        AlertDialog(
            onDismissRequest = { resetStep = 0 },
            icon = { Icon(Icons.Default.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Confirmation définitive") },
            text = { Text("Toutes les données locales de Reelio seront supprimées. Cette action est irréversible sans sauvegarde.") },
            confirmButton = {
                TextButton(onClick = {
                    resetStep = 0
                    vm.resetAll { Toast.makeText(context, "Reelio a été réinitialisé", Toast.LENGTH_SHORT).show() }
                }) { Text("Tout effacer", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { resetStep = 0 }) { Text("Annuler") } }
        )
    }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            icon = { ReelioReelLogo(Modifier.size(54.dp)) },
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Reelio", fontWeight = FontWeight.ExtraBold, color = ReelioBrandPurple)
                    Text("Version ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Votre collection cinéma, simplement organisée.")
                    Text("by ED", fontWeight = FontWeight.Bold, color = ReelioBrandPurple)
                    HorizontalDivider()
                    Text("Reelio vous permet de gérer votre bibliothèque, vos souhaits, vos sagas et de choisir quoi regarder.")
                    Text("Données cinéma et affiches fournies par TMDB. Reelio utilise l’API TMDB mais n’est ni approuvé ni certifié par TMDB.", style = MaterialTheme.typography.bodySmall)
                    Text("Votre collection est enregistrée localement sur votre appareil.", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { showAbout = false }) { Text("Fermer") }
            }
        )
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
        if (title.isNotBlank()) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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
