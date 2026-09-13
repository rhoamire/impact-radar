BEGIN;

DROP FUNCTION IF EXISTS public.analyze_impact(uuid);

CREATE FUNCTION public.analyze_impact(p_file_id uuid)
RETURNS TABLE(
    root_file_id uuid,
    affected_file_id uuid,
    affected_file_name text,
    impact_depth integer,
    path text,
    relationship_path text,
    provenance_path text,
    min_confidence numeric,
    relationship_count integer,
    path_count integer,
    impact_score numeric,
    impact_level text
)
LANGUAGE sql
AS $function$

WITH RECURSIVE impact AS (

    -- Direct dependents of the changed file.
    SELECT
        p_file_id AS root_file_id,

        r.source_file_id AS affected_file_id,

        1 AS impact_depth,

        ARRAY[
            p_file_id,
            r.source_file_id
        ]::UUID[] AS visited,

        ARRAY[
            root_file.name,
            affected_file.name
        ]::TEXT[] AS file_path,

        ARRAY[
            r.relationship_type::TEXT
        ] AS rel_path,

        ARRAY[
            r.provenance::TEXT
        ] AS provenance_path,

        /*
         * Explicit relationships have NULL confidence because
         * they are declared rather than probabilistic.
         *
         * For ranking purposes, an explicit relationship is
         * treated as full-strength.
         *
         * Inferred relationships retain their actual confidence.
         */
        COALESCE(
            r.confidence,
            1.0000
        )::NUMERIC(5,4) AS min_confidence,

        1 AS relationship_count,

        (
            COALESCE(
                r.confidence,
                1.0000
            )
            * rw.impact_weight
        )::NUMERIC(8,6) AS path_score

    FROM file_relationships r

    JOIN relationship_weights rw
        ON rw.relationship_type = r.relationship_type

    JOIN files root_file
        ON root_file.id = p_file_id

    JOIN files affected_file
        ON affected_file.id = r.source_file_id

    /*
     * Relationship direction in our graph is:

         dependent --depends_on--> dependency

     * Therefore, to find files affected by a changed dependency,
     * walk from target_file_id back to source_file_id.
     */
    WHERE r.target_file_id = p_file_id


    UNION ALL


    -- Continue from the currently affected file.
    SELECT
        i.root_file_id,

        r.source_file_id AS affected_file_id,

        i.impact_depth + 1,

        i.visited || r.source_file_id,

        i.file_path || affected_file.name,

        i.rel_path || r.relationship_type::TEXT,

        i.provenance_path || r.provenance::TEXT,

        LEAST(
            i.min_confidence,
            COALESCE(
                r.confidence,
                1.0000
            )
        )::NUMERIC(5,4),

        i.relationship_count + 1,

        (
            i.path_score
            * COALESCE(
                r.confidence,
                1.0000
            )
            * rw.impact_weight
            * 0.850000
        )::NUMERIC(8,6) AS path_score

    FROM impact i

    JOIN file_relationships r
        ON r.target_file_id = i.affected_file_id

    JOIN relationship_weights rw
        ON rw.relationship_type = r.relationship_type

    JOIN files affected_file
        ON affected_file.id = r.source_file_id

    WHERE NOT (
        r.source_file_id = ANY(i.visited)
    )
),


path_stats AS (
    SELECT
        i.affected_file_id,

        COUNT(*)::INTEGER AS path_count,

        MAX(
            i.path_score
        )::NUMERIC(8,6) AS impact_score

    FROM impact i

    GROUP BY i.affected_file_id
),


best_path AS (
    SELECT DISTINCT ON (i.affected_file_id)

        i.root_file_id,

        i.affected_file_id,

        i.impact_depth,

        i.file_path,

        i.rel_path,

        i.provenance_path,

        i.min_confidence,

        i.relationship_count,

        i.path_score

    FROM impact i

    ORDER BY
        i.affected_file_id,
        i.path_score DESC,
        i.min_confidence DESC,
        i.impact_depth ASC
)


SELECT

    b.root_file_id,

    b.affected_file_id,

    f.name AS affected_file_name,

    b.impact_depth,

    array_to_string(
        b.file_path,
        ' -> '
    ) AS path,

    array_to_string(
        b.rel_path,
        ' -> '
    ) AS relationship_path,

    array_to_string(
        b.provenance_path,
        ' -> '
    ) AS provenance_path,

    /*
     * This is now explicitly the effective path reliability
     * used by the ranking algorithm.
     *
     * It is NOT a probability of failure.
     */
    b.min_confidence,

    b.relationship_count,

    s.path_count,

    s.impact_score,

    CASE
        WHEN s.impact_score >= 0.750000
            THEN 'HIGH'

        WHEN s.impact_score >= 0.450000
            THEN 'MEDIUM'

        ELSE 'LOW'
    END AS impact_level

FROM best_path b

JOIN path_stats s
    ON s.affected_file_id = b.affected_file_id

JOIN files f
    ON f.id = b.affected_file_id

ORDER BY
    s.impact_score DESC,
    b.impact_depth ASC;

$function$;


COMMENT ON FUNCTION public.analyze_impact(uuid) IS
'Walks typed downstream relationships recursively. Explicit relationships are treated as full-strength for impact propagation, while inferred relationships use their evidence confidence. provenance_path identifies the provenance of each relationship on the selected path. min_confidence is an effective path reliability used for ranking, not a probability of failure.';

COMMIT;