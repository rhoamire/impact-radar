package com.impactradar.dto;

import java.util.UUID;

public record FileResponse(
    UUID id,
    String name
) {}