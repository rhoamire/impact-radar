package com.impactradar.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.impactradar.dto.RelationshipCandidateResponse;
import com.impactradar.repository.RelationshipCandidateRepository;

@RestController
@RequestMapping("/api")
public class RelationshipCandidateController {

    private final RelationshipCandidateRepository repository;

    public RelationshipCandidateController(
            RelationshipCandidateRepository repository) {
        this.repository = repository;
    }

    @GetMapping(
        "/files/{fileId}/candidates"
    )
    public List<RelationshipCandidateResponse>
    getCandidates(
            @PathVariable UUID fileId) {

        return repository.findByFile(fileId);
    }

    @PostMapping("/candidates/{candidateId}/accept")
public void accept(@PathVariable UUID candidateId) {
    try {
        repository.accept(candidateId);
    } catch (DataAccessException ex) {
        throw new ResponseStatusException(
            HttpStatus.CONFLICT,
            "Relationship candidate cannot be accepted in its current state.",
            ex
        );
    }
}

    @PostMapping("/candidates/{candidateId}/reject")
public void reject(@PathVariable UUID candidateId) {
    try {
        repository.reject(candidateId);
    } catch (DataAccessException ex) {
        throw new ResponseStatusException(
            HttpStatus.CONFLICT,
            "Relationship candidate cannot be rejected in its current state.",
            ex
        );
    }
}

@PostMapping("/candidates/{candidateId}/undo")
public void undo(@PathVariable UUID candidateId) {
    try {
        repository.undo(candidateId);
    } catch (DataAccessException ex) {
        throw new ResponseStatusException(
            HttpStatus.CONFLICT,
            "Relationship candidate cannot be undone in its current state.",
            ex
        );
    }
}
}