package com.impactradar.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.impactradar.service.ChangeSummaryService;
import com.impactradar.service.ImpactExplanationService;

@RestController
@RequestMapping("/api/genai")
public class GenAIController {

    private final ChangeSummaryService changeSummaryService;
    private final ImpactExplanationService impactExplanationService;

    public GenAIController(
            ChangeSummaryService changeSummaryService,
            ImpactExplanationService impactExplanationService
    ) {
        this.changeSummaryService = changeSummaryService;
        this.impactExplanationService = impactExplanationService;
    }

    @PostMapping("/changes/{changeEventId}/summary")
    public String summarizeChange(
            @PathVariable UUID changeEventId
    ) {
        return changeSummaryService.summarize(changeEventId);
    }

    @PostMapping(
            "/changes/{changeEventId}/impact/{affectedFileId}/explanation"
    )
    public String explainImpact(
            @PathVariable UUID changeEventId,
            @PathVariable UUID affectedFileId
    ) {
        return impactExplanationService.explain(
                changeEventId,
                affectedFileId
        );
    }
}