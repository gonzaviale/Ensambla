package com.ensambla.backend.infrastructure.presentation.dto.inventario;

import java.util.List;

/** Mismas claves que {@code pageSchema} del frontend. */
public record PaginaStockResponse(
        List<StockResponse> content,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
}
