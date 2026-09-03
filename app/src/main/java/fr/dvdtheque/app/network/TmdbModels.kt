package fr.dvdtheque.app.network

import com.google.gson.annotations.SerializedName

data class TmdbSearchResponse(val results: List<TmdbMovieResult> = emptyList())
data class TmdbTvSearchResponse(val results: List<TmdbTvResult> = emptyList())

data class TmdbMovieResult(
    val id: Int,
    val title: String,
    @SerializedName("original_title") val originalTitle: String = "",
    @SerializedName("release_date") val releaseDate: String = "",
    @SerializedName("poster_path") val posterPath: String? = null,
    val overview: String = "",
    val mediaType: String = "movie"
) {
    val year: Int? get() = releaseDate.take(4).toIntOrNull()
    val posterUrl: String get() = posterPath?.let { "https://image.tmdb.org/t/p/w500$it" } ?: ""
}

data class TmdbMovieDetails(
    val id: Int,
    val title: String,
    @SerializedName("original_title") val originalTitle: String = "",
    @SerializedName("release_date") val releaseDate: String = "",
    val runtime: Int? = null,
    val overview: String = "",
    @SerializedName("poster_path") val posterPath: String? = null,
    @SerializedName("backdrop_path") val backdropPath: String? = null,
    val genres: List<TmdbGenre> = emptyList(),
    val credits: TmdbCredits? = null,
    val mediaType: String = "movie",
    val totalSeasons: Int? = null,
    val totalEpisodes: Int? = null
) {
    val year: Int? get() = releaseDate.take(4).toIntOrNull()
    val posterUrl: String get() = posterPath?.let { "https://image.tmdb.org/t/p/w500$it" } ?: ""
    val backdropUrl: String get() = backdropPath?.let { "https://image.tmdb.org/t/p/w1280$it" } ?: ""
}

data class TmdbGenre(val id: Int, val name: String)
data class TmdbCredits(val cast: List<TmdbCast> = emptyList(), val crew: List<TmdbCrew> = emptyList())
data class TmdbCast(val id: Int, val name: String, val order: Int = 0)
data class TmdbCrew(val id: Int, val name: String, val job: String = "")

data class TmdbReleaseDatesResponse(val results: List<TmdbCountryRelease> = emptyList())
data class TmdbCountryRelease(
    @SerializedName("iso_3166_1") val country: String = "",
    @SerializedName("release_dates") val releaseDates: List<TmdbReleaseDate> = emptyList()
)
data class TmdbReleaseDate(
    @SerializedName("release_date") val date: String = "",
    val type: Int = 0
)


data class TmdbVideoResponse(val results: List<TmdbVideo> = emptyList())
data class TmdbVideo(
    val id: String = "",
    val key: String = "",
    val name: String = "",
    val site: String = "",
    val type: String = "",
    val official: Boolean = false
)


data class TmdbTvResult(
    val id: Int,
    val name: String,
    @SerializedName("original_name") val originalName: String = "",
    @SerializedName("first_air_date") val firstAirDate: String = "",
    @SerializedName("poster_path") val posterPath: String? = null,
    val overview: String = ""
) {
    val year: Int? get() = firstAirDate.take(4).toIntOrNull()
    val posterUrl: String get() = posterPath?.let { "https://image.tmdb.org/t/p/w500$it" } ?: ""
    fun asCatalogResult() = TmdbMovieResult(id, name, originalName, firstAirDate, posterPath, overview, "tv")
}

data class TmdbTvDetails(
    val id: Int,
    val name: String,
    @SerializedName("original_name") val originalName: String = "",
    @SerializedName("first_air_date") val firstAirDate: String = "",
    val overview: String = "",
    @SerializedName("poster_path") val posterPath: String? = null,
    @SerializedName("backdrop_path") val backdropPath: String? = null,
    val genres: List<TmdbGenre> = emptyList(),
    val credits: TmdbCredits? = null,
    @SerializedName("number_of_seasons") val numberOfSeasons: Int? = null,
    @SerializedName("number_of_episodes") val numberOfEpisodes: Int? = null,
    @SerializedName("episode_run_time") val episodeRunTime: List<Int> = emptyList()
) {
    fun asMovieDetails() = TmdbMovieDetails(
        id = id, title = name, originalTitle = originalName, releaseDate = firstAirDate,
        runtime = episodeRunTime.firstOrNull(), overview = overview, posterPath = posterPath,
        backdropPath = backdropPath, genres = genres, credits = credits, mediaType = "tv",
        totalSeasons = numberOfSeasons, totalEpisodes = numberOfEpisodes
    )
}
