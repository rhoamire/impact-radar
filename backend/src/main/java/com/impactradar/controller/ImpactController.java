package com.impactradar.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.impactradar.dto.ImpactPredictionResponse;
import com.impactradar.service.ImpactRepository;

@RestController
@RequestMapping("/api/files")
public class ImpactController {

    private final ImpactRepository impactRepository;

    public ImpactController(ImpactRepository impactRepository) {
        this.impactRepository = impactRepository;
    }

    @GetMapping("/{fileId}/impact")
    public List<ImpactPredictionResponse> analyzeImpact(
            @PathVariable UUID fileId) {

        return impactRepository.analyze(fileId);
    }
}