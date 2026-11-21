# Changelog

## 3.0.0

**Service `posts`**
- Le contenu d'un post est désormais limité en longueur : au-delà, `POST /posts`
  répond `400` (`content must be at most N characters`).
- Nouvelle variable d'environnement **obligatoire** : `POSTS_MAX_CONTENT_LENGTH`
  (ex. `280`). Elle n'a **pas de valeur par défaut** : le service ne démarre pas sans elle.
- `docker-compose.yml` et `.env.example` mis à jour.

## 2.0.0

Changements visibles pour tester un déploiement entre deux versions.

**Frontend**
- Thème vert, badge « v2 » dans la barre de navigation et bandeau d'information.
- Compteur de caractères sous le formulaire de publication (indicatif, 280 caractères).
- Pied de page affichant la version du frontend et la version de chaque service,
  rafraîchie toutes les 5 secondes (chaque appel atteint un pod au hasard : pendant un
  rolling update, on voit alterner l'ancienne et la nouvelle version).

**Backend** (`users`, `posts`, `notifications`)
- Nouvel endpoint `GET /version` (public) : `{"service": "...", "version": "2.0.0"}`.
  Accessible via le gateway : `/api/<service>/version`.
- Nouvelle propriété `app.version` (dans `application.yml`).

Aucune nouvelle variable d'environnement, aucun changement de schéma de base de données.

## 1.0.0

Version initiale.
