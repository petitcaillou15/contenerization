--liquibase formatted sql

-- Hash BCrypt (cost 10) du mot de passe littéral "password", vérifié à la génération.
--changeset users:100-seed-demo-users context:demo
INSERT INTO users (id, pseudo, email, password_hash, bio, created_at) VALUES
  (1, 'alice', 'alice@example.com', '$2a$10$PJgZO0kE2zEc64xr5aP8Gu2RpzbfRSsKCzk29qRNpij8K5SpVMa76', 'Bonjour, je suis Alice.', CURRENT_TIMESTAMP),
  (2, 'bob',   'bob@example.com',   '$2a$10$PJgZO0kE2zEc64xr5aP8Gu2RpzbfRSsKCzk29qRNpij8K5SpVMa76', 'Bob, ravi de vous lire.', CURRENT_TIMESTAMP);
--rollback DELETE FROM users WHERE id IN (1, 2);

-- Les id ci-dessus sont insérés en dur : la séquence IDENTITY n'a pas avancé.
-- Sans ce RESTART, le premier INSERT applicatif réutiliserait l'id 1 -> collision.
--changeset users:101-reset-identity context:demo
ALTER TABLE users ALTER COLUMN id RESTART WITH 3;
--rollback ALTER TABLE users ALTER COLUMN id RESTART WITH 1;
