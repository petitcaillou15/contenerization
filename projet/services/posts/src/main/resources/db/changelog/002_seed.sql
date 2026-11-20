--liquibase formatted sql

--changeset posts:100-seed-demo context:demo
INSERT INTO posts (id, title, content, author_id, created_at) VALUES
  (1, 'Bienvenue', 'Premier post du fil de démonstration.', 1, CURRENT_TIMESTAMP),
  (2, 'Astuce Kubernetes', 'Pensez aux readiness probes.', 2, CURRENT_TIMESTAMP);
INSERT INTO comments (id, post_id, author_id, content, created_at) VALUES
  (1, 1, 2, 'Super, merci !', CURRENT_TIMESTAMP);
--rollback DELETE FROM comments WHERE id = 1; DELETE FROM posts WHERE id IN (1,2);

-- Les id ci-dessus sont insérés en dur : la séquence IDENTITY n'a pas avancé.
-- Sans ce RESTART, le premier INSERT applicatif réutiliserait l'id 1 -> collision (posts_pkey / comments_pkey).
--changeset posts:101-reset-identity context:demo
ALTER TABLE posts ALTER COLUMN id RESTART WITH 3;
ALTER TABLE comments ALTER COLUMN id RESTART WITH 2;
--rollback ALTER TABLE posts ALTER COLUMN id RESTART WITH 1; ALTER TABLE comments ALTER COLUMN id RESTART WITH 1;
