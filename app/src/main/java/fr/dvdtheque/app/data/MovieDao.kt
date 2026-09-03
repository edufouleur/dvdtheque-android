package fr.dvdtheque.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MovieDao {
    @Query("SELECT * FROM movies")
    fun observeAll(): Flow<List<Movie>>

    @Query("SELECT * FROM movies")
    suspend fun getAllOnce(): List<Movie>

    @Query("SELECT * FROM movies WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<Movie?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(movie: Movie): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(movies: List<Movie>)

    @Delete
    suspend fun delete(movie: Movie)

    @Query("DELETE FROM movies")
    suspend fun deleteAll()
}
