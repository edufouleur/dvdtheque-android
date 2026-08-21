package fr.dvdtheque.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fr.dvdtheque.app.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MovieViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MovieRepository(AppDatabase.get(application).movieDao())

    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    private val _sort = MutableStateFlow(MovieSort.TITLE_ASC)
    val sort = _sort.asStateFlow()

    val movies: StateFlow<List<Movie>> = combine(repository.movies, _query, _sort) { movies, query, sort ->
        val filtered = if (query.isBlank()) movies else movies.filter {
            it.title.contains(query, true) ||
            it.director.contains(query, true) ||
            it.genre.contains(query, true) ||
            it.actors.contains(query, true)
        }
        when (sort) {
            MovieSort.TITLE_ASC -> filtered.sortedBy { it.title.lowercase() }
            MovieSort.TITLE_DESC -> filtered.sortedByDescending { it.title.lowercase() }
            MovieSort.YEAR_DESC -> filtered.sortedByDescending { it.year ?: 0 }
            MovieSort.YEAR_ASC -> filtered.sortedBy { it.year ?: Int.MAX_VALUE }
            MovieSort.RECENTLY_ADDED -> filtered.sortedByDescending { it.addedAt }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun movie(id: Long): Flow<Movie?> = repository.movie(id)
    fun setQuery(value: String) { _query.value = value }
    fun setSort(value: MovieSort) { _sort.value = value }

    fun save(movie: Movie, onSaved: (() -> Unit)? = null) = viewModelScope.launch {
        repository.save(movie)
        onSaved?.invoke()
    }

    fun toggleStatus(movie: Movie) = save(
        movie.copy(status = if (movie.status == MovieStatus.OWNED) MovieStatus.WANTED else MovieStatus.OWNED)
    )

    fun delete(movie: Movie, onDeleted: (() -> Unit)? = null) = viewModelScope.launch {
        repository.delete(movie)
        onDeleted?.invoke()
    }
}
