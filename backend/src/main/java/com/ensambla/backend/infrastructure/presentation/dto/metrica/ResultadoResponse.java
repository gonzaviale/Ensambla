package com.ensambla.backend.infrastructure.presentation.dto.metrica;

import com.ensambla.backend.domain.model.metrica.FormatoMetrica;
import com.ensambla.backend.domain.model.metrica.FuenteMetrica;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** El número de una métrica; si no se pudo calcular, {@code valor} es {@code null} y {@code motivo} dice por qué. */
public record ResultadoResponse(
        FormatoMetrica formato, BigDecimal valor, List<FilaResponse> filas, String motivo,
        DeDondeSaleResponse deDondeSale) {

    public record FilaResponse(String etiqueta, BigDecimal valor) {
    }

    public record DeDondeSaleResponse(
            FuenteMetrica fuente, long filasUsadas, long filasSinDato, LocalDate desde, LocalDate hasta) {
    }
}
