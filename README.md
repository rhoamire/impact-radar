# Impact Radar

Impact Radar is a change-impact analysis system for interdependent technical documents.

When one technical document changes, the difficult question is not only **"what changed?"** but **"what else should be reviewed, and why?"**

Impact Radar models document dependencies as a directed relationship graph, tracks document versions and changes, discovers possible semantic relationships, ranks downstream impact, and uses grounded AI to explain the results.

## What it does

- Tracks files and every version of each file
- Models typed document relationships:
  - `depends_on`
  - `derived_from`
  - `supersedes`
  - `references`
- Computes transitive downstream impact with recursive SQL
- Calculates relationship-aware impact scores and severity levels
- Tracks relationship provenance and path confidence
- Records document change events and diffs
- Stores impact predictions for individual change events
- Uses `pgvector` for semantic relationship discovery
- Separates semantic relationship candidates from authoritative relationships
- Supports human acceptance, rejection, and undo of relationship decisions
- Persists human rejection overrides so rejected relationships are not repeatedly suggested
- Generates grounded change summaries and impact explanations with an LLM
- Exposes core analysis capabilities through an MCP server
- Provides a React-based graph and impact-analysis UI
- Runs locally as a Docker Compose stack with PostgreSQL, Spring Boot, and Nginx

## Architecture

```text
                         React UI
                            |
                            v
                         Nginx
                       /       \
                    /api       /mcp
                     |           |
                     v           v
                Spring Boot backend
                     |
          +----------+-----------+
          |          |           |
          v          v           v
      PostgreSQL   Semantic    GenAI
       + pgvector  discovery   provider
          |
          v
   Version / relationship
      / impact data
```

The application deliberately keeps deterministic impact analysis separate from AI-generated explanations.

```text
Graph + change data
        |
        v
"What is affected?"
        |
        v
Impact analysis
        |
        v
"Why is it affected?"
        |
        v
Grounded AI explanation
```

The LLM does not determine the blast radius. The relationship graph and impact-scoring logic do that work first.

## Core data model

The PostgreSQL database contains the document, version, relationship, semantic-discovery, change-event, and AI-generation data used by the application.

Important concepts include:

- `files` — tracked technical documents
- `file_versions` — immutable document revisions
- `file_relationships` — authoritative directed relationships
- `relationship_candidates` — semantic relationship suggestions awaiting human review
- `relationship_overrides` — persistent human vetoes for rejected directed relationships
- `change_events` — recorded document changes and diffs
- `impact_predictions` — stored downstream impact predictions
- `document_chunks` — content embeddings used for semantic discovery
- `ai_generations` — persisted grounded AI outputs

Relationships are stored from a **dependent document to the document it depends on**.

For impact analysis, traversal therefore proceeds in the reverse logical direction: from a changed document to documents that depend on it.

## Impact scoring

Impact is not based on semantic similarity alone.

Relationship confidence represents how certain the system is that a relationship exists.

Relationship impact weight represents how strongly that relationship contributes to downstream impact.

The current scoring model combines these values and applies a decay factor across recursive hops.

Impact levels are derived from the resulting impact score:

- `HIGH`
- `MEDIUM`
- `LOW`

Provenance is tracked separately from confidence, allowing the system to distinguish explicit relationships from inferred relationships.

## Semantic relationship discovery

Semantic discovery uses embeddings to find documents that may be related but are not yet represented in the authoritative graph.

The workflow is:

```text
document
   |
   v
embedding similarity
   |
   v
relationship candidate
   |
   +------ reject ------> persistent override
   |
   +------ accept ------> authoritative graph
```

Semantic similarity alone does not create an authoritative relationship.

Candidates include supporting evidence and a suggested relationship type, and are subject to human review.

Human decisions are reversible:

```text
CANDIDATE → ACCEPTED
CANDIDATE → REJECTED
ACCEPTED  → CANDIDATE
REJECTED  → CANDIDATE
```

Reversing an accepted inferred relationship removes only the relationship created from that candidate. Reversing a rejection removes its persistent veto so the candidate can be reconsidered.

## Version-aware change tracking

When a new document version is recorded, Impact Radar:

1. Stores the new version
2. Computes a diff against the previous version
3. Records a change event
4. Runs downstream impact analysis
5. Stores impact predictions

This lets the UI connect a concrete change to the documents that should be reviewed.

## Grounded GenAI

The AI layer is intentionally downstream of deterministic analysis.

The backend provides the model with structured change and impact context such as:

- changed document
- affected document
- impact level
- impact score
- impact depth
- path count
- relationship path
- relationship types
- confidence
- relationship evidence
- document diff

The prompts explicitly instruct the model to use only supplied evidence and avoid inventing requirements, dependencies, consequences, or technical behavior.

Generated summaries and explanations are persisted in `ai_generations`.

The current local implementation uses Ollama, while the AI provider is separated from the application services so the provider can be replaced for a production deployment.

## MCP server

Impact Radar exposes its core capabilities through MCP over Streamable HTTP.

Current tools include:

- `analyze_impact`
- `explain_relationship`
- `summarize_change`

The MCP layer exposes existing Impact Radar capabilities rather than introducing a second implementation of the analysis logic.

## Web application

The frontend is a React application providing:

- document selection
- dependency/impact graph visualization
- impact inspection
- document version history
- change diffs
- semantic relationship candidate review
- accept/reject/undo decisions
- grounded AI summaries and impact explanations

The graph reverses the stored dependency direction visually so that downstream impact is easier to follow.

## Local deployment

The repository includes a Docker Compose stack containing:

```text
PostgreSQL + pgvector
        |
        v
Spring Boot backend
        |
        v
Nginx + React frontend
```

The frontend proxies API and MCP requests to the backend, allowing the Dockerized application to run through a single browser origin.

### Prerequisites

- Docker Desktop
- Node.js for frontend development
- Java 17 / Maven wrapper for backend development
- Ollama with the models used by the local configuration

### Start the application

From the repository root:

```powershell
docker compose up -d --build
```

The application is then available at:

```text
http://localhost
```

Backend API:

```text
http://localhost:8080
```

Health:

```text
http://localhost/actuator/health
```

Readiness:

```text
http://localhost/actuator/health/readiness
```

Liveness:

```text
http://localhost/actuator/health/liveness
```

MCP:

```text
http://localhost/mcp
```

## API

The backend currently exposes endpoints for:

### Documents

```text
GET /api/files
GET /api/files/{fileId}/graph
GET /api/files/{fileId}/impact
```

### Versions and changes

```text
GET  /api/files/{fileId}/versions
GET  /api/versions/{versionId}/change
POST /api/files/{fileId}/versions
```

### Semantic discovery

```text
GET /api/semantic-search/{fileId}
POST /api/semantic-candidates/{fileId}
GET /api/files/{fileId}/candidates
GET /api/semantic-evaluation
```

### Candidate review

```text
POST /api/candidates/{candidateId}/accept
POST /api/candidates/{candidateId}/reject
POST /api/candidates/{candidateId}/undo
```

### AI

```text
POST /api/genai/changes/{changeEventId}/summary
POST /api/genai/changes/{changeEventId}/impact/{affectedFileId}/explanation
```

### Embeddings

```text
POST /api/embeddings/{fileId}
```

## Example workflow

A typical Impact Radar workflow looks like:

```text
Change API Specification
        |
        v
Create a new version
        |
        v
Generate diff + change event
        |
        v
Run downstream impact analysis
        |
        v
Rank affected documents
        |
        v
Inspect relationship paths + provenance
        |
        v
Review semantic relationship candidates
        |
        v
Accept / reject / undo human decisions
        |
        v
Generate grounded change and impact explanations
        |
        v
Expose the same analysis through MCP
```

## Database initialization

The SQL files under `db/` contain the database schema, seed data, impact-analysis functions, version tracking, semantic discovery, GenAI support, provenance handling, and human-review logic.

The database is intended to run through the PostgreSQL/pgvector Docker container defined in `docker-compose.yml`.

## Project structure

```text
impact-radar/
├── backend/
│   ├── src/
│   ├── Dockerfile
│   └── pom.xml
├── frontend/
│   ├── src/
│   ├── Dockerfile
│   └── nginx.conf
├── db/
├── docker-compose.yml
└── README.md
```

## Production direction

The application is containerized and uses environment-based configuration for database and AI endpoints.

The local environment uses:

```text
PostgreSQL + pgvector
Ollama
Docker Compose
```

A production deployment can replace the local infrastructure with managed services without changing the core impact-analysis architecture.

The intended AWS direction is:

```text
React frontend
       |
       v
Spring Boot container
       |
   +---+---+
   |       |
   v       v
RDS     managed AI provider
PostgreSQL
+ pgvector
```

AWS deployment is intentionally separate from the local development stack so the project can be developed and demonstrated without requiring a continuously running paid cloud environment.
