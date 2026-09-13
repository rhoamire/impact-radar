package com.impactradar.mcp;

import java.util.List;
import java.util.UUID;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.stereotype.Component;

import com.impactradar.dto.ImpactPredictionResponse;
import com.impactradar.repository.ChangeEventRepository;
import com.impactradar.service.ChangeSummaryService;
import com.impactradar.service.ImpactRepository;
import com.impactradar.service.RelationshipExplanationService;

@Component
public class ImpactRadarTools {

    private final ImpactRepository impactRepository;
    private final RelationshipExplanationService relationshipExplanationService;
    private final ChangeSummaryService changeSummaryService;
    private final ChangeEventRepository changeEventRepository;

    public ImpactRadarTools(
        ImpactRepository impactRepository,
        RelationshipExplanationService relationshipExplanationService,
        ChangeSummaryService changeSummaryService,
        ChangeEventRepository changeEventRepository
) {
    this.impactRepository = impactRepository;
    this.relationshipExplanationService = relationshipExplanationService;
    this.changeSummaryService = changeSummaryService;
    this.changeEventRepository = changeEventRepository;
}
    @McpTool(
            name = "analyze_impact",
            description = "Analyze the downstream documents affected by a change to a technical document.",
            annotations = @McpTool.McpAnnotations(
                    readOnlyHint = true,
                    destructiveHint = false
            )
    )
    public List<ImpactPredictionResponse> analyzeImpact(UUID fileId) {
        return impactRepository.analyze(fileId);
    }

    @McpTool(
            name = "explain_relationship",
            description = "Explain the authoritative relationship between two technical documents.",
            annotations = @McpTool.McpAnnotations(
                    readOnlyHint = true,
                    destructiveHint = false
            )
    )
    public RelationshipExplanationService.RelationshipExplanation explainRelationship(
            UUID fileA,
            UUID fileB
    ) {
        return relationshipExplanationService.explain(fileA, fileB);
    }
    @McpTool(
        name = "summarize_change",
        description = "Generate a concise technical summary of a document version change.",
        annotations = @McpTool.McpAnnotations(
                readOnlyHint = false,
                destructiveHint = false
        )
)
      public String summarizeChange(UUID versionId) {
          UUID changeEventId =
                  changeEventRepository.findChangeEventIdByVersion(versionId);

          return changeSummaryService.summarize(changeEventId);
      }
}