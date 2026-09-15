package com.impactradar.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.impactradar.dto.ChangeDetailResponse;
import com.impactradar.dto.VersionSummaryResponse;
import com.impactradar.repository.VersionHistoryRepository;

@RestController
public class VersionHistoryController {

    private final VersionHistoryRepository repository;

    public VersionHistoryController(
            VersionHistoryRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/api/files/{fileId}/versions")
    public List<VersionSummaryResponse> getVersions(
            @PathVariable UUID fileId) {

        return repository.findVersions(fileId);
    }

    @GetMapping("/api/versions/{versionId}/change")
    public ChangeDetailResponse getChange(
            @PathVariable UUID versionId) {

        return repository.findChange(versionId);
    }
}