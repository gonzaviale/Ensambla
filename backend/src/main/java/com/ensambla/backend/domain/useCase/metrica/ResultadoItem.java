package com.ensambla.backend.domain.useCase.metrica;

import com.ensambla.backend.domain.model.metrica.DeDondeSale;
import com.ensambla.backend.domain.model.metrica.FormatoMetrica;
import com.ensambla.backend.domain.model.metrica.FuenteMetrica;
import com.ensambla.backend.domain.model.metrica.ResultadoMetrica;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Lo que da una métrica, listo para mostrar: el número, su reparto y de dónde sale. Si no se pudo calcular,
 * {@code valor} es {@code null} y {@code motivo} dice por qué.
 */
public record ResultadoItem(
        FormatoMetrica formato, BigDecimal valor, List<FilaItem> filas, String motivo, DeDondeSaleItem deDondeSale) {

    public static ResultadoItem de(ResultadoMetrica resultado) {
        return new ResultadoItem(
                resultado.formato(),
                resultado.valor(),
                resultado.filas().stream().map(fila -> new FilaItem(fila.etiqueta(), fila.valor())).toList(),
                resultado.motivo(),
                DeDondeSaleItem.de(resultado.deDondeSale()));
    }

    public record FilaItem(String etiqueta, BigDecimal valor) {
    }

    public record DeDondeSaleItem(
            FuenteMetrica fuente, long filasUsadas, long filasSinDato, LocalDate desde, LocalDate hasta) {

        static DeDondeSaleItem de(DeDondeSale deDondeSale) {
            return new DeDondeSaleItem(deDondeSale.fuente(), deDondeSale.filasUsadas(), deDondeSale.filasSinDato(),
                    deDondeSale.desde(), deDondeSale.hasta());
        }
    }
}
