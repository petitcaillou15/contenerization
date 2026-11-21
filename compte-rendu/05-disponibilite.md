# 5. Disponibilité

**Statut** : traité

## Ce qui a été fait

**Probes utilisées**

| Composant | Startup | Readiness | Liveness |
|---|---|---|---|
| `users`, `posts`, `notifications` | `GET /health/liveness`, toutes les 5 s, 60 échecs tolérés (300 s) | `GET /health/readiness`, toutes les 10 s | `GET /health/liveness`, toutes les 10 s, 3 échecs |
| `gateway` | aucune | `GET /healthz`, toutes les 10 s | `GET /healthz`, toutes les 10 s, 3 échecs |
| `frontend` | aucune | `GET /`, toutes les 10 s | `GET /`, toutes les 10 s, 3 échecs |
| `redis` | aucune | commande `redis-cli ping` | commande `redis-cli ping` |

**Composants avec plusieurs réplicas** : `gateway` et `frontend` (2 chacun).

**Comportements observés** :
- **Pod supprimé** : il est recréé automatiquement (voir la partie 2).
- **Pod non prêt** : pendant une mise à jour, le nouveau Pod reste à `0/1` tant que sa readiness probe n'a pas réussi, et l'ancien n'est arrêté qu'ensuite.

## Comment

Les sondes sont dans chaque `k8s/<composant>-deployment.yaml`, par exemple pour `users` :

```yaml
startupProbe:
  httpGet:
    path: /health/liveness
    port: 8080
  periodSeconds: 5
  failureThreshold: 60
readinessProbe:
  httpGet:
    path: /health/readiness
    port: 8080
  periodSeconds: 10
  timeoutSeconds: 3
  failureThreshold: 3
livenessProbe:
  httpGet:
    path: /health/liveness
    port: 8080
  periodSeconds: 10
  timeoutSeconds: 3
  failureThreshold: 3
```

Pod non prêt, observé pendant un redémarrage de `users` avec `kubectl get pods -n mini-reseau -w` :

```
users-86586f575d-z2knw   0/1   Running       0   31s
users-86d5dc75d6-drl5v   1/1   Running       0   44m
users-86586f575d-z2knw   1/1   Running       0   61s
users-86d5dc75d6-drl5v   1/1   Terminating   0   44m
```

Le nouveau Pod (`z2knw`) passe à `1/1` au bout de 61 s ; l'ancien (`drl5v`) n'est arrêté qu'à ce moment-là, et il a servi les requêtes jusque-là.

## Pourquoi ces choix

- **Readiness** : un Pod ne reçoit du trafic que s'il est prêt. Sans elle, le Service enverrait des requêtes à un Java encore en train de démarrer.
- **Liveness** : si un conteneur est bloqué, Kubernetes le redémarre.
- **Startup, seulement sur les services Java** : ils mettent longtemps à démarrer. La startup probe empêche la liveness de les tuer pendant ce temps. Les nginx et Redis démarrent en quelques secondes et n'en ont pas besoin.
- **Les chemins `/health/...`** viennent de l'application (Spring Actuator) ; on les a testés avant de les utiliser.
- **2 réplicas pour `gateway` et `frontend`** : ce sont les points d'entrée de l'application. Les services Java restent à 1 à cause des limites de notre machine (voir partie 2).

## Difficultés rencontrées

- La première startup probe laissait 150 s (30 échecs). Sur notre machine, `posts` a mis plus longtemps à démarrer et a été tué puis relancé. On a passé le seuil à 60 échecs (300 s).
- Après un redémarrage du cluster, un Pod `gateway` plantait (`host not found in upstream "users"`). Kubernetes l'a relancé jusqu'à ce que ça marche.

## Éléments à relire

- Avec un seul réplica, supprimer un Pod Java rend ce service indisponible le temps qu'il redémarre.