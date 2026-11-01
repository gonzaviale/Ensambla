package com.ensambla.backend.domain.useCase.procesamiento;

import com.ensambla.backend.domain.model.procesamiento.ConsumoDiario;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ConsumoItem(LocalDate fecha, int limite, int paginas, int reservadas, int disponibles,
                          long tokensEntrada, long tokensSalida, BigDecimal costoEstimadoUsd,
                          boolean consumoCompleto) {

    public static ConsumoItem de(ConsumoDiario consumo, int limite) {
        return new ConsumoItem(consumo.fecha(), limite, consumo.paginas(), consumo.reservadas(),
                Math.max(0, limite - consumo.paginas() - consumo.reservadas()),
                consumo.tokensEntrada(), consumo.tokensSalida(), consumo.costoEstimadoUsd(), consumo.consumoCompleto());
    }
}
