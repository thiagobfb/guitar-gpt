ALTER TABLE tracks ADD COLUMN updated_at TIMESTAMP;

UPDATE tracks SET updated_at = created_at WHERE updated_at IS NULL;
