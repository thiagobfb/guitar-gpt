CREATE TABLE generation_requests (
    id                 UUID PRIMARY KEY,
    project_id         UUID         NOT NULL REFERENCES musical_projects (id) ON DELETE CASCADE,
    prompt_template_id UUID         NOT NULL REFERENCES prompt_templates (id),
    user_prompt        TEXT         NOT NULL,
    status             VARCHAR(50)  NOT NULL,
    result_text        TEXT,
    error_message      TEXT,
    created_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_generation_requests_project_id ON generation_requests (project_id);
CREATE INDEX idx_generation_requests_status ON generation_requests (status);
