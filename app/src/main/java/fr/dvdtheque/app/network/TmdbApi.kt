package fr.dvdtheque.app.network

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApi {
    @GET("search/movie")
    suspend fun searchMovies(
        @Header("Authorization") authorization: String,
        @Query("query") query: String,
        @Query("language") language: String = "fr-FR",
        @Query("include_adult") includeAdult: Boolean = false,
        @Query("page") page: Int = 1
    ): TmdbSearchResponse


    @GET("search/tv")
    suspend fun searchTv(
        @Header("Authorization") authorization: String,
        @Query("query") query: String,
        @Query("language") language: String = "fr-FR",
        @Query("include_adult") includeAdult: Boolean = false,
        @Query("page") page: Int = 1
    ): TmdbTvSearchResponse

    @GET("tv/{tvId}")
    suspend fun tvDetails(
        @Header("Authorization") authorization: String,
        @Path("tvId") tvId: Int,
        @Query("language") language: String = "fr-FR",
        @Query("append_to_response") appendToResponse: String = "credits"
    ): TmdbTvDetails

    @GET("tv/{tvId}/videos")
    suspend fun tvVideos(
        @Header("Authorization") authorization: String,
        @Path("tvId") tvId: Int,
        @Query("language") language: String = "fr-FR"
    ): TmdbVideoResponse

    @GET("movie/{movieId}")
    suspend fun movieDetails(
        @Header("Authorization") authorization: String,
        @Path("movieId") movieId: Int,
        @Query("language") language: String = "fr-FR",
        @Query("append_to_response") appendToResponse: String = "credits"
    ): TmdbMovieDetails

    @GET("movie/{movieId}/videos")
    suspend fun movieVideos(
        @Header("Authorization") authorization: String,
        @Path("movieId") movieId: Int,
        @Query("language") language: String = "fr-FR"
    ): TmdbVideoResponse

    @GET("movie/now_playing")
    suspend fun nowPlaying(
        @Header("Authorization") authorization: String,
        @Query("language") language: String = "fr-FR",
        @Query("region") region: String = "FR",
        @Query("page") page: Int = 1
    ): TmdbSearchResponse

    @GET("movie/popular")
    suspend fun popular(
        @Header("Authorization") authorization: String,
        @Query("language") language: String = "fr-FR",
        @Query("region") region: String = "FR",
        @Query("page") page: Int = 1
    ): TmdbSearchResponse

    @GET("movie/{movieId}/recommendations")
    suspend fun recommendations(
        @Header("Authorization") authorization: String,
        @Path("movieId") movieId: Int,
        @Query("language") language: String = "fr-FR",
        @Query("page") page: Int = 1
    ): TmdbSearchResponse

    @GET("movie/{movieId}/release_dates")
    suspend fun releaseDates(
        @Header("Authorization") authorization: String,
        @Path("movieId") movieId: Int
    ): TmdbReleaseDatesResponse
}
