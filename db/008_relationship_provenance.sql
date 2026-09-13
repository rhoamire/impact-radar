-- V5 cleanup:
-- Distinguish authoritative provenance from inferred confidence.

CREATE TYPE relationship_provenance AS ENUM (
    'EXPLICIT',
    'INFERRED'
);


ALTER TABLE file_relationships
ADD COLUMN provenance relationship_provenance
    NOT NULL DEFAULT 'EXPLICIT';


ALTER TABLE file_relationships
ALTER COLUMN confidence DROP NOT NULL;


-- Existing relationships were originally seeded explicitly.
-- Reclassify the one relationship that was promoted from a semantic candidate.
UPDATE file_relationships fr
SET
    provenance = 'INFERRED',
    confidence = rc.confidence
FROM relationship_candidates rc
WHERE rc.status = 'ACCEPTED'
  AND rc.source_file_id = fr.source_file_id
  AND rc.target_file_id = fr.target_file_id
  AND rc.suggested_relationship_type = fr.relationship_type;


-- Explicit relationships do not need a probability-like confidence.
UPDATE file_relationships
SET confidence = NULL
WHERE provenance = 'EXPLICIT';