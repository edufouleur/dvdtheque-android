package fr.dvdtheque.app.network

import fr.dvdtheque.app.BuildConfig
import fr.dvdtheque.app.data.Movie
import fr.dvdtheque.app.data.MovieStatus
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.OffsetDateTime

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

    suspend fun search(query: String): List<TmdbMovieResult> {
        val clean = query.trim()
        val movies = api.searchMovies(authorization, clean).results
        val series = api.searchTv(authorization, clean).results.map { it.asCatalogResult() }
        return (movies + series).sortedWith(compareBy<TmdbMovieResult> { if (it.mediaType == "movie") 0 else 1 }.thenByDescending { it.year ?: 0 })
    }

    suspend fun details(id: Int, mediaType: String = "movie"): TmdbMovieDetails =
        if (mediaType == "tv") api.tvDetails(authorization, id).asMovieDetails() else api.movieDetails(authorization, id)

    suspend fun trailer(id: Int, mediaType: String = "movie"): TmdbVideo? {
        fun choose(videos: List<TmdbVideo>): TmdbVideo? = videos
            .filter { it.site.equals("YouTube", true) && it.key.isNotBlank() }
            .sortedWith(compareByDescending<TmdbVideo> { it.official }.thenBy { video ->
                when {
                    video.type.equals("Trailer", true) -> 0
                    video.type.equals("Teaser", true) -> 1
                    else -> 2
                }
            })
            .firstOrNull()

        val frenchVideos = if (mediaType == "tv") api.tvVideos(authorization, id, "fr-FR").results else api.movieVideos(authorization, id, "fr-FR").results
        val french = choose(frenchVideos)
        if (french != null) return french
        val englishVideos = if (mediaType == "tv") api.tvVideos(authorization, id, "en-US").results else api.movieVideos(authorization, id, "en-US").results
        return choose(englishVideos)
    }
    suspend fun nowPlaying(): List<TmdbMovieResult> = api.nowPlaying(authorization).results.take(20)
    suspend fun popular(): List<TmdbMovieResult> = api.popular(authorization).results.take(20)

    suspend fun recommendations(movies: List<Movie>): List<TmdbMovieResult> {
        val seed = movies.filter { it.status == MovieStatus.OWNED && it.tmdbId != null && it.mediaType == "movie" }
            .maxByOrNull { it.addedAt }?.tmdbId
        return if (seed != null) api.recommendations(authorization, seed).results.take(20) else popular()
    }

    suspend fun physicalReleases(): List<TmdbMovieResult> {
        val candidates = api.popular(authorization).results.take(14)
        return candidates.filter { movie ->
            runCatching {
                val response = api.releaseDates(authorization, movie.id)
                response.results.firstOrNull { it.country == "FR" }
                    ?.releaseDates
                    ?.any { release -> release.type == 5 && isPast(release.date) } == true
            }.getOrDefault(false)
        }.take(12)
    }

    private fun isPast(raw: String): Boolean = runCatching {
        OffsetDateTime.parse(raw).isBefore(OffsetDateTime.now())
    }.getOrDefault(false)

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
            status = status,
            tmdbId = details.id,
            mediaType = details.mediaType,
            totalSeasons = details.totalSeasons,
            totalEpisodes = details.totalEpisodes
        )
    }
}
