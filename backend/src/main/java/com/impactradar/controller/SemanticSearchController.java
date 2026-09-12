package com.impactradar.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.impactradar.dto.SemanticSearchResult;
import com.impactradar.service.SemanticSearchService;

@RestController
@RequestMapping("/api/semantic-search")
public class SemanticSearchController {

    private final SemanticSearchService semanticSearchService;

    public SemanticSearchController(
            SemanticSearchService semanticSearchService
    ) {
        this.semanticSearchService = semanticSearchService;
    }

    @GetMapping("/{fileId}")
    public List<SemanticSearchResult> search(
            @PathVariable UUID fileId,
            @RequestParam(defaultValue = "5") int limit
    ) {
        return semanticSearchService.search(
                fileId,
                limit
        );
    }
}