export interface VersionSummary {
  versionId: string
  versionNumber: number
  current: boolean
  createdAt: string
  changeEventId: string | null
  linesAdded: number
  linesRemoved: number
}

export interface ChangeDetail {
  changeEventId: string
  fileId: string
  fileName: string

  previousVersionId: string | null
  previousVersionNumber: number | null

  newVersionId: string
  newVersionNumber: number

  createdAt: string

  changeSummary: string | null
  diffText: string | null

  linesAdded: number
  linesRemoved: number
}