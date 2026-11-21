# 3. Communication entre les services

**Statut** : traité

## Ce qui a été fait

**Services créés et découverte par le DNS**

Chaque composant a un Service. On n'a pas précisé de type, donc Kubernetes utilise le type par défaut, **ClusterIP** : le Service n'est joignable que depuis l'intérieur du cluster.

| Service | Port |
|---|---|
| `users`, `posts`, `notifications` | 8080 |
| `gateway`, `frontend` | 80 |
| `redis` | 6379 |

Kubernetes donne à chaque Service un nom DNS : dans le Namespace `mini-reseau`, il suffit d'écrire `users` pour joindre le Service `users`.

**Comment les composants se joignent**

Toujours par le nom du Service, jamais par une IP :

```
navigateur → Ingress → frontend → gateway → users / posts / notifications
posts → redis → notifications
```

Par exemple, `posts` appelle `http://users:8080` pour vérifier qu'un utilisateur est connecté, et publie sur `redis` quand un commentaire est créé.

**Comment l'application est exposée hors du cluster**

Avec un **Ingress** : il reçoit les requêtes envoyées à `mini-reseau.local` et les transmet au Service `frontend`.

**Comment les Pods joignent la base de données externe**

Par un Service `postgres` de type **ExternalName**, qui renvoie vers `host.minikube.internal`, c'est-à-dire la machine où tourne la base. Les services se connectent donc à `postgres:5432`.

## Comment

**Les Services.** Chaque composant a son fichier `projet/k8s/<composant>-service.yaml`. Il indique le nom du Service, son port, et quels Pods il regroupe (ceux qui portent le label `app: <composant>`). On l'applique avec :

```bash
kubectl apply -f projet/k8s/users-service.yaml
```

(même chose pour chaque composant).

**L'adresse des autres services.** Chaque service reçoit les noms à utiliser par des variables de configuration, par exemple `USERS_SERVICE_URL=http://users:8080` et `REDIS_HOST=redis` (détails dans la partie 4). La gateway, elle, a les noms `users:8080`, `posts:8080` et `notifications:8080` dans son Dockerfile.

**L'Ingress.** Il est décrit dans `projet/k8s/ingress.yaml`. Une ressource Ingress ne fait rien seule : il faut aussi un programme qui lit ses règles et route le trafic. Minikube en fournit un, qu'on active :

```bash
minikube addons enable ingress
kubectl apply -f projet/k8s/ingress.yaml
```

Pour que le navigateur sache que `mini-reseau.local` désigne minikube, on ajoute une ligne dans le fichier `/etc/hosts` de la machine :

```bash
echo "$(minikube ip)  mini-reseau.local" | sudo tee -a /etc/hosts
```

L'application s'ouvre alors sur `http://mini-reseau.local`.

**La base externe.** Le Service est décrit dans `projet/k8s/postgres-external.yaml` et appliqué avec `kubectl apply -f`. Dans le Deployment de chaque service Java, l'adresse de la base utilise ce nom, par exemple pour `users` :

```
jdbc:postgresql://postgres:5432/$(POSTGRES_DB)?currentSchema=users
```

**Vérification.** Avec un Pod de test, on a appelé chaque service par son nom (`http://users:8080/health/readiness`, `http://gateway/healthz`...) : tous répondent. Dans le navigateur, on s'est inscrit, connecté, on a publié un post, un 2e compte l'a commenté, et le 1er compte a reçu la notification : tous les composants ont été traversés.

## Pourquoi ces choix

- **Des noms plutôt que des IP** : un Pod change d'adresse IP à chaque recréation, alors que le nom d'un Service ne change pas. Les composants se retrouvent donc toujours.
- **Ingress plutôt que NodePort** : un NodePort ouvre un port entre 30000 et 32767 sur la machine, qu'il faut connaître pour accéder à l'application. L'Ingress donne une adresse lisible (`mini-reseau.local`) et un seul point d'entrée. En contrepartie, il faut activer un contrôleur et modifier `/etc/hosts`.
- **ExternalName pour la base** : la base n'est pas dans Kubernetes. Avec ce Service, son adresse n'est écrite qu'à un seul endroit, et les services utilisent un nom simple (`postgres`), comme si elle était dans le cluster. Si la base change d'adresse, on ne modifie que ce Service.

## Difficultés rencontrées

- **Accès sous WSL.** Sous WSL avec Docker Desktop, l'adresse de minikube (`minikube ip`) n'est pas joignable : la requête vers `mini-reseau.local` restait sans réponse. On a lancé `minikube tunnel`, qui rend l'Ingress accessible sur `127.0.0.1`, et associé `mini-reseau.local` à `127.0.0.1` dans `/etc/hosts`.

## Éléments à relire

- La ligne dans `/etc/hosts` est à ajouter à la main sur chaque machine.