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

## Pourquoi ces choix

- **Deployment plutôt que Pod seul** : un Pod créé seul n'est pas recréé s'il disparaît. Un Deployment maintient le nombre de Pods demandé et permet les mises à jour progressives.
- **Tag `1.0.0` et `imagePullPolicy: IfNotPresent`** : pas de `latest` (on sait quelle version tourne), et Kubernetes utilise l'image locale de minikube au lieu d'essayer de la télécharger.
- **Plusieurs réplicas sur `gateway` et `frontend`** : ce sont les points d'entrée de l'application. Si l'un de leurs Pods tombe, l'autre prend le relais. Les services Java (`users`, `posts`, `notifications`) n'ont qu'un réplica à cause des limites de notre machine (voir « Difficultés rencontrées »). `redis` reste à 1 : il ne sert qu'à transmettre les événements entre `posts` et `notifications` et ne stocke rien d'important.

## Difficultés rencontrées

- **Machine saturée.** Quand plusieurs Pods Java démarraient en même temps, la machine virtuelle arrivait à saturation : l'API de Kubernetes ne répondait plus et les Pods redémarraient en boucle. On a d'abord ramené `posts` et `notifications` à 1 réplica, mais le problème est revenu en reconstruisant les images. Nous n'avons pas identifié la cause exacte. On a donc adapté le cluster à la machine : `users` passe aussi à 1 réplica, et après un redémarrage on relance les services Java un par un. Les nombres de réplicas sont écrits dans les fichiers, sinon un `kubectl apply` remettrait l'ancien nombre.

## Éléments à relire

- Sur une machine plus puissante, on remonterait les services Java à 2 réplicas.
- Les images nginx (`gateway`, `frontend`) tournent encore en root (voir partie 1).