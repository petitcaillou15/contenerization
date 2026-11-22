# 1. Images Docker

**Statut** : traité (sauf l'utilisateur non root sur les images nginx, voir plus bas)

On a fait les 5 images (une par composant), le multi-stage sur les services Java et le frontend, les `.dockerignore`, l'ordre des layers pour le cache, l'utilisateur non root sur les 3 services Java, et les mesures de taille. Les images nginx (frontend et gateway) tournent encore en root : on l'explique plus bas.

## Ce qui a été fait

**Les images**

On a une image par composant :
- `users`, `posts`, `notifications` : les 3 services Java (Spring Boot)
- `frontend` : l'appli Angular, servie par nginx
- `gateway` : nginx qui redirige les requêtes vers les bons services

PostgreSQL et Redis utilisent les images officielles, on n'a rien construit pour eux.

**Multi-stage : où et pourquoi ?**

- **Services Java** : une 1re étape avec Maven + JDK compile le code et produit un `.jar`. Une 2e étape avec seulement le JRE copie ce `.jar` et le lance.
- **Frontend** : une 1re étape avec Node compile l'appli Angular. Une 2e étape avec nginx ne garde que les fichiers générés (HTML, JS, CSS).
- **Gateway** : pas de multi-stage, il n'y a rien à compiler, juste un fichier de config nginx.

Ce qui n'est plus dans l'image finale : Maven, le JDK, le code source et `target/` (Java), Node et `node_modules` (frontend). On en a besoin pour construire, pas pour faire tourner l'appli.

**`.dockerignore`**

- Services Java : `target/ .git/ .idea/ .vscode/ *.log .env`
- Frontend : `node_modules/ dist/ .angular/ .git/ .idea/ .vscode/ .env`
- Gateway : pas de `.dockerignore` puisque son Dockerfile ne copie plus que le template nginx.

**Ordre des layers et cache** 

Docker réutilise une étape déjà construite tant que ce qu'elle copie n'a pas changé. On a donc séparé l'installation des dépendances du reste du code :

- Services Java : on copie seulement `pom.xml`, on télécharge les dépendances Maven (`mvn dependency:go-offline`), et ensuite seulement on copie `src/` et on compile.
- Frontend : on copie seulement `package.json` et `package-lock.json`, on lance `npm ci`, et ensuite seulement on copie le code et on compile.

Résultat : si on modifie une ligne de code, les dépendances (qui changent rarement) ne sont pas retéléchargées.

**Utilisateur non root**  (services Java)

Dans l'image finale des 3 services Java, on crée un utilisateur `app` sans droits spéciaux et on lance l'appli avec `USER app`. Vérification dans le cluster :

```bash
kubectl exec -n mini-reseau deploy/users -- id
# uid=999(app) gid=999(app) groups=999(app)
```

Même résultat pour `posts` et `notifications` (`uid=999`, donc pas root).

**Tailles avant / après (`docker images`)**

| Image | Avant | Après |
|---|---|---|
| frontend | 2,3 GB | 93,6 MB |
| gateway | 367 MB | 75,9 MB |
| users | 1,36 GB | 608 MB |
| posts | 1,39 GB | 624 MB |
| notifications | 1,39 GB | 624 MB |

(Ces tailles ont été mesurées avant la modification du cache et du `USER`)

**Images de base, utilisateur non root, secrets**

- Images de base : versions fixées (`nginx:1.27-alpine`, `maven:3.9-…`, `eclipse-temurin:21-jre`), plus de `latest`.
- Utilisateur non root : fait sur les 3 services Java. Pas fait sur `frontend` et `gateway` (nginx), voir "Éléments à relire".
- Secrets : aucun dans les images (détails plus bas).

## Comment

**Services Java** (même Dockerfile pour les 3) :

```dockerfile
# étape 1 : on compile
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# on copie seulement pom.xml et on télécharge les dépendances (couche mise en cache)
COPY pom.xml .
RUN mvn -B dependency:go-offline
# ensuite seulement le code, puis on compile
COPY src ./src
RUN mvn -B clean package -DskipTests

# étape 2 : on lance
FROM eclipse-temurin:21-jre
WORKDIR /app
# utilisateur sans droits spéciaux
RUN groupadd --system app && useradd --system --gid app --no-create-home app
COPY --from=build --chown=app:app /app/target/*.jar app.jar
USER app
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
```

La 1re étape fabrique le `.jar`. La 2e repart d'une image propre et ne récupère que le `.jar` avec `COPY --from=build`. Tout le reste de la 1re étape est jeté.

**Frontend** :

```dockerfile
FROM node:22 AS build
WORKDIR /app
# on copie seulement les fichiers de dépendances et on les installe (couche mise en cache)
COPY package.json package-lock.json ./
RUN npm ci
# ensuite seulement le code, puis on compile
COPY . .
RUN npm run build

FROM nginx:alpine
ENV GATEWAY_UPSTREAM=gateway:80
COPY default.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html
EXPOSE 80
```

L'ancien Dockerfile installait nginx à la main avec `apt`, copiait deux fois les fichiers et utilisait un `CMD` avec `envsubst`. Maintenant on utilise le mécanisme de l'image nginx officielle : tout fichier mis dans `/etc/nginx/templates/` a ses variables remplacées automatiquement au démarrage. Il n'y a donc plus de `CMD`.

**Gateway** :

```dockerfile
FROM nginx:1.27-alpine
COPY default.conf.template /etc/nginx/templates/default.conf.template
ENV USERS_UPSTREAM=users:8080 POSTS_UPSTREAM=posts:8080 NOTIFICATIONS_UPSTREAM=notifications:8080 CORS_ALLOW_ORIGIN=*
EXPOSE 80
```

On a enlevé `apt-get install curl wget vim procps` et `COPY . /app`, qui ne servaient à rien, et on a fixé la version de nginx.

**Reconstruire les images dans minikube** (après avoir modifié un Dockerfile) :

```bash
minikube image build -t mini-reseau-social-users:1.0.0 services/users
kubectl rollout restart deployment/users -n mini-reseau
```

(même chose pour `posts`, `notifications` et `frontend`).

**Vérification**

- `docker compose up --build` dans le dossier `projet/`, puis `docker compose ps` : les 7 services sont `healthy`, le site s'affiche et l'API répond 200.
- Dans Kubernetes, après avoir reconstruit les images : `kubectl exec ... -- id` sur les 3 services Java (pas root), puis le parcours complet dans le navigateur (inscription, post, commentaire par un 2e compte, notification) : tout fonctionne.

**Git** : tout est fait sur la branche `multi-stage`, poussée avec `git push -u origin multi-stage`, puis merge request vers `main`. Le cache et l'utilisateur non root ont été ajoutés ensuite sur la branche `docker-cache-nonroot`.


## Gestion des versions des images

Les images Docker sont identifiées par des tags explicites
afin de pouvoir retrouver et déployer une version précise
de chaque service.

Les tags utilisés sont :
- `1.0.0` : version initiale de l'application ;
- `2.0.0` : version évoluée de l'application ;
- `3.0.0` : évolution spécifique du service `posts`.

La version 3.0.0 ne concerne que le service `posts`.
Les autres services conservent leurs images en version 2.0.0.

La correspondance des images utilisées pour les différentes
versions est la suivante :

| Service | Version initiale | Version évoluée | Évolution spécifique |
|---|---|---|---|
| users | 1.0.0 | 2.0.0 | 2.0.0 |
| posts | 1.0.0 | 2.0.0 | 3.0.0 |
| notifications | 1.0.0 | 2.0.0 | 2.0.0 |
| gateway | 1.0.0 | 2.0.0 | 2.0.0 |
| frontend | 1.0.0 | 2.0.0 | 2.0.0 |

Les tags Docker permettent de sélectionner une image,
mais ils ne construisent pas automatiquement le code
correspondant à une version.

Il faut donc construire chaque image à partir du code
source correspondant avant de la déployer.


## Pourquoi ces choix

### Pourquoi telle image de base ?

On prend l'image dont on a besoin à ce moment-là, pas plus :

| Image | Pourquoi |
|---|---|
| `maven:3.9-eclipse-temurin-21` | Pour **compiler** : il faut Maven et le JDK. |
| `eclipse-temurin:21-jre` | Pour **lancer** un `.jar` déjà compilé, le JRE suffit (le JDK est plus gros pour rien). |
| `node:22` | Pour **construire** Angular : il faut Node et npm. |
| `nginx:alpine` | Après le build, le frontend n'est que des fichiers statiques. Un petit serveur web suffit, et Alpine est une version de Linux très légère. |
| `nginx:1.27-alpine` | Pareil pour la gateway, avec une version précise. |

Pourquoi une version précise et pas `latest` : `latest` peut changer sans qu'on le sache et donc le build peut être cassé si la version change.

### Qu'y a-t-il dans l'image finale, et pourquoi ?

Seulement ce qui sert à faire tourner l'appli :

- **Java** : le JRE et `app.jar`.
- **Frontend** : nginx et les fichiers Angular déjà compilés.
- **Gateway** : nginx et son fichier de config.

### Pourquoi cet ordre des layers (cache) ?

Docker construit l'image étape par étape et garde chaque étape en mémoire. Si ce qu'une étape copie n'a pas changé, il la réutilise au lieu de la refaire. Le code change souvent, les dépendances (`pom.xml`, `package.json`) rarement. On installe donc d'abord les dépendances, puis on copie le code : en changeant une ligne de code, seule la compilation est refaite, pas le téléchargement des dépendances. C'est comme préparer les ingrédients une fois pour toutes et ne refaire que la cuisson.

### Pourquoi un utilisateur non root ?

Par défaut, un conteneur tourne en root (tous les droits dans le conteneur). Si quelqu'un trouve une faille dans l'appli, il aurait ces droits. Avec `USER app`, il n'a que ceux d'un utilisateur ordinaire, ce qui limite les dégâts.

### Pas de secret dans l'image

- `.env` est dans les `.dockerignore`, donc il n'est jamais copié dans l'image, même avec `COPY . .`.
- `.env` est dans le `.gitignore` donc il n'est pas commité sur Git.
- Aucun mot de passe écrit dans un Dockerfile. Les `ENV` de la gateway ne contiennent que des adresses de services et un réglage CORS.

## Difficultés rencontrées

En reconstruisant les images puis en redémarrant plusieurs services Java en même temps, la machine virtuelle a saturé (charge très élevée, l'API de Kubernetes ne répondait plus). On a arrêté minikube, attendu que la charge redescende, puis remonté les services un par un. Les limites de ressources (partie 2) permettraient d'éviter ça.

## Éléments à relire

- **Frontend et gateway tournent encore en root** : nginx a besoin du port 80, que seul root peut ouvrir. Pour le passer en non root, il faudrait utiliser l'image `nginxinc/nginx-unprivileged` (port 8080), ce qui change le template nginx, les Services et les probes. On ne l'a pas fait.
- **Image plus légère** : peut-être tester un JRE alpine pour réduire encore les tailles.