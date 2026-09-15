package com.impactradar.dto;

import java.util.List;

public record ImpactGraphResponse(
    GraphNode root,
    List<GraphNode> nodes,
    List<GraphEdge> edges
) {}