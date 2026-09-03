package fr.dvdtheque.app.data

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.text.Normalizer
import java.util.Locale

/**
 * Codec rétrocompatible des sauvegardes Reelio.
 *
 * Formats reconnus :
 * - sauvegardes historiques v1.x : tableau JSON direct de films ;
 * - sauvegardes v2.x : tableau JSON direct ;
 * - formats enveloppés {"movies": [...]}, {"collection": [...]} ou {"data": [...]}.
 *
 * Les champs Films/Séries ajoutés en v2 sont reconstruits avec des valeurs sûres
 * lorsqu'ils n'existent pas dans une ancienne sauvegarde.
 */
object MovieBackupCodec {
    private val gson = Gson()

    fun encode(movies: List<Movie>): String = gson.toJson(movies)

    fun decode(rawJson: String): List<Movie> {
        val json = rawJson.removePrefix("\uFEFF").trim()
        require(json.isNotBlank()) { "Le fichier est vide" }

        val root = parseRootLenient(json)
        val array = when {
            root.isJsonArray -> root.asJsonArray
            root.isJsonObject -> {
                val obj = root.asJsonObject
                sequenceOf("movies", "collection", "data", "items")
                    .mapNotNull { key -> obj.get(key)?.takeIf { it.isJsonArray }?.asJsonArray }
                    .firstOrNull()
                    ?: throw IllegalArgumentException("Format de sauvegarde Reelio non reconnu")
            }
            else -> throw IllegalArgumentException("Format de sauvegarde Reelio non reconnu")
        }

        val parsed = array.mapNotNull { element ->
            val obj = element.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapNotNull null
            parseMovie(obj)
        }

        // Un ancien fichier peut contenir des doublons. On garde la première entrée
        // et on privilégie TMDB ID + type ; sinon titre normalisé + année + type.
        val seen = mutableSetOf<String>()
        return parsed.filter { movie ->
            val key = movie.tmdbId?.let { "tmdb:${safeType(movie.mediaType)}:$it" }
                ?: "title:${safeType(movie.mediaType)}:${normalizeTitle(movie.title)}:${movie.year ?: 0}"
            seen.add(key)
        }
    }


    /**
     * Certaines anciennes sauvegardes Reelio ont pu recevoir un crochet fermant
     * supplémentaire à la fin (ex: ...}]] au lieu de ...}]). On tente d'abord
     * un JSON strict puis, uniquement en cas d'échec, on retire jusqu'à trois
     * délimiteurs fermants surnuméraires de fin de fichier.
     */
    private fun parseRootLenient(json: String): JsonElement {
        var candidate = json
        var lastError: Throwable? = null
        repeat(4) {
            try {
                return JsonParser.parseString(candidate)
            } catch (e: Throwable) {
                lastError = e
                candidate = when {
                    candidate.endsWith("]") -> candidate.dropLast(1).trimEnd()
                    candidate.endsWith("}") -> candidate.dropLast(1).trimEnd()
                    else -> candidate
                }
            }
        }
        throw IllegalArgumentException("JSON de sauvegarde illisible", lastError)
    }

    private fun parseMovie(obj: JsonObject): Movie? {
        val title = obj.string("title", obj.string("name")).trim()
        if (title.isBlank()) return null

        val mediaType = when (obj.string("mediaType", obj.string("type", "movie")).lowercase(Locale.ROOT)) {
            "tv", "series", "serie", "série" -> "tv"
            else -> "movie"
        }

        return Movie(
            id = obj.long("id") ?: 0L,
            title = title,
            originalTitle = obj.string("originalTitle", obj.string("original_name")),
            year = obj.int("year"),
            director = obj.string("director"),
            actors = obj.string("actors"),
            genre = obj.string("genre"),
            durationMinutes = obj.int("durationMinutes") ?: obj.int("duration"),
            synopsis = obj.string("synopsis", obj.string("overview")),
            posterUrl = obj.string("posterUrl", obj.string("poster")),
            status = parseStatus(obj.string("status", MovieStatus.OWNED.name)),
            rating = obj.int("rating")?.takeIf { it in 1..5 },
            notes = obj.string("notes"),
            edition = obj.string("edition"),
            discCount = obj.int("discCount"),
            location = obj.string("location"),
            addedAt = obj.long("addedAt") ?: System.currentTimeMillis(),
            watched = obj.bool("watched") ?: false,
            tmdbId = obj.int("tmdbId") ?: obj.int("tmdb_id"),
            mediaType = mediaType,
            totalSeasons = obj.int("totalSeasons"),
            totalEpisodes = obj.int("totalEpisodes"),
            ownedSeasons = obj.string("ownedSeasons"),
            currentSeason = obj.int("currentSeason"),
            currentEpisode = obj.int("currentEpisode")
        )
    }

    private fun parseStatus(value: String): MovieStatus = when (value.trim().uppercase(Locale.ROOT)) {
        "WANTED", "WISH", "WISHLIST", "SOUHAIT" -> MovieStatus.WANTED
        else -> MovieStatus.OWNED
    }

    private fun safeType(value: String) = if (value == "tv") "tv" else "movie"

    private fun normalizeTitle(value: String): String = Normalizer
        .normalize(value.lowercase(Locale.FRENCH), Normalizer.Form.NFD)
        .replace("\\p{M}+".toRegex(), "")
        .replace("[’']".toRegex(), " ")
        .replace("[^a-z0-9]+".toRegex(), " ")
        .trim()

    private fun JsonObject.string(name: String, default: String = ""): String =
        get(name)?.takeUnless { it.isJsonNull }?.runCatching { asString }?.getOrNull() ?: default

    private fun JsonObject.int(name: String): Int? =
        get(name)?.takeUnless { it.isJsonNull }?.runCatching { asInt }?.getOrNull()

    private fun JsonObject.long(name: String): Long? =
        get(name)?.takeUnless { it.isJsonNull }?.runCatching { asLong }?.getOrNull()

    private fun JsonObject.bool(name: String): Boolean? =
        get(name)?.takeUnless { it.isJsonNull }?.runCatching { asBoolean }?.getOrNull()
}
