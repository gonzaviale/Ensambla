package com.ensambla.backend.domain.dto.inventario;

import java.util.List;

public record ConsultarStockResponse(
        List<StockItem> contenido,
        int pagina,
        int tamanio,
        long totalElementos,
        int totalPaginas) {
}
