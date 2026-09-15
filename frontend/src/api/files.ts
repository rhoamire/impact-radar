import type { Document } from "../types/document"

const API_BASE_URL = "http://localhost:8080"

export async function fetchDocuments(): Promise<Document[]> {
  const response = await fetch(`${API_BASE_URL}/api/files`)

  if (!response.ok) {
    throw new Error(`Failed to fetch documents: ${response.status}`)
  }

  return response.json()
}