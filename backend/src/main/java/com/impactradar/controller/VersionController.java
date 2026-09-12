package com.impactradar.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.impactradar.dto.CreateVersionRequest;
import com.impactradar.dto.CreateVersionResponse;
import com.impactradar.service.VersionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/files")
public class VersionController {

    private final VersionService versionService;

    public VersionController(VersionService versionService) {
        this.versionService = versionService;
    }

    @PostMapping("/{fileId}/versions")
    @ResponseStatus(HttpStatus.CREATED)
    public CreateVersionResponse createVersion(
            @PathVariable UUID fileId,
            @Valid @RequestBody CreateVersionRequest request
    ) {
        return versionService.createVersion(
                fileId,
                request.content()
        );
    }
}