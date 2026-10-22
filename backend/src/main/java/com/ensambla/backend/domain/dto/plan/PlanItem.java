package com.ensambla.backend.domain.dto.plan;

import java.math.BigDecimal;
import java.util.List;

public record PlanItem(String codigo, String nombre, boolean precioConfigurado, boolean limitesConfigurados,
                       BigDecimal precioMensual,
                       String moneda, Integer limiteProyectos, Long limiteRegistros,
                       Integer limitePaginasIaMensuales, List<String> funciones) {
}
