package fr.dvdtheque.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "movies")
data class Movie(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val originalTitle: String = "",
    val year: Int? = null,
    val director: String = "",
    val actors: String = "",
    val genre: String = "",
    val durationMinutes: Int? = null,
    val synopsis: String = "",
    val posterUrl: String = "",
    val status: MovieStatus = MovieStatus.OWNED,
    val rating: Int? = null,
    val notes: String = "",
    val edition: String = "",
    val discCount: Int? = null,
    val location: String = "",
    val addedAt: Long = System.currentTimeMillis(),
    val watched: Boolean = false,
    val tmdbId: Int? = null
)

enum class MovieStatus { OWNED, WANTED }
enum class MovieSort { TITLE_ASC, TITLE_DESC, YEAR_DESC, YEAR_ASC, RECENTLY_ADDED, RATING_DESC }
