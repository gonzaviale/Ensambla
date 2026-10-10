package com.ensambla.backend.domain.model.procesamiento;

import com.ensambla.backend.domain.model.lectura.ConsumoIa;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;

public record PoliticaProcesamiento(int maxIntentos, int maxPaginas, int limiteDiario,
                                     Duration vigenciaReclamo, Duration demoraReintento,
                                     BigDecimal precioEntradaMillon, BigDecimal precioSalidaMillon) {

    public BigDecimal costo(ConsumoIa consumo) {
        if ("simulado".equals(consumo.proveedor())) {
            return BigDecimal.ZERO;
        }
        if (precioEntradaMillon == null || precioSalidaMillon == null) {
            return null;
        }
        return precioEntradaMillon.multiply(BigDecimal.valueOf(consumo.tokensEntrada()))
                .add(precioSalidaMillon.multiply(BigDecimal.valueOf(consumo.tokensSalida())))
                .divide(BigDecimal.valueOf(1_000_000), 6, RoundingMode.HALF_UP);
    }
}
