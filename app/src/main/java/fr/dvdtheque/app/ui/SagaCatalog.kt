package fr.dvdtheque.app.ui

import fr.dvdtheque.app.data.Movie
import java.text.Normalizer

internal data class GuideEntry(
    val title: String,
    val year: Int? = null,
    val kind: String = "Film"
)

internal data class UniverseGuide(
    val name: String,
    val chronological: List<GuideEntry>,
    val releaseOrder: List<GuideEntry> = chronological
)

internal object SagaCatalog {
    val marvel = UniverseGuide(
        name = "Marvel",
        chronological = listOf(
            GuideEntry("Captain America: First Avenger", 2011),
            GuideEntry("Captain Marvel", 2019),
            GuideEntry("Iron Man", 2008),
            GuideEntry("Iron Man 2", 2010),
            GuideEntry("L'Incroyable Hulk", 2008),
            GuideEntry("Thor", 2011),
            GuideEntry("Avengers", 2012),
            GuideEntry("Iron Man 3", 2013),
            GuideEntry("Thor : Le Monde des ténèbres", 2013),
            GuideEntry("Captain America : Le Soldat de l'hiver", 2014),
            GuideEntry("Les Gardiens de la Galaxie", 2014),
            GuideEntry("Les Gardiens de la Galaxie Vol. 2", 2017),
            GuideEntry("Avengers : L'Ère d'Ultron", 2015),
            GuideEntry("Ant-Man", 2015),
            GuideEntry("Captain America: Civil War", 2016),
            GuideEntry("Black Widow", 2021),
            GuideEntry("Black Panther", 2018),
            GuideEntry("Spider-Man: Homecoming", 2017),
            GuideEntry("Doctor Strange", 2016),
            GuideEntry("Thor: Ragnarok", 2017),
            GuideEntry("Ant-Man et la Guêpe", 2018),
            GuideEntry("Avengers: Infinity War", 2018),
            GuideEntry("Avengers: Endgame", 2019),
            GuideEntry("Loki - Saison 1", 2021, "Série"),
            GuideEntry("WandaVision", 2021, "Série"),
            GuideEntry("Falcon et le Soldat de l'Hiver", 2021, "Série"),
            GuideEntry("Shang-Chi et la Légende des Dix Anneaux", 2021),
            GuideEntry("Eternals", 2021),
            GuideEntry("Spider-Man: Far From Home", 2019),
            GuideEntry("Spider-Man: No Way Home", 2021),
            GuideEntry("Doctor Strange in the Multiverse of Madness", 2022),
            GuideEntry("Hawkeye", 2021, "Série"),
            GuideEntry("Moon Knight", 2022, "Série"),
            GuideEntry("Black Panther: Wakanda Forever", 2022),
            GuideEntry("Ant-Man et la Guêpe : Quantumania", 2023),
            GuideEntry("Les Gardiens de la Galaxie Vol. 3", 2023),
            GuideEntry("The Marvels", 2023),
            GuideEntry("Deadpool & Wolverine", 2024)
        ),
        releaseOrder = listOf(
            GuideEntry("Iron Man", 2008), GuideEntry("L'Incroyable Hulk", 2008),
            GuideEntry("Iron Man 2", 2010), GuideEntry("Thor", 2011),
            GuideEntry("Captain America: First Avenger", 2011), GuideEntry("Avengers", 2012),
            GuideEntry("Iron Man 3", 2013), GuideEntry("Thor : Le Monde des ténèbres", 2013),
            GuideEntry("Captain America : Le Soldat de l'hiver", 2014), GuideEntry("Les Gardiens de la Galaxie", 2014),
            GuideEntry("Avengers : L'Ère d'Ultron", 2015), GuideEntry("Ant-Man", 2015),
            GuideEntry("Captain America: Civil War", 2016), GuideEntry("Doctor Strange", 2016),
            GuideEntry("Les Gardiens de la Galaxie Vol. 2", 2017), GuideEntry("Spider-Man: Homecoming", 2017),
            GuideEntry("Thor: Ragnarok", 2017), GuideEntry("Black Panther", 2018),
            GuideEntry("Avengers: Infinity War", 2018), GuideEntry("Ant-Man et la Guêpe", 2018),
            GuideEntry("Captain Marvel", 2019), GuideEntry("Avengers: Endgame", 2019),
            GuideEntry("Spider-Man: Far From Home", 2019), GuideEntry("WandaVision", 2021, "Série"),
            GuideEntry("Falcon et le Soldat de l'Hiver", 2021, "Série"), GuideEntry("Loki - Saison 1", 2021, "Série"),
            GuideEntry("Black Widow", 2021), GuideEntry("Shang-Chi et la Légende des Dix Anneaux", 2021),
            GuideEntry("Eternals", 2021), GuideEntry("Hawkeye", 2021, "Série"),
            GuideEntry("Spider-Man: No Way Home", 2021), GuideEntry("Moon Knight", 2022, "Série"),
            GuideEntry("Doctor Strange in the Multiverse of Madness", 2022), GuideEntry("Black Panther: Wakanda Forever", 2022),
            GuideEntry("Ant-Man et la Guêpe : Quantumania", 2023), GuideEntry("Les Gardiens de la Galaxie Vol. 3", 2023),
            GuideEntry("The Marvels", 2023), GuideEntry("Deadpool & Wolverine", 2024)
        )
    )

    val starWars = UniverseGuide("Star Wars", listOf(
        GuideEntry("Star Wars, épisode I : La Menace fantôme", 1999),
        GuideEntry("Star Wars, épisode II : L'Attaque des clones", 2002),
        GuideEntry("Star Wars: The Clone Wars", 2008),
        GuideEntry("Star Wars, épisode III : La Revanche des Sith", 2005),
        GuideEntry("Solo: A Star Wars Story", 2018),
        GuideEntry("Rogue One: A Star Wars Story", 2016),
        GuideEntry("Star Wars, épisode IV : Un nouvel espoir", 1977),
        GuideEntry("Star Wars, épisode V : L'Empire contre-attaque", 1980),
        GuideEntry("Star Wars, épisode VI : Le Retour du Jedi", 1983),
        GuideEntry("Star Wars, épisode VII : Le Réveil de la Force", 2015),
        GuideEntry("Star Wars, épisode VIII : Les Derniers Jedi", 2017),
        GuideEntry("Star Wars, épisode IX : L'Ascension de Skywalker", 2019)
    ))

    val harryPotter = UniverseGuide("Harry Potter", listOf(
        GuideEntry("Harry Potter à l'école des sorciers", 2001), GuideEntry("Harry Potter et la Chambre des secrets", 2002),
        GuideEntry("Harry Potter et le Prisonnier d'Azkaban", 2004), GuideEntry("Harry Potter et la Coupe de feu", 2005),
        GuideEntry("Harry Potter et l'Ordre du Phénix", 2007), GuideEntry("Harry Potter et le Prince de sang-mêlé", 2009),
        GuideEntry("Harry Potter et les Reliques de la Mort : 1re partie", 2010), GuideEntry("Harry Potter et les Reliques de la Mort : 2e partie", 2011)
    ))

    val lordOfRings = UniverseGuide("Le Seigneur des Anneaux", listOf(
        GuideEntry("Le Hobbit : Un voyage inattendu", 2012), GuideEntry("Le Hobbit : La Désolation de Smaug", 2013),
        GuideEntry("Le Hobbit : La Bataille des Cinq Armées", 2014), GuideEntry("Le Seigneur des anneaux : La Communauté de l'anneau", 2001),
        GuideEntry("Le Seigneur des anneaux : Les Deux Tours", 2002), GuideEntry("Le Seigneur des anneaux : Le Retour du roi", 2003)
    ))

    val universes = listOf(marvel, starWars, harryPotter, lordOfRings)

    fun findOwnedMatch(entry: GuideEntry, movies: List<Movie>): Movie? {
        val target = normalize(entry.title)
        return movies.firstOrNull { movie ->
            val title = normalize(movie.title)
            title == target || title.contains(target) || target.contains(title)
        }
    }

    fun nextInKnownSaga(movie: Movie, movies: List<Movie>): GuideEntry? {
        val guide = universes.firstOrNull { universe -> universe.chronological.any { findOwnedMatch(it, listOf(movie)) != null } } ?: return null
        val index = guide.chronological.indexOfFirst { findOwnedMatch(it, listOf(movie)) != null }
        if (index < 0 || index >= guide.chronological.lastIndex) return null
        return guide.chronological[index + 1]
    }

    private fun normalize(value: String): String = Normalizer.normalize(value.lowercase(), Normalizer.Form.NFD)
        .replace("\\p{M}+".toRegex(), "")
        .replace("[^a-z0-9]".toRegex(), "")
}
