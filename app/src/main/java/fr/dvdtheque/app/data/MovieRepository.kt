package fr.dvdtheque.app.data

class MovieRepository(private val dao: MovieDao) {
    val movies = dao.observeAll()
    fun movie(id: Long) = dao.observeById(id)
    suspend fun save(movie: Movie) = dao.upsert(movie)
    suspend fun delete(movie: Movie) = dao.delete(movie)
    suspend fun replaceAll(movies: List<Movie>) {
        dao.deleteAll()
        dao.upsertAll(movies)
    }
}
