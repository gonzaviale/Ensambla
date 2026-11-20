package com.ensambla.backend.domain.useCase.reporte.obtenerPanelNegocio;

import com.ensambla.backend.domain.model.reporte.PanelNegocio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * El panel de la home. Sin ventas en el período {@code hayDatos} es {@code false}, {@code metricas} es {@code null} y
 * el flujo de caja viene vacío.
 */
public record ObtenerPanelNegocioResponse(
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

        static Metricas de(PanelNegocio.Metricas metricas) {
            return new Metricas(
                    metricas.vendiste(),
                    metricas.mercaderia(),
                    metricas.gastos(),
                    metricas.queda(),
                    metricas.teDeben(),
                    metricas.margen(),
                    metricas.ticketPromedio(),
                    metricas.variacion());
        }
    }

    public record Semana(
            LocalDate desde,
            LocalDate hasta,
            BigDecimal entradas,
            BigDecimal salidas,
            BigDecimal saldo,
            BigDecimal saldoAcumulado) {

        static Semana de(PanelNegocio.SemanaDeCaja semana) {
            return new Semana(
                    semana.desde(),
                    semana.hasta(),
                    semana.entradas(),
                    semana.salidas(),
                    semana.saldo(),
                    semana.saldoAcumulado());
        }
    }

    public static ObtenerPanelNegocioResponse de(PanelNegocio panel) {
        return new ObtenerPanelNegocioResponse(
                panel.periodo().desde(),
                panel.periodo().hasta(),
                panel.periodo().dias(),
                !panel.vacio(),
                panel.vacio() ? null : Metricas.de(panel.metricas()),
                panel.flujoDeCaja().stream().map(Semana::de).toList());
    }
}
