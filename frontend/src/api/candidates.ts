import type { RelationshipCandidate } from "../types/candidate"

const API_BASE_URL = "http://localhost:8080"

export async function fetchCandidates(
  fileId: string
): Promise<RelationshipCandidate[]> {
  const response = await fetch(
    `${API_BASE_URL}/api/files/${fileId}/candidates`
  )

  if (!response.ok) {
    throw new Error(
      `Failed to fetch candidates: ${response.status}`
    )
  }

  return response.json()
}

export async function acceptCandidate(
  candidateId: string
): Promise<void> {
  const response = await fetch(
    `${API_BASE_URL}/api/candidates/${candidateId}/accept`,
    {
      method: "POST",
    }
  )

  if (!response.ok) {
    const message = await response.text()

    throw new Error(
      message ||
        `Failed to accept candidate: ${response.status}`
    )
  }
}

export async function rejectCandidate(
  candidateId: string
): Promise<void> {
  const response = await fetch(
    `${API_BASE_URL}/api/candidates/${candidateId}/reject`,
    {
      method: "POST",
    }
  )

  if (!response.ok) {
    const message = await response.text()

    throw new Error(
      message ||
        `Failed to reject candidate: ${response.status}`
    )
  }
}

export async function undoCandidate(
  candidateId: string
): Promise<void> {
  const response = await fetch(
    `${API_BASE_URL}/api/candidates/${candidateId}/undo`,
    {
      method: "POST",
    }
  )

  if (!response.ok) {
    throw new Error(
      "Relationship candidate cannot be undone."
    )
  }
}