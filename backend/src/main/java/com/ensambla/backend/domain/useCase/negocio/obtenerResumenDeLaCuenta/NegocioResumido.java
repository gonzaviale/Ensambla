package com.ensambla.backend.domain.useCase.negocio.obtenerResumenDeLaCuenta;

import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.negocio.RubroNegocio;
import com.ensambla.backend.domain.model.reporte.PanelNegocio;
import com.ensambla.backend.domain.model.reporte.PanelNegocio.Metricas;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Un negocio en la vista conjunta, con las métricas del panel de la home ({@code docs/panel-negocio.md}) sumadas sobre
 * todos sus sistemas. Sin ventas en el período {@code hayDatos} es {@code false} y las cuatro métricas van en
 * {@code null}: no se manda un resumen de ceros. Una métrica que ningún sistema puede calcular también va en
 * {@code null}.
 */
public record NegocioResumido(
        NegocioId id,
        String nombre,
        RubroNegocio rubro,
        boolean activo,
        boolean hayDatos,
        BigDecimal vendiste,
        BigDecimal gastos,
        BigDecimal queda,
        BigDecimal teDeben,
        long avisosPendientes) {

    public static NegocioResumido de(
            Negocio negocio, boolean activo, List<PanelNegocio> paneles, long avisosPendientes) {
        List<Metricas> metricas = paneles.stream()
                .filter(panel -> !panel.vacio())
                .map(PanelNegocio::metricas)
                .toList();
        return new NegocioResumido(
                negocio.getId(),
                negocio.getNombre(),
                negocio.getRubro(),
                activo,
                !metricas.isEmpty(),
                suma(metricas, Metricas::vendiste),
                suma(metricas, Metricas::gastos),
                suma(metricas, Metricas::queda),
                suma(metricas, Metricas::teDeben),
                avisosPendientes);
    }

    /** Lo que suman los sistemas que tienen la métrica; {@code null} si ninguno la tiene. */
    private static BigDecimal suma(List<Metricas> metricas, Function<Metricas, BigDecimal> metrica) {
        return metricas.stream().map(metrica).filter(Objects::nonNull).reduce(BigDecimal::add).orElse(null);
    }
}
