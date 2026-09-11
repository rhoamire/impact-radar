CREATE EXTENSION IF NOT EXISTS vector;

CREATE TYPE relationship_type AS ENUM (
  'depends_on',
  'derived_from',
  'supersedes',
  'references'
);

CREATE TABLE files (
  id UUID PRIMARY KEY,
  name TEXT NOT NULL,
  description TEXT,
  document_type TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE file_versions (
  id UUID PRIMARY KEY,
  file_id UUID NOT NULL REFERENCES files(id) ON DELETE CASCADE,
  version_number INTEGER NOT NULL CHECK (version_number > 0),
  content TEXT NOT NULL,
  content_hash TEXT NOT NULL,
  embedding VECTOR(1536),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  is_current BOOLEAN NOT NULL DEFAULT FALSE,
  UNIQUE(file_id, version_number)
);

CREATE UNIQUE INDEX one_current_version_per_file
  ON file_versions(file_id)
  WHERE is_current;

CREATE TABLE file_relationships (
  id UUID PRIMARY KEY,
  source_file_id UUID NOT NULL REFERENCES files(id) ON DELETE CASCADE,
  target_file_id UUID NOT NULL REFERENCES files(id) ON DELETE CASCADE,
  relationship_type relationship_type NOT NULL,
  confidence NUMERIC(5,4) NOT NULL CHECK (confidence >= 0 AND confidence <= 1),
  evidence TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CHECK (source_file_id <> target_file_id),
  UNIQUE(source_file_id, target_file_id, relationship_type)
);

CREATE INDEX idx_relationships_source ON file_relationships(source_file_id);
CREATE INDEX idx_relationships_target ON file_relationships(target_file_id);
CREATE INDEX idx_versions_file ON file_versions(file_id);

CREATE TABLE change_events (
  id UUID PRIMARY KEY,
  file_id UUID NOT NULL REFERENCES files(id) ON DELETE CASCADE,
  previous_version_id UUID REFERENCES file_versions(id) ON DELETE SET NULL,
  new_version_id UUID NOT NULL REFERENCES file_versions(id) ON DELETE CASCADE,
  change_summary TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
