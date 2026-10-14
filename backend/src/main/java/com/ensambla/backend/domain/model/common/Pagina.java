package com.ensambla.backend.domain.model.common;

import java.util.List;
import java.util.function.Function;

/** Una página de resultados; {@code numero} arranca en 0. */
public record Pagina<T>(List<T> contenido, int numero, int tamanio, long totalElementos) {

    public <R> Pagina<R> map(Function<T, R> transformacion) {
        return new Pagina<>(contenido.stream().map(transformacion).toList(), numero, tamanio, totalElementos);
    }

    public int totalPaginas() {
        return Math.max(1, (int) Math.ceil((double) totalElementos / tamanio));
    }
}
