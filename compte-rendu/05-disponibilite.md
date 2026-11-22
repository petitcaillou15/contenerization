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

## Mise à jour des services et retour à une version précédente

Les mises à jour applicatives sont réalisées en modifiant
l'image des Deployments avec la commande `kubectl set image`.

Kubernetes prend alors en charge le remplacement des Pods
selon la stratégie de mise à jour configurée sur le Deployment.

La commande `kubectl rollout status` permet de suivre
le déploiement et d'attendre que les nouveaux Pods
soient opérationnels.

### Résultat du test du rolling update du frontend

Lors du test, nous avons effectué une mise à jour du frontend de la version 1.0.0 vers la version 2.0.0.

L'observation des pods avec la commande `kubectl get pods -n mini-reseau -w` montre les différentes étapes du remplacement :

1. Les anciens pods sont initialement en état `Running` et `1/1`.
2. Kubernetes crée les nouveaux pods, qui passent par les états `Pending` et `ContainerCreating`.
3. Les nouveaux pods deviennent ensuite `Running` et atteignent l'état `1/1`.
4. Une fois les nouveaux pods démarrés, les anciens passent en état `Terminating`, puis disparaissent.

Ce résultat montre que Kubernetes réalise progressivement le remplacement des pods, sans supprimer immédiatement les anciennes instances.

Le rolling update du frontend a donc été observé avec succès : les nouveaux pods ont atteint l'état `Ready` avant la terminaison des anciens pods.

``` text
NAME                              READY   STATUS              RESTARTS   AGE
frontend-7f4b54cbb6-5z749         1/1     Running             3          12h
frontend-7f4b54cbb6-z7tfd         1/1     Running             3          12h
frontend-8ddc7d47-bb66p           0/1     Pending             0          0s
frontend-8ddc7d47-bb66p           0/1     ContainerCreating   0          1s
frontend-8ddc7d47-bb66p           0/1     ContainerCreating   0          1s
frontend-8ddc7d47-bb66p           0/1     Running             0          1s
frontend-8ddc7d47-bb66p           0/1     Running             0          2s
frontend-8ddc7d47-bb66p           0/1     Terminating         3          12h
frontend-8ddc7d47-z7tfd           0/1     Terminating         3          12h
frontend-8ddc7d47-2sb7s           0/1     Pending             0          0s
frontend-8ddc7d47-z7tfd           0/1     Pending             0          0s
frontend-8ddc7d47-2sb7s           0/1     ContainerCreating   0          0s
frontend-8ddc7d47-2sb7s           0/1     ContainerCreating   0          1s
frontend-7f4b54cbb6-5z749         1/1     Running             3          12h
frontend-7f4b54cbb6-z7tfd         1/1     Running             3          12h
frontend-7f4b54cbb6-2sb7s         0/1     Completed           3          12h
frontend-7f4b54cbb6-2sb7s         1/1     Running             0          1s
frontend-7f4b54cbb6-2sb7s         1/1     Running             0          11s
frontend-7f4b54cbb6-5z749         0/1     Terminating         3          12h
frontend-7f4b54cbb6-z7tfd         0/1     Terminating         3          12h
frontend-7f4b54cbb6-5z749         0/1     Completed           3          12h
frontend-7f4b54cbb6-z7tfd         0/1     Completed           3          12h
```

## V3 correction
### Passage de V2 à V3 

La version 3.0.0 ne concerne que le service `posts`.

Le passage en V3 consiste à :
1. Réappliquer le ConfigMap `app-config`.
2. Remplacer l'image du Deployment `posts`
   par `mini-reseau-social-posts:3.0.0`.
3. Attendre la fin du déploiement.

Les autres services restent en version 2.0.0.

### Retour à V2

Pour revenir à la version 2.0.0 après avoir déployé V3,
le script permet de redéployer les cinq services en V2 :

```bash
./projet/scripts/switch_version.sh 2.0.0
```

Il est également possible de revenir uniquement
sur le service `posts` avec :

```bash
kubectl set image deployment/posts \
  posts=mini-reseau-social-posts:2.0.0 \
  -n mini-reseau

kubectl rollout status deployment/posts -n mini-reseau
```

Cette seconde méthode ne modifie pas les autres services.

### Vérification après le retour à V2

Les images utilisées par les Deployments peuvent être
contrôlées avec :

```bash
kubectl get deployments -n mini-reseau \
  -o custom-columns=NAME:.metadata.name,IMAGE:.spec.template.spec.containers[*].image
```

Le service `posts` doit alors utiliser l'image
`mini-reseau-social-posts:2.0.0`.

## Rétablissement du service par rollback

Après avoir identifié le problème de configuration de la version 3 du service `posts`, nous avons choisi d'effectuer un rollback.

Cette opération permet de revenir à la révision précédente du Deployment, correspondant à la version fonctionnelle utilisée avant le déploiement défaillant.

Nous avons exécuté la commande suivante :

```bash
kubectl rollout undo deployment/posts -n mini-reseau
```

Kubernetes a retourné le message suivant :

```text
deployment.apps/posts rolled back
```

Le rollback a ainsi été demandé à Kubernetes afin de revenir à la révision précédente du Deployment.

La vérification de la fin du rollback et du retour effectif à la version 2.0.0 est réalisée avec :

```bash
kubectl rollout status deployment/posts -n mini-reseau

kubectl get deployments -n mini-reseau \
  -o custom-columns=NAME:.metadata.name,IMAGE:.spec.template.spec.containers[*].image

```

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