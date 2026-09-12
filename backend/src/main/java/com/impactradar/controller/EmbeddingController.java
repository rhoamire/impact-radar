package com.impactradar.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.impactradar.service.EmbeddingIndexService;

@RestController
@RequestMapping("/api/embeddings")
public class EmbeddingController {

    private final EmbeddingIndexService embeddingIndexService;

    public EmbeddingController(
            EmbeddingIndexService embeddingIndexService
    ) {
        this.embeddingIndexService = embeddingIndexService;
    }

    @PostMapping("/{fileId}")
    public ResponseEntity<Void> index(
            @PathVariable UUID fileId
    ) {
        embeddingIndexService.indexCurrentVersion(fileId);
        return ResponseEntity.accepted().build();
    }
}