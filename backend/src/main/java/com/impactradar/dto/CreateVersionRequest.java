package com.impactradar.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateVersionRequest(
        @NotBlank(message = "Document content must not be blank")
        String content
) {
}