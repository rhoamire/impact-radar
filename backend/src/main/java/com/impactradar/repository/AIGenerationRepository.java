package com.impactradar.repository;

import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class AIGenerationRepository {

    private final JdbcClient jdbcClient;

    public AIGenerationRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public void save(
            String generationType,
            UUID changeEventId,
            UUID affectedFileId,
            String modelName,
            String sourceContext,
            String generatedText
    ) {
        jdbcClient
                .sql("""
                        INSERT INTO ai_generations (
                            generation_type,
                            change_event_id,
                            affected_file_id,
                            model_name,
                            source_context,
                            generated_text
                        )
                        VALUES (
                            :generationType,
                            :changeEventId,
                            :affectedFileId,
                            :modelName,
                            :sourceContext,
                            :generatedText
                        )
                        """)
                .param("generationType", generationType)
                .param("changeEventId", changeEventId)
                .param("affectedFileId", affectedFileId)
                .param("modelName", modelName)
                .param("sourceContext", sourceContext)
                .param("generatedText", generatedText)
                .update();
    }
}