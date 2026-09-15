export interface RelationshipCandidate {
  id: string

  sourceFileId: string
  sourceFileName: string

  targetFileId: string
  targetFileName: string

  sourceVersionId: string
  targetVersionId: string

  similarity: number
  suggestedRelationshipType: string | null
  confidence: number | null

  evidence: string | null
  modelName: string
  status: "CANDIDATE" | "ACCEPTED" | "REJECTED"

  createdAt: string
  reviewedAt: string | null
}