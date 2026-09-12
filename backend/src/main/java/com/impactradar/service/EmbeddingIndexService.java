package com.impactradar.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.springframework.stereotype.Service;

import com.impactradar.model.FileVersion;
import com.impactradar.repository.DocumentRepository;
import com.impactradar.repository.EmbeddingRepository;

@Service
public class EmbeddingIndexService {

    private final EmbeddingService embeddingService;
    private final EmbeddingRepository embeddingRepository;
    private final DocumentRepository documentRepository;

    public EmbeddingIndexService(
            EmbeddingService embeddingService,
            EmbeddingRepository embeddingRepository,
            DocumentRepository documentRepository
    ) {
        this.embeddingService = embeddingService;
        this.embeddingRepository = embeddingRepository;
        this.documentRepository = documentRepository;
    }

    public void indexCurrentVersion(
            java.util.UUID fileId
    ) {
        FileVersion version = documentRepository
                .findCurrentVersion(fileId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "No current version found for file: " + fileId
                        )
                );

        String content = version.content();

        var embedding = embeddingService.embed(content);

        if (embedding.size() != 384) {
            throw new IllegalStateException(
                    "Expected 384-dimensional embedding, got "
                            + embedding.size()
            );
        }

        embeddingRepository.saveEmbedding(
                version.id(),
                0,
                content,
                sha256(content),
                embedding
        );
    }

    private String sha256(String content) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash =
                    digest.digest(
                            content.getBytes(StandardCharsets.UTF_8)
                    );

            StringBuilder hex = new StringBuilder();

            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }

            return hex.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "SHA-256 algorithm is unavailable",
                    e
            );
        }
    }
}