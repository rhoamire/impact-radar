BEGIN;

ALTER TABLE file_relationships
ADD COLUMN source_candidate_id UUID
    REFERENCES relationship_candidates(id);

CREATE INDEX idx_file_relationships_source_candidate
    ON file_relationships(source_candidate_id);


-- Link the already-accepted inferred relationship to
-- the candidate that created it.
UPDATE file_relationships fr
SET source_candidate_id = rc.id
FROM relationship_candidates rc
WHERE rc.status = 'ACCEPTED'
  AND fr.provenance = 'INFERRED'
  AND fr.source_file_id = rc.source_file_id
  AND fr.target_file_id = rc.target_file_id
  AND fr.relationship_type = rc.suggested_relationship_type;


-- Acceptance now records which candidate created the
-- inferred authoritative relationship.
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


-- Undo either a rejection or an acceptance.
--
-- ACCEPTED -> CANDIDATE:
--   remove the inferred relationship created by this candidate.
--
-- REJECTED -> CANDIDATE:
--   simply restore the candidate; there is no relationship
--   to remove.
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

    UPDATE relationship_candidates
    SET
        status = 'CANDIDATE',
        reviewed_at = NULL
    WHERE id = p_candidate_id;
END;
$function$;

COMMIT;