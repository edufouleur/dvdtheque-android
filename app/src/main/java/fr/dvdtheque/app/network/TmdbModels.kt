package fr.dvdtheque.app.network

import com.google.gson.annotations.SerializedName

data class TmdbSearchResponse(val results: List<TmdbMovieResult> = emptyList())

data class TmdbMovieResult(
    val id: Int,
    val title: String,
    @SerializedName("original_title") val originalTitle: String = "",
    @SerializedName("release_date") val releaseDate: String = "",
    @SerializedName("poster_path") val posterPath: String? = null,
    val overview: String = ""
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
    val genres: List<TmdbGenre> = emptyList(),
    val credits: TmdbCredits? = null
) {
    val year: Int? get() = releaseDate.take(4).toIntOrNull()
    val posterUrl: String get() = posterPath?.let { "https://image.tmdb.org/t/p/w500$it" } ?: ""
}

data class TmdbGenre(val id: Int, val name: String)
data class TmdbCredits(val cast: List<TmdbCast> = emptyList(), val crew: List<TmdbCrew> = emptyList())
data class TmdbCast(val id: Int, val name: String, val order: Int = 0)
data class TmdbCrew(val id: Int, val name: String, val job: String = "")
