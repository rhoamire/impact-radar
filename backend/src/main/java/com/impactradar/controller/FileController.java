package com.impactradar.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.impactradar.dto.FileResponse;
import com.impactradar.repository.DocumentRepository;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final DocumentRepository documentRepository;

    public FileController(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    @GetMapping
    public List<FileResponse> getFiles() {
        return documentRepository.findAllFiles();
    }
}