# Specs — Mini réseau social / forum minimaliste

## Contexte

Application pédagogique support d'un TD de conteneurisation/Kubernetes pour élèves ingénieurs (dernière année). **Les étudiants ne développent pas l'application** : ils reçoivent le code déjà fonctionnel et travaillent uniquement sur la conteneurisation, le déploiement Kubernetes, le service discovery et le monitoring.

Conséquence pour le développement : le code doit être **simple, lisible, bien commenté**, sans fioritures inutiles. Pas besoin de tests exhaustifs ni d'optimisations poussées — la valeur pédagogique est dans l'infra, pas dans le code métier.

---

## 1. Specs fonctionnelles

### 1.1 Périmètre fonctionnel (volontairement minimal)

**Utilisateurs**
- Inscription (pseudo, email, mot de passe)
- Connexion / déconnexion (JWT)
- Consultation d'un profil simple (pseudo, bio, date d'inscription)
- Modification de son propre profil (bio uniquement)

**Posts**
- Création d'un post (titre + contenu texte, auteur = utilisateur connecté)
- Liste des posts récents (fil d'actualité, paginé, triés par date décroissante)
- Consultation d'un post avec ses commentaires
- Suppression d'un post (par son auteur uniquement)

**Commentaires**
- Ajout d'un commentaire sur un post (auteur = utilisateur connecté)
- Liste des commentaires d'un post

**Notifications**
- Un utilisateur reçoit une notification quand quelqu'un commente un de ses posts
- Liste de ses notifications (lues/non lues)
- Marquer une notification comme lue

### 1.2 Hors périmètre (explicitement exclu)

- Pas de likes, pas de partage, pas de messages privés
- Pas d'upload d'images/fichiers
- Pas de recherche full-text
- Pas de vrai envoi d'email/SMS (les notifications restent en base)
- Pas de front-end soigné : une UI minimale suffit (ou juste une collection Postman/fichier `.http`), l'important est l'API

---

## 2. Specs techniques

### 2.1 Architecture — 3 services + infra

```
gateway (nginx/Traefik) → users, posts, notifications
users        → Postgres (users_db)
posts        → Postgres (posts_db) + Redis (cache fil d'actualité)
notifications → Postgres (notifications_db) + Redis (pub/sub)
```

- **3 services applicatifs indépendants**, chacun avec sa propre base de données (schéma "1 service = 1 DB", pas de DB partagée) — pour que le TD illustre un vrai découplage.
- Communication **synchrone HTTP** entre `posts` et `users` (vérification de token).
- Communication **asynchrone** entre `posts`/`notifications` via Redis pub/sub (découplage fort, `posts` ne doit pas savoir que `notifications` existe).
- Un **gateway** (reverse proxy) comme point d'entrée unique — prépare à l'Ingress Kubernetes.

### 2.2 Service `users`

- Stack libre mais légère (suggestion : Node.js/Express ou Python/FastAPI)
- Endpoints :
  - `POST /register` — {pseudo, email, password} → 201
  - `POST /login` — {email, password} → {token}
  - `GET /verify` — header `Authorization: Bearer <token>` → {valid: bool, user_id, pseudo} (endpoint interne, appelé par `posts`)
  - `GET /users/:id` — profil public
  - `PATCH /users/me` — modifier sa bio (auth requise)
  - `GET /health` — healthcheck (200 si DB accessible)
  - `GET /metrics` — format Prometheus (voir §2.6)
- DB : table `users (id, pseudo, email, password_hash, bio, created_at)`
- JWT signé avec un secret partagé via variable d'environnement (deviendra un Secret k8s)

### 2.3 Service `posts`

- Endpoints :
  - `GET /posts?page=&limit=` — liste paginée, lit le cache Redis si dispo, sinon DB puis remplit le cache (TTL court, ex. 30s)
  - `POST /posts` — {title, content} + auth → appelle `GET /verify` sur `users`, invalide le cache
  - `GET /posts/:id` — détail + commentaires
  - `DELETE /posts/:id` — auth requise, vérifie que l'auteur correspond
  - `POST /posts/:id/comments` — {content} + auth → publie un événement Redis `comment.created` après écriture
  - `GET /health` — doit répondre **dégradé mais pas planter** si `users` est injoignable (ex: écritures désactivées mais lecture OK) — point pédagogique important pour la résilience
  - `GET /metrics`
- DB : tables `posts (id, title, content, author_id, created_at)`, `comments (id, post_id, author_id, content, created_at)`
- Variable d'env pour l'URL de `users` (ex: `USERS_SERVICE_URL=http://users:3000`) — **jamais d'IP en dur**, c'est le point clé du TD service discovery

### 2.4 Service `notifications`

- S'abonne au canal Redis `comment.created` (pub/sub, pas de queue durable nécessaire — simplicité pédagogique)
- À la réception : crée une notification pour l'auteur du post concerné
- Endpoints :
  - `GET /notifications` — auth requise, liste les notifs de l'utilisateur
  - `PATCH /notifications/:id/read`
  - `GET /health`
  - `GET /metrics`
- DB : table `notifications (id, user_id, type, payload, read, created_at)`
- Doit aussi vérifier le token via `users` pour les endpoints authentifiés

### 2.5 Gateway

- nginx ou Traefik en reverse proxy simple
- Routes : `/api/users/*` → users, `/api/posts/*` → posts, `/api/notifications/*` → notifications
- Pas de logique métier, juste du routing (+ CORS si front séparé)

### 2.6 Observabilité (prépare la séance monitoring)

Chaque service doit exposer `/metrics` au format Prometheus, avec au minimum :
- `http_requests_total{method, route, status}` (compteur)
- `http_request_duration_seconds{route}` (histogramme)
- Une métrique "métier" par service :
  - `users` : `users_registered_total`
  - `posts` : `posts_created_total`, `comments_created_total`
  - `notifications` : `notifications_sent_total`, et idéalement un gauge sur le lag de traitement pub/sub

Utiliser une lib standard (`prom-client` en Node, `prometheus_client` en Python) plutôt que du fait-main.

### 2.7 Configuration / secrets

- Tout via variables d'environnement (aucune valeur en dur dans le code) :
  - URLs des services (`USERS_SERVICE_URL`, etc.)
  - Connexions DB/Redis
  - `JWT_SECRET`
- Objectif : que les étudiants puissent basculer ces valeurs en ConfigMap/Secret k8s sans toucher au code.

### 2.8 Conteneurisation attendue (livrable du dev, base du TD)

- 1 Dockerfile **multi-stage** par service (stage build + stage run, image finale légère)
- 1 `docker-compose.yml` de référence à la racine, fonctionnel de bout en bout (les 3 services + gateway + Postgres×3 + Redis), avec healthchecks définis
- Un `docker-compose.override.yml` ou profil "dev" avec hot-reload (bind mount du code source) — sert la séance 1 (Docker pour le dev)
- Un `seed.sql` ou script d'init pour peupler un jeu de données de démo (quelques users/posts/comments) pour ne pas partir d'une base vide

### 2.9 Structure de repo attendue

```
/
├── docker-compose.yml
├── docker-compose.override.yml
├── services/
│   ├── users/
│   │   ├── Dockerfile
│   │   ├── src/
│   │   └── ...
│   ├── posts/
│   │   ├── Dockerfile
│   │   ├── src/
│   │   └── ...
│   └── notifications/
│       ├── Dockerfile
│       ├── src/
│       └── ...
├── gateway/
│   ├── Dockerfile
│   └── nginx.conf
├── seed/
│   └── seed.sql
└── README.md  (comment lancer le tout, quelles URLs pour tester)
```

---

## 3. Points de vigilance à donner à Claude Code

- **Ne pas coupler les services entre eux au-delà de ce qui est décrit** (pas d'appel direct `posts → notifications`, ça doit passer par Redis pub/sub)
- **Aucune IP en dur**, uniquement des noms de service résolus par variable d'env — c'est le cœur pédagogique du TD service discovery
- Le endpoint `/health` de `posts` doit vraiment gérer le cas où `users` est down (timeout court + fallback), pas planter
- Code commenté et volontairement simple : les étudiants liront le code pour comprendre les dépendances entre services, mais n'auront pas à le modifier
- Fournir un `README.md` clair avec les commandes pour lancer, tester chaque endpoint (curl ou collection Postman), et un exemple de scénario de bout en bout (inscription → post → commentaire → notification)
