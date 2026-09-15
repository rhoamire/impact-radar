import type {
  ChangeDetail,
  VersionSummary,
} from "../types/version"

import { API_BASE_URL } from "./config"

export async function fetchVersions(
  fileId: string
): Promise<VersionSummary[]> {
  const response = await fetch(
    `${API_BASE_URL}/api/files/${fileId}/versions`
  )

  if (!response.ok) {
    throw new Error(
      `Failed to fetch versions: ${response.status}`
    )
  }

  return response.json()
}

export async function fetchChange(
  versionId: string
): Promise<ChangeDetail> {
  const response = await fetch(
    `${API_BASE_URL}/api/versions/${versionId}/change`
  )

  if (!response.ok) {
    throw new Error(
      `Failed to fetch change: ${response.status}`
    )
  }

  return response.json()
}