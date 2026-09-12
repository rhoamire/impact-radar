-- V4: Semantic relationship discovery

CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pgcrypto;


-- A version can be split into multiple chunks.
-- For our current tiny synthetic documents, we'll initially create
-- one chunk per version. The model will still work for larger documents later.
CREATE TABLE IF NOT EXISTS document_chunks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    file_version_id UUID NOT NULL
        REFERENCES file_versions(id)
        ON DELETE CASCADE,

    chunk_index INTEGER NOT NULL,

    content TEXT NOT NULL,

    content_hash TEXT NOT NULL,

    embedding VECTOR(384) NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    UNIQUE(file_version_id, chunk_index)
);


-- HNSW supports efficient cosine-distance search.
CREATE INDEX IF NOT EXISTS idx_document_chunks_embedding
    ON document_chunks
    USING hnsw (embedding vector_cosine_ops);


CREATE INDEX IF NOT EXISTS idx_document_chunks_version
    ON document_chunks(file_version_id);


-- Semantic discovery does NOT directly modify file_relationships.
-- It creates candidates here.
CREATE TABLE IF NOT EXISTS relationship_candidates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    source_file_id UUID NOT NULL
        REFERENCES files(id)
        ON DELETE CASCADE,

    target_file_id UUID NOT NULL
        REFERENCES files(id)
        ON DELETE CASCADE,

    source_version_id UUID NOT NULL
        REFERENCES file_versions(id)
        ON DELETE CASCADE,

    target_version_id UUID NOT NULL
        REFERENCES file_versions(id)
        ON DELETE CASCADE,

    similarity NUMERIC(6,5) NOT NULL
        CHECK (similarity >= 0 AND similarity <= 1),

    suggested_relationship_type relationship_type,

    confidence NUMERIC(6,5)
        CHECK (confidence >= 0 AND confidence <= 1),

    evidence TEXT,

    model_name TEXT NOT NULL,

    status TEXT NOT NULL DEFAULT 'CANDIDATE'
        CHECK (status IN ('CANDIDATE', 'ACCEPTED', 'REJECTED')),

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    reviewed_at TIMESTAMPTZ,

    UNIQUE(
        source_file_id,
        target_file_id,
        source_version_id,
        target_version_id
    ),

    CHECK (source_file_id <> target_file_id)
);


CREATE INDEX IF NOT EXISTS idx_relationship_candidates_status
    ON relationship_candidates(status);


CREATE INDEX IF NOT EXISTS idx_relationship_candidates_similarity
    ON relationship_candidates(similarity DESC);


CREATE INDEX IF NOT EXISTS idx_relationship_candidates_source
    ON relationship_candidates(source_file_id);


CREATE INDEX IF NOT EXISTS idx_relationship_candidates_target
    ON relationship_candidates(target_file_id);
    
ALTER TABLE file_relationships
ALTER COLUMN id SET DEFAULT gen_random_uuid();

    -- Promote an accepted semantic candidate into the authoritative graph.
CREATE OR REPLACE FUNCTION accept_relationship_candidate(
    p_candidate_id UUID
)
RETURNS VOID
LANGUAGE plpgsql
AS $function$
DECLARE
    candidate relationship_candidates%ROWTYPE;
BEGIN
    SELECT *
    INTO candidate
    FROM relationship_candidates
    WHERE id = p_candidate_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION
            'Relationship candidate % does not exist',
            p_candidate_id;
    END IF;

    IF candidate.status <> 'CANDIDATE' THEN
        RAISE EXCEPTION
            'Relationship candidate % is already %',
            p_candidate_id,
            candidate.status;
    END IF;

    IF candidate.suggested_relationship_type IS NULL THEN
        RAISE EXCEPTION
            'Cannot accept candidate % without a suggested relationship type',
            p_candidate_id;
    END IF;

    INSERT INTO file_relationships (
        source_file_id,
        target_file_id,
        relationship_type,
        confidence,
        evidence
    )
    VALUES (
        candidate.source_file_id,
        candidate.target_file_id,
        candidate.suggested_relationship_type,
        candidate.confidence,
        candidate.evidence
    )
    ON CONFLICT (
        source_file_id,
        target_file_id,
        relationship_type
    )
    DO NOTHING;

    UPDATE relationship_candidates
    SET
        status = 'ACCEPTED',
        reviewed_at = NOW()
    WHERE id = p_candidate_id;
END;
$function$;


-- Reject a candidate without changing the authoritative graph.
CREATE OR REPLACE FUNCTION reject_relationship_candidate(
    p_candidate_id UUID
)
RETURNS VOID
LANGUAGE sql
AS $function$
    UPDATE relationship_candidates
    SET
        status = 'REJECTED',
        reviewed_at = NOW()
    WHERE id = p_candidate_id
      AND status = 'CANDIDATE';
$function$;

-- V4 evaluation ground truth.
--
-- This table contains known positive/negative cases used only
-- to evaluate semantic relationship discovery.
-- It does not participate in normal impact analysis.

CREATE TABLE IF NOT EXISTS semantic_evaluation_cases (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    source_file_id UUID NOT NULL
        REFERENCES files(id)
        ON DELETE CASCADE,

    target_file_id UUID NOT NULL
        REFERENCES files(id)
        ON DELETE CASCADE,

    expected_relationship_type relationship_type,

    relationship_exists BOOLEAN NOT NULL,

    description TEXT NOT NULL,

    UNIQUE(source_file_id, target_file_id)
);