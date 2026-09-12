package com.impactradar.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class EmbeddingService {

    private final RestClient restClient;

    public EmbeddingService() {
        this.restClient = RestClient.builder()
                .baseUrl("http://localhost:11434")
                .build();
    }

    public List<Float> embed(String text) {
        EmbedRequest request = new EmbedRequest(
                "all-minilm",
                text
        );

        EmbedResponse response = restClient
                .post()
                .uri("/api/embed")
                .body(request)
                .retrieve()
                .body(EmbedResponse.class);

        if (response == null
                || response.embeddings() == null
                || response.embeddings().isEmpty()) {
            throw new IllegalStateException(
                    "Ollama returned no embedding"
            );
        }

        return response.embeddings().get(0);
    }

    private record EmbedRequest(
            String model,
            String input
    ) {
    }

    private record EmbedResponse(
            String model,
            List<List<Float>> embeddings
    ) {
    }
}