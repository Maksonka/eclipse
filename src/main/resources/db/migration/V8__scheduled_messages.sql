-- ShadowVibe scheduled (delayed) messages (V8)
-- Messages scheduled to be sent at a future date/time.
-- target_type: DIRECT (receiver_username) or GROUP (group_id)

CREATE TABLE scheduled_messages (
    id               BIGSERIAL PRIMARY KEY,
    sender_id        BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    target_type      VARCHAR(8)   NOT NULL,
    receiver_username VARCHAR(255),
    group_id         BIGINT       REFERENCES chat_groups(id) ON DELETE CASCADE,
    content          VARCHAR(2000) NOT NULL,
    reply_to_id      BIGINT,
    schedule_at      TIMESTAMP(6) NOT NULL,
    status           VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    error_message    VARCHAR(500),
    sent_at          TIMESTAMP(6),
    created_at       TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_scheduled_pending_due ON scheduled_messages(status, schedule_at);
CREATE INDEX idx_scheduled_sender_pending ON scheduled_messages(sender_id, status, schedule_at);
