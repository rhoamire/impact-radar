-- V5: GenAI summaries and impact explanations

CREATE TABLE IF NOT EXISTS ai_generations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    generation_type TEXT NOT NULL
        CHECK (
            generation_type IN (
                'CHANGE_SUMMARY',
                'IMPACT_EXPLANATION'
            )
        ),

    change_event_id UUID NOT NULL
        REFERENCES change_events(id)
        ON DELETE CASCADE,

    affected_file_id UUID
        REFERENCES files(id)
        ON DELETE CASCADE,

    model_name TEXT NOT NULL,

    source_context TEXT NOT NULL,

    generated_text TEXT NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


CREATE INDEX IF NOT EXISTS idx_ai_generations_change_event
    ON ai_generations(change_event_id);


CREATE INDEX IF NOT EXISTS idx_ai_generations_affected_file
    ON ai_generations(affected_file_id);


CREATE INDEX IF NOT EXISTS idx_ai_generations_type
    ON ai_generations(generation_type);