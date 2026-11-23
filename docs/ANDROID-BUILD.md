# Squelette Android : construction à vérifier

JDK 17, Gradle 8.13 et Android SDK Platform 36 (compatibilité :
[AGP 8.13](https://developer.android.com/build/releases/agp-8-13-0-release-notes)).
Aucun wrapper n'est encore livré. Dans un environnement Android configuré :

    gradle :app:assembleDebug

Ce contrôle n'a pas été exécuté ici : aucun SDK Android ou Gradle disponible.
Le parcours Android n'a donc pas encore été compilé ou essayé sur un appareil.
Vérifications locales : `sh test-local.sh` (cœur Java) et
`python3 tests/check_android_smoke.py` (structure du manifeste et garde-fous source).

Pour un essai futur autorisé : ouvrir les réglages Android depuis l'application,
accorder l'accès au listener, revenir et activer le test pour cette session.
Avec Android 13+, autoriser séparément l'affichage, puis retoucher le bouton
d'envoi fictif. Actualiser l'état et chercher « Notification fictive reçue ».
Vérifier qu'une notification d'une autre application ne change pas cet état,
puis retirer l'accès système et vérifier qu'une reconnexion demande un nouveau
consentement local. Le système accorde techniquement un accès général au
listener; le code filtre le profil, l'application, l'identifiant, le tag local
et le canal sans consulter le contenu ni les notifications historiques.
Aucun réseau, action, effacement ou stockage de contenu. Le canal Android
créé pour l'essai et la notification fictive elle-même sont gérés par Android.
La notification peut être effacée manuellement dans le panneau système.
