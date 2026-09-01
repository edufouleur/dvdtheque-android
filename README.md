# Reelio v1.10 — Cinéma Premium

Cette version consolide les corrections graphiques et fonctionnelles demandées.

## Nouveautés et corrections

- Nouvelle icône officielle du lanceur Android : bobine violette, fond noir et cadre violet lumineux, sans texte.
- Logo Reelio interne corrigé avec transparence réelle : plus de carré coloré.
- Le logo Reelio interne suit la couleur choisie dans **Paramètres > Couleurs**.
- Icône **Thème** corrigée avec un pinceau transparent et tintable, à la même taille que l'icône **Couleurs**.
- Suppression totale du pop-corn dans l'application.
- **Ce soir** utilise une icône de dé et son libellé reste sur une seule ligne.
- **Souhaits** reste sur une seule ligne dans la barre du bas.
- Le bouton **Aléatoire** sélectionne un film au hasard dans la bibliothèque.
- Fiche film : boutons **Modifier** et **Partager** absents.
- Fiche film : menu **⋮** actif avec bascule bibliothèque/souhaits, vu/non vu, actualisation TMDB et suppression avec confirmation.
- Paramètres : aucun menu décoratif sans action.
- Recherche par image améliorée : photo ou galerie, OCR ML Kit, filtrage des mots parasites, plusieurs propositions de titre, correction manuelle et résultats TMDB avec affiche/année.

## Compatibilité

- `applicationId` conservé : `fr.dvdtheque.app`
- Base Room conservée : `dvdtheque.db`
- Version : `1.10` (`versionCode = 10`)
- Secret GitHub requis : `TMDB_TOKEN`

## Compilation GitHub Actions

Le workflow `.github/workflows/build-apk.yml` compile l'APK de débogage avec Java 17 et Gradle 9.3.1.
