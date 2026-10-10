package com.ensambla.backend.domain.model.common;

import java.util.List;

/** Una página de resultados; {@code numero} arranca en 0. */
public record Pagina<T>(List<T> contenido, int numero, int tamanio, long totalElementos) {

    public int totalPaginas() {
        return Math.max(1, (int) Math.ceil((double) totalElementos / tamanio));
    }
}
