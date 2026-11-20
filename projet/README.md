# TD Kubernetes — Mini réseau social (projet exemple)

Application pédagogique support d'un TD de conteneurisation / Kubernetes.
**Les étudiants ne développent pas l'application** : ils reçoivent le code fonctionnel
et travaillent sur la conteneurisation, le déploiement Kubernetes, le service discovery
et le monitoring.

Spécification fonctionnelle de référence : [`specs-mini-reseau-social.md`](specs-mini-reseau-social.md).

---

## 1. Contexte du TD

Forum minimaliste : utilisateurs, posts, commentaires, notifications. 3 microservices
Spring Boot indépendants derrière un gateway nginx, une instance Postgres (un schéma +
un rôle par service), un Redis pour le pub/sub. Le code métier est volontairement
simple et commenté ; la valeur pédagogique est dans l'infra.

## 2. Prérequis

- **Docker** + **Docker Compose v2** (`docker compose version` ≥ 2.x).
- Rien d'autre pour lancer la stack. Java 21 / Node ne servent qu'à lancer les tests
  ou le profil dev sans conteneur.

## 3. Démarrage

```bash
cp .env.example .env
docker compose up --build
```

- **Navigateur** (SPA Angular servie par nginx) : http://localhost:8080
- **API** (curl / Postman / `api.http`) : http://localhost:8081/api/...
- Health du gateway : http://localhost:8081/healthz → `ok`

`SEED_ENABLED=1` (défaut de `.env.example`) charge le jeu de démo (2 utilisateurs
`alice` / `bob`, mot de passe `password`, 2 posts, 1 commentaire, 1 notification).
Commenter la ligne pour une base vide (cible k8s).

## 5. Services, ports, rôles

| Service | Image / build | Port hôte | Rôle Postgres | Schéma |
|---|---|---|---|---|
| postgres | postgres:16-alpine | — | — | — |
| redis | redis:7-alpine | — | — | — |
| users | ./services/users | — | users_svc | users |
| posts | ./services/posts | — | posts_svc | posts |
| notifications | ./services/notifications | — | notifications_svc | notifications |
| gateway | ./gateway | 8081 | — | — |
| frontend | ./frontend | 8080 | — | — |

Les services ne sont **pas** publiés : tout passe par le gateway (`:8081`).

## 6. Tester l'API

Collection prête à l'emploi : [`api.http`](api.http) (IntelliJ HTTP Client / VS Code REST Client).
Le gateway retire le préfixe `/api/<service>/` ; la route du service suit. Certains
chemins ont donc un segment « doublé » (`/api/users/users/1` → `users` reçoit `/users/1`).

```bash
G=http://localhost:8081/api
curl -s $G/users/health
curl -s $G/posts/health
curl -s $G/notifications/health
curl -s "$G/posts/posts?page=0&limit=20"           # fil d'actualité (public)
curl -s $G/posts/posts/1                             # post + commentaires
curl -s $G/users/users/1                             # profil public
```

## 7. Scénario bout-en-bout

Inscription A → inscription B → login A → A crée un post → login B → B commente
→ (Redis `comment.created`) → A voit la notification → A la marque lue.

```bash
G=http://localhost:8081/api
curl -fsS -XPOST $G/users/register -H 'Content-Type: application/json' \
  -d '{"pseudo":"alice2","email":"alice2@example.com","password":"password"}'
curl -fsS -XPOST $G/users/register -H 'Content-Type: application/json' \
  -d '{"pseudo":"bob2","email":"bob2@example.com","password":"password"}'
TOKA=$(curl -fsS -XPOST $G/users/login -H 'Content-Type: application/json' \
  -d '{"email":"alice2@example.com","password":"password"}' | sed -E 's/.*"token":"([^"]+)".*/\1/')
PID=$(curl -fsS -XPOST $G/posts/posts -H "Authorization: Bearer $TOKA" \
  -H 'Content-Type: application/json' -d '{"title":"t","content":"c"}' | sed -E 's/.*"id":([0-9]+).*/\1/')
TOKB=$(curl -fsS -XPOST $G/users/login -H 'Content-Type: application/json' \
  -d '{"email":"bob2@example.com","password":"password"}' | sed -E 's/.*"token":"([^"]+)".*/\1/')
curl -fsS -XPOST "$G/posts/posts/$PID/comments" -H "Authorization: Bearer $TOKB" \
  -H 'Content-Type: application/json' -d '{"content":"Super !"}'
sleep 2
curl -fsS $G/notifications/notifications -H "Authorization: Bearer $TOKA"
# -> [{"id":..,"type":"comment_on_post","payload":{... "comment_author_pseudo":"bob2" ...},"read":false,...}]
```

## 8. Démo résilience (couplage dégradé)

`posts` et `notifications` ne dépendent que de `postgres` + `redis` *healthy*, jamais de
`users`. On peut donc couper `users` :

```bash
G=http://localhost:8081/api
docker compose -f docker-compose.yml stop users
curl -s -o /dev/null -w '%{http_code}\n' "$G/posts/posts"          # 200  (lectures OK)
curl -s -o /dev/null -w '%{http_code}\n' -X POST "$G/posts/posts" \
  -H 'Authorization: Bearer x' -H 'Content-Type: application/json' -d '{"title":"x","content":"y"}'
#   -> 503  {"error":"writes temporarily disabled"}
curl -s "$G/posts/health" -w '\n%{http_code}\n'
#   -> body "status":"DEGRADED" (composant usersDependency), HTTP 200
docker compose -f docker-compose.yml start users
```

`users` revenu, `posts/health` repasse `UP` et les écritures sont ré-autorisées.

## 9. Observabilité

Chaque service expose Prometheus sous `/metrics` (via le gateway :
`curl :8081/api/<service>/metrics`).

**Métriques métier :**

| Métrique (nom du *meter* Micrometer) | Nom exposé dans `/metrics` | Type | Service |
|---|---|---|---|
| `users_registered_total` | `users_registered_total` | Counter | users |
| `posts_created_total` | `posts_total` ¹ | Counter | posts |
| `comments_created_total` | `comments_total` ¹ | Counter | posts |
| `notifications_sent_total` | `notifications_sent_total` | Counter | notifications |
| `notifications_pubsub_lag_seconds` | `notifications_pubsub_lag_seconds` | Gauge | notifications |

¹ Le client Prometheus (Micrometer 1.14) traite `_created` comme un suffixe OpenMetrics
réservé et le retire : le *meter* `posts_created_total` ressort donc `posts_total`. La
valeur est correcte ; seul le nom diffère de la spec §6. À réconcilier côté service
`posts` (renommer le compteur en `posts_created` / `comments_created`).

**Libellé de la spec ↔ nom Micrometer réel** (on garde les conventions Micrometer
standard plutôt que des noms maison) :

| Libellé spec | Nom Micrometer réel |
|---|---|
| `http_requests_total{method,route,status}` | `http_server_requests_seconds_count{method,uri,status}` |
| `http_request_duration_seconds{route}` | `http_server_requests_seconds_bucket` / `_sum` / `_count{uri}` |
| `users_registered_total` | `users_registered_total` (inchangé) |
| `posts_created_total`, `comments_created_total` | `posts_total`, `comments_total` (cf. note ¹) |
| `notifications_sent_total` | inchangé |
| lag pub/sub | `notifications_pubsub_lag_seconds` (Gauge) |

## 10. Architecture & règles de couplage

```
Navigateur ──▶ frontend (nginx, :8080) ──▶ gateway (nginx, :8081)
                                              ├─▶ users          ─ schéma users
curl/Postman ─────────────────────────────────┼─▶ posts          ─ schéma posts
                                              └─▶ notifications  ─ schéma notifications

posts ──HTTP /verify──▶ users                 (synchrone, vérif token ; users down -> 503 en écriture)
posts ──Redis "comment.created"──▶ notifications   (asynchrone ; posts ignore que notifications existe)
Postgres 16 : 1 instance, base "social", schémas users|posts|notifications, rôles *_svc cloisonnés
Redis 7 : pub/sub uniquement
```

- `posts` et `notifications` **ne s'appellent jamais** directement : uniquement Redis pub/sub.
- `posts` **ne connaît pas** `notifications` (il publie un événement, sans destinataire).
- `notifications` **ne décode jamais** de JWT et n'appelle jamais `posts`.
- Découverte de service **par nom** via variables d'environnement, **jamais d'IP en dur**.
- Le **gateway** est le point d'entrée unique (prépare l'Ingress k8s).

## 11. Pour aller plus loin

- Cache Redis du fil d'actualité (`posts`, TTL ~30 s).
- Secrets via un gestionnaire dédié (Vault, External Secrets Operator) au lieu de `.env`.
- Tests d'intégration Testcontainers (Postgres + Redis réels).
- Image plus fine : jar en couches (`layertools`) ou `jlink`.
- Envoi réel e-mail / SMS des notifications (aujourd'hui : en base uniquement).

## 12. Écarts assumés vs spec d'origine

- **1 instance Postgres / 3 schémas / isolation par rôles** au lieu d'« une base par
  service ». L'isolation par droits sur schéma (`db/00-init.sql` : `GRANT` + `ALTER ROLE
  … SET search_path`, accès inter-schéma refusé) préserve le point pédagogique du
  découplage.
- **`notifications.payload` stocké en `VARCHAR` (texte JSON)** et non `jsonb`, pour que
  le même changelog Liquibase tourne aussi sur la base H2 des tests.
- **Mots de passe des rôles en clair dans `db/00-init.sql`** (valeurs de dev jetables,
  identiques à `.env.example`). En k8s : injecter via un Secret (`ALTER ROLE … PASSWORD`).
- Table complète des écarts : voir le document de conception lié en tête de ce README.
