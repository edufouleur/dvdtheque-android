package fr.dvdtheque.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.dvdtheque.app.data.*
import fr.dvdtheque.app.network.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface TmdbUiState {
    data object Idle : TmdbUiState
    data object Loading : TmdbUiState
    data class Results(val movies: List<TmdbMovieResult>) : TmdbUiState
    data class Preview(val details: TmdbMovieDetails) : TmdbUiState
    data class Error(val message: String) : TmdbUiState
}

class MovieViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MovieRepository(AppDatabase.get(application).movieDao())
    private val tmdbRepository = TmdbRepository()

    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    private val _sort = MutableStateFlow(MovieSort.TITLE_ASC)
    val sort = _sort.asStateFlow()

    private val _tmdbState = MutableStateFlow<TmdbUiState>(TmdbUiState.Idle)
    val tmdbState = _tmdbState.asStateFlow()

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

    fun loadTmdbDetails(id: Int) = viewModelScope.launch {
        _tmdbState.value = TmdbUiState.Loading
        _tmdbState.value = try { TmdbUiState.Preview(tmdbRepository.details(id)) }
        catch (e: Exception) { TmdbUiState.Error(e.message ?: "Impossible de charger les détails") }
    }

    fun addTmdbMovie(details: TmdbMovieDetails, status: MovieStatus, onSaved: () -> Unit) = viewModelScope.launch {
        repository.save(tmdbRepository.toMovie(details, status))
        _tmdbState.value = TmdbUiState.Idle
        onSaved()
    }

    fun resetTmdb() { _tmdbState.value = TmdbUiState.Idle }
    fun save(movie: Movie, onSaved: (() -> Unit)? = null) = viewModelScope.launch {
        repository.save(movie); onSaved?.invoke()
    }
    fun toggleStatus(movie: Movie) = save(movie.copy(status = if (movie.status == MovieStatus.OWNED) MovieStatus.WANTED else MovieStatus.OWNED))
    fun setWatched(movie: Movie, watched: Boolean) = save(movie.copy(watched = watched))
    fun setRating(movie: Movie, rating: Int) = save(movie.copy(rating = rating.coerceIn(1, 5)))
    fun delete(movie: Movie, onDeleted: (() -> Unit)? = null) = viewModelScope.launch {
        repository.delete(movie); onDeleted?.invoke()
    }
}
