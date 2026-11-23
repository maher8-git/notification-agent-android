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

### Filtre avant apprentissage

`IngestionBoundary` commence désactivée et sans application autorisée. Même après opt-in, seul un identifiant opaque et local sous une application autorisée, avec catégorie et décision de confidentialité explicite du futur adaptateur, passe le filtre. Une décision inconnue ou sensible est ignorée; aucun contenu ne peut être transmis. Retirer une application efface par prudence l'historique d'apprentissage en mémoire et invalide les anciennes suggestions. Désactiver l'admission vide aussi la liste d'applications. Les préférences ne sont pas stockées dans ce prototype.

Les tests JVM ne vérifient pas la disponibilité des métadonnées, la classification d'une notification réelle, les restrictions Android ou constructeur, ni la révocation côté système. Aucun callback Android ni permission n'est inclus.

`SessionIntake` prépare la frontière entre Android et le moteur : connexion de
la plateforme, opt-in local pour la session et autorisation d'un canal précis
sont distincts. La déconnexion efface l'apprentissage en RAM et demande une
nouvelle autorisation locale. Ce module ne collecte pas les notifications
Android et ne branche aucun service système à lui seul.

`DaypartFeedback` teste des préférences par plage horaire sur des choix explicites et fictifs, en mémoire. Sans signaux suffisants, le score général est conservé. Le module n'est pas encore relié à Android.

Le parcours de session synthétique distingue la priorité générale de la préférence apprise pour cette plage horaire. Une suggestion retirée ou purgée ne redevient pas valide au retour du même identifiant.
