package com.impactradar.service;

public interface GenerationService {

    String generate(
            String systemPrompt,
            String prompt
    );

    String model();
}