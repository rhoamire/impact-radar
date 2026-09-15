package com.impactradar.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record GraphNode(
    UUID id,
    String name,
    int depth,
    BigDecimal impactScore,
    String impactLevel,
    String provenance
) {}