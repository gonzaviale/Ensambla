package com.ensambla.backend.domain.dto.registro;

import java.util.List;

public record ListarRegistrosResponse(
        List<RegistroItem> contenido,
        int pagina,
        int tamanio,
        long totalElementos,
        int totalPaginas) {
}
