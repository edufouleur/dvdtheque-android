# Reelio v2.00 — Films & Séries

Reelio est une vidéothèque personnelle Android pour gérer une collection de films et de séries DVD/Blu-ray, les souhaits et le suivi de visionnage.

# Reelio v1.16 – Cinéma Premium

Mise à jour visuelle et gestion des données.

## Changements v1.16
- Le grand visuel TMDB de la fiche film apparaît avec un fondu plus lent (environ 800 ms), sans afficher l'affiche comme fond temporaire.
- Les titres composés d'un seul mot restent sur une seule ligne et leur taille s'adapte afin d'éviter une coupure du mot.
- Les titres multi-mots conservent un affichage adaptatif sur deux lignes maximum.
- Nouvelle rubrique **🔒 Données** dans Paramètres :
  - Sauvegarder la collection en JSON.
  - Restaurer une sauvegarde Reelio.
  - Exporter la collection et les souhaits en CSV.
  - Réinitialiser Reelio avec double confirmation et possibilité de sauvegarder avant l'effacement.
- Le logo Reelio affiché dans l'application reprend la bobine de l'icône du lanceur, sans fond ni cadre.
- La bobine interne est un masque transparent recoloré dynamiquement selon la couleur choisie dans **Paramètres → Couleurs**.
- L'icône du lanceur Android reste inchangée : bobine violette, fond sombre et cadre violet.
- Le thème sombre reste le thème par défaut à la première installation.

Le même `applicationId` (`fr.dvdtheque.app`) est conservé afin de préserver la compatibilité de mise à jour et les données locales.
