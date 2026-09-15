package com.impactradar.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class OllamaService implements GenerationService {

    private final RestClient restClient;
    private final String model;

    public OllamaService(
            @Value("${impact-radar.ollama.base-url}") String baseUrl,
            @Value("${impact-radar.ollama.model}") String model
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();

        this.model = model;
    }

    public String generate(
            String systemPrompt,
            String prompt
    ) {
        GenerateRequest request =
                new GenerateRequest(
                        model,
                        prompt,
                        systemPrompt,
                        false
                );

        GenerateResponse response =
                restClient
                        .post()
                        .uri("/api/generate")
                        .body(request)
                        .retrieve()
                        .body(GenerateResponse.class);

        if (response == null
                || response.response() == null
                || response.response().isBlank()) {

            throw new IllegalStateException(
                    "Ollama returned an empty response"
            );
        }

        return response.response().trim();
    }

    public String model() {
        return model;
    }

    private record GenerateRequest(
            String model,
            String prompt,
            String system,
            boolean stream
    ) {
    }

    private record GenerateResponse(
            String model,
            String response,
            boolean done
    ) {
    }
}