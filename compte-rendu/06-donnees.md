# 6. Données

**Statut** : traité

## Ce qui a été fait

**Base de données hors Kubernetes**

PostgreSQL tourne dans un conteneur Docker, lancé par un `docker-compose.yml` séparé (`db/`). Le conteneur s'appelle `base-postgres`, utilise l'image `postgres:16-alpine` et publie le port 5432 sur la machine.

Au premier démarrage, le script `db/00-init.sql` crée un schéma et un utilisateur par service (`users_svc`, `posts_svc`, `notifications_svc`). Chaque service n'a accès qu'à son schéma. Les mots de passe ne sont pas écrits dans ce script : il les lit dans des variables d'environnement (commande `\getenv`), fournies par le fichier `k8s/.env.secrets` grâce à `env_file` dans le `docker-compose.yml`.

**Persistance**

Les données sont dans un **volume Docker nommé** (`pg_data`), séparé du conteneur. Supprimer et recréer le conteneur ne touche pas au volume : les données sont conservées.

**Connexion des composants à la base externe**

Par le Service `postgres` de type `ExternalName`, qui renvoie vers `host.minikube.internal`, la machine où tourne la base (voir partie 3). Chaque service Java s'y connecte avec son propre utilisateur et son mot de passe, pris dans le Secret (voir partie 4).

## Comment

**Lancer la base** (depuis `projet/`). Il faut deux fichiers non versionnés, créés à partir de leurs modèles : `db/.env` (l'administrateur de la base) et `k8s/.env.secrets` (les mots de passe des services) :

```bash
cd db
docker compose up -d
docker ps --filter name=base-postgres
```

**Vérifier la persistance** : on compte les posts, on supprime le conteneur, on le recrée, puis on recompte.

```
$ docker exec base-postgres psql -U admin -d social -c "select count(*) from posts.posts"
 count
-------
     5

$ docker compose down
 ✔ Container base-postgres  Removed

$ docker compose up -d
 ✔ Container base-postgres  Started

$ docker ps --filter name=base-postgres
CONTAINER ID   IMAGE                CREATED         STATUS
f3f356fb0214   postgres:16-alpine   9 seconds ago   Up 8 seconds (healthy)

$ docker exec base-postgres psql -U admin -d social -c "select count(*) from posts.posts"
 count
-------
     5
```

Le conteneur est neuf (créé 9 secondes plus tôt), mais les 5 posts sont toujours là : ils étaient dans le volume, pas dans le conteneur.


## Conservation des données lors des changements de version

La base de données PostgreSQL est exécutée en dehors
du cluster Kubernetes, dans un conteneur Docker séparé.

Ses données sont conservées dans un volume Docker nommé
`pg_data`.

Les changements de version de l'application concernent
les images des services applicatifs et ne nécessitent
pas de supprimer ou de recréer le volume PostgreSQL.

Les versions 2.0.0 et 3.0.0 ne nécessitent pas
de modification du schéma de la base de données.

Ainsi, les publications, les commentaires, les utilisateurs
et les notifications enregistrés restent conservés
lors des changements de version applicative.

Cette séparation permet de faire évoluer les services
sans modifier le stockage des données.


## Pourquoi ces choix

- **Pas de mot de passe dans `00-init.sql`** : à l'origine, le script contenait les mots de passe en clair. On les a sortis du dépôt, et la base les lit dans le même fichier que le Secret Kubernetes.
- **Hors Kubernetes** : la base garde ses données même si on supprime tout le cluster.
- **Un volume nommé** : les données d'un conteneur disparaissent avec lui ; un volume vit indépendamment.
- **Un utilisateur et un schéma par service** : un service ne peut pas lire ou modifier les données d'un autre.
- **`restart: always`** : la base redémarre toute seule avec la machine.

## Difficultés rencontrées

Pas de difficultés remarquables.

## Éléments à relire