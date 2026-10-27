package com.ensambla.backend.infrastructure.presentation.dto.plan;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record PlanesResponse(OffsetDateTime consultadoEn, List<PlanResponse> planes,
                             SuscripcionResponse suscripcion, UsoResponse uso, PruebaResponse prueba) {

    public record PlanResponse(String codigo, String nombre, boolean precioConfigurado, boolean limitesConfigurados,
                                BigDecimal precioMensual,
                                String moneda, Integer limiteProyectos, Long limiteRegistros,
                                Integer limitePaginasIaMensuales, List<String> funciones) {
    }

    public record SuscripcionResponse(String codigoPlanActual, boolean pruebaActiva, boolean puedeIniciarPrueba,
                                      OffsetDateTime inicioEn, OffsetDateTime finEn) {
    }

    public record UsoResponse(long proyectos, long registros, long paginasIa, Integer limitePaginasIa,
                              String periodoIa, OffsetDateTime inicioPeriodoIa, OffsetDateTime finPeriodoIa) {
    }
}
