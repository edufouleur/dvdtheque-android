package fr.dvdtheque.app.network

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApi {

    @GET("search/movie")
    suspend fun searchMovies(
        @Header("Authorization")
        authorization: String,

        @Query("query")
        query: String,

        @Query("language")
        language: String = "fr-FR",

        @Query("include_adult")
        includeAdult: Boolean = false,

        @Query("page")
        page: Int = 1
    ): TmdbSearchResponse

    @GET("movie/{movieId}")
    suspend fun movieDetails(
        @Header("Authorization")
        authorization: String,

        @Path("movieId")
        movieId: Int,

        @Query("language")
        language: String = "fr-FR",

        @Query("append_to_response")
        appendToResponse: String = "credits"
    ): TmdbMovieDetails
}
