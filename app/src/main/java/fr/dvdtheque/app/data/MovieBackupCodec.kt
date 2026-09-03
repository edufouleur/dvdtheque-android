package fr.dvdtheque.app.data

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * Codec tolérant pour les sauvegardes Reelio.
 *
 * Les anciennes sauvegardes (avant v2) ne contiennent pas les champs séries.
 * Gson ne réapplique pas forcément les valeurs par défaut Kotlin lorsqu'il
 * désérialise directement une data class, ce qui peut produire des null sur
 * des propriétés déclarées non-null. On reconstruit donc explicitement chaque
 * Movie avec des valeurs sûres et rétrocompatibles.
 */
object MovieBackupCodec {
    private val gson = Gson()

    fun encode(movies: List<Movie>): String = gson.toJson(movies)

    fun decode(json: String): List<Movie> {
        if (json.isBlank()) return emptyList()
        val root = JsonParser.parseString(json)
        require(root.isJsonArray) { "Format de sauvegarde Reelio non reconnu" }

        return root.asJsonArray.mapNotNull { element ->
            val obj = element.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapNotNull null
            val title = obj.string("title").trim()
            if (title.isBlank()) return@mapNotNull null

            Movie(
                id = obj.long("id") ?: 0L,
                title = title,
                originalTitle = obj.string("originalTitle"),
                year = obj.int("year"),
                director = obj.string("director"),
                actors = obj.string("actors"),
                genre = obj.string("genre"),
                durationMinutes = obj.int("durationMinutes"),
                synopsis = obj.string("synopsis"),
                posterUrl = obj.string("posterUrl"),
                status = runCatching {
                    MovieStatus.valueOf(obj.string("status", MovieStatus.OWNED.name))
                }.getOrDefault(MovieStatus.OWNED),
                rating = obj.int("rating")?.coerceIn(1, 5),
                notes = obj.string("notes"),
                edition = obj.string("edition"),
                discCount = obj.int("discCount"),
                location = obj.string("location"),
                addedAt = obj.long("addedAt") ?: System.currentTimeMillis(),
                watched = obj.bool("watched") ?: false,
                tmdbId = obj.int("tmdbId"),
                mediaType = if (obj.string("mediaType", "movie") == "tv") "tv" else "movie",
                totalSeasons = obj.int("totalSeasons"),
                totalEpisodes = obj.int("totalEpisodes"),
                ownedSeasons = obj.string("ownedSeasons"),
                currentSeason = obj.int("currentSeason"),
                currentEpisode = obj.int("currentEpisode")
            )
        }
    }

    private fun JsonObject.string(name: String, default: String = ""): String =
        get(name)?.takeUnless { it.isJsonNull }?.runCatching { asString }?.getOrNull() ?: default

    private fun JsonObject.int(name: String): Int? =
        get(name)?.takeUnless { it.isJsonNull }?.runCatching { asInt }?.getOrNull()

    private fun JsonObject.long(name: String): Long? =
        get(name)?.takeUnless { it.isJsonNull }?.runCatching { asLong }?.getOrNull()

    private fun JsonObject.bool(name: String): Boolean? =
        get(name)?.takeUnless { it.isJsonNull }?.runCatching { asBoolean }?.getOrNull()
}
