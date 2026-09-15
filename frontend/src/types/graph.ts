export interface GraphNode {
  id: string
  name: string
  depth: number
  impactScore: number | null
  impactLevel: string
  provenance: string
}

export interface GraphEdge {
  source: string
  target: string
  relationshipType: string
  provenance: string
  evidence: string | null
}

export interface ImpactGraphResponse {
  root: GraphNode
  nodes: GraphNode[]
  edges: GraphEdge[]
}