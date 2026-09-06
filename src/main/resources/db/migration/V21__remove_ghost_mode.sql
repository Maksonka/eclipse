-- Ghost Mode (Premium) removed entirely
DROP TABLE IF EXISTS ghost_exceptions;

ALTER TABLE users DROP COLUMN IF EXISTS ghost_mode;