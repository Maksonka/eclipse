ALTER TABLE users ADD COLUMN ghost_mode BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE ghost_exceptions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    exception_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    show_activity BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE(user_id, exception_user_id)
);
