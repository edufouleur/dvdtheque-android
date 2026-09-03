package fr.dvdtheque.app.data

class MovieRepository(private val dao: MovieDao) {
    val movies = dao.observeAll()
    suspend fun snapshot() = dao.getAllOnce()
    fun movie(id: Long) = dao.observeById(id)
    suspend fun save(movie: Movie) = dao.upsert(movie)
    suspend fun delete(movie: Movie) = dao.delete(movie)
    suspend fun deleteAll() = dao.deleteAll()
    suspend fun replaceAll(movies: List<Movie>) {
        dao.deleteAll()
        dao.upsertAll(movies)
    }
}
