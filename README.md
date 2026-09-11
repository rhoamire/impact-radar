# Impact Radar — Phase 1

The first milestone is a deterministic change-impact graph for interdependent technical documents.

## What is implemented

- PostgreSQL as the system of record
- Versioned files
- Typed relationships: `depends_on`, `derived_from`, `supersedes`, `references`
- Per-edge confidence and evidence
- Synthetic documents with deliberate dependency chains
- Recursive SQL function for transitive downstream impact
- `pgvector` extension installed, but intentionally unused in Phase 1

## Start it

```bash
docker compose up -d
```

The database is initialized automatically from `db/001_schema.sql`, `db/002_seed.sql`, and `db/003_impact.sql`.

## Run the first demo query

```bash
docker exec -it impact-radar-postgres psql -U impact_radar -d impact_radar
```

Then:

```sql
SELECT affected_file_name,
       impact_depth,
       path,
       min_confidence,
       relationship_count
FROM analyze_impact('10000000-0000-0000-0000-000000000001')
ORDER BY impact_depth, min_confidence DESC;
```

This starts at **API Specification** and walks outward through dependent documents.

## Design choice

Relationships are directed from a dependent document to the document it depends on. Therefore impact analysis for a changed file walks the graph in the **reverse logical direction**: from the changed target to documents whose `source_file_id` points at it. The seed data is intentionally arranged to make this behavior visible.

## Next milestone

Add explicit/inferred provenance and a normalized impact score without involving an LLM. After that, add version diffs and change-event generation.
