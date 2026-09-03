package fr.dvdtheque.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.dvdtheque.app.data.*
import fr.dvdtheque.app.network.*
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface TmdbUiState {
    data object Idle : TmdbUiState
    data object Loading : TmdbUiState
    data class Results(val movies: List<TmdbMovieResult>) : TmdbUiState
    data class Preview(val details: TmdbMovieDetails) : TmdbUiState
    data class Error(val message: String) : TmdbUiState
}

data class DiscoveryState(
    val loading: Boolean = false,
    val cinema: List<TmdbMovieResult> = emptyList(),
    val forYou: List<TmdbMovieResult> = emptyList(),
    val physical: List<TmdbMovieResult> = emptyList(),
    val error: String? = null
)

sealed interface CinemaUiState {
    data object Idle : CinemaUiState
    data object Loading : CinemaUiState
    data class Ready(val details: TmdbMovieDetails, val trailer: TmdbVideo?) : CinemaUiState
    data class Error(val message: String) : CinemaUiState
}

class MovieViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MovieRepository(AppDatabase.get(application).movieDao())
    private val tmdbRepository = TmdbRepository()
    private val backupManager = BackupManager(application, repository)

    init {
        viewModelScope.launch { backupManager.ensureDailyBackup() }
    }

    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    private val _sort = MutableStateFlow(MovieSort.TITLE_ASC)
    val sort = _sort.asStateFlow()

    private val _tmdbState = MutableStateFlow<TmdbUiState>(TmdbUiState.Idle)
    val tmdbState = _tmdbState.asStateFlow()

    private val _discovery = MutableStateFlow(DiscoveryState())
    val discovery = _discovery.asStateFlow()

    private val _cinemaState = MutableStateFlow<CinemaUiState>(CinemaUiState.Idle)
    val cinemaState = _cinemaState.asStateFlow()

    val movies: StateFlow<List<Movie>> = combine(repository.movies, _query, _sort) { movies, query, sort ->
        val filtered = if (query.isBlank()) movies else movies.filter {
            it.title.contains(query, true) || it.director.contains(query, true) ||
                it.genre.contains(query, true) || it.actors.contains(query, true)
        }
        when (sort) {
            MovieSort.TITLE_ASC -> filtered.sortedBy { it.title.lowercase() }
            MovieSort.TITLE_DESC -> filtered.sortedByDescending { it.title.lowercase() }
            MovieSort.YEAR_DESC -> filtered.sortedByDescending { it.year ?: 0 }
            MovieSort.YEAR_ASC -> filtered.sortedBy { it.year ?: Int.MAX_VALUE }
            MovieSort.RECENTLY_ADDED -> filtered.sortedByDescending { it.addedAt }
            MovieSort.RATING_DESC -> filtered.sortedByDescending { it.rating ?: 0 }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun movie(id: Long): Flow<Movie?> = repository.movie(id)
    fun setQuery(value: String) { _query.value = value }
    fun setSort(value: MovieSort) { _sort.value = value }

    fun searchTmdb(query: String) = viewModelScope.launch {
        if (query.isBlank()) return@launch
        _tmdbState.value = TmdbUiState.Loading
        _tmdbState.value = try { TmdbUiState.Results(tmdbRepository.search(query)) }
        catch (e: Exception) { TmdbUiState.Error(e.message ?: "Erreur de recherche TMDB") }
    }

    fun loadTmdbDetails(id: Int, mediaType: String = "movie") = viewModelScope.launch {
        _tmdbState.value = TmdbUiState.Loading
        _tmdbState.value = try { TmdbUiState.Preview(tmdbRepository.details(id, mediaType)) }
        catch (e: Exception) { TmdbUiState.Error(e.message ?: "Impossible de charger les détails") }
    }

    fun loadDiscovery() = viewModelScope.launch {
        if (_discovery.value.loading) return@launch
        _discovery.value = _discovery.value.copy(loading = true, error = null)
        try {
            val current = movies.value
            val cinema = async { tmdbRepository.nowPlaying() }
            val forYou = async { tmdbRepository.recommendations(current) }
            val physical = async { tmdbRepository.physicalReleases() }
            _discovery.value = DiscoveryState(
                cinema = cinema.await(),
                forYou = forYou.await(),
                physical = physical.await()
            )
        } catch (e: Exception) {
            _discovery.value = DiscoveryState(error = e.message ?: "Impossible de charger les suggestions")
        }
    }

    fun addTmdbMovie(
        details: TmdbMovieDetails,
        status: MovieStatus,
        onSaved: () -> Unit,
        onDuplicate: ((String) -> Unit)? = null
    ) = viewModelScope.launch {
        val candidate = tmdbRepository.toMovie(details, status)
        val existing = repository.findDuplicate(candidate)
        if (existing != null) {
            if (existing.status == MovieStatus.WANTED && status == MovieStatus.OWNED) {
                repository.save(existing.copy(
                    status = MovieStatus.OWNED,
                    title = candidate.title,
                    originalTitle = candidate.originalTitle,
                    year = candidate.year,
                    director = candidate.director,
                    actors = candidate.actors,
                    genre = candidate.genre,
                    durationMinutes = candidate.durationMinutes,
                    synopsis = candidate.synopsis,
                    posterUrl = candidate.posterUrl,
                    tmdbId = candidate.tmdbId,
                    mediaType = candidate.mediaType,
                    totalSeasons = candidate.totalSeasons,
                    totalEpisodes = candidate.totalEpisodes
                ))
                backupManager.createActiveBackup()
                _tmdbState.value = TmdbUiState.Idle
                onSaved()
            } else {
                val kind = if (candidate.mediaType == "tv") "Cette série" else "Ce film"
                onDuplicate?.invoke("$kind est déjà dans ${if (existing.status == MovieStatus.OWNED) "votre bibliothèque" else "vos souhaits"}.")
            }
            return@launch
        }
        repository.save(candidate)
        backupManager.createActiveBackup()
        _tmdbState.value = TmdbUiState.Idle
        onSaved()
    }

    fun addSuggestionToWishlist(result: TmdbMovieResult, onSaved: (() -> Unit)? = null) = viewModelScope.launch {
        try {
            val details = tmdbRepository.details(result.id, result.mediaType)
            val candidate = tmdbRepository.toMovie(details, MovieStatus.WANTED)
            if (repository.findDuplicate(candidate) == null) {
                repository.save(candidate)
                backupManager.createActiveBackup()
                onSaved?.invoke()
            }
        } catch (_: Exception) { }
    }

    fun addManualMovie(movie: Movie, onSaved: () -> Unit, onDuplicate: ((String) -> Unit)? = null) = viewModelScope.launch {
        val existing = repository.findDuplicate(movie)
        if (existing != null) {
            val kind = if (movie.mediaType == "tv") "Cette série" else "Ce film"
            onDuplicate?.invoke("$kind est déjà enregistré dans Reelio.")
            return@launch
        }
        repository.save(movie)
        backupManager.createActiveBackup()
        onSaved()
    }

    fun resetTmdb() { _tmdbState.value = TmdbUiState.Idle }

    fun loadCinema(tmdbId: Int?, mediaType: String = "movie") = viewModelScope.launch {
        if (tmdbId == null) {
            _cinemaState.value = CinemaUiState.Error("Aucune fiche TMDB liée à ce film.")
            return@launch
        }
        _cinemaState.value = CinemaUiState.Loading
        _cinemaState.value = try {
            val details = tmdbRepository.details(tmdbId, mediaType)
            val trailer = runCatching { tmdbRepository.trailer(tmdbId, mediaType) }.getOrNull()
            CinemaUiState.Ready(details, trailer)
        } catch (e: Exception) {
            CinemaUiState.Error(e.message ?: "Impossible de charger le mode cinéma")
        }
    }

    fun resetCinema() { _cinemaState.value = CinemaUiState.Idle }
    fun save(movie: Movie, onSaved: (() -> Unit)? = null) = viewModelScope.launch {
        repository.save(movie)
        backupManager.createActiveBackup()
        onSaved?.invoke()
    }
    fun toggleStatus(movie: Movie) = save(movie.copy(status = if (movie.status == MovieStatus.OWNED) MovieStatus.WANTED else MovieStatus.OWNED))
    fun setWatched(movie: Movie, watched: Boolean) = save(movie.copy(watched = watched))
    fun setRating(movie: Movie, rating: Int) = save(movie.copy(rating = rating.coerceIn(1, 5)))

    fun refreshMovieFromTmdb(
        movie: Movie,
        onDone: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) = viewModelScope.launch {
        val tmdbId = movie.tmdbId ?: run {
            onError?.invoke("Ce film n'est pas lié à TMDB.")
            return@launch
        }
        try {
            val details = tmdbRepository.details(tmdbId, movie.mediaType)
            val refreshed = tmdbRepository.toMovie(details, movie.status).copy(
                id = movie.id,
                rating = movie.rating,
                notes = movie.notes,
                edition = movie.edition,
                discCount = movie.discCount,
                location = movie.location,
                addedAt = movie.addedAt,
                watched = movie.watched,
                mediaType = movie.mediaType,
                totalSeasons = details.totalSeasons ?: movie.totalSeasons,
                totalEpisodes = details.totalEpisodes ?: movie.totalEpisodes,
                ownedSeasons = movie.ownedSeasons,
                currentSeason = movie.currentSeason,
                currentEpisode = movie.currentEpisode
            )
            repository.save(refreshed)
            backupManager.createActiveBackup()
            onDone?.invoke()
        } catch (e: Exception) {
            onError?.invoke(e.message ?: "Impossible d'actualiser ce film.")
        }
    }

    fun delete(movie: Movie, onDeleted: (() -> Unit)? = null) = viewModelScope.launch {
        repository.delete(movie)
        backupManager.createActiveBackup()
        onDeleted?.invoke()
    }
    fun restoreMovies(
        restored: List<Movie>,
        onDone: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) = viewModelScope.launch {
        try {
            // Le fichier a déjà été entièrement décodé/validé avant cette étape.
            repository.replaceAll(restored)
            backupManager.createActiveBackup()
            onDone?.invoke()
        } catch (e: Exception) {
            onError?.invoke(e.message ?: "Impossible de restaurer cette sauvegarde")
        }
    }

    fun moveToLibrary(
        movie: Movie,
        onDone: (() -> Unit)? = null,
        onInfo: ((String) -> Unit)? = null
    ) = viewModelScope.launch {
        try {
            if (movie.status == MovieStatus.OWNED) {
                onDone?.invoke()
                return@launch
            }
            val duplicate = repository.findDuplicate(movie.copy(status = MovieStatus.OWNED), excludingId = movie.id)
            if (duplicate != null && duplicate.status == MovieStatus.OWNED) {
                repository.delete(movie)
                onInfo?.invoke(if (movie.mediaType == "tv") "Cette série est déjà dans votre bibliothèque." else "Ce film est déjà dans votre bibliothèque.")
            } else {
                repository.save(movie.copy(status = MovieStatus.OWNED))
            }
            backupManager.createActiveBackup()
            onDone?.invoke()
        } catch (e: Exception) {
            onInfo?.invoke(e.message ?: "Impossible d'ajouter ce titre à la bibliothèque")
        }
    }

    fun toggleOwnedSeason(movie: Movie, season: Int) {
        val current = movie.ownedSeasons.split(",").mapNotNull { it.toIntOrNull() }.toMutableSet()
        if (!current.add(season)) current.remove(season)
        save(movie.copy(ownedSeasons = current.sorted().joinToString(",")))
    }

    fun setSeriesProgress(movie: Movie, season: Int?, episode: Int?) =
        save(movie.copy(currentSeason = season, currentEpisode = episode))

    fun backupFolderPath(): String = backupManager.folderPath()
    fun latestDailyBackupName(): String? = backupManager.dailyFile()?.name
    fun activeBackupNames(): List<String> = backupManager.activeFiles().map { it.name }
    fun allInternalBackupNames(): List<String> = backupManager.allBackupNames()

    fun restoreInternalBackup(name: String, onDone: (() -> Unit)? = null, onError: ((String) -> Unit)? = null) = viewModelScope.launch {
        try {
            repository.replaceAll(backupManager.read(name))
            backupManager.createActiveBackup()
            onDone?.invoke()
        } catch (e: Exception) {
            onError?.invoke(e.message ?: "Sauvegarde invalide")
        }
    }

    fun resetAll(onDone: (() -> Unit)? = null) = viewModelScope.launch {
        repository.deleteAll()
        backupManager.createActiveBackup()
        _query.value = ""
        _sort.value = MovieSort.TITLE_ASC
        _tmdbState.value = TmdbUiState.Idle
        _discovery.value = DiscoveryState()
        _cinemaState.value = CinemaUiState.Idle
        onDone?.invoke()
    }
}
