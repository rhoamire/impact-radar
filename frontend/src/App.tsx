import { useEffect, useMemo, useState } from "react"

import {
  Activity,
  Check,
  ChevronRight,
  FileText,
  GitBranch,
  Network,
  Search,
  Settings,
  X,
  Sparkles,
  LoaderCircle,
} from "lucide-react"

import ELK from "elkjs/lib/elk.bundled.js"

import {
  applyNodeChanges,
  Background,
  Controls,
  MiniMap,
  ReactFlow,
  Position,
  type Edge,
  type Node,
  type NodeChange,
} from "@xyflow/react"

import type { Document } from "./types/document"
import type {
  GraphEdge,
  GraphNode,
  ImpactGraphResponse,
} from "./types/graph"

import type {
  ChangeDetail,
  VersionSummary,
} from "./types/version"

import type { RelationshipCandidate } from "./types/candidate"
import "@xyflow/react/dist/style.css"
import { fetchDocuments } from "./api/files"
import { fetchGraph } from "./api/graph"

import {
  fetchChange,
  fetchVersions,
} from "./api/versions"

import {
  fetchCandidates,
  acceptCandidate,
  rejectCandidate,
  undoCandidate,
} from "./api/candidates"

import {
  summarizeChange,
  explainImpact,
} from "./api/ai"

const elk = new ELK()

const NODE_WIDTH = 220
const NODE_HEIGHT = 76

async function layoutGraph(
  graphData: ImpactGraphResponse
): Promise<{
  nodes: Node[]
  edges: Edge[]
}> {
  const allNodes = [
    graphData.root,
    ...graphData.nodes,
  ]

  const edges: Edge[] = graphData.edges.map(
    (edge, index) => ({
      id: `${edge.source}-${edge.target}-${index}`,
      source: edge.target,
      target: edge.source,
      label: edge.relationshipType,
      type: "smoothstep",

      style: {
        stroke: "#64748b",
        strokeWidth: 1.5,
      },

      labelStyle: {
        fill: "#94a3b8",
        fontSize: 10,
      },

      labelBgStyle: {
        fill: "#020617",
      },

      labelBgPadding: [4, 2],
      labelBgBorderRadius: 3,
    })
  )

  const elkGraph = {
    id: "impact-radar",

    layoutOptions: {
      "elk.algorithm": "layered",
      "elk.direction": "RIGHT",
      "elk.layered.spacing.nodeNodeBetweenLayers":
        "100",
      "elk.spacing.nodeNode": "80",
      "elk.layered.nodePlacement.strategy":
        "NETWORK_SIMPLEX",
      "elk.edgeRouting": "ORTHOGONAL",
    },

    children: allNodes.map((node) => ({
      id: node.id,
      width: NODE_WIDTH,
      height: NODE_HEIGHT,
    })),

    edges: edges.map((edge) => ({
      id: edge.id,
      sources: [edge.source],
      targets: [edge.target],
    })),
  }

  const layoutedGraph =
    await elk.layout(elkGraph)

  const nodes: Node[] = allNodes.map((node) => {
    const layoutNode =
      layoutedGraph.children?.find(
        (item) => item.id === node.id
      )

    const isRoot =
      node.id === graphData.root.id

    let borderColor = "#475569"
    let backgroundColor = "#0f172a"

    if (isRoot) {
      borderColor = "#94a3b8"
      backgroundColor = "#1e293b"
    } else if (node.impactLevel === "HIGH") {
      borderColor = "#f87171"
    } else if (
      node.impactLevel === "MEDIUM"
    ) {
      borderColor = "#fbbf24"
    } else if (node.impactLevel === "LOW") {
      borderColor = "#64748b"
    }

    return {
      id: node.id,

      position: {
        x: layoutNode?.x ?? 0,
        y: layoutNode?.y ?? 0,
      },

      sourcePosition: Position.Right,
      targetPosition: Position.Left,

      data: {
        label: (
          <div className="space-y-1">
            <div className="font-medium leading-5">
              {node.name}
            </div>

            <div
              className={`text-[10px] uppercase tracking-wide ${isRoot
                  ? "text-slate-400"
                  : node.impactLevel === "HIGH"
                    ? "text-red-400"
                    : node.impactLevel === "MEDIUM"
                      ? "text-amber-400"
                      : "text-slate-500"
                }`}
            >
              {isRoot
                ? "ROOT"
                : node.impactLevel}
            </div>
          </div>
        ),
      },

      style: {
        width: NODE_WIDTH,
        height: NODE_HEIGHT,
        padding: "12px 14px",
        borderRadius: "10px",
        border: `1px solid ${borderColor}`,
        background: backgroundColor,
        color: "#f8fafc",
      },
    }
  })

  return {
    nodes,
    edges,
  }
}

function App() {
  const [documents, setDocuments] =
    useState<Document[]>([])

  const [selectedDocument, setSelectedDocument] =
    useState<Document | null>(null)

  const [graphData, setGraphData] =
    useState<ImpactGraphResponse | null>(null)

  const [graphNodes, setGraphNodes] =
    useState<Node[]>([])

  const [graphEdges, setGraphEdges] =
    useState<Edge[]>([])

  const [selectedNode, setSelectedNode] =
    useState<GraphNode | null>(null)

  const [selectedEdge, setSelectedEdge] =
    useState<GraphEdge | null>(null)

  const [versions, setVersions] =
    useState<VersionSummary[]>([])

  const [selectedVersion, setSelectedVersion] =
    useState<VersionSummary | null>(null)

  const [changeDetail, setChangeDetail] =
    useState<ChangeDetail | null>(null)

  const [candidates, setCandidates] =
    useState<RelationshipCandidate[]>([])

  const [selectedCandidate, setSelectedCandidate] =
    useState<RelationshipCandidate | null>(null)

  const [activeTab, setActiveTab] =
    useState<
      "impact" | "change" | "relationships"
    >("impact")

  const [loadingDocuments, setLoadingDocuments] =
    useState(true)

  const [loadingGraph, setLoadingGraph] =
    useState(false)

  const [loadingVersions, setLoadingVersions] =
    useState(false)

  const [loadingChange, setLoadingChange] =
    useState(false)

  const [loadingCandidates, setLoadingCandidates] =
    useState(false)

  const [reviewingCandidate, setReviewingCandidate] =
    useState(false)

  const [generatingSummary, setGeneratingSummary] =
    useState(false)

  const [generatingExplanation, setGeneratingExplanation] =
    useState(false)

  const [aiSummary, setAiSummary] =
    useState<string | null>(null)

  const [aiExplanation, setAiExplanation] =
    useState<string | null>(null)

  const [error, setError] =
    useState<string | null>(null)

  const [searchQuery, setSearchQuery] =
    useState("")

  /*
   * Load documents.
   */
  useEffect(() => {
    fetchDocuments()
      .then((data) => {
        setDocuments(data)
        setSelectedDocument(data[0] ?? null)
      })
      .catch((err) => {
        setError(
          err instanceof Error
            ? err.message
            : "Failed to load documents"
        )
      })
      .finally(() => {
        setLoadingDocuments(false)
      })
  }, [])

  /*
   * Load graph, versions and candidates
   * whenever the selected document changes.
   */
  useEffect(() => {
    if (!selectedDocument) {
      return
    }

    setError(null)

    setSelectedNode(null)
    setSelectedEdge(null)
    setSelectedCandidate(null)

    setSelectedVersion(null)
    setChangeDetail(null)

    setAiSummary(null)
    setAiExplanation(null)

    setLoadingGraph(true)
    setLoadingVersions(true)
    setLoadingCandidates(true)

    fetchGraph(selectedDocument.id)
      .then(async (data) => {
        setGraphData(data)

        const layouted =
          await layoutGraph(data)

        setGraphNodes(layouted.nodes)
        setGraphEdges(layouted.edges)
      })
      .catch((err) => {
        setGraphData(null)
        setGraphNodes([])
        setGraphEdges([])

        setError(
          err instanceof Error
            ? err.message
            : "Failed to load impact graph"
        )
      })
      .finally(() => {
        setLoadingGraph(false)
      })

    fetchVersions(selectedDocument.id)
      .then((data) => {
        setVersions(data)

        const currentVersion =
          data.find((version) =>
            version.current
          )

        setSelectedVersion(
          currentVersion ?? data[0] ?? null
        )
      })
      .catch((err) => {
        setVersions([])

        setError(
          err instanceof Error
            ? err.message
            : "Failed to load versions"
        )
      })
      .finally(() => {
        setLoadingVersions(false)
      })

    fetchCandidates(selectedDocument.id)
      .then((data) => {
        setCandidates(data)

        const firstCandidate =
          data.find(
            (candidate) =>
              candidate.status === "CANDIDATE"
          ) ?? data[0] ?? null

        setSelectedCandidate(firstCandidate)
      })
      .catch((err) => {
        setCandidates([])

        setError(
          err instanceof Error
            ? err.message
            : "Failed to load relationship candidates"
        )
      })
      .finally(() => {
        setLoadingCandidates(false)
      })
  }, [selectedDocument])

  /*
   * Load change detail for the selected version.
   */
  useEffect(() => {
    if (
      !selectedVersion ||
      !selectedVersion.changeEventId
    ) {
      setChangeDetail(null)
      setAiSummary(null)
      return
    }

    setLoadingChange(true)
    setError(null)

    fetchChange(selectedVersion.versionId)
      .then((data) => {
        setChangeDetail(data)
      })
      .catch((err) => {
        setChangeDetail(null)

        setError(
          err instanceof Error
            ? err.message
            : "Failed to load change details"
        )
      })
      .finally(() => {
        setLoadingChange(false)
      })
  }, [selectedVersion])

  const filteredDocuments = useMemo(() => {
    const query = searchQuery
      .trim()
      .toLowerCase()

    if (!query) {
      return documents
    }

    return documents.filter((document) =>
      document.name
        .toLowerCase()
        .includes(query)
    )
  }, [documents, searchQuery])

  const pendingCandidates = useMemo(
    () =>
      candidates.filter(
        (candidate) =>
          candidate.status === "CANDIDATE"
      ),
    [candidates]
  )

  function handleNodesChange(changes: NodeChange[]) {
    setGraphNodes((nodes) =>
      applyNodeChanges(changes, nodes)
    )
  }

  function handleNodeClick(nodeId: string) {
    if (!graphData) {
      return
    }

    const allNodes = [
      graphData.root,
      ...graphData.nodes,
    ]

    const node = allNodes.find(
      (item) => item.id === nodeId
    )

    setSelectedNode(node ?? null)
    setSelectedEdge(null)
    setAiExplanation(null)
  }

  function handleEdgeClick(edgeId: string) {
    if (!graphData) {
      return
    }

    const index =
      graphData.edges.findIndex(
        (edge, index) =>
          `${edge.source}-${edge.target}-${index}` ===
          edgeId
      )

    if (index === -1) {
      return
    }

    setSelectedEdge(
      graphData.edges[index]
    )

    setSelectedNode(null)
    setAiExplanation(null)
  }

  function getDocumentName(fileId: string) {
    if (graphData?.root.id === fileId) {
      return graphData.root.name
    }

    return (
      graphData?.nodes.find(
        (node) => node.id === fileId
      )?.name ?? fileId
    )
  }

  async function refreshGraph() {
    if (!selectedDocument) {
      return
    }

    try {
      const data = await fetchGraph(
        selectedDocument.id
      )

      setGraphData(data)

      const layouted =
        await layoutGraph(data)

      setGraphNodes(layouted.nodes)
      setGraphEdges(layouted.edges)
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Failed to refresh graph"
      )
    }
  }

  async function handleAcceptCandidate(
    candidateId: string
  ) {
    setReviewingCandidate(true)
    setError(null)

    try {
      await acceptCandidate(candidateId)

      if (selectedDocument) {
        const updated =
          await fetchCandidates(
            selectedDocument.id
          )

        setCandidates(updated)

        setSelectedCandidate(
          updated.find(
            (candidate) =>
              candidate.id === candidateId
          ) ?? null
        )
      }

      await refreshGraph()
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Failed to accept candidate"
      )
    } finally {
      setReviewingCandidate(false)
    }
  }

  async function handleRejectCandidate(
    candidateId: string
  ) {
    setReviewingCandidate(true)
    setError(null)

    try {
      await rejectCandidate(candidateId)

      if (selectedDocument) {
        const updated =
          await fetchCandidates(
            selectedDocument.id
          )

        setCandidates(updated)

        setSelectedCandidate(
          updated.find(
            (candidate) =>
              candidate.id === candidateId
          ) ?? null
        )
      }
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Failed to reject candidate"
      )
    } finally {
      setReviewingCandidate(false)
    }
  }

  async function handleUndoCandidate(
    candidateId: string
  ) {
    setReviewingCandidate(true)
    setError(null)

    try {
      await undoCandidate(candidateId)

      if (selectedDocument) {
        const updated =
          await fetchCandidates(selectedDocument.id)

        setCandidates(updated)

        setSelectedCandidate(
          updated.find(
            (candidate) =>
              candidate.id === candidateId
          ) ?? null
        )
      }

      await refreshGraph()
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Failed to undo candidate"
      )
    } finally {
      setReviewingCandidate(false)
    }
  }

  async function handleGenerateSummary() {
    if (!selectedVersion?.changeEventId) {
      return
    }

    setGeneratingSummary(true)
    setAiSummary(null)
    setError(null)

    try {
      const result = await summarizeChange(
        selectedVersion.changeEventId
      )

      setAiSummary(result)
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Failed to generate summary"
      )
    } finally {
      setGeneratingSummary(false)
    }
  }

  async function handleGenerateExplanation() {
    if (
      !selectedVersion?.changeEventId ||
      !selectedNode ||
      selectedNode.id === graphData?.root.id
    ) {
      return
    }

    setGeneratingExplanation(true)
    setAiExplanation(null)
    setError(null)

    try {
      const result = await explainImpact(
        selectedVersion.changeEventId,
        selectedNode.id
      )

      setAiExplanation(result)
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Failed to generate impact explanation"
      )
    } finally {
      setGeneratingExplanation(false)
    }
  }

  return (
    <div
      className="min-h-screen
                 bg-slate-950
                 text-slate-100"
    >
      {/* Header */}
      <header
        className="h-16 border-b
                   border-slate-800
                   flex items-center
                   px-6"
      >
        <div className="flex items-center gap-3">

          <div
            className="h-9 w-9 rounded-lg
                       bg-slate-800
                       flex items-center
                       justify-center"
          >
            <Activity size={20} />
          </div>

          <div>
            <h1
              className="font-semibold
                         tracking-tight"
            >
              Impact Radar
            </h1>

            <p
              className="text-xs
                         text-slate-500"
            >
              Change-impact analysis
            </p>
          </div>

        </div>

        <div
          className="ml-auto
                     flex items-center
                     gap-4
                     text-slate-400"
        >
          <Network size={18} />
          <Settings size={18} />
        </div>
      </header>

      <main
        className="flex
                   h-[calc(100vh-4rem)]"
      >
        {/* Documents sidebar */}
        <aside
          className="w-72
                     border-r
                     border-slate-800
                     flex flex-col"
        >
          <div
            className="p-4
                       border-b
                       border-slate-800"
          >
            <div
              className="flex
                         items-center
                         gap-2
                         text-sm
                         font-medium"
            >
              <FileText size={16} />
              Documents
            </div>

            <div
              className="mt-3
                         relative"
            >
              <Search
                size={15}
                className="absolute
                           left-3
                           top-2.5
                           text-slate-500"
              />

              <input
                value={searchQuery}
                onChange={(event) =>
                  setSearchQuery(
                    event.target.value
                  )
                }
                placeholder="Search documents..."
                className="w-full
                           rounded-md
                           border
                           border-slate-800
                           bg-slate-900
                           py-2
                           pl-9
                           pr-3
                           text-sm
                           outline-none
                           placeholder:text-slate-600
                           focus:border-slate-600"
              />
            </div>
          </div>

          <div
            className="flex-1
                       overflow-y-auto
                       p-2"
          >
            {loadingDocuments && (
              <div
                className="px-3
                           py-2
                           text-sm
                           text-slate-500"
              >
                Loading documents...
              </div>
            )}

            {!loadingDocuments &&
              filteredDocuments.length === 0 && (
                <div
                  className="px-3 py-2
                             text-sm
                             text-slate-500"
                >
                  No documents found.
                </div>
              )}

            {!loadingDocuments &&
              filteredDocuments.map(
                (document) => {
                  const selected =
                    selectedDocument?.id ===
                    document.id

                  return (
                    <button
                      key={document.id}
                      onClick={() => {
                        setSelectedDocument(
                          document
                        )
                        setActiveTab("impact")
                      }}
                      className={`w-full
                        flex items-center
                        gap-3 rounded-md
                        px-3 py-2.5
                        text-left text-sm mb-1
                        ${selected
                          ? "bg-slate-800 text-white"
                          : "text-slate-400 hover:bg-slate-900 hover:text-slate-200"
                        }`}
                    >
                      <FileText
                        size={16}
                        className="shrink-0"
                      />

                      <span className="truncate">
                        {document.name}
                      </span>

                      {selected && (
                        <ChevronRight
                          size={15}
                          className="ml-auto
                                     shrink-0"
                        />
                      )}
                    </button>
                  )
                }
              )}
          </div>

          <div
            className="border-t
                       border-slate-800
                       p-4"
          >
            <div
              className="text-xs
                         text-slate-500"
            >
              {documents.length} documents
            </div>

            <div
              className="text-xs
                         text-slate-600
                         mt-1"
            >
              {pendingCandidates.length} relationship
              candidates
            </div>
          </div>
        </aside>

        {/* Center */}
        <section
          className="flex-1
                     flex flex-col
                     min-w-0"
        >
          <div
            className="h-14
                       border-b
                       border-slate-800
                       flex items-center
                       px-5"
          >
            <div>
              <div
                className="text-sm
                           font-medium"
              >
                {selectedDocument?.name ??
                  "No document selected"}
              </div>

              <div
                className="text-xs
                           text-slate-500"
              >
                {activeTab === "impact"
                  ? "Change impact"
                  : activeTab === "change"
                    ? "Version history"
                    : "Semantic relationship discovery"}
              </div>
            </div>

            <div
              className="ml-auto
                         flex items-center
                         gap-2"
            >
              <span
                className="text-xs
                           text-slate-500"
              >
                {activeTab === "impact"
                  ? "Graph view"
                  : activeTab === "change"
                    ? "Change view"
                    : "Relationship review"}
              </span>

              {activeTab ===
                "impact" && (
                  <GitBranch
                    size={16}
                    className="text-slate-500"
                  />
                )}
            </div>
          </div>

          {/* Impact graph */}
          {activeTab === "impact" && (
            <div className="flex-1">
              {loadingGraph ? (
                <div
                  className="h-full
                             flex items-center
                             justify-center"
                >
                  <span
                    className="text-sm
                               text-slate-500"
                  >
                    Laying out impact graph...
                  </span>
                </div>
              ) : graphData ? (
                <ReactFlow
                  nodes={graphNodes}
                  edges={graphEdges}
                  onNodesChange={handleNodesChange}
                  fitView
                  colorMode="dark"
                  nodesConnectable={false}
                  onNodeClick={(_, node) =>
                    handleNodeClick(node.id)
                  }
                  onEdgeClick={(_, edge) =>
                    handleEdgeClick(edge.id)
                  }
                >
                  <Background gap={18} size={1} />

                  <Controls showInteractive={false} />

                  <MiniMap
                    nodeColor={(node) => {
                      const graphNode =
                        graphData.root.id === node.id
                          ? graphData.root
                          : graphData.nodes.find((item) => item.id === node.id);

                      if (graphNode?.impactLevel === "HIGH") {
                        return "#f87171";
                      }

                      if (graphNode?.impactLevel === "MEDIUM") {
                        return "#fbbf24";
                      }

                      return "#64748b";
                    }}
                    nodeStrokeWidth={2}
                    maskColor="rgba(15, 23, 42, 0.65)"
                  />
                </ReactFlow>
              ) : (
                <div
                  className="h-full
                             flex items-center
                             justify-center"
                >
                  <div className="text-center">
                    <Network
                      size={32}
                      className="mx-auto
                                 mb-3
                                 text-slate-600"
                    />

                    <p
                      className="text-sm
                                 text-slate-500"
                    >
                      No graph available
                    </p>
                  </div>
                </div>
              )}
            </div>
          )}

          {/* Change view */}
          {activeTab === "change" && (
            <div
              className="flex-1
                         overflow-y-auto
                         p-6"
            >
              <div
                className="max-w-4xl
                           mx-auto"
              >
                <div className="mb-5">
                  <div
                    className="text-xs
                               uppercase
                               tracking-wide
                               text-slate-500"
                  >
                    Version history
                  </div>

                  <h2
                    className="mt-1
                               text-lg
                               font-medium"
                  >
                    {selectedDocument?.name}
                  </h2>
                </div>

                <div
                  className="grid
                             grid-cols-[220px_1fr]
                             gap-6"
                >
                  {/* Version list */}
                  <div>
                    <div
                      className="text-xs
                                 uppercase
                                 tracking-wide
                                 text-slate-500
                                 mb-2"
                    >
                      Versions
                    </div>

                    {loadingVersions ? (
                      <div
                        className="text-sm
                                   text-slate-500"
                      >
                        Loading versions...
                      </div>
                    ) : (
                      <div
                        className="space-y-1"
                      >
                        {versions.map(
                          (version) => {
                            const selected =
                              selectedVersion
                                ?.versionId ===
                              version.versionId

                            return (
                              <button
                                key={
                                  version.versionId
                                }
                                onClick={() =>
                                  setSelectedVersion(
                                    version
                                  )
                                }
                                className={`w-full
                                  rounded-md
                                  border px-3
                                  py-2.5
                                  text-left
                                  ${selected
                                    ? "border-slate-600 bg-slate-800"
                                    : "border-transparent hover:bg-slate-900"
                                  }`}
                              >
                                <div
                                  className="flex
                                             items-center
                                             justify-between"
                                >
                                  <span
                                    className="text-sm
                                               font-medium"
                                  >
                                    v
                                    {
                                      version.versionNumber
                                    }
                                  </span>

                                  {version.current && (
                                    <span
                                      className="text-[10px]
                                                 uppercase
                                                 tracking-wide
                                                 text-emerald-400"
                                    >
                                      Current
                                    </span>
                                  )}
                                </div>

                                <div
                                  className="mt-1
                                             text-xs
                                             text-slate-500"
                                >
                                  +
                                  {
                                    version.linesAdded
                                  }{" "}
                                  / -
                                  {
                                    version.linesRemoved
                                  }
                                </div>
                              </button>
                            )
                          }
                        )}
                      </div>
                    )}
                  </div>

                  {/* Change detail */}
                  <div>
                    {!selectedVersion ? (
                      <div
                        className="rounded-lg
                                   border
                                   border-slate-800
                                   bg-slate-900
                                   p-5
                                   text-sm
                                   text-slate-500"
                      >
                        Select a version.
                      </div>
                    ) : selectedVersion.changeEventId ===
                      null ? (
                      <div
                        className="rounded-lg
                                   border
                                   border-slate-800
                                   bg-slate-900
                                   p-5"
                      >
                        <div
                          className="text-sm
                                     font-medium"
                        >
                          Version{" "}
                          {
                            selectedVersion.versionNumber
                          }
                        </div>

                        <div
                          className="mt-2
                                     text-sm
                                     text-slate-500"
                        >
                          No change event is
                          associated with this
                          version.
                        </div>
                      </div>
                    ) : loadingChange ? (
                      <div
                        className="rounded-lg
                                   border
                                   border-slate-800
                                   bg-slate-900
                                   p-5
                                   text-sm
                                   text-slate-500"
                      >
                        Loading change details...
                      </div>
                    ) : changeDetail ? (
                      <div className="space-y-5">

                        <div>
                          <div
                            className="text-xs
                                       uppercase
                                       tracking-wide
                                       text-slate-500"
                          >
                            Change event
                          </div>

                          <div
                            className="mt-1
                                       flex items-center
                                       gap-3"
                          >
                            <h2
                              className="text-lg
                                         font-medium"
                            >
                              v
                              {
                                changeDetail.newVersionNumber
                              }
                            </h2>

                            <span
                              className="text-xs
                                         text-slate-500"
                            >
                              from v
                              {
                                changeDetail.previousVersionNumber ??
                                "—"
                              }
                            </span>
                          </div>
                        </div>

                        <div
                          className="grid
                                     grid-cols-2
                                     gap-3"
                        >
                          <div
                            className="rounded-lg
                                       border
                                       border-slate-800
                                       bg-slate-900
                                       p-4"
                          >
                            <div
                              className="text-xs
                                         text-slate-500"
                            >
                              Lines added
                            </div>

                            <div
                              className="mt-1
                                         text-xl
                                         font-semibold"
                            >
                              +
                              {
                                changeDetail.linesAdded
                              }
                            </div>
                          </div>

                          <div
                            className="rounded-lg
                                       border
                                       border-slate-800
                                       bg-slate-900
                                       p-4"
                          >
                            <div
                              className="text-xs
                                         text-slate-500"
                            >
                              Lines removed
                            </div>

                            <div
                              className="mt-1
                                         text-xl
                                         font-semibold"
                            >
                              -
                              {
                                changeDetail.linesRemoved
                              }
                            </div>
                          </div>
                        </div>

                        <div>
                          <div
                            className="text-xs
                                       uppercase
                                       tracking-wide
                                       text-slate-500"
                          >
                            Change summary
                          </div>

                          <div
                            className="mt-2
                                       rounded-lg
                                       border
                                       border-slate-800
                                       bg-slate-900
                                       p-4
                                       text-sm
                                       leading-6
                                       text-slate-300"
                          >
                            {changeDetail.changeSummary ??
                              "No recorded summary for this change."}
                          </div>

                        </div>

                        {/* AI summary */}
                        <div>
                          <div
                            className="flex
                                       items-center
                                       justify-between"
                          >
                            <div>
                              <div
                                className="text-xs
                                           uppercase
                                           tracking-wide
                                           text-slate-500"
                              >
                                AI summary
                              </div>

                              <div
                                className="mt-1
                                           text-[11px]
                                           text-slate-600"
                              >
                                Grounded in the recorded
                                change event
                              </div>
                            </div>

                            <button
                              disabled={
                                generatingSummary
                              }
                              onClick={
                                handleGenerateSummary
                              }
                              className="flex
                                         items-center
                                         gap-2
                                         rounded-md
                                         border
                                         border-slate-700
                                         bg-slate-900
                                         px-3 py-2
                                         text-xs
                                         font-medium
                                         text-slate-300
                                         hover:bg-slate-800
                                         disabled:opacity-50"
                            >
                              {generatingSummary ? (
                                <>
                                  <LoaderCircle
                                    size={14}
                                    className="animate-spin"
                                  />
                                  Generating...
                                </>
                              ) : (
                                <>
                                  <Sparkles
                                    size={14}
                                  />
                                  Summarize with AI
                                </>
                              )}
                            </button>
                          </div>

                          {aiSummary && (
                            <div
                              className="mt-2
                                         rounded-lg
                                         border
                                         border-slate-800
                                         bg-slate-900
                                         p-4
                                         text-sm
                                         leading-6
                                         text-slate-300
                                         whitespace-pre-wrap"
                            >
                              {aiSummary}
                            </div>
                          )}
                        </div>

                        <div>
                          <div
                            className="text-xs
                                       uppercase
                                       tracking-wide
                                       text-slate-500"
                          >
                            Diff
                          </div>

                          <pre
                            className="mt-2
                                       max-h-[420px]
                                       overflow-auto
                                       rounded-lg
                                       border
                                       border-slate-800
                                       bg-slate-950
                                       p-4
                                       text-xs
                                       leading-6
                                       text-slate-300
                                       font-mono
                                       whitespace-pre-wrap"
                          >
                            {changeDetail.diffText ??
                              "No diff recorded."}
                          </pre>
                        </div>

                      </div>
                    ) : (
                      <div
                        className="rounded-lg
                                   border
                                   border-slate-800
                                   bg-slate-900
                                   p-5
                                   text-sm
                                   text-slate-500"
                      >
                        No change details available.
                      </div>
                    )}
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* Relationships */}
          {activeTab === "relationships" && (
            <div
              className="flex-1
                         overflow-y-auto
                         p-6"
            >
              <div
                className="max-w-5xl
                           mx-auto"
              >
                <div className="mb-5">
                  <div
                    className="text-xs
                               uppercase
                               tracking-wide
                               text-slate-500"
                  >
                    Semantic relationship discovery
                  </div>

                  <h2
                    className="mt-1
                               text-lg
                               font-medium"
                  >
                    Candidate relationships
                  </h2>

                  <p
                    className="mt-1
                               text-sm
                               leading-6
                               text-slate-500"
                  >
                    Semantic similarity suggests
                    possible relationships. Only
                    accepted candidates enter the
                    authoritative relationship graph.
                  </p>
                </div>

                {loadingCandidates ? (
                  <div
                    className="rounded-lg
                               border
                               border-slate-800
                               bg-slate-900
                               p-6
                               text-sm
                               text-slate-500"
                  >
                    Loading relationship candidates...
                  </div>
                ) : candidates.length === 0 ? (
                  <div
                    className="rounded-lg
                               border
                               border-slate-800
                               bg-slate-900
                               p-6
                               text-sm
                               text-slate-500"
                  >
                    No relationship candidates found.
                  </div>
                ) : (
                  <div
                    className="grid
                               grid-cols-[320px_1fr]
                               gap-6"
                  >

                    {/* Candidate list */}
                    <div>
                      <div
                        className="mb-2
                                   text-xs
                                   uppercase
                                   tracking-wide
                                   text-slate-500"
                      >
                        Candidates
                      </div>

                      <div
                        className="space-y-2"
                      >
                        {candidates.map(
                          (candidate) => {

                            const selected =
                              selectedCandidate
                                ?.id ===
                              candidate.id

                            const otherFileName =
                              candidate.sourceFileId ===
                                selectedDocument?.id
                                ? candidate.targetFileName
                                : candidate.sourceFileName

                            return (
                              <button
                                key={
                                  candidate.id
                                }
                                onClick={() =>
                                  setSelectedCandidate(
                                    candidate
                                  )
                                }
                                className={`w-full
                                  rounded-lg
                                  border
                                  p-3
                                  text-left
                                  ${selected
                                    ? "border-slate-600 bg-slate-800"
                                    : "border-slate-800 bg-slate-900 hover:bg-slate-800"
                                  }`}
                              >

                                <div
                                  className="flex
                                             items-start
                                             justify-between
                                             gap-3"
                                >

                                  <div
                                    className="min-w-0"
                                  >
                                    <div
                                      className="text-sm
                                                 font-medium
                                                 truncate"
                                    >
                                      {
                                        otherFileName
                                      }
                                    </div>

                                    <div
                                      className="mt-1
                                                 text-xs
                                                 text-slate-500"
                                    >
                                      {
                                        candidate
                                          .suggestedRelationshipType ??
                                        "Relationship type unresolved"
                                      }
                                    </div>
                                  </div>

                                  <span
                                    className={`shrink-0
                                      rounded-md
                                      px-2 py-1
                                      text-[10px]
                                      uppercase
                                      tracking-wide
                                      ${candidate.status ===
                                        "CANDIDATE"
                                        ? "bg-slate-800 text-slate-300"
                                        : candidate.status ===
                                          "ACCEPTED"
                                          ? "bg-emerald-950 text-emerald-300"
                                          : "bg-red-950 text-red-300"
                                      }`}
                                  >
                                    {
                                      candidate.status
                                    }
                                  </span>
                                </div>

                                <div
                                  className="mt-3
                                             text-xs
                                             text-slate-500"
                                >
                                  Similarity{" "}
                                  {
                                    candidate.similarity.toFixed(
                                      3
                                    )
                                  }
                                </div>

                              </button>
                            )
                          }
                        )}
                      </div>
                    </div>

                    {/* Candidate detail */}
                    <div>
                      {selectedCandidate ? (
                        <div
                          className="rounded-lg
                                     border
                                     border-slate-800
                                     bg-slate-900
                                     p-5"
                        >

                          <div
                            className="flex
                                       items-start
                                       justify-between
                                       gap-4"
                          >
                            <div>
                              <div
                                className="text-xs
                                           uppercase
                                           tracking-wide
                                           text-slate-500"
                              >
                                Candidate
                              </div>

                              <h2
                                className="mt-1
                                           text-lg
                                           font-medium"
                              >
                                {
                                  selectedCandidate
                                    .sourceFileName
                                }
                              </h2>

                              <div
                                className="my-2
                                           text-xs
                                           text-slate-600"
                              >
                                ↓
                              </div>

                              <h3
                                className="text-sm
                                           font-medium
                                           text-slate-300"
                              >
                                {
                                  selectedCandidate
                                    .targetFileName
                                }
                              </h3>
                            </div>

                            <span
                              className={`rounded-md
                                border px-2.5
                                py-1 text-xs
                                font-medium
                                ${selectedCandidate.status ===
                                  "CANDIDATE"
                                  ? "border-slate-700 bg-slate-800 text-slate-300"
                                  : selectedCandidate.status ===
                                    "ACCEPTED"
                                    ? "border-emerald-800 bg-emerald-950 text-emerald-300"
                                    : "border-red-900 bg-red-950 text-red-300"
                                }`}
                            >
                              {
                                selectedCandidate.status
                              }
                            </span>
                          </div>

                          <div
                            className="mt-6
                                       grid
                                       grid-cols-3
                                       gap-3"
                          >
                            <div
                              className="rounded-lg
                                         border
                                         border-slate-800
                                         bg-slate-950
                                         p-3"
                            >
                              <div
                                className="text-xs
                                           text-slate-500"
                              >
                                Similarity
                              </div>

                              <div
                                className="mt-1
                                           font-semibold"
                              >
                                {
                                  selectedCandidate
                                    .similarity
                                    .toFixed(3)
                                }
                              </div>
                            </div>

                            <div
                              className="rounded-lg
                                         border
                                         border-slate-800
                                         bg-slate-950
                                         p-3"
                            >
                              <div
                                className="text-xs
                                           text-slate-500"
                              >
                                Confidence
                              </div>

                              <div
                                className="mt-1
                                           font-semibold"
                              >
                                {
                                  selectedCandidate
                                    .confidence ===
                                    null
                                    ? "—"
                                    : selectedCandidate
                                      .confidence
                                      .toFixed(3)
                                }
                              </div>
                            </div>

                            <div
                              className="rounded-lg
                                         border
                                         border-slate-800
                                         bg-slate-950
                                         p-3"
                            >
                              <div
                                className="text-xs
                                           text-slate-500"
                              >
                                Suggested type
                              </div>

                              <div
                                className="mt-1
                                           font-semibold
                                           text-xs"
                              >
                                {
                                  selectedCandidate
                                    .suggestedRelationshipType ??
                                  "UNRESOLVED"
                                }
                              </div>
                            </div>
                          </div>

                          <div className="mt-6">
                            <div
                              className="text-xs
                                         uppercase
                                         tracking-wide
                                         text-slate-500"
                            >
                              Evidence
                            </div>

                            <div
                              className="mt-2
                                         rounded-lg
                                         border
                                         border-slate-800
                                         bg-slate-950
                                         p-4
                                         text-sm
                                         leading-6
                                         text-slate-400"
                            >
                              {
                                selectedCandidate.evidence ??
                                "No evidence recorded."
                              }
                            </div>
                          </div>

                          <div className="mt-4">
                            <div
                              className="text-xs
                                         uppercase
                                         tracking-wide
                                         text-slate-500"
                            >
                              Model
                            </div>

                            <div
                              className="mt-2
                                         text-sm
                                         text-slate-400"
                            >
                              {
                                selectedCandidate.modelName
                              }
                            </div>
                          </div>

                          {selectedCandidate
                            .status ===
                            "CANDIDATE" && (
                              <div
                                className="mt-6
                                         flex gap-3"
                              >
                                <button
                                  disabled={
                                    reviewingCandidate
                                  }
                                  onClick={() =>
                                    handleRejectCandidate(
                                      selectedCandidate.id
                                    )
                                  }
                                  className="flex-1
                                           flex
                                           items-center
                                           justify-center
                                           gap-2
                                           rounded-md
                                           border
                                           border-red-900
                                           bg-red-950
                                           px-4 py-2
                                           text-sm
                                           font-medium
                                           text-red-300
                                           hover:bg-red-900
                                           disabled:opacity-50"
                                >
                                  <X size={15} />
                                  Reject
                                </button>

                                <button
                                  disabled={
                                    reviewingCandidate
                                  }
                                  onClick={() =>
                                    handleAcceptCandidate(
                                      selectedCandidate.id
                                    )
                                  }
                                  className="flex-1
                                           flex
                                           items-center
                                           justify-center
                                           gap-2
                                           rounded-md
                                           bg-slate-100
                                           px-4 py-2
                                           text-sm
                                           font-medium
                                           text-slate-900
                                           hover:bg-white
                                           disabled:opacity-50"
                                >
                                  <Check size={15} />
                                  Accept
                                </button>
                              </div>
                            )}

{selectedCandidate.status === "ACCEPTED" && (
  <div className="mt-6">
    <div
      className="rounded-md
                 border
                 border-emerald-900
                 bg-emerald-950
                 p-3
                 text-sm
                 text-emerald-300"
    >
      This candidate is now part of the
      authoritative relationship graph.
    </div>

    <button
      disabled={reviewingCandidate}
      onClick={() =>
        handleUndoCandidate(
          selectedCandidate.id
        )
      }
      className="mt-3
                 w-full
                 rounded-md
                 border
                 border-slate-700
                 bg-slate-900
                 px-4 py-2
                 text-sm
                 font-medium
                 text-slate-300
                 hover:bg-slate-800
                 disabled:opacity-50"
    >
      Undo decision
    </button>
  </div>
)}

{selectedCandidate.status === "REJECTED" && (
  <div className="mt-6">
    <div
      className="rounded-md
                 border
                 border-red-900
                 bg-red-950
                 p-3
                 text-sm
                 text-red-300"
    >
      This relationship was rejected by a human
      and will not be suggested again.
    </div>

    <button
      disabled={reviewingCandidate}
      onClick={() =>
        handleUndoCandidate(
          selectedCandidate.id
        )
      }
      className="mt-3
                 w-full
                 rounded-md
                 border
                 border-slate-700
                 bg-slate-900
                 px-4 py-2
                 text-sm
                 font-medium
                 text-slate-300
                 hover:bg-slate-800
                 disabled:opacity-50"
    >
      Undo decision
    </button>
  </div>
)}

                        </div>
                      ) : (
                        <div
                          className="rounded-lg
                                     border
                                     border-slate-800
                                     bg-slate-900
                                     p-6
                                     text-sm
                                     text-slate-500"
                        >
                          Select a candidate to inspect
                          its semantic evidence.
                        </div>
                      )}
                    </div>
                  </div>
                )}
              </div>
            </div>
          )}
        </section>

        {/* Right inspector */}
        <aside
          className="w-80
                     border-l
                     border-slate-800"
        >
          <div
            className="h-14
                       border-b
                       border-slate-800
                       flex items-end
                       px-4 gap-5"
          >
            <button
              onClick={() =>
                setActiveTab("impact")
              }
              className={`h-full
                         border-b-2
                         text-sm
                         font-medium
                         ${activeTab === "impact"
                  ? "border-slate-300 text-white"
                  : "border-transparent text-slate-500"
                }`}
            >
              Impact
            </button>

            <button
              onClick={() =>
                setActiveTab("change")
              }
              className={`h-full
                         border-b-2
                         text-sm
                         font-medium
                         ${activeTab === "change"
                  ? "border-slate-300 text-white"
                  : "border-transparent text-slate-500"
                }`}
            >
              Change
            </button>

            <button
              onClick={() =>
                setActiveTab("relationships")
              }
              className={`h-full
                         border-b-2
                         text-sm
                         font-medium
                         ${activeTab ===
                  "relationships"
                  ? "border-slate-300 text-white"
                  : "border-transparent text-slate-500"
                }`}
            >
              Relations
            </button>
          </div>

          <div className="p-5">

            {/* Change tab */}
            {activeTab === "change" ? (
              <div
                className="text-sm
                           leading-6
                           text-slate-500"
              >
                Select a version from the Change
                view to inspect its history and
                generate an AI summary.
              </div>

              /* Relationships tab */
            ) : activeTab ===
              "relationships" ? (
              <div
                className="text-sm
                           leading-6
                           text-slate-500"
              >
                Review semantic candidates in the
                Relationships view.
              </div>

              /* Edge selected */
            ) : selectedEdge ? (
              <>
                <div
                  className="text-xs
                             uppercase
                             tracking-wide
                             text-slate-500"
                >
                  Relationship
                </div>

                <h2
                  className="mt-2
                             font-medium"
                >
                  {
                    selectedEdge.relationshipType
                  }
                </h2>

                <div className="mt-5">
                  <div
                    className="text-xs
                               uppercase
                               tracking-wide
                               text-slate-500"
                  >
                    Direction
                  </div>

                  <div
                    className="mt-2
                               text-sm
                               text-slate-300"
                  >
                    <span className="text-white">
                      {getDocumentName(
                        selectedEdge.source
                      )}
                    </span>

                    <div
                      className="my-2
                                 text-xs
                                 text-slate-600"
                    >
                      ↓{" "}
                      {
                        selectedEdge.relationshipType
                      }
                    </div>

                    <span className="text-white">
                      {getDocumentName(
                        selectedEdge.target
                      )}
                    </span>
                  </div>
                </div>

                <div className="mt-6">
                  <div
                    className="text-xs
                               uppercase
                               tracking-wide
                               text-slate-500"
                  >
                    Provenance
                  </div>

                  <div
                    className={`mt-2
                      inline-flex
                      rounded-md
                      border px-2.5 py-1
                      text-xs font-medium
                      ${selectedEdge.provenance ===
                        "INFERRED"
                        ? "border-amber-800 bg-amber-950 text-amber-300"
                        : "border-slate-700 bg-slate-900 text-slate-300"
                      }`}
                  >
                    {
                      selectedEdge.provenance
                    }
                  </div>
                </div>

                <div className="mt-6">
                  <div
                    className="text-xs
                               uppercase
                               tracking-wide
                               text-slate-500"
                  >
                    Evidence
                  </div>

                  <div
                    className="mt-2
                               text-sm
                               leading-6
                               text-slate-400"
                  >
                    {
                      selectedEdge.evidence ??
                      "No evidence recorded."
                    }
                  </div>
                </div>
              </>

              /* Node selected */
            ) : selectedNode ? (
              <>
                <div
                  className="text-xs
                             uppercase
                             tracking-wide
                             text-slate-500"
                >
                  {selectedNode.id ===
                    graphData?.root.id
                    ? "Root document"
                    : "Affected document"}
                </div>

                <h2
                  className="mt-2
                             font-medium
                             leading-6"
                >
                  {selectedNode.name}
                </h2>

                <div
                  className="mt-5
                             grid
                             grid-cols-2
                             gap-3"
                >

                  <div
                    className="rounded-lg
                               border
                               border-slate-800
                               bg-slate-900
                               p-3"
                  >
                    <div
                      className="text-xs
                                 text-slate-500"
                    >
                      Impact
                    </div>

                    <div
                      className="mt-1
                                 font-semibold"
                    >
                      {
                        selectedNode.impactLevel
                      }
                    </div>
                  </div>

                  <div
                    className="rounded-lg
                               border
                               border-slate-800
                               bg-slate-900
                               p-3"
                  >
                    <div
                      className="text-xs
                                 text-slate-500"
                    >
                      Depth
                    </div>

                    <div
                      className="mt-1
                                 font-semibold"
                    >
                      {selectedNode.depth}
                    </div>
                  </div>

                  <div
                    className="rounded-lg
                               border
                               border-slate-800
                               bg-slate-900
                               p-3"
                  >
                    <div
                      className="text-xs
                                 text-slate-500"
                    >
                      Priority
                    </div>

                    <div
                      className="mt-1
                                 font-semibold"
                    >
                      {selectedNode.impactScore ===
                        null
                        ? "—"
                        : selectedNode.impactScore.toFixed(
                          3
                        )}
                    </div>
                  </div>

                  <div
                    className="rounded-lg
                               border
                               border-slate-800
                               bg-slate-900
                               p-3"
                  >
                    <div
                      className="text-xs
                                 text-slate-500"
                    >
                      Provenance
                    </div>

                    <div
                      className="mt-1
                                 font-semibold
                                 text-xs"
                    >
                      {
                        selectedNode.provenance
                      }
                    </div>
                  </div>

                </div>

                {selectedNode.id !==
                  graphData?.root.id && (
                    <>
                      <div className="mt-6">
                        <div
                          className="text-xs
                                   uppercase
                                   tracking-wide
                                   text-slate-500"
                        >
                          Impact
                        </div>

                        <div
                          className="mt-2
                                   text-sm
                                   leading-6
                                   text-slate-400"
                        >
                          This document is affected
                          by changes to{" "}
                          <span className="text-white">
                            {
                              selectedDocument?.name
                            }
                          </span>
                          .
                        </div>
                      </div>

                      {/* AI impact explanation */}
                      <div className="mt-6">

                        <button
                          disabled={
                            generatingExplanation ||
                            !selectedVersion?.changeEventId
                          }
                          onClick={
                            handleGenerateExplanation
                          }
                          className="w-full
                                   flex
                                   items-center
                                   justify-center
                                   gap-2
                                   rounded-md
                                   bg-slate-100
                                   px-4 py-2
                                   text-sm
                                   font-medium
                                   text-slate-900
                                   hover:bg-white
                                   disabled:opacity-50"
                        >
                          {generatingExplanation ? (
                            <>
                              <LoaderCircle
                                size={15}
                                className="animate-spin"
                              />
                              Generating explanation...
                            </>
                          ) : (
                            <>
                              <Sparkles size={15} />
                              Explain with AI
                            </>
                          )}
                        </button>

                        {aiExplanation && (
                          <div
                            className="mt-3
                                     rounded-lg
                                     border
                                     border-slate-800
                                     bg-slate-900
                                     p-4"
                          >
                            <div
                              className="flex
                                       items-center
                                       gap-2"
                            >
                              <Sparkles
                                size={14}
                                className="text-slate-400"
                              />

                              <div
                                className="text-xs
                                         uppercase
                                         tracking-wide
                                         text-slate-500"
                              >
                                AI explanation
                              </div>
                            </div>

                            <div
                              className="mt-3
                                       text-sm
                                       leading-6
                                       text-slate-300
                                       whitespace-pre-wrap"
                            >
                              {aiExplanation}
                            </div>

                            <div
                              className="mt-3
                                       text-[10px]
                                       leading-4
                                       text-slate-600"
                            >
                              Generated from the recorded
                              change event and impact graph.
                            </div>
                          </div>
                        )}

                      </div>
                    </>
                  )}
              </>

            ) : (

              <div
                className="text-sm
                           leading-6
                           text-slate-500"
              >
                Select a document or relationship
                in the graph to inspect its details.
              </div>

            )}

          </div>
        </aside>
      </main>

      {/* Error toast */}
      {error && (
        <div
          className="fixed
                     bottom-4
                     left-1/2
                     -translate-x-1/2
                     max-w-lg
                     rounded-md
                     border
                     border-red-900
                     bg-red-950
                     px-4 py-2
                     text-sm
                     text-red-300
                     shadow-lg"
        >
          {error}
        </div>
      )}
    </div>
  )
}

export default App