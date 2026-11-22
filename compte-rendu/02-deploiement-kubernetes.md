# 2. Déploiement Kubernetes

**Statut** : traité

Les 6 composants de l'application tournent dans Kubernetes : `users`, `posts`, `notifications`, `gateway`, `frontend` et `redis`. La base PostgreSQL est hors du cluster.

## Ce qui a été fait

**Environnement : minikube**

On utilise minikube avec le driver Docker, sur une machine virtuelle Linux. Il crée un cluster Kubernetes à un seul nœud sur notre machine.

**Namespace**

Tout est dans le Namespace `mini-reseau` : chaque manifeste le déclare dans `metadata.namespace`. Ça isole l'application du reste du cluster (on peut tout lister ou tout supprimer d'un coup avec `-n mini-reseau`).

**Un Deployment par composant**

| Composant | Image | Réplicas |
|---|---|---|
| `users` | `mini-reseau-social-users:1.0.0` | 1 |
| `gateway` | `mini-reseau-social-gateway:1.0.0` | 2 |
| `frontend` | `mini-reseau-social-frontend:1.0.0` | 2 |
| `posts` | `mini-reseau-social-posts:1.0.0` | 1 |
| `notifications` | `mini-reseau-social-notifications:1.0.0` | 1 |
| `redis` | `redis:7-alpine` | 1 |

Les manifestes sont dans `projet/k8s/` (un fichier `<composant>-deployment.yaml` par composant).

**Ce qui se passe quand on supprime un Pod**

Ce test a été fait quand `users` avait encore 2 réplicas (voir « Difficultés rencontrées »).

On a supprimé un Pod `users` et observé l'état toutes les 10 secondes :

```
$ kubectl delete -n mini-reseau --wait=false $(kubectl get pod -n mini-reseau -l app=users -o name | head -1)
pod "users-7d5b5d9f8b-k4pz9" deleted from mini-reseau namespace

16:56:37
users-7d5b5d9f8b-k4pz9   1/1     Terminating         0          8m23s
users-7d5b5d9f8b-mblpm   1/1     Running             0          7m31s
users-7d5b5d9f8b-mjplt   0/1     ContainerCreating   0          0s

16:56:58
users-7d5b5d9f8b-mblpm   1/1     Running             0          7m52s
users-7d5b5d9f8b-mjplt   0/1     Running             0          21s

16:57:08
users-7d5b5d9f8b-mblpm   1/1     Running             0          8m3s
users-7d5b5d9f8b-mjplt   1/1     Running             0          32s
```

Le remplaçant (`mjplt`) est créé dans la seconde qui suit la suppression. Il reste à `0/1` pendant son démarrage, puis passe à `1/1`. Pendant ce temps, l'autre Pod `users` a continué de répondre.

## Comment

**Construire les images dans minikube.** Minikube a ses propres images, séparées du Docker de la machine. On construit donc les images directement dedans, avec les noms et tags exacts des manifestes :

```bash
minikube image build -t mini-reseau-social-users:1.0.0 services/users
# (même chose pour posts, notifications, gateway, frontend)
minikube image ls        # pour vérifier
```
Par la suite, on a ajouté un script qui construit les 5 images d'un coup, avec la version donnée en paramètre : `./scripts/build_images.sh 1.0.0`.


**Déployer.** Avec `kubectl apply -f` :

```bash
kubectl apply -f projet/k8s/<fichier>.yaml
kubectl get pods -n mini-reseau
kubectl rollout status deployment/<nom> -n mini-reseau
```
Par la suite, on a ajouté un script qui déploie toute l'application : `./scripts/deploy.sh`.


## Gestion des versions applicatives

Les différentes versions de l'application sont déployées
à partir d'images Docker portant des tags explicites.

Le changement de version est automatisé par le script
`projet/scripts/switch_version.sh`.

Ce script utilise la commande `kubectl set image`
pour modifier l'image utilisée par un Deployment,
puis `kubectl rollout status` pour attendre la fin
du déploiement.

### Déploiement de la version initiale

La commande suivante permet de déployer la version 1.0.0
sur les cinq services :

```bash
./projet/scripts/switch_version.sh 1.0.0
```
## V3 correction

### Déploiement de la version évoluée

La commande suivante permet de déployer la version 2.0.0
sur les cinq services :

```bash
./projet/scripts/switch_version.sh 2.0.0
```

### Déploiement de la version 3.0.0

La version 3.0.0 ne concerne que le service `posts`.

Le script commence par réappliquer le ConfigMap
`projet/k8s/app-config.yaml`, puis met à jour uniquement
le Deployment `posts`.

```bash
./projet/scripts/switch_version.sh 3.0.0
```

Les services `users`, `notifications`, `gateway`
et `frontend` restent en version 2.0.0.

### Vérification des images déployées

Les images réellement utilisées par les Deployments
peuvent être vérifiées avec :

```bash
kubectl get deployments -n mini-reseau \
  -o custom-columns=NAME:.metadata.name,IMAGE:.spec.template.spec.containers[0].image
```

Cette commande permet de vérifier que les cinq services
utilisent les images attendues après chaque changement
de version.

### Test de la vérification des versions avant le passage en V3

Nous avons testé le mécanisme de vérification ajouté au script `switch_version.sh` avant le déploiement de la version 3.0.0.

Pour rappel, la V3 concerne uniquement le service `posts`. Les quatre autres services (`users`, `notifications`, `gateway` et `frontend`) doivent donc être en version 2.0.0.

Lors du test, le script a détecté les versions suivantes :

| Service | Version détectée | Version attendue |
|---|---|---|
| users | 2.0.0 | 2.0.0 |
| notifications | 2.0.0 | 2.0.0 |
| gateway | 2.0.0 | 2.0.0 |
| frontend | 1.0.0 | 2.0.0 |

``` text
Passage en V3 : vérification des versions préalables...
Vérification de users...
users est bien en version 2.0.0.
Vérification de notifications...
notifications est bien en version 2.0.0.
Vérification de gateway...
gateway est bien en version 2.0.0.
Vérification de frontend...
ERREUR : frontend n'est pas en version 2.0.0.
Image actuelle : mini-reseau-social-frontend:1.0.0
Image attendue : mini-reseau-social-frontend:2.0.0
Passage en V3 annulé.
```

Le script a correctement identifié que le frontend était encore en version 1.0.0.

Il a alors affiché un message d'erreur indiquant la version actuelle et la version attendue, puis a annulé le passage en V3.

Ce test est concluant : le script empêche le déploiement de la V3 lorsque les conditions préalables ne sont pas respectées. Il évite ainsi de déployer une version de `posts` dans un environnement dont les autres services ne correspondent pas à la version attendue.

### Vérification des versions avant le passage en V3

La version 3.0.0 concerne uniquement le service `posts`. Les services `users`, `notifications`, `gateway` et `frontend` doivent donc rester en version 2.0.0.

Afin d'éviter un déploiement incohérent, le script `switch_version.sh` vérifie désormais les versions de ces quatre services avant d'autoriser le passage en V3.

Si les quatre services sont bien en version 2.0.0, le script réapplique la configuration Kubernetes, puis met à jour uniquement le service `posts`.

Si l'un des services n'est pas en version 2.0.0, le script affiche un message d'erreur et interrompt l'opération avant de modifier le déploiement.

Cette vérification permet de garantir que la V3 est déployée sur la base de la V2 attendue, sans modifier involontairement les autres services.

## V3 rollback
### Démonstration du déploiement et du rollback

Nous avons réalisé un scénario de mise à jour comprenant trois étapes :

1. Déploiement de la version 2.0.0, qui fonctionne correctement.
2. Déploiement de la version 3.0.0 du service `posts`, qui provoque une erreur au démarrage en raison de l'absence de la variable `POSTS_MAX_CONTENT_LENGTH`.
3. Diagnostic du problème, puis demande de rollback vers la révision précédente avec `kubectl rollout undo`.

Cette manipulation permet de démontrer le mécanisme de mise à jour progressive de Kubernetes, la possibilité d'observer un déploiement défaillant et la capacité de revenir à une révision précédente.

## Pourquoi ces choix

- **Deployment plutôt que Pod seul** : un Pod créé seul n'est pas recréé s'il disparaît. Un Deployment maintient le nombre de Pods demandé et permet les mises à jour progressives.
- **Tag `1.0.0` et `imagePullPolicy: IfNotPresent`** : pas de `latest` (on sait quelle version tourne), et Kubernetes utilise l'image locale de minikube au lieu d'essayer de la télécharger.
- **Plusieurs réplicas sur `gateway` et `frontend`** : ce sont les points d'entrée de l'application. Si l'un de leurs Pods tombe, l'autre prend le relais. Les services Java (`users`, `posts`, `notifications`) n'ont qu'un réplica à cause des limites de notre machine (voir « Difficultés rencontrées »). `redis` reste à 1 : il ne sert qu'à transmettre les événements entre `posts` et `notifications` et ne stocke rien d'important.

## Difficultés rencontrées

- **Machine saturée.** Quand plusieurs Pods Java démarraient en même temps, la machine virtuelle arrivait à saturation : l'API de Kubernetes ne répondait plus et les Pods redémarraient en boucle. On a d'abord ramené `posts` et `notifications` à 1 réplica, mais le problème est revenu en reconstruisant les images. Nous n'avons pas identifié la cause exacte. On a donc adapté le cluster à la machine : `users` passe aussi à 1 réplica, et après un redémarrage on relance les services Java un par un. Les nombres de réplicas sont écrits dans les fichiers, sinon un `kubectl apply` remettrait l'ancien nombre.

## Éléments à relire

- Sur une machine plus puissante, on remonterait les services Java à 2 réplicas.
- Les images nginx (`gateway`, `frontend`) tournent encore en root (voir partie 1).