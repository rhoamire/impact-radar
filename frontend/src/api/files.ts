import type { Document } from "../types/document"

import { API_BASE_URL } from "./config"

export async function fetchDocuments(): Promise<Document[]> {
  const response = await fetch(`${API_BASE_URL}/api/files`)

  if (!response.ok) {
    throw new Error(`Failed to fetch documents: ${response.status}`)
  }

  return response.json()
}