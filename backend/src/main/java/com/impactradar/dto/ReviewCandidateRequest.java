package com.impactradar.dto;

import jakarta.validation.constraints.NotBlank;

public record ReviewCandidateRequest(
        @NotBlank
        String status
) {
}