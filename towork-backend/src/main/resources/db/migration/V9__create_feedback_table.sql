DROP TABLE IF EXISTS feedbacks;

CREATE TABLE feedbacks (
    id BIGSERIAL PRIMARY KEY,
    mission_id BIGINT NOT NULL,
    author_user_id BIGINT NOT NULL,
    target_user_id BIGINT NOT NULL,
    direction VARCHAR(40) NOT NULL,
    rating INTEGER NOT NULL,
    comment VARCHAR(2000),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_feedback_mission FOREIGN KEY (mission_id) REFERENCES missions(id) ON DELETE CASCADE,
    CONSTRAINT chk_feedback_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT uk_feedback_mission_author UNIQUE (mission_id, author_user_id)
);

CREATE INDEX idx_feedback_mission ON feedbacks (mission_id);
CREATE INDEX idx_feedback_target ON feedbacks (target_user_id);
