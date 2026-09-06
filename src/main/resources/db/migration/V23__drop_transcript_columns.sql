-- Voice transcription removed entirely
ALTER TABLE message DROP COLUMN IF EXISTS transcript;

ALTER TABLE group_messages DROP COLUMN IF EXISTS transcript;