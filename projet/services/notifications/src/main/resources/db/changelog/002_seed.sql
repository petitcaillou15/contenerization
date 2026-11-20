--liquibase formatted sql

--changeset notifications:100-seed-demo context:demo
INSERT INTO notifications (id, user_id, type, payload, read, created_at) VALUES
  (1, 1, 'comment_on_post',
   '{"post_id":1,"post_author_id":1,"comment_author_id":2,"comment_author_pseudo":"bob","comment_id":1,"created_at":"2026-08-28T09:00:00Z"}',
   FALSE, CURRENT_TIMESTAMP);
--rollback DELETE FROM notifications WHERE id = 1;

-- L'id ci-dessus est inséré en dur : la séquence IDENTITY n'a pas avancé.
-- Sans ce RESTART, le premier INSERT applicatif réutiliserait l'id 1 -> collision.
--changeset notifications:101-reset-identity context:demo
ALTER TABLE notifications ALTER COLUMN id RESTART WITH 2;
--rollback ALTER TABLE notifications ALTER COLUMN id RESTART WITH 1;
