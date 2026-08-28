package fr.dvdtheque.app.network

import fr.dvdtheque.app.BuildConfig
import fr.dvdtheque.app.data.Movie
import fr.dvdtheque.app.data.MovieStatus
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class TmdbRepository {
    private val api: TmdbApi = Retrofit.Builder()
        .baseUrl("https://api.themoviedb.org/3/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(TmdbApi::class.java)

    private val authorization: String
        get() {
            val token = BuildConfig.TMDB_TOKEN.trim()
            require(token.isNotBlank()) { "Le jeton TMDB n'est pas configuré." }
            return "Bearer $token"
        }

    suspend fun search(query: String): List<TmdbMovieResult> = api.searchMovies(authorization, query.trim()).results
    suspend fun details(id: Int): TmdbMovieDetails = api.movieDetails(authorization, id)

    fun toMovie(details: TmdbMovieDetails, status: MovieStatus): Movie {
        val director = details.credits?.crew?.firstOrNull { it.job.equals("Director", true) }?.name.orEmpty()
        val actors = details.credits?.cast?.sortedBy { it.order }?.take(8)?.joinToString(", ") { it.name }.orEmpty()
        return Movie(
            title = details.title,
            originalTitle = details.originalTitle,
            year = details.year,
            director = director,
            actors = actors,
            genre = details.genres.joinToString(", ") { it.name },
            durationMinutes = details.runtime,
            synopsis = details.overview,
            posterUrl = details.posterUrl,
            status = status
        )
    }
}
