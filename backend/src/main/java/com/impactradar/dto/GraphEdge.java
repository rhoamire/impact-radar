package com.impactradar.dto;

import java.util.UUID;

public record GraphEdge(
    UUID source,
    UUID target,
    String relationshipType,
    String provenance,
    String evidence
) {}