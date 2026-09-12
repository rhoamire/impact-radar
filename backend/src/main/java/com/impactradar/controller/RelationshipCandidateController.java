package com.impactradar.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.impactradar.dto.RelationshipCandidateResult;
import com.impactradar.service.RelationshipCandidateService;

@RestController
@RequestMapping("/api/semantic-candidates")
public class RelationshipCandidateController {

    private final RelationshipCandidateService candidateService;

    public RelationshipCandidateController(
            RelationshipCandidateService candidateService
    ) {
        this.candidateService = candidateService;
    }

    @PostMapping("/{fileId}")
    public List<RelationshipCandidateResult> discover(
            @PathVariable UUID fileId
    ) {
        return candidateService.discoverCandidates(fileId);
    }
}