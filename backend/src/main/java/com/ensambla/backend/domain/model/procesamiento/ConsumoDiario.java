package com.ensambla.backend.domain.model.procesamiento;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ConsumoDiario(LocalDate fecha, int paginas, int reservadas, long tokensEntrada,
                            long tokensSalida, BigDecimal costoEstimadoUsd, boolean consumoCompleto) {
}
