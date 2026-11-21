-- db/00-init.sql
-- Exécuté par Postgres au PREMIER démarrage (/docker-entrypoint-initdb.d/), sur la base "social".
-- But pédagogique : 1 instance Postgres, 3 schémas, 3 rôles cloisonnés.
--
-- Les mots de passe ci-dessous sont des valeurs de DEV jetables (identiques à .env.example).
-- En k8s : ne PAS utiliser ce fichier tel quel ; injecter les mots de passe via un Secret
-- (ex. un initContainer qui fait `ALTER ROLE ... PASSWORD ...`, ou un values override Helm).

\getenv users_pwd USERS_DB_PASSWORD
\getenv posts_pwd POSTS_DB_PASSWORD
\getenv notifications_pwd NOTIFICATIONS_DB_PASSWORD

-- ── users ───────────────────────────────────────────────────────────────
CREATE SCHEMA IF NOT EXISTS users;
CREATE ROLE users_svc LOGIN PASSWORD :'users_pwd';
GRANT ALL ON SCHEMA users TO users_svc;
ALTER SCHEMA users OWNER TO users_svc;
ALTER ROLE users_svc SET search_path = users;

-- ── posts ───────────────────────────────────────────────────────────────
CREATE SCHEMA IF NOT EXISTS posts;
CREATE ROLE posts_svc LOGIN PASSWORD :'posts_pwd';
GRANT ALL ON SCHEMA posts TO posts_svc;
ALTER SCHEMA posts OWNER TO posts_svc;
ALTER ROLE posts_svc SET search_path = posts;

-- ── notifications ───────────────────────────────────────────────────────
CREATE SCHEMA IF NOT EXISTS notifications;
CREATE ROLE notifications_svc LOGIN PASSWORD :'notifications_pwd';
GRANT ALL ON SCHEMA notifications TO notifications_svc;
ALTER SCHEMA notifications OWNER TO notifications_svc;
ALTER ROLE notifications_svc SET search_path = notifications;

-- ── cloisonnement ──────────────────────────────────────────────────────
-- Personne d'autre ne touche ces schémas. Un service ne peut pas lire celui d'un autre.
REVOKE ALL ON SCHEMA users, posts, notifications FROM PUBLIC;
REVOKE ALL ON ALL TABLES IN SCHEMA users FROM posts_svc, notifications_svc;
REVOKE ALL ON ALL TABLES IN SCHEMA posts FROM users_svc, notifications_svc;
REVOKE ALL ON ALL TABLES IN SCHEMA notifications FROM users_svc, posts_svc;
