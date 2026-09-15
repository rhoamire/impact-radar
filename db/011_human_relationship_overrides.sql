BEGIN;

-- A human can explicitly veto a directed relationship.
-- This survives future semantic discovery runs.
CREATE TABLE IF NOT EXISTS relationship_overrides (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    source_file_id UUID NOT NULL
        REFERENCES files(id)
        ON DELETE CASCADE,

    target_file_id UUID NOT NULL
        REFERENCES files(id)
        ON DELETE CASCADE,

    reason TEXT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    UNIQUE(source_file_id, target_file_id),

    CHECK (source_file_id <> target_file_id)
);


CREATE INDEX IF NOT EXISTS idx_relationship_overrides_source
    ON relationship_overrides(source_file_id);

CREATE INDEX IF NOT EXISTS idx_relationship_overrides_target
    ON relationship_overrides(target_file_id);


-- Do not allow semantic discovery to recreate a relationship
-- that a human has explicitly rejected.
--
-- The override is pair-based, not relationship-type-based:
-- "these two documents are not related in this direction."
CREATE OR REPLACE FUNCTION reject_relationship_candidate(
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

    INSERT INTO relationship_overrides (
        source_file_id,
        target_file_id,
        reason
    )
    VALUES (
        candidate.source_file_id,
        candidate.target_file_id,
        candidate.evidence
    )
    ON CONFLICT (
        source_file_id,
        target_file_id
    )
    DO UPDATE SET
        reason = EXCLUDED.reason;

    UPDATE relationship_candidates
    SET
        status = 'REJECTED',
        reviewed_at = NOW()
    WHERE id = p_candidate_id;
END;
$function$;


-- Protect the authoritative graph from accepting a relationship
-- that a human has explicitly vetoed.
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

    IF EXISTS (
        SELECT 1
        FROM relationship_overrides ro
        WHERE ro.source_file_id = candidate.source_file_id
          AND ro.target_file_id = candidate.target_file_id
    ) THEN
        RAISE EXCEPTION
            'Relationship % -> % has a human override and cannot be accepted',
            candidate.source_file_id,
            candidate.target_file_id;
    END IF;

    INSERT INTO file_relationships (
        source_file_id,
        target_file_id,
        relationship_type,
        confidence,
        evidence,
        provenance,
        source_candidate_id
    )
    VALUES (
        candidate.source_file_id,
        candidate.target_file_id,
        candidate.suggested_relationship_type,
        candidate.confidence,
        candidate.evidence,
        'INFERRED',
        candidate.id
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


-- Rework undo so both human decisions are reversible.
--
-- ACCEPTED -> CANDIDATE
--   remove candidate-created inferred relationship
--
-- REJECTED -> CANDIDATE
--   remove human veto
CREATE OR REPLACE FUNCTION undo_relationship_candidate(
    p_candidate_id UUID
)
RETURNS VOID
LANGUAGE plpgsql
AS $function$
DECLARE
    v_status TEXT;
BEGIN
    SELECT status
    INTO v_status
    FROM relationship_candidates
    WHERE id = p_candidate_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION
            'Relationship candidate % does not exist',
            p_candidate_id;
    END IF;

    IF v_status NOT IN ('ACCEPTED', 'REJECTED') THEN
        RAISE EXCEPTION
            'Relationship candidate % cannot be undone from status %',
            p_candidate_id,
            v_status;
    END IF;

    IF v_status = 'ACCEPTED' THEN
        DELETE FROM file_relationships
        WHERE source_candidate_id = p_candidate_id;
    END IF;

    IF v_status = 'REJECTED' THEN
        DELETE FROM relationship_overrides ro
        USING relationship_candidates rc
        WHERE rc.id = p_candidate_id
          AND ro.source_file_id = rc.source_file_id
          AND ro.target_file_id = rc.target_file_id;
    END IF;

    UPDATE relationship_candidates
    SET
        status = 'CANDIDATE',
        reviewed_at = NULL
    WHERE id = p_candidate_id;
END;
$function$;

COMMIT;