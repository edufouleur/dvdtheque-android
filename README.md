# Reelio 1.5

Application Android personnelle de gestion de films, avec interface **Cinéma Premium**.

## Nouveautés 1.5

- Nom **Reelio** affiché en haut à gauche de chaque écran.
- Style de boutons **Cinéma Premium**.
- Thèmes **Auto / Clair / Sombre**. Le mode Auto suit uniquement l'heure du téléphone (clair le jour, sombre la nuit).
- Couleur d'accentuation : palette étendue de 26 nuances, du carmin au rose poudré, en passant par orange, ambre, jaune, verts, turquoise, cyan, bleus, indigo, violet, lavande et magenta.
- Écran **Souhaits** enrichi : Mes souhaits, Au cinéma, Pour vous, DVD/Blu-ray.
- Suggestions « Pour vous » basées sur un film TMDB déjà présent dans la bibliothèque lorsque possible.
- Détection TMDB des sorties physiques françaises (type de sortie physique).
- Informations de saga directement dans la fiche film.
- « Que regarder ce soir ? » : **Aléatoire**, continuer une saga, ordre de visionnage.
- Guides Marvel, Star Wars, Harry Potter et Le Seigneur des Anneaux.
- Statistiques supprimées.
- Onglet **Paramètres** : apparence, sauvegarde/restauration JSON, export CSV et informations Reelio.
- Migration Room conservant les anciennes données et ajoutant l'identifiant TMDB.

## Compilation GitHub

Conserver le secret GitHub Actions `TMDB_TOKEN`, puis lancer le workflow **Compiler APK Reelio**.
L'artefact produit s'appelle `Reelio-APK`.

## Ajustements 1.5

- Le libellé **« Au hasard »** est remplacé par **« Aléatoire »**.
- La palette de couleur d'accentuation passe à 26 nuances et s'affiche en grille dans Paramètres.
