package com.ensambla.backend.infrastructure.presentation.dto.reporte;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PanelNegocioResponse(
        LocalDate desde,
        LocalDate hasta,
        long dias,
        boolean hayDatos,
        Metricas metricas,
        List<Semana> flujoDeCaja) {

    public record Metricas(
            BigDecimal vendiste,
            BigDecimal mercaderia,
            BigDecimal gastos,
            BigDecimal queda,
            BigDecimal teDeben,
            BigDecimal margen,
            BigDecimal ticketPromedio,
            BigDecimal variacion) {
    }

    public record Semana(
            LocalDate desde,
            LocalDate hasta,
            BigDecimal entradas,
            BigDecimal salidas,
            BigDecimal saldo,
            BigDecimal saldoAcumulado) {
    }
}
