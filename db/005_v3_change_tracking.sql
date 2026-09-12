-- V3: Version-aware change tracking
CREATE EXTENSION IF NOT EXISTS pgcrypto;
-- Store the actual diff associated with a change event.
ALTER TABLE change_events
ADD COLUMN IF NOT EXISTS diff_text TEXT,
ADD COLUMN IF NOT EXISTS lines_added INTEGER NOT NULL DEFAULT 0,
ADD COLUMN IF NOT EXISTS lines_removed INTEGER NOT NULL DEFAULT 0;


-- Store the impact prediction generated for each change event.
CREATE TABLE IF NOT EXISTS impact_predictions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    change_event_id UUID NOT NULL
        REFERENCES change_events(id)
        ON DELETE CASCADE,

    affected_file_id UUID NOT NULL
        REFERENCES files(id)
        ON DELETE CASCADE,

    impact_depth INTEGER NOT NULL,
    impact_score NUMERIC(8,6) NOT NULL,
    impact_level TEXT NOT NULL,

    min_confidence NUMERIC(5,4),
    path TEXT,
    relationship_path TEXT,

    path_count INTEGER NOT NULL DEFAULT 1,

    predicted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    UNIQUE(change_event_id, affected_file_id)
);


CREATE INDEX IF NOT EXISTS idx_impact_predictions_event
    ON impact_predictions(change_event_id);

CREATE INDEX IF NOT EXISTS idx_impact_predictions_affected_file
    ON impact_predictions(affected_file_id);

-- Record a change event and return its ID.
CREATE OR REPLACE FUNCTION record_change_event(
    p_file_id UUID,
    p_previous_version_id UUID,
    p_new_version_id UUID,
    p_change_summary TEXT,
    p_diff_text TEXT,
    p_lines_added INTEGER,
    p_lines_removed INTEGER
)
RETURNS UUID
LANGUAGE plpgsql
AS $$
DECLARE
    v_event_id UUID;
BEGIN
    INSERT INTO change_events (
        id,
        file_id,
        previous_version_id,
        new_version_id,
        change_summary,
        diff_text,
        lines_added,
        lines_removed
    )
    VALUES (
        gen_random_uuid(),
        p_file_id,
        p_previous_version_id,
        p_new_version_id,
        p_change_summary,
        p_diff_text,
        p_lines_added,
        p_lines_removed
    )
    RETURNING id INTO v_event_id;

    RETURN v_event_id;
END;
$$;