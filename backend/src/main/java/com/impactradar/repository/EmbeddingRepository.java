package com.impactradar.repository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class EmbeddingRepository {

    private final JdbcClient jdbcClient;

    public EmbeddingRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public void saveEmbedding(
            UUID versionId,
            int chunkIndex,
            String content,
            String contentHash,
            List<Float> embedding
    ) {
        String vectorLiteral = embedding.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",", "[", "]"));

        jdbcClient
                .sql("""
                        INSERT INTO document_chunks (
                            file_version_id,
                            chunk_index,
                            content,
                            content_hash,
                            embedding
                        )
                        VALUES (
                            :versionId,
                            :chunkIndex,
                            :content,
                            :contentHash,
                            CAST(:embedding AS vector)
                        )
                        ON CONFLICT (file_version_id, chunk_index)
                        DO UPDATE SET
                            content = EXCLUDED.content,
                            content_hash = EXCLUDED.content_hash,
                            embedding = EXCLUDED.embedding
                        """)
                .param("versionId", versionId)
                .param("chunkIndex", chunkIndex)
                .param("content", content)
                .param("contentHash", contentHash)
                .param("embedding", vectorLiteral)
                .update();
    }
}