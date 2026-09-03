package fr.dvdtheque.app.data

import java.text.Normalizer
import java.util.Locale

class MovieRepository(private val dao: MovieDao) {
    val movies = dao.observeAll()
    suspend fun snapshot() = dao.getAllOnce()
    fun movie(id: Long) = dao.observeById(id)
    suspend fun save(movie: Movie) = dao.upsert(movie)

    suspend fun findDuplicate(candidate: Movie, excludingId: Long = 0): Movie? {
        val all = dao.getAllOnce().filter { it.id != excludingId }
        candidate.tmdbId?.let { tmdbId ->
            all.firstOrNull { it.tmdbId == tmdbId && it.mediaType == candidate.mediaType }?.let { return it }
        }

        val candidateTitle = normalizeTitle(candidate.title)
        val candidateOriginal = normalizeTitle(candidate.originalTitle)
        return all.firstOrNull { existing ->
            if (existing.mediaType != candidate.mediaType) return@firstOrNull false
            val sameYear = candidate.year == null || existing.year == null || candidate.year == existing.year
            if (!sameYear) return@firstOrNull false
            val existingTitle = normalizeTitle(existing.title)
            val existingOriginal = normalizeTitle(existing.originalTitle)
            candidateTitle.isNotBlank() && (
                candidateTitle == existingTitle ||
                    (candidateOriginal.isNotBlank() && candidateOriginal == existingOriginal) ||
                    (candidateOriginal.isNotBlank() && candidateOriginal == existingTitle) ||
                    (existingOriginal.isNotBlank() && candidateTitle == existingOriginal)
                )
        }
    }

    private fun normalizeTitle(value: String): String = Normalizer
        .normalize(value.lowercase(Locale.FRENCH), Normalizer.Form.NFD)
        .replace("\\p{M}+".toRegex(), "")
        .replace("[’']".toRegex(), " ")
        .replace("[^a-z0-9]+".toRegex(), " ")
        .trim()
        .split("\\s+".toRegex())
        .filter { it !in setOf("le", "la", "les", "l", "un", "une", "des") }
        .joinToString(" ")
    suspend fun delete(movie: Movie) = dao.delete(movie)
    suspend fun deleteAll() = dao.deleteAll()
    suspend fun replaceAll(movies: List<Movie>) = dao.replaceAll(movies)
}
