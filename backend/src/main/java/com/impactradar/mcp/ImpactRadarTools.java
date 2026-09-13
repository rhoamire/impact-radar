package com.impactradar.mcp;

import java.util.List;
import java.util.UUID;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.stereotype.Component;

import com.impactradar.dto.ImpactPredictionResponse;
import com.impactradar.service.ImpactRepository;

@Component
public class ImpactRadarTools {

    private final ImpactRepository impactRepository;

    public ImpactRadarTools(ImpactRepository impactRepository) {
        this.impactRepository = impactRepository;
    }

    @McpTool(
        name = "analyze_impact",
        description = "Analyze the downstream documents affected by a change to a technical document."
    )
    public List<ImpactPredictionResponse> analyzeImpact(UUID fileId) {
        return impactRepository.analyze(fileId);
    }
}