package com.ensambla.backend.infrastructure.presentation.dto.registro;

import java.util.List;

/** Mismas claves que {@code pageSchema} del frontend. */
public record PaginaRegistrosResponse(
        List<RegistroResponse> content,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
}
