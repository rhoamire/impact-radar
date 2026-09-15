import type {
  ChangeDetail,
  VersionSummary,
} from "../types/version"

const API_BASE_URL = "http://localhost:8080"

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