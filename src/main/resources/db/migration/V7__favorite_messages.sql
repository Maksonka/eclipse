-- ShadowVibe favorite (starred) messages (V7)
-- Personal favorites: each user can star direct and group messages.

CREATE TABLE favorite_messages (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    target_type  VARCHAR(8)   NOT NULL,
    message_id   BIGINT       NOT NULL,
    favorited_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_favorite_user_type_msg ON favorite_messages(user_id, target_type, message_id);
CREATE INDEX idx_favorite_user_type_time ON favorite_messages(user_id, target_type, favorited_at DESC);
