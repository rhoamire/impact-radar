package com.impactradar.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.impactradar.repository.AIGenerationRepository;
import com.impactradar.repository.AIGroundingRepository;
import com.impactradar.repository.AIGroundingRepository.ChangeContext;

@Service
public class ChangeSummaryService {

    private final AIGroundingRepository groundingRepository;
    private final OllamaService ollamaService;
    private final AIGenerationRepository generationRepository;

    public ChangeSummaryService(
            AIGroundingRepository groundingRepository,
            OllamaService ollamaService,
            AIGenerationRepository generationRepository
    ) {
        this.groundingRepository = groundingRepository;
        this.ollamaService = ollamaService;
        this.generationRepository = generationRepository;
    }

    @Transactional
    public String summarize(UUID changeEventId) {

        ChangeContext context =
                groundingRepository.getChangeContext(changeEventId);

        String systemPrompt = """
                You summarize technical document changes.

                Use ONLY the supplied change data.
                Do not invent requirements, implementation details,
                affected systems, or consequences that are not present
                in the supplied context.

                Produce one concise technical paragraph.
                """;

        String prompt = """
                Changed document:
                %s

                Lines added: %d
                Lines removed: %d

                Existing change summary:
                %s

                Diff:
                %s
                """.formatted(
                context.fileName(),
                context.linesAdded(),
                context.linesRemoved(),
                context.changeSummary(),
                context.diff()
        );

        String generated =
                ollamaService.generate(
                        systemPrompt,
                        prompt
                );

        generationRepository.save(
                "CHANGE_SUMMARY",
                changeEventId,
                null,
                ollamaService.model(),
                prompt,
                generated
        );

        return generated;
    }
}