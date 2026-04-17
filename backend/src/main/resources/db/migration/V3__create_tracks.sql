CREATE TABLE tracks (
    id          UUID PRIMARY KEY,
    project_id  UUID         NOT NULL REFERENCES musical_projects (id) ON DELETE CASCADE,
    name        VARCHAR(255) NOT NULL,
    type        VARCHAR(50)  NOT NULL,
    description TEXT,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tracks_project_id ON tracks (project_id);
