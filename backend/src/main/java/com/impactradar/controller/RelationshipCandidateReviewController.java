package com.impactradar.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.impactradar.dto.ReviewCandidateRequest;
import com.impactradar.service.RelationshipCandidateReviewService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/semantic-candidates")
public class RelationshipCandidateReviewController {

    private final RelationshipCandidateReviewService reviewService;

    public RelationshipCandidateReviewController(
            RelationshipCandidateReviewService reviewService
    ) {
        this.reviewService = reviewService;
    }

    @PatchMapping("/{candidateId}")
    public ResponseEntity<Void> review(
            @PathVariable UUID candidateId,
            @Valid @RequestBody ReviewCandidateRequest request
    ) {
        reviewService.review(
                candidateId,
                request.status()
        );

        return ResponseEntity.noContent().build();
    }
}