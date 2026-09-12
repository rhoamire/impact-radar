package com.impactradar.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.impactradar.repository.AIGenerationRepository;
import com.impactradar.repository.AIGroundingRepository;
import com.impactradar.repository.AIGroundingRepository.ImpactContext;

@Service
public class ImpactExplanationService {

    private final AIGroundingRepository groundingRepository;
    private final OllamaService ollamaService;
    private final AIGenerationRepository generationRepository;

    public ImpactExplanationService(
            AIGroundingRepository groundingRepository,
            OllamaService ollamaService,
            AIGenerationRepository generationRepository
    ) {
        this.groundingRepository = groundingRepository;
        this.ollamaService = ollamaService;
        this.generationRepository = generationRepository;
    }

    @Transactional
    public String explain(
            UUID changeEventId,
            UUID affectedFileId
    ) {
        ImpactContext context =
                groundingRepository.getImpactContext(
                        changeEventId,
                        affectedFileId
                );

        String systemPrompt = """
                You explain why a technical document was flagged
                as impacted by a document change.

                Use ONLY the supplied structured evidence.
                Do not invent dependencies, scores, requirements,
                or technical behavior.

                Explain:
                1. what changed,
                2. how the relationship path connects the documents,
                3. why the affected document should be reviewed.

                Clearly distinguish facts from inference.
                Keep the response concise and technical.
                """;

        String prompt = """
                Changed document:
                %s

                Affected document:
                %s

                Impact level:
                %s

                Impact score:
                %s

                Impact depth:
                %d

                Number of paths:
                %d

                Minimum path confidence:
                %s

                Relationship path:
                %s

                Relationship types:
                %s

                Direct relationship confidence:
                %s

                Relationship evidence:
                %s
                """.formatted(
                context.changedFileName(),
                context.affectedFileName(),
                context.impactLevel(),
                context.impactScore(),
                context.impactDepth(),
                context.pathCount(),
                context.minConfidence(),
                context.path(),
                context.relationshipPath(),
                context.relationshipConfidence(),
                context.evidence()
        );

        String generated =
                ollamaService.generate(
                        systemPrompt,
                        prompt
                );

        generationRepository.save(
                "IMPACT_EXPLANATION",
                changeEventId,
                affectedFileId,
                ollamaService.model(),
                prompt,
                generated
        );

        return generated;
    }
}