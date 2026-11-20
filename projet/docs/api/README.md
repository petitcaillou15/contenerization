# Contrat d'interface HTTP — Mini réseau social

Document de référence pour un **client externe** (application Flutter développée
dans un autre cours). Il décrit **la totalité de la surface HTTP publique** des
trois micro-services, telle qu'elle est exposée **derrière le gateway**.

- Spécifications machine : [`openapi-users.yaml`](openapi-users.yaml),
  [`openapi-posts.yaml`](openapi-posts.yaml),
  [`openapi-notifications.yaml`](openapi-notifications.yaml) (OpenAPI 3.1).
- Ce README : tout le transverse (auth, routage, erreurs, conventions, scénario).

Ce contrat reflète le code au 2026-09-02 (`main` @ `b67c8cb`). Version `1.0.0`.

---

## 1. Point d'entrée unique : le gateway

Un client n'appelle **jamais** un service directement. Tout passe par le gateway
nginx :

```
http://<host>:8081/api/<service>/<route du service>
```

- `<host>` : `localhost` en développement (`docker compose`).
- `<service>` : `users`, `posts` ou `notifications`.
- Le gateway **retire** le préfixe `/api/<service>/` puis relaie au service.

### Le « segment doublé »

Le préfixe enlevé est `/api/<service>/`, **pas** le nom de la ressource. Quand la
route interne du service commence elle-même par son nom, le segment apparaît deux
fois dans l'URL publique :

| URL publique (ce que le client appelle) | Reçu par le service |
|---|---|
| `POST /api/users/register` | `POST /register` |
| `POST /api/users/login` | `POST /login` |
| `GET  /api/users/users/1` | `GET /users/1` |
| `PATCH /api/users/users/me` | `PATCH /users/me` |
| `GET  /api/posts/posts?page=0&limit=20` | `GET /posts?page=0&limit=20` |
| `POST /api/posts/posts` | `POST /posts` |
| `POST /api/posts/posts/1/comments` | `POST /posts/1/comments` |
| `GET  /api/notifications/notifications` | `GET /notifications` |
| `PATCH /api/notifications/notifications/1/read` | `PATCH /notifications/1/read` |

Dans les fichiers OpenAPI, `servers.url` vaut `http://{host}/api/<service>` et les
`paths` sont les routes internes : la concaténation redonne l'URL publique, segment
doublé compris.

---

## 2. Authentification — JWT

### Obtention

`POST /api/users/login` avec `{ "email", "password" }` renvoie
`{ "token": "<JWT>" }`.

### Format du token

- Algorithme **HS256**. Le secret n'est connu **que du service `users`** ; `posts`
  et `notifications` ne décodent jamais le token, ils appellent `users` en interne
  pour le valider.
- Claims : `sub` = identifiant utilisateur (chaîne numérique), `pseudo` = pseudo,
  `iat`, `exp`.
- Durée de vie : **86 400 s** (24 h) par défaut (variable `JWT_EXPIRATION`).
- Pas de *refresh token* : à l'expiration, se reconnecter via `/login`.

### Utilisation

En-tête sur chaque appel protégé :

```
Authorization: Bearer <token>
```

Token absent, mal formé, mal signé ou expiré → **401**
`{"error":"unauthorized"}` (la requête n'atteint pas la logique métier).

### Quelles routes exigent le token

| Route publique | Auth |
|---|---|
| `POST /api/users/register` | — |
| `POST /api/users/login` | — |
| `GET /api/users/users/{id}` | — |
| `PATCH /api/users/users/me` | **Bearer** |
| `GET /api/posts/posts` | — |
| `GET /api/posts/posts/{id}` | — |
| `POST /api/posts/posts` | **Bearer** |
| `POST /api/posts/posts/{id}/comments` | **Bearer** |
| `DELETE /api/posts/posts/{id}` | **Bearer** |
| `GET /api/notifications/notifications` | **Bearer** |
| `PATCH /api/notifications/notifications/{id}/read` | **Bearer** |
| `GET /api/<service>/health` | — |

Règle simple : **toutes les écritures** sont protégées ; côté `notifications`, la
lecture l'est aussi (les notifications sont privées). Côté `posts`, les lectures
(fil, détail) sont **publiques**.

---

## 3. Modèle d'erreur

Toutes les erreurs métier ont le même corps :

```json
{ "error": "message lisible" }
```

| HTTP | Quand | `error` (exemple) |
|---|---|---|
| `400` | Corps invalide (validation) | `"password size must be between 6 and 100"` |
| `401` | Token absent / invalide / expiré | `"unauthorized"` |
| `403` | Authentifié mais pas propriétaire de la ressource | `"not the author"`, `"not the recipient"` |
| `404` | Ressource inexistante | `"post 42 not found"`, `"user 42 not found"` |
| `409` | Pseudo ou email déjà pris (register) | `"pseudo or email already used"` |
| `503` | `posts` : `users` injoignable → écritures coupées | `"writes temporarily disabled"` |
| `503` | `notifications` : `users` injoignable | `"users service unavailable"` |

Le `503` est **temporaire** (dépendance `users` momentanément indisponible) :
réessayer plus tard. Les lectures de `posts` continuent de fonctionner pendant ce
temps.

---

## 4. Conventions de données

- **JSON en `snake_case`** : `author_id`, `created_at`, `post_id`…
- **Dates** : chaîne ISO-8601 en UTC, précision milliseconde, suffixe `Z`
  (ex. `2026-09-02T13:50:43.618Z`). Type OpenAPI `string` / `format: date-time`.
- **Identifiants** : entiers 64 bits (`format: int64`), toujours > 0.
- **Corps de réponse d'une création** : `{ "id": <nombre> }` (pas la ressource
  complète). Faire un `GET` ensuite si besoin du détail.
- `201` sans corps pour `register` ; `204` sans corps pour les `DELETE` et le
  `PATCH …/read`.
- `Content-Type: application/json` attendu sur toute requête avec corps.

---

## 5. Schéma du `payload` des notifications

`GET /api/notifications/notifications` renvoie une liste d'objets
`{ id, type, payload, read, created_at }`. Aujourd'hui il n'existe **qu'un seul
type** :

### `type: "comment_on_post"`

Émis quand **quelqu'un d'autre** commente un de vos posts (jamais pour un
auto-commentaire). `payload` :

| Champ | Type | Sens |
|---|---|---|
| `post_id` | int64 | post commenté |
| `post_author_id` | int64 | destinataire de la notification (vous) |
| `comment_author_id` | int64 | auteur du commentaire |
| `comment_author_pseudo` | string | pseudo de l'auteur du commentaire |
| `comment_id` | int64 | identifiant du commentaire |
| `created_at` | date-time | date du commentaire |

`payload` est modélisé `type: object` **ouvert** dans l'OpenAPI : un client
robuste teste `type` avant de lire les champs, pour rester compatible si d'autres
types apparaissent.

---

## 6. Scénario bout-en-bout

Le parcours nominal complet (deux comptes, un post, un commentaire, une
notification). `G` = `http://localhost:8081/api`.

```bash
G=http://localhost:8081/api

# 1. Deux inscriptions
curl -fsS -XPOST $G/users/register -H 'Content-Type: application/json' \
  -d '{"pseudo":"alice","email":"alice@example.com","password":"password"}'
curl -fsS -XPOST $G/users/register -H 'Content-Type: application/json' \
  -d '{"pseudo":"bob","email":"bob@example.com","password":"password"}'

# 2. alice se connecte -> token
TOKA=$(curl -fsS -XPOST $G/users/login -H 'Content-Type: application/json' \
  -d '{"email":"alice@example.com","password":"password"}' \
  | sed -E 's/.*"token":"([^"]+)".*/\1/')

# 3. alice publie un post -> {"id":N}
PID=$(curl -fsS -XPOST $G/posts/posts -H "Authorization: Bearer $TOKA" \
  -H 'Content-Type: application/json' \
  -d '{"title":"Mon premier post","content":"Bonjour"}' \
  | sed -E 's/.*"id":([0-9]+).*/\1/')

# 4. Fil public + détail (sans token)
curl -fsS "$G/posts/posts?page=0&limit=20"
curl -fsS "$G/posts/posts/$PID"

# 5. bob se connecte puis commente le post d'alice
TOKB=$(curl -fsS -XPOST $G/users/login -H 'Content-Type: application/json' \
  -d '{"email":"bob@example.com","password":"password"}' \
  | sed -E 's/.*"token":"([^"]+)".*/\1/')
curl -fsS -XPOST "$G/posts/posts/$PID/comments" -H "Authorization: Bearer $TOKB" \
  -H 'Content-Type: application/json' -d '{"content":"Super post !"}'

# 6. alice voit la notification (propagée par Redis, ~immédiat)
sleep 1
curl -fsS $G/notifications/notifications -H "Authorization: Bearer $TOKA"
# -> [{"id":1,"type":"comment_on_post","payload":{...,"comment_author_pseudo":"bob"},"read":false,"created_at":"..."}]

# 7. alice marque la notification 1 comme lue -> 204
curl -fsS -XPATCH $G/notifications/notifications/1/read -H "Authorization: Bearer $TOKA"

# 8. alice supprime son post -> 204 (commentaires supprimés en cascade)
curl -fsS -XDELETE "$G/posts/posts/$PID" -H "Authorization: Bearer $TOKA"
```

Jeu de démo (si `SEED_ENABLED=1`) : comptes `alice@example.com` /
`bob@example.com`, mot de passe `password`.

---

## 7. CORS

Le gateway ajoute sur **toutes** les réponses :

```
Access-Control-Allow-Origin:  *          (valeur de CORS_ALLOW_ORIGIN)
Access-Control-Allow-Methods: GET, POST, PATCH, DELETE, OPTIONS
Access-Control-Allow-Headers: Authorization, Content-Type
```

Les requêtes de pré-vol `OPTIONS` reçoivent `204` sans atteindre les services.
Suffisant pour un client Flutter **Web** ; sans objet pour Flutter mobile/desktop.

---

## 8. Santé et observabilité

| Endpoint | Contenu |
|---|---|
| `GET http://<host>:8081/healthz` | vivacité du **gateway** seul → texte `ok` |
| `GET /api/<service>/health` | santé Spring Boot Actuator : `{"status":"UP","components":{…}}` |
| `GET /api/<service>/metrics` | métriques Prometheus (format texte) — **hors périmètre client**, cité pour information |

Cas particulier `posts` : si `users` est injoignable, `/api/posts/health` renvoie
`{"status":"DEGRADED",…}` **avec un code HTTP 200** (le pod reste en service, seules
les écritures sont coupées).

Hors périmètre de ce contrat : `GET /api/users/verify` (appel **interne**
service-à-service, jamais destiné à un client).

---

## 9. Générer un client Dart

Les fichiers OpenAPI 3.1 sont exploitables tels quels, par ex. avec
[`openapi-generator`](https://openapi-generator.tech/) :

```bash
openapi-generator-cli generate \
  -i docs/api/openapi-posts.yaml \
  -g dart-dio \
  -o build/api-client/posts
```

Générer les trois clients séparément (un par service) garde le découpage en
briques du projet. Renseigner `host` (variable de serveur) à `localhost:8081` en
dev.
