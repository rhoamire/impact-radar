package com.impactradar.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SemanticSearchResult(
        UUID fileId,
        String fileName,
        BigDecimal similarity
) {
}