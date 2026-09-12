package com.impactradar.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.impactradar.dto.SemanticSearchResult;
import com.impactradar.repository.SemanticSearchRepository;

@Service
public class SemanticSearchService {

    private final SemanticSearchRepository repository;

    public SemanticSearchService(
            SemanticSearchRepository repository
    ) {
        this.repository = repository;
    }

    public List<SemanticSearchResult> search(
            UUID fileId,
            int limit
    ) {
        if (limit < 1 || limit > 20) {
            throw new IllegalArgumentException(
                    "Limit must be between 1 and 20"
            );
        }

        return repository.findSimilarDocuments(
                fileId,
                limit
        );
    }
}

