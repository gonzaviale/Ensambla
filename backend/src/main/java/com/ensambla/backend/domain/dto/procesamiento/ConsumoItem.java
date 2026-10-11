package com.ensambla.backend.domain.dto.procesamiento;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ConsumoItem(LocalDate fecha, int limite, int paginas, int reservadas, int disponibles,
                          long tokensEntrada, long tokensSalida, BigDecimal costoEstimadoUsd,
                          boolean consumoCompleto) {
}
