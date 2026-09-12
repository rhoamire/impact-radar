package com.impactradar.service;

import org.springframework.stereotype.Service;

import com.impactradar.dto.SemanticEvaluationResult;
import com.impactradar.repository.SemanticEvaluationRepository;

@Service
public class SemanticEvaluationService {

    private final SemanticEvaluationRepository repository;

    public SemanticEvaluationService(
            SemanticEvaluationRepository repository
    ) {
        this.repository = repository;
    }

    public SemanticEvaluationResult evaluate() {
        return repository.evaluate();
    }
}