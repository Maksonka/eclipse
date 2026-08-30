CREATE TABLE group_invites (
    id          BIGSERIAL PRIMARY KEY,
    group_id    BIGINT NOT NULL REFERENCES chat_groups(id) ON DELETE CASCADE,
    invited_by_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    invited_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status      VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (group_id, invited_user_id)
);

CREATE INDEX idx_group_invites_user ON group_invites(invited_user_id, status);
CREATE INDEX idx_group_invites_group ON group_invites(group_id, status);
