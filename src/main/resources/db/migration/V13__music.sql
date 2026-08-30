CREATE TABLE music_track (
    id BIGSERIAL PRIMARY KEY,
    uploader_id BIGINT NOT NULL REFERENCES users(id),
    stored_filename VARCHAR(128) NOT NULL UNIQUE,
    original_name VARCHAR(255),
    title VARCHAR(200) NOT NULL,
    artist VARCHAR(200),
    duration_seconds INT NOT NULL DEFAULT 0,
    file_size BIGINT NOT NULL DEFAULT 0,
    uploaded_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE playlist (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL REFERENCES users(id),
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE playlist_item (
    id BIGSERIAL PRIMARY KEY,
    playlist_id BIGINT NOT NULL REFERENCES playlist(id) ON DELETE CASCADE,
    track_id BIGINT NOT NULL REFERENCES music_track(id) ON DELETE CASCADE,
    position INT NOT NULL,
    added_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_playlist_item UNIQUE (playlist_id, track_id)
);

CREATE INDEX idx_playlist_item_playlist ON playlist_item(playlist_id);
CREATE INDEX idx_music_track_uploaded ON music_track(uploaded_at DESC);
