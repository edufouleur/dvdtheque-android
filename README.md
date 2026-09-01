# Reelio v1.8 — Cinéma Premium

Cette version intègre les corrections visuelles validées et la nouvelle recherche de film par image.

## Nouveautés principales

- Recherche par image dans l'écran Ajouter :
  - prise de photo d'une jaquette avec l'appareil photo Android ;
  - choix d'une image depuis la galerie ;
  - lecture automatique du texte avec Google ML Kit Text Recognition ;
  - détection d'un titre probable ;
  - possibilité de corriger le titre reconnu ;
  - recherche automatique du film sur TMDB puis ajout à la Bibliothèque ou aux Souhaits.
- Le logo Reelio officiel fourni est utilisé dans l'application et prend la couleur d'accentuation sélectionnée.
- La boîte de pop-corn fournie remplace l'ancienne icône de dé pour « Ce soir » dans la barre du bas.
- Les intitulés « Souhaits » et « Ce soir » sont forcés sur une seule ligne.
- Le bouton Modifier et le bouton Partager ont été supprimés de la fiche film.
- Le menu ⋮ de la fiche film est actif : Bibliothèque/Souhaits, Vu/Non vu, actualisation TMDB et suppression avec confirmation.
- Le bouton Aléatoire reprend le style validé, avec le sous-titre « Un film au hasard dans votre bibliothèque ».
- Le pinceau de Thème utilise le visuel fourni et a la même taille que l'icône Couleurs.
- Thème : Auto / Clair / Sombre.
- Palette de 12 couleurs conservée.
- « À propos de Reelio » et le crédit « by ED » sont conservés.

## Recherche par image

La reconnaissance de texte fonctionne localement sur l'appareil via Google ML Kit. Une connexion Internet est ensuite nécessaire pour interroger TMDB avec le titre détecté.

Aucune permission caméra permanente n'est demandée : Reelio utilise l'application photo Android via un URI temporaire sécurisé avec FileProvider.

## TMDB

Le secret GitHub doit rester nommé `TMDB_TOKEN`.

## Compilation

Le workflow `.github/workflows/build-apk.yml` compile l'APK de debug via GitHub Actions.
