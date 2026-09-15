const API_BASE_URL = "http://localhost:8080"

export async function summarizeChange(
  changeEventId: string
): Promise<string> {
  const response = await fetch(
    `${API_BASE_URL}/api/genai/changes/${changeEventId}/summary`,
    {
      method: "POST",
    }
  )

  if (!response.ok) {
    const message = await response.text()

    throw new Error(
      message ||
        `Failed to generate change summary: ${response.status}`
    )
  }

  return response.text()
}

export async function explainImpact(
  changeEventId: string,
  affectedFileId: string
): Promise<string> {
  const response = await fetch(
    `${API_BASE_URL}/api/genai/changes/${changeEventId}/impact/${affectedFileId}/explanation`,
    {
      method: "POST",
    }
  )

  if (!response.ok) {
    const message = await response.text()

    throw new Error(
      message ||
        `Failed to generate impact explanation: ${response.status}`
    )
  }

  return response.text()
}