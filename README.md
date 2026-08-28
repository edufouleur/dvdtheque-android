# Reelio 1.3

Application Android Kotlin + Jetpack Compose pour gérer une collection de DVD et une liste de souhaits.

## Nouveautés 1.3
- Interface sombre et bibliothèque visuelle avec affiches.
- Recherche automatique TMDB en français.
- Notes personnelles de 1 à 5 étoiles.
- Statut Vu / Pas vu.
- Souhaits et passage vers la collection.
- « Que regarder ce soir ? » : tirage au hasard parmi les films non vus.
- « Continuer une saga » : suggestion du titre suivant lorsqu'une saga connue est reconnue.
- Ordres de visionnage : Marvel (films + séries), Star Wars, Harry Potter et Le Seigneur des Anneaux.
- Ordre chronologique ou ordre de sortie pour Marvel.
- Statistiques de collection.
- Aucun module de prêt de DVD.

## TMDB
Dans GitHub : Settings > Secrets and variables > Actions > New repository secret.
Créer le secret `TMDB_TOKEN` avec l'API Read Access Token de TMDB.

## Compilation
Le workflow `.github/workflows/build-apk.yml` compile automatiquement l'APK sur GitHub Actions.
L'artefact final s'appelle `Reelio-APK` et contient `app-debug.apk`.

## Mise à jour de la base locale
La version 1.3 ajoute le champ `watched` avec une migration Room 1 -> 2 pour conserver les films déjà enregistrés.

## Attribution TMDB
Ce produit utilise l'API TMDB mais n'est ni approuvé ni certifié par TMDB.
