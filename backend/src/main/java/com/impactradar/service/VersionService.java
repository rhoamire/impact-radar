package com.impactradar.service;

import com.impactradar.dto.CreateVersionResponse;
import com.impactradar.dto.ImpactPredictionResponse;
import com.impactradar.model.FileVersion;
import com.impactradar.repository.DocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class VersionService {

    private final DocumentRepository documentRepository;
    private final TextDiffService textDiffService;
    private final ImpactRepository impactRepository;

    public VersionService(
            DocumentRepository documentRepository,
            TextDiffService textDiffService,
            ImpactRepository impactRepository
    ) {
        this.documentRepository = documentRepository;
        this.textDiffService = textDiffService;
        this.impactRepository = impactRepository;
    }

    @Transactional
    public CreateVersionResponse createVersion(
            UUID fileId,
            String newContent
    ) {
        FileVersion previousVersion = documentRepository
                .findCurrentVersion(fileId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                NOT_FOUND,
                                "No current version found for file: " + fileId
                        )
                );

        TextDiffService.DiffResult diff =
                textDiffService.diff(previousVersion.content(), newContent);

        if (diff.linesAdded() == 0 && diff.linesRemoved() == 0) {
            throw new IllegalArgumentException(
                    "New content is identical to the current version"
            );
        }

        int nextVersionNumber =
                previousVersion.versionNumber() + 1;

        String contentHash = sha256(newContent);

        documentRepository.markNotCurrent(previousVersion.id());

        FileVersion newVersion =
                documentRepository.insertVersion(
                        fileId,
                        nextVersionNumber,
                        newContent,
                        contentHash
                );

        String summary =
                String.format(
                        "Version %d created from version %d: %d lines added, %d lines removed.",
                        nextVersionNumber,
                        previousVersion.versionNumber(),
                        diff.linesAdded(),
                        diff.linesRemoved()
                );

        UUID changeEventId =
                documentRepository.recordChangeEvent(
                        fileId,
                        previousVersion.id(),
                        newVersion.id(),
                        summary,
                        diff.text(),
                        diff.linesAdded(),
                        diff.linesRemoved()
                );

        List<ImpactPredictionResponse> impact =
                impactRepository.analyzeAndStore(
                        changeEventId,
                        fileId
                );

        return new CreateVersionResponse(
                newVersion.id(),
                newVersion.versionNumber(),
                changeEventId,
                diff.text(),
                diff.linesAdded(),
                diff.linesRemoved(),
                impact
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