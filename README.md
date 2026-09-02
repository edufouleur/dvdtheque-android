# Reelio v1.11 — Mode Cinéma

Cette version ajoute une expérience immersive verticale directement depuis la fiche d'un film, tout en conservant les fonctions de Reelio v1.10.

## Nouveauté principale : Mode Cinéma

- Nouveau bouton **Mode cinéma** sur la fiche du film.
- Affichage vertical plein écran pensé pour un téléphone.
- Grande affiche, arrière-plan cinématographique issu de TMDB, titre, année, durée, synopsis, réalisateur et acteurs.
- Statut bibliothèque/souhaits, vu/à voir et note visibles dans le même écran.
- Transition d'ouverture volontairement discrète.
- Bouton **Regarder la bande-annonce** intégré au Mode Cinéma.
- Reelio cherche d'abord une bande-annonce YouTube en français sur TMDB, puis tente une version anglaise si nécessaire.
- Si aucune bande-annonce n'est disponible, le Mode Cinéma reste accessible et le bouton vidéo est désactivé.
- Bouton **X** toujours visible pour revenir immédiatement à la fiche du film.

La maquette validée est conservée dans `docs/mode-cinema-vertical-reference.png`.

## Fonctions conservées

- Icône officielle du lanceur Reelio.
- Logo Reelio interne tinté selon la couleur choisie.
- 12 couleurs d'accentuation et thèmes Auto / Clair / Sombre.
- Dé pour **Ce soir**, sans pop-corn.
- Bouton **Aléatoire** fonctionnel.
- Menu **⋮** fonctionnel sur la fiche film.
- Recherche par image améliorée avec OCR ML Kit et recherche TMDB.
- Bibliothèque, souhaits, sauvegarde/restauration et export.

## Compatibilité

- `applicationId` : `fr.dvdtheque.app`
- Base Room : `dvdtheque.db`
- Version : `1.11` (`versionCode = 11`)
- Secret GitHub requis : `TMDB_TOKEN`

## Compilation

Le projet reste prévu pour la compilation via GitHub Actions avec Java 17 et Gradle 9.3.1.
