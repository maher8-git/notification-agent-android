# Assistant de notifications Android

Première brique gratuite et hors ligne : classer des notifications fictives dans une vue personnelle, sans en masquer, en supprimer ou en envoyer une seule. Ce dépôt contient le moteur Java compatible avec un futur module Android, pas encore une application installable.

Le moteur part d'une priorité normale et apprend par application et catégorie à partir des choix explicites « important » et « moins important » dans la future interface. Le score et les identifiants fictifs sont bornés, restent en mémoire et sont effaçables. Une notification déjà notée ne compte pas deux fois pendant sa présence dans cette mémoire. Aucun titre, texte, expéditeur, code ou secret n'entre dans le moteur. Les codes à usage unique, les notifications sensibles et les cas non classés sont exclus. Une suggestion « faible » concerne seulement l'ordre de la vue propre à l'application.

## Vérifier localement

Avec un JDK, sans Gradle, SDK Android ni accès Internet :

```sh
sh test-local.sh
```

## Étape Android distincte

Ajouter une interface locale, puis un `NotificationListenerService` activé volontairement dans les réglages. Les callbacks devront classer les métadonnées admissibles et ignorer par défaut toute source ou catégorie non validée, avant de transmettre un identifiant local opaque au moteur. Tester sur un appareil autorisé les notifications de plusieurs applications, la révocation immédiate de l'accès et sa désactivation. Ne consulter ni utiliser le contenu des messages, ne pas ouvrir d'action Android et ne pas reproduire les notifications. Aucun service externe, coût ou permission n'est intégré ici.

## Soutien facultatif

Ce projet est sous licence MIT. Les dons sont facultatifs et ne débloquent aucune fonction. Bitcoin, réseau principal uniquement : `3HaGinyFJQJXFAMgU8SBuDxdS5aG5BQUNU`.
