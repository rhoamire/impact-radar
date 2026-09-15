package com.impactradar.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.impactradar.dto.ImpactGraphResponse;
import com.impactradar.service.ImpactRepository;

@RestController
@RequestMapping("/api/files")
public class GraphController {

    private final ImpactRepository impactRepository;

    public GraphController(ImpactRepository impactRepository) {
        this.impactRepository = impactRepository;
    }

    @GetMapping("/{fileId}/graph")
    public ImpactGraphResponse getGraph(
            @PathVariable UUID fileId) {

        return impactRepository.getGraph(fileId);
    }
}