package fr.dvdtheque.app.network

import fr.dvdtheque.app.BuildConfig
import fr.dvdtheque.app.data.Movie
import fr.dvdtheque.app.data.MovieStatus
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.OffsetDateTime
import java.text.Normalizer
import java.util.Locale

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
        val clean = query.trim().replace("\\s+".toRegex(), " ")
        if (clean.isBlank()) return emptyList()

        val collected = linkedMapOf<String, TmdbMovieResult>()
        suspend fun collect(term: String) {
            if (term.isBlank()) return
            val movies = api.searchMovies(authorization, term).results.map { it.copy(mediaType = "movie") }
            val series = api.searchTv(authorization, term).results.map { it.asCatalogResult() }
            (movies + series).forEach { result -> collected["${result.mediaType}:${result.id}"] = result }
        }

        // Première recherche : saisie exacte de l'utilisateur.
        collect(clean)

        // Si TMDB donne peu de résultats, Reelio élargit automatiquement la requête.
        // Cela permet notamment « sens de la fete » -> « Le Sens de la fête ».
        if (collected.size < 8) {
            val withoutLeadingArticle = removeLeadingArticle(clean)
            if (!withoutLeadingArticle.equals(clean, ignoreCase = true)) collect(withoutLeadingArticle)

            val normalizedWords = normalizeSearch(clean)
            if (normalizedWords.isNotBlank() && !normalizedWords.equals(normalizeSearch(withoutLeadingArticle), true)) {
                collect(normalizedWords)
            }

            if (!startsWithFrenchArticle(clean)) {
                for (article in listOf("le", "la", "les", "un", "une")) {
                    collect("$article $clean")
                    if (collected.size >= 20) break
                }
            }
        }

        return collected.values
            .sortedWith(
                compareByDescending<TmdbMovieResult> { relevanceScore(clean, it) }
                    .thenByDescending { it.year ?: 0 }
            )
            .take(40)
    }

    private fun startsWithFrenchArticle(value: String): Boolean {
        val first = normalizeBasic(value).substringBefore(' ')
        return first in setOf("le", "la", "les", "l", "un", "une", "des")
    }

    private fun removeLeadingArticle(value: String): String {
        val cleaned = value.trim()
        return cleaned.replace(Regex("(?i)^(le|la|les|un|une|des)\\s+|^l[’']\\s*"), "").trim()
    }

    private fun normalizeBasic(value: String): String = Normalizer
        .normalize(value.lowercase(Locale.FRENCH), Normalizer.Form.NFD)
        .replace("\\p{M}+".toRegex(), "")
        .replace("[’']".toRegex(), " ")
        .replace("[^a-z0-9]+".toRegex(), " ")
        .trim()
        .replace("\\s+".toRegex(), " ")

    private fun normalizeSearch(value: String): String = normalizeBasic(value)
        .split(" ")
        .filter { it.isNotBlank() && it !in setOf("le", "la", "les", "l", "un", "une", "des") }
        .joinToString(" ")

    private fun relevanceScore(query: String, result: TmdbMovieResult): Int {
        val q = normalizeSearch(query)
        val title = normalizeSearch(result.title)
        val original = normalizeSearch(result.originalTitle)
        if (q.isBlank()) return 0
        val words = q.split(" ").filter { it.isNotBlank() }
        return maxOf(scoreAgainst(q, words, title), scoreAgainst(q, words, original))
    }

    private fun scoreAgainst(query: String, words: List<String>, candidate: String): Int {
        if (candidate.isBlank()) return 0
        var score = 0
        if (candidate == query) score += 200
        if (candidate.startsWith(query)) score += 100
        if (candidate.contains(query)) score += 70
        val matches = words.count { candidate.split(" ").contains(it) }
        score += matches * 20
        if (words.isNotEmpty() && matches == words.size) score += 50
        return score
    }

    suspend fun details(id: Int, mediaType: String = "movie"): TmdbMovieDetails =
        if (mediaType == "tv") {
            api.tvDetails(authorization, id).asMovieDetails()
        } else {
            // Même protection pour movie/{id}: le JSON TMDB ne contient pas mediaType.
            api.movieDetails(authorization, id).copy(mediaType = "movie")
        }

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
        val owned = movies.filter { it.status == MovieStatus.OWNED && it.tmdbId != null }
            .sortedByDescending { it.addedAt }
        val excluded = movies.mapNotNull { movie -> movie.tmdbId?.let { id -> id to movie.mediaType } }.toSet()
        val suggestions = linkedMapOf<Pair<Int, String>, TmdbMovieResult>()

        // 1) Priorité aux suites / trilogies / sagas des films déjà possédés.
        owned.filter { it.mediaType == "movie" }.take(8).forEach { movie ->
            runCatching {
                val details = api.movieDetails(authorization, movie.tmdbId!!)
                val collectionId = details.belongsToCollection?.id ?: return@runCatching
                api.collectionDetails(authorization, collectionId).parts
                    .sortedBy { it.releaseDate }
                    .forEach { part ->
                        val normalized = part.copy(mediaType = "movie")
                        val key = normalized.id to "movie"
                        if (key !in excluded) suggestions.putIfAbsent(key, normalized)
                    }
            }
        }

        // 2) Recommandations TMDB issues de plusieurs titres possédés, films et séries.
        owned.take(8).forEach { movie ->
            runCatching {
                val results = if (movie.mediaType == "tv") {
                    api.tvRecommendations(authorization, movie.tmdbId!!).results.map { it.asCatalogResult() }
                } else {
                    api.recommendations(authorization, movie.tmdbId!!).results.map { it.copy(mediaType = "movie") }
                }
                results.take(8).forEach { candidate ->
                    val key = candidate.id to candidate.mediaType
                    if (key !in excluded) suggestions.putIfAbsent(key, candidate)
                }
            }
        }

        // 3) Repli sur les films populaires si la collection est encore trop petite.
        if (suggestions.size < 12) {
            popular().forEach { candidate ->
                val key = candidate.id to "movie"
                if (key !in excluded) suggestions.putIfAbsent(key, candidate.copy(mediaType = "movie"))
            }
        }
        return suggestions.values.take(24)
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
