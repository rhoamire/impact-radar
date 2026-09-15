import type { ImpactGraphResponse } from "../types/graph"

import { API_BASE_URL } from "./config"

export async function fetchGraph(
  fileId: string
): Promise<ImpactGraphResponse> {
  const response = await fetch(
    `${API_BASE_URL}/api/files/${fileId}/graph`
  )

  if (!response.ok) {
    throw new Error(
      `Failed to fetch graph: ${response.status}`
    )
  }

  return response.json()
}