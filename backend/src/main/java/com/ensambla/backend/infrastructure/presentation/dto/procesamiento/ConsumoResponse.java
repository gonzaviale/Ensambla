package com.ensambla.backend.infrastructure.presentation.dto.procesamiento;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ConsumoResponse(LocalDate fecha, int limite, int paginas, int reservadas, int disponibles,
                              long tokensEntrada, long tokensSalida, BigDecimal costoEstimadoUsd,
                              boolean consumoCompleto) {
}
