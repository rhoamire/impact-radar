package com.impactradar.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.impactradar.dto.SemanticEvaluationResult;
import com.impactradar.service.SemanticEvaluationService;

@RestController
@RequestMapping("/api/semantic-evaluation")
public class SemanticEvaluationController {

    private final SemanticEvaluationService evaluationService;

    public SemanticEvaluationController(
            SemanticEvaluationService evaluationService
    ) {
        this.evaluationService = evaluationService;
    }

    @GetMapping
    public SemanticEvaluationResult evaluate() {
        return evaluationService.evaluate();
    }
}